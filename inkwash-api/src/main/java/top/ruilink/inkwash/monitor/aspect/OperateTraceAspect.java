/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.monitor.aspect;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.base.util.ClientInfoUtil;
import top.ruilink.inkwash.monitor.domain.Journal;
import top.ruilink.inkwash.monitor.service.JournalService;
import top.ruilink.inkwash.monitor.support.AuditLossMetrics;
import top.ruilink.inkwash.security.util.SecurityUtil;

/**
 * Operation trace recording aspect.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Aspect
@Component
@Slf4j
public class OperateTraceAspect {

	private final JsonMapper objectMapper;
	private final JournalService journalService;
	private final AuditLossMetrics auditLossMetrics;

	public OperateTraceAspect(JsonMapper objectMapper, JournalService journalService,
			AuditLossMetrics auditLossMetrics) {
		this.objectMapper = objectMapper;
		this.journalService = journalService;
		this.auditLossMetrics = auditLossMetrics;
	}

	private static final Set<Class<?>> SKIP_PARAM_TYPES = Set.of(HttpServletRequest.class, HttpServletResponse.class,
			BindingResult.class, MultipartFile.class);

	private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "plainPassword", "oldPassword",
			"newPassword", "confirmPassword", "idcode", "smsCode", "captchaCode");

	private static final Pattern SENSITIVE_PATTERN = Pattern
			.compile("\"(%s)\":\"[^\"]*\"".formatted(String.join("|", SENSITIVE_FIELDS)));

	@Around("@within(top.ruilink.inkwash.base.annotation.OperateTrace) "
			+ "|| @annotation(top.ruilink.inkwash.base.annotation.OperateTrace)")
	public Object around(ProceedingJoinPoint point) throws Throwable {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs != null && !isMutation(attrs.getRequest().getMethod())) {
			return point.proceed();
		}
		long start = System.currentTimeMillis();
		Object result;
		Throwable failReason = null;
		try {
			result = point.proceed();
			return result;
		} catch (Throwable e) {
			failReason = e;
			throw e;
		} finally {
			try {
				long duration = System.currentTimeMillis() - start;
				Journal entity = buildLogEntity(point, failReason, duration);
				journalService.save(entity);
			} catch (Exception e) {
				// This catch only covers submission to the async executor; once the task is
				// queued, failures land on the async thread and are counted by
				// JournalServiceImpl (ISS-042). Either way the row is lost, so both stages
				// feed the same counter.
				auditLossMetrics.recordSubmitLoss();
				log.error("提交操作日志失败, method={}", point.getSignature().toShortString(), e);
			}
		}
	}

	private static boolean isMutation(String httpMethod) {
		return !("GET".equalsIgnoreCase(httpMethod) || "HEAD".equalsIgnoreCase(httpMethod)
				|| "OPTIONS".equalsIgnoreCase(httpMethod));
	}

	private Journal buildLogEntity(ProceedingJoinPoint point, Throwable failReason, long duration) {
		Journal entity = new Journal();
		MethodSignature signature = (MethodSignature) point.getSignature();

		try {
			entity.setUserId(SecurityUtil.getCurrentUserId());
		} catch (Exception e) {
			log.warn("获取当前用户ID异常: {}", e.getMessage());
		}

		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs != null) {
			HttpServletRequest request = attrs.getRequest();
			entity.setUrl(request.getRequestURI());
			entity.setIp(ClientInfoUtil.getClientIp(request));
			entity.setOperation(request.getMethod());
		}

		entity.setMethod(signature.getDeclaringType().getSimpleName() + "." + signature.getName());

		OperateTrace operateTrace = resolveOperateTrace(signature);
		if (operateTrace != null && !operateTrace.module().isBlank()) {
			entity.setModule(operateTrace.module());
		}
		try {
			String username = SecurityUtil.getCurrentUsername();
			if (username != null && !username.isBlank()) {
				entity.setUserName(username);
			}
		} catch (Exception e) {
			log.warn("获取当前用户名称异常: {}", e.getMessage());
		}

		Object[] args = point.getArgs();
		if (args != null && args.length > 0) {
			Object[] filteredArgs = Arrays.stream(args)
					.filter(arg -> arg == null || !SKIP_PARAM_TYPES.contains(arg.getClass())).toArray();
			if (filteredArgs.length > 0) {
				try {
					Object[] truncatedArgs = Arrays.stream(filteredArgs)
							.map(arg -> (arg instanceof String s && s.length() > 4000) ? s.substring(0, 4000) : arg)
							.toArray();
					String json = objectMapper
							.writeValueAsString(truncatedArgs.length == 1 ? truncatedArgs[0] : truncatedArgs);
					json = SENSITIVE_PATTERN.matcher(json).replaceAll(m -> "\"%s\":\"***\"".formatted(m.group(1)));
					entity.setParam(json.length() > 4000 ? json.substring(0, 4000) : json);
				} catch (Exception e) {
					entity.setParam("[serialization error]");
				}
			}
		}

		entity.setResult(failReason == null ? 1 : 0);
		if (failReason != null) {
			String trace = failReason.toString();
			entity.setFailReason(trace.length() > 4000 ? trace.substring(0, 4000) : trace);
		}
		entity.setDuration(duration);
		entity.setCreator(entity.getUserId());
		entity.setCreateTime(LocalDateTime.now());
		return entity;
	}

	private static OperateTrace resolveOperateTrace(MethodSignature signature) {
		Method method = signature.getMethod();
		OperateTrace methodTrace = method.getAnnotation(OperateTrace.class);
		if (methodTrace != null) {
			return methodTrace;
		}
		Class<?> declaringType = signature.getDeclaringType();
		return declaringType.getAnnotation(OperateTrace.class);
	}
}
