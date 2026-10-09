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
package top.ruilink.inkwash.base;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.method.ControllerAdviceBean;

import top.ruilink.inkwash.base.advice.GlobalExceptionHandler;
import top.ruilink.inkwash.base.convert.StringToBaseStatusConverter;
import top.ruilink.inkwash.base.handler.BaseEnumTypeHandler;
import top.ruilink.inkwash.monitor.support.AuditLossMetrics;
import top.ruilink.inkwash.system.convert.StringToUserStatusConverter;
import top.ruilink.inkwash.system.handler.CredentialTypeHandler;

/**
 * Verifies that the classes relocated by ISS-060, and the bean added by
 * ISS-042, are still picked up by the running context.
 *
 * <p>
 * This matters because a package move is the kind of change that compiles
 * cleanly and then fails at runtime: component scanning is path-based, so a
 * class that ends up outside the scanned tree simply disappears.
 * `PackageLayoutTest` proves the files sit in the right directories; this test
 * proves the application still sees them.
 *
 * <p>
 * Note {@code InkwashApplicationTests} is
 * {@code @Disabled("Requires test database
 * connection")}. That premise does not hold here — this test boots the same
 * full context successfully under the {@code dev} profile's in-memory H2, which
 * is also how {@code QueryEnumConverterRegistrationTest} has been running.
 */
@SpringBootTest
@DisplayName("跨包移动后的组件仍能被上下文装配")
class ComponentWiringTest {

	@Autowired
	private ApplicationContext context;

	@Test
	@DisplayName("GlobalExceptionHandler 已移入 base/advice，且仍是生效的 advice")
	void globalExceptionHandler_isRegisteredAsAdvice() {
		GlobalExceptionHandler handler = context.getBean(GlobalExceptionHandler.class);

		assertNotNull(handler, "统一异常处理器应存在于上下文中");
		assertNotNull(
				GlobalExceptionHandler.class
						.getAnnotation(org.springframework.web.bind.annotation.RestControllerAdvice.class),
				"移动后必须仍是 @RestControllerAdvice，否则异常契约失效");

		boolean registeredAsAdvice = context.getBeanNamesForType(ControllerAdviceBean.class).length > 0
				|| context.getBeanNamesForType(GlobalExceptionHandler.class).length > 0;
		assertTrue(registeredAsAdvice, "GlobalExceptionHandler 未被注册为 controller advice");
	}

	@Test
	@DisplayName("移入 convert 包的枚举转换器仍是可用的 Bean")
	void enumConverters_areRegistered() {
		assertNotNull(context.getBean(StringToBaseStatusConverter.class), "base/convert 的转换器应装配");
		assertNotNull(context.getBean(StringToUserStatusConverter.class), "system/convert 的转换器应装配");
	}

	@Test
	@DisplayName("MyBatis TypeHandler 仍位于被扫描的 handler 包")
	void typeHandlers_stayInScannedPackages() {
		assertTrue(BaseEnumTypeHandler.class.getPackageName().endsWith(".base.handler"),
				"BaseEnumTypeHandler 应留在 base/handler");
		assertTrue(CredentialTypeHandler.class.getPackageName().endsWith(".system.handler"),
				"CredentialTypeHandler 应留在 system/handler");
	}

	@Test
	@DisplayName("ISS-042 的审计丢失计数器已装配且可读")
	void auditLossMetrics_isAvailable() {
		AuditLossMetrics metrics = context.getBean(AuditLossMetrics.class);

		assertNotNull(metrics, "AuditLossMetrics 应装配");
		assertTrue(metrics.totalLosses() >= 0);
	}
}