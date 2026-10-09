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
package top.ruilink.inkwash.base.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.base.util.CurrentUserProvider;

/**
 * Guards the layer boundary required by design doc S1 (ISS-048).
 *
 * <p>
 * {@code base} is the platform kernel and must not depend on an upper layer.
 * Two files broke that by calling {@code SecurityUtil} directly:
 * {@code FileUploadServiceImpl} (to namespace stored files by uploader) and
 * {@code DataMaskSerializer} (for the self-view exemption). The dependency is
 * now inverted — {@code base} declares {@code CurrentUserProvider} and
 * {@code security} installs it.
 *
 * <p>
 * Assertions are made on source text rather than at runtime because the illegal
 * dependency is a compile-time edge; Spring Security itself is a framework
 * dependency and therefore allowed.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("分层约束：base 不得依赖 security（ISS-048）")
class BaseLayerBoundaryTest {

	private static final Path BASE = Path.of("src/main/java/top/ruilink/inkwash/base");

	@Test
	@DisplayName("base 包内不得 import 项目自身的 security 包")
	void baseDoesNotImportProjectSecurity() throws IOException {
		List<String> offenders = new ArrayList<>();

		try (Stream<Path> files = Files.walk(BASE)) {
			for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
				String src = Files.readString(p, StandardCharsets.UTF_8);
				for (String line : src.split("\r?\n")) {
					String trimmed = line.trim();
					if (trimmed.startsWith("import ") && trimmed.contains("top.ruilink.inkwash.security")) {
						offenders.add(p.getFileName() + " -> " + trimmed);
					}
				}
			}
		}

		assertTrue(offenders.isEmpty(), () -> "base 依赖了上层 security；请在 base 侧声明抽象并由 security 反向装配: " + offenders);
	}

	@Test
	@DisplayName("跨层取当前用户的唯一入口是 CurrentUserProvider")
	void currentUserIsReachedThroughTheAbstraction() throws IOException {
		Path provider = BASE.resolve("util/CurrentUserProvider.java");
		assertTrue(Files.exists(provider), "base 侧必须有 CurrentUserProvider 抽象，否则调用方会退回直接依赖 security");

		Path installer = Path.of("src/main/java/top/ruilink/inkwash/security/config/CurrentUserProviderInstaller.java");
		assertTrue(Files.exists(installer), "security 侧必须有装配类，否则运行期拿不到当前用户");

		String installerSrc = Files.readString(installer, StandardCharsets.UTF_8);
		assertTrue(installerSrc.contains("CurrentUserProvider.install("), "装配类必须调用 CurrentUserProvider.install(...)");
		assertTrue(installerSrc.contains("SecurityUtil::getCurrentUserIdOrNull"), "实现应委托给 SecurityUtil，保持唯一身份来源");
	}

	@Test
	@DisplayName("CurrentUserProvider 未装配时按匿名处理，不抛异常")
	void uninstalledProviderIsAnonymous() {
		CurrentUserProvider.install(null);
		org.junit.jupiter.api.Assertions.assertNull(CurrentUserProvider.getCurrentUserIdOrNull());

		CurrentUserProvider.install(() -> 42L);
		org.junit.jupiter.api.Assertions.assertEquals(42L, CurrentUserProvider.getCurrentUserIdOrNull());

		CurrentUserProvider.install(null);
	}
}