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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the package-role separation documented in {@code system-design.md}
 * §2.1 (ISS-060).
 *
 * <p>
 * {@code base/handler} and {@code system/handler} used to hold three unrelated
 * roles at once: MyBatis {@code TypeHandler}s, Spring {@code Converter}s and a
 * {@code @RestControllerAdvice}. The first group is scanned package-wide by
 * {@code mybatis.type-handlers-package}, so the other two made the persistence
 * config point at a namespace with mixed responsibilities.
 *
 * <p>
 * These tests read the source tree directly instead of using reflection: a
 * class that moved to the wrong package still compiles and still loads, so only
 * its declared package reveals the violation.
 */
class PackageLayoutTest {

	private static final Path MAIN_ROOT = Path.of("src", "main", "java", "top", "ruilink", "inkwash");

	@Test
	@DisplayName("handler 包内只允许出现 MyBatis TypeHandler")
	void handlerPackages_containOnlyTypeHandlers() throws IOException {
		for (String module : List.of("base", "system")) {
			Path handlerDir = MAIN_ROOT.resolve(module).resolve("handler");
			assertTrue(Files.isDirectory(handlerDir), module + "/handler 目录应存在");

			List<String> offenders = new ArrayList<>();
			for (Path file : javaFilesIn(handlerDir)) {
				String simpleName = file.getFileName().toString().replace(".java", "");
				Class<?> type = load(module, "handler", simpleName);
				if (!isTypeHandler(type)) {
					offenders.add(module + "/handler/" + simpleName);
				}
			}
			assertTrue(offenders.isEmpty(),
					"以下类混入了 handler 包，该包应只放 MyBatis TypeHandler（见 system-design.md §2.1）：" + offenders);
		}
	}

	@Test
	@DisplayName("String→枚举的 Spring Converter 位于 convert 包，而非 handler")
	void stringToEnumConverters_liveInConvertPackages() throws IOException {
		List<String> expected = List.of("base/convert/StringToBaseStatusConverter",
				"cms/convert/StringToArticleStatusConverter", "system/convert/StringToUserStatusConverter");
		for (String fqn : expected) {
			assertTrue(Files.isRegularFile(MAIN_ROOT.resolve(fqn + ".java")), fqn + ".java 应存在");
		}
	}

	@Test
	@DisplayName("统一异常出口位于 base/advice")
	void globalExceptionHandler_livesInAdvicePackage() throws IOException {
		Path advice = MAIN_ROOT.resolve("base").resolve("advice").resolve("GlobalExceptionHandler.java");
		assertTrue(Files.isRegularFile(advice), "GlobalExceptionHandler 应位于 base/advice");

		String source = Files.readString(advice, StandardCharsets.UTF_8);
		assertTrue(source.contains("@RestControllerAdvice"), "GlobalExceptionHandler 应仍是 @RestControllerAdvice");
	}

	@Test
	@DisplayName("type-handlers-package 配置只指向纯 TypeHandler 的包")
	void typeHandlersPackage_pointsAtHandlerPackagesOnly() throws IOException {
		String yaml = Files.readString(Path.of("src", "main", "resources", "application.yaml"), StandardCharsets.UTF_8);
		int at = yaml.indexOf("type-handlers-package:");
		assertTrue(at >= 0, "application.yaml 应配置 mybatis.type-handlers-package");

		String value = yaml.substring(at, yaml.indexOf('\n', at));
		for (String pkg : value.split(",")) {
			String cleaned = pkg.replace("type-handlers-package:", "").trim();
			if (cleaned.isEmpty()) {
				continue;
			}
			assertTrue(cleaned.endsWith(".handler"),
					"type-handlers-package 不应指向非 handler 包（Web advice / Spring Converter 与持久化无关）：" + cleaned);
		}
	}

	private static boolean isTypeHandler(Class<?> type) {
		return org.apache.ibatis.type.BaseTypeHandler.class.isAssignableFrom(type);
	}

	private static Class<?> load(String module, String subPackage, String simpleName) {
		String fqn = "top.ruilink.inkwash." + module + "." + subPackage + "." + simpleName;
		try {
			return Class.forName(fqn, false, PackageLayoutTest.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new AssertionError("源码存在但类加载失败，检查包声明：" + fqn, e);
		}
	}

	private static List<Path> javaFilesIn(Path dir) throws IOException {
		try (var stream = Files.list(dir)) {
			return stream.filter(p -> p.getFileName().toString().endsWith(".java")).toList();
		}
	}
}