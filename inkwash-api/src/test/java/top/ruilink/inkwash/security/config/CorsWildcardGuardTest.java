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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.security.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Locks the "no wildcard in {@code cors.allowed-origin}" rule (ISS-062).
 *
 * <p>
 * The same property is the CORS allowlist <em>and</em> the CSRF Origin
 * allowlist. {@code OriginCheckCsrfFilter#originAllowed} skips any entry
 * containing {@code *}, so a wildcard silently removes that source from CSRF
 * checking while the configuration still looks populated and nothing fails.
 * {@link CorsConfig#rejectWildcardOrigins()} now turns that misconfiguration
 * into a startup failure.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("CORS/CSRF 白名单不得含通配符（ISS-062）")
class CorsWildcardGuardTest {

	/**
	 * Builds a CorsConfig with the given raw property value, bypassing the Spring
	 * container.
	 */
	private static CorsConfig configWith(String raw) throws Exception {
		CorsConfig config = new CorsConfig();
		Field field = CorsConfig.class.getDeclaredField("allowedOrigin");
		field.setAccessible(true);
		field.set(config, raw);
		return config;
	}

	@Test
	@DisplayName("含通配符时启动即失败")
	void wildcardIsRejected() throws Exception {
		CorsConfig config = configWith("http://localhost:9089, *");

		IllegalStateException e = assertThrows(IllegalStateException.class, config::rejectWildcardOrigins,
				"含 '*' 必须启动失败");
		assertTrue(e.getMessage().contains("*"), e.getMessage());
		assertTrue(e.getMessage().contains("CSRF"), "错误信息应说明该配置同时承担 CSRF 白名单职责");
	}

	@Test
	@DisplayName("各种通配符写法都被拦截")
	void allWildcardFormsAreRejected() throws Exception {
		for (String raw : List.of("*", "http://*.example.com", "https://localhost:*/", " * ", "http://a.com,*")) {
			CorsConfig config = configWith(raw);
			assertThrows(IllegalStateException.class, config::rejectWildcardOrigins, "应拦截: " + raw);
		}
	}

	@Test
	@DisplayName("精确来源列表通过校验")
	void exactOriginsPass() throws Exception {
		CorsConfig config = configWith(
				"http://localhost:9089,http://localhost:9090,http://localhost:3000,https://cms.example.com");

		assertDoesNotThrow(config::rejectWildcardOrigins);
		assertEquals(4, config.getAllowedOrigins().size());
	}

	@Test
	@DisplayName("空配置不报错（由 parseOrigins 过滤空项）")
	void emptyConfigPasses() throws Exception {
		CorsConfig config = configWith("");
		assertDoesNotThrow(config::rejectWildcardOrigins);
		assertTrue(config.getAllowedOrigins().isEmpty());
	}

	@Test
	@DisplayName("application.yaml 的实际配置不含通配符")
	void shippedConfigurationHasNoWildcard() throws Exception {
		Resource resource = new ClassPathResource("application.yaml");
		assertTrue(resource.exists(), "找不到 application.yaml");

		String yaml = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		List<String> lines = yaml.lines().map(String::trim).filter(line -> line.startsWith("allowed-origin:")).toList();

		assertEquals(1, lines.size(), "应恰好有一处 cors.allowed-origin，实际: " + lines);
		String raw = lines.get(0).substring("allowed-origin:".length());
		assertFalse(raw.contains("*"), "application.yaml 的 cors.allowed-origin 含通配符: " + raw);

		CorsConfig config = configWith(raw);
		assertDoesNotThrow(config::rejectWildcardOrigins, "随包配置必须能通过启动校验，否则应用起不来");
		assertTrue(config.getAllowedOrigins().size() >= 3, "实际配置应包含多个精确来源（含管理端、门户、文件服务）");
	}
}