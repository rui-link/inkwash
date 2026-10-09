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
package top.ruilink.inkwash.base.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import top.ruilink.inkwash.base.annotation.DataMask;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Locks the ISS-032 opt-in self-view exemption.
 *
 * <p>
 * Before the fix, {@code DataMaskSerializer} exempted <em>any</em> bean
 * exposing a {@code Long getId()}, so adding a new {@code @DataMask} field to a
 * view silently leaked plaintext to the record owner. The exemption is now
 * declared per field via {@link DataMask#exemptForSelf()}.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("数据脱敏自视图豁免（显式 opt-in）")
class DataMaskSerializerSelfExemptTest {

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();

	/**
	 * Plain JavaBean with a {@code getId()} accessor, matching the shape of
	 * {@code UserView}/{@code SysUser}.
	 *
	 * <p>
	 * Java {@code record}s are deliberately <em>not</em> used here: they expose
	 * {@code id()} rather than {@code getId()}, and
	 * {@link DataMaskSerializer#resolveId(Object)} reflects on the bean-style
	 * accessor, so a record can never be recognised as a self-view.
	 */
	static class OptedInView {

		private final Long id;
		@DataMask(value = DataMask.MaskType.PHONE, exemptForSelf = true)
		private final String phone;

		OptedInView(Long id, String phone) {
			this.id = id;
			this.phone = phone;
		}

		public Long getId() {
			return id;
		}

		public String getPhone() {
			return phone;
		}
	}

	/**
	 * Same shape, but without {@code exemptForSelf}. Declared standalone rather
	 * than extending {@link OptedInView}: Jackson introspects the whole superclass
	 * chain, so an inherited field would keep the exemption and the test would
	 * prove nothing.
	 */
	static class DefaultView {

		private final Long id;
		@DataMask(DataMask.MaskType.PHONE)
		private final String phone;

		DefaultView(Long id, String phone) {
			this.id = id;
			this.phone = phone;
		}

		public Long getId() {
			return id;
		}

		public String getPhone() {
			return phone;
		}
	}

	static class MixedView {

		private final Long id;
		@DataMask(DataMask.MaskType.EMAIL)
		private final String email;
		@DataMask(DataMask.MaskType.NAME)
		private final String name;
		@DataMask(DataMask.MaskType.ID_CARD)
		private final String idCard;
		@DataMask(DataMask.MaskType.ADDRESS)
		private final String address;

		MixedView(Long id, String email, String name, String idCard, String address) {
			this.id = id;
			this.email = email;
			this.name = name;
			this.idCard = idCard;
			this.address = address;
		}

		public Long getId() {
			return id;
		}

		public String getEmail() {
			return email;
		}

		public String getName() {
			return name;
		}

		public String getIdCard() {
			return idCard;
		}

		public String getAddress() {
			return address;
		}
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
		// ISS-048: DataMaskSerializer no longer calls SecurityUtil; it reads
		// CurrentUserProvider, which the security layer installs at startup. Uninstall
		// here so
		// the "no provider installed" default can be asserted and does not leak between
		// tests.
		CurrentUserProvider.install(null);
	}

	/**
	 * Stands in for the bean {@code CurrentUserProviderInstaller} creates in
	 * production.
	 *
	 * <p>
	 * The real implementation delegates to {@code SecurityUtil}, which itself reads
	 * {@code SecurityContextHolder}; wiring it directly keeps this test independent
	 * of the {@code security} module.
	 */
	private static void signIn(long userId) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, List.of()));
		SecurityContextHolder.setContext(context);
		CurrentUserProvider.install(() -> {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			return authentication == null || !authentication.isAuthenticated() ? null
					: Long.parseLong(authentication.getName());
		});
	}

	@Test
	void optedInFieldIsPlaintextForOwner() {
		signIn(7L);

		String json = MAPPER.writeValueAsString(new OptedInView(7L, "13800138000"));

		assertTrue(json.contains("13800138000"), "显式豁免的自视图字段应输出明文: " + json);
	}

	@Test
	void optedInFieldIsStillMaskedForOtherUsers() {
		signIn(99L);

		String json = MAPPER.writeValueAsString(new OptedInView(7L, "13800138000"));

		assertTrue(json.contains("138****8000"), "非本人视图必须脱敏: " + json);
		assertTrue(!json.contains("13800138000"), "非本人视图不得出现明文: " + json);
	}

	@Test
	void defaultFieldStaysMaskedForOwner() {
		signIn(7L);

		String json = MAPPER.writeValueAsString(new DefaultView(7L, "13800138000"));

		assertTrue(json.contains("138****8000"), "未声明豁免的字段对本人同样脱敏: " + json);
		assertTrue(!json.contains("13800138000"), "未声明豁免的字段不得输出明文: " + json);
	}

	@Test
	void maskingIsUnaffectedWhenNobodyIsSignedIn() {
		CurrentUserProvider.install(null);
		String json = MAPPER.writeValueAsString(new OptedInView(7L, "13800138000"));

		assertTrue(json.contains("138****8000"), "匿名访问一律脱敏: " + json);
	}

	@Test
	void maskTypesStillApply() {
		signIn(99L);

		String json = MAPPER.writeValueAsString(
				new MixedView(7L, "zhangsan@example.com", "张三丰", "11010119900307123X", "北京市朝阳区建国路88号"));

		assertTrue(json.contains("z***n@example.com"), json);
		assertTrue(json.contains("张*丰"), json);
		// 6 leading characters, 8 mask characters, trailing 4 of an 18-char value.
		assertTrue(json.contains("110101********123X"), json);
		assertTrue(json.contains("北京市朝阳区******"), json);
	}

	@Test
	void nullAndShortValuesArePassedThrough() {
		signIn(99L);
		record Edge(Long id, @DataMask(DataMask.MaskType.PHONE) String phone,
				@DataMask(DataMask.MaskType.EMAIL) String email) {
		}

		String json = MAPPER.writeValueAsString(new Edge(7L, null, "a@b.c"));

		assertTrue(json.contains("\"phone\":null"), json);
		assertTrue(json.contains("a@b.c"), "邮箱过短时不做截断: " + json);
	}
}