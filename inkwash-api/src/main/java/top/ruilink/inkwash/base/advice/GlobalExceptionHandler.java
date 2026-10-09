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
package top.ruilink.inkwash.base.advice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.exception.AuthException;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.exception.ConflictException;
import top.ruilink.inkwash.base.exception.NotFoundException;
import top.ruilink.inkwash.base.util.LocaleUtil;

/**
 * Global exception handler producing problem detail responses.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ProblemDetail> handleNotFoundException(NotFoundException e) {
		log.warn("资源不存在: {}", e.getMessage());
		return buildResponse(e.getHttpStatus(), e.getMessage(), e.getMessageKey());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ProblemDetail> handleConflictException(ConflictException e) {
		log.warn("资源冲突: {}", e.getMessage());
		return buildResponse(e.getHttpStatus(), e.getMessage(), e.getMessageKey());
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException e) {
		log.warn("业务异常: message={}", e.getMessage());
		return buildResponse(e.getHttpStatus(), e.getMessage(), e.getMessageKey());
	}

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ProblemDetail> handleAuthException(AuthException e) {
		log.warn("认证异常: {}", e.getMessage());
		return buildResponse(e.getHttpStatus(), e.getMessage(), e.getMessageKey());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining("; "));
		log.warn("参数验证异常: {}", message);
		return buildResponse(HttpStatus.BAD_REQUEST, message, null);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException e) {
		String message = e.getConstraintViolations().stream().map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.collect(Collectors.joining("; "));
		log.warn("参数校验异常: {}", message);
		return buildResponse(HttpStatus.BAD_REQUEST, message, null);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ProblemDetail> handleMissingParam(MissingServletRequestParameterException e) {
		log.warn("缺少请求参数: {}", e.getMessage());
		return buildResponse(HttpStatus.BAD_REQUEST, "缺少必要参数: " + e.getParameterName(), null);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
		log.warn("不支持的请求方法: {}", e.getMessage());
		return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "不支持的请求方法: " + e.getMethod(), null);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
		log.warn("不支持的Content-Type: {}", e.getMessage());
		return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的Content-Type", null);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleMessageNotReadable(HttpMessageNotReadableException e) {
		log.warn("请求体解析失败: {}", e.getMessage());
		return buildResponse(HttpStatus.BAD_REQUEST, "请求体格式错误", null);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		log.warn("参数类型不匹配: {} -> {}", e.getName(), e.getValue());
		return buildResponse(HttpStatus.BAD_REQUEST, "参数类型错误: " + e.getName(), null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException e) {
		log.warn("权限不足: {}", e.getMessage());
		return buildResponse(HttpStatus.FORBIDDEN, LocaleUtil.getValue("error.forbidden"), null);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ProblemDetail> handleNoResourceFound(NoResourceFoundException e) {
		log.warn("请求的资源不存在: {}", e.getResourcePath());
		return buildResponse(HttpStatus.NOT_FOUND, "资源不存在: " + e.getResourcePath(), null);
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ProblemDetail> handleRuntimeException(RuntimeException e) {
		log.error("未处理的运行时异常", e);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙", null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleException(Exception e) {
		log.error("系统异常: ", e);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误", null);
	}

	private static ResponseEntity<ProblemDetail> buildResponse(HttpStatus status, String detail, String messageKey) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
		pd.setTitle(status.getReasonPhrase());
		pd.setProperty("timestamp", LocalDateTime.now().toString());
		if (messageKey != null) {
			pd.setProperty("messageKey", messageKey);
		}
		return ResponseEntity.status(status).body(pd);
	}
}
