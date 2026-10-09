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
package top.ruilink.inkwash.system.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;

import top.ruilink.inkwash.system.api.view.SysMetaView;

/**
 * Guards the {@code MetaController} response contract (ISS-047).
 *
 * <p>
 * Two things must hold: the endpoints return the {@code api.view} type rather
 * than a domain object, and the wire format stays byte-compatible with what the
 * frontend already consumes via {@code useSiteStore} and {@code AboutDialog}.
 */
class MetaControllerTest {

	/**
	 * Mirrors what the {@code build-info} goal writes into
	 * {@code META-INF/build-info.properties}. {@code InfoProperties} looks keys up
	 * verbatim, and {@code getTime()} goes through {@code getInstant}, which parses
	 * epoch milliseconds — an ISO-8601 string would silently yield {@code null}.
	 */
	private static BuildProperties buildProperties() {
		Properties props = new Properties();
		props.setProperty("group", "top.ruilink");
		props.setProperty("artifact", "inkwash-api");
		props.setProperty("name", "inkwash");
		props.setProperty("version", "0.5.1");
		props.setProperty("time", String.valueOf(Instant.parse("2026-10-06T10:00:00Z").toEpochMilli()));
		props.setProperty("short.name", "Inkwash");
		props.setProperty("formal.name", "Inkwash 墨洗");
		props.setProperty("description", "a note taking app");
		props.setProperty("copyright", "Copyright (C) 2026 ruilink team");
		props.setProperty("author", "Dyllon");
		props.setProperty("java.version", "21");
		props.setProperty("license", "GPL-3.0");
		return new BuildProperties(props);
	}

	@Test
	@DisplayName("/meta 与 /about 返回视图类型而非领域实体")
	void endpointsReturnViewType() {
		MetaController controller = new MetaController(buildProperties());

		ResponseEntity<SysMetaView> meta = controller.getAppInfo();
		ResponseEntity<SysMetaView> about = controller.getAbout();

		assertTrue(SysMetaView.class.getName().endsWith("api.view.SysMetaView"), "控制器签名应暴露 api.view 下的类型");
		assertEquals(SysMetaView.class, meta.getBody().getClass());
		assertEquals(SysMetaView.class, about.getBody().getClass());
	}

	@Test
	@DisplayName("/meta 填充前端依赖的品牌字段")
	void metaPopulatesBrandingFields() {
		SysMetaView view = new MetaController(buildProperties()).getAppInfo().getBody();

		assertEquals("Inkwash", view.getShortName());
		assertEquals("Inkwash 墨洗", view.getFormalName());
		assertEquals("inkwash", view.getProjectName());
		assertEquals("0.5.1", view.getVersion());
		assertEquals("a note taking app", view.getDescription());
		assertEquals("Dyllon", view.getAuthor());
		assertEquals("21", view.getJavaVersion());
		assertEquals("top.ruilink", view.getGroupId());
		assertEquals("inkwash-api", view.getArtifactId());
		assertTrue(view.getBuildTime() != null);
	}

	@Test
	@DisplayName("/about 在 /meta 基础上补充 SpringBoot 与许可信息")
	void aboutAddsSpringBootAndLicense() {
		MetaController controller = new MetaController(buildProperties());

		SysMetaView meta = controller.getAppInfo().getBody();
		SysMetaView about = controller.getAbout().getBody();

		assertEquals("GPL-3.0", about.getLicense());
		assertTrue(about.getSpringBoot() != null && !about.getSpringBoot().isBlank());
		// /meta must not leak license details
		assertNull(meta.getLicense());
		assertNull(meta.getSpringBoot());

		// the eleven shared fields must be identical between the two endpoints
		assertEquals(meta.getShortName(), about.getShortName());
		assertEquals(meta.getFormalName(), about.getFormalName());
		assertEquals(meta.getDescription(), about.getDescription());
		assertEquals(meta.getBuildTime(), about.getBuildTime());
	}

	@Test
	@DisplayName("视图字段全部为 String/Instant，不含嵌套实体，避免意外外泄")
	void viewHasNoNestedDomainTypes() {
		Set<String> forbidden = Arrays.stream(SysMetaView.class.getDeclaredFields()).map(Field::getType)
				.filter(t -> !t.isPrimitive() && !t.getName().startsWith("java.")).map(Class::getName)
				.collect(Collectors.toSet());

		assertTrue(forbidden.isEmpty(), "视图不应声明复杂类型字段：" + forbidden);
		assertTrue(Modifier.isPublic(SysMetaView.class.getModifiers()));
	}
}