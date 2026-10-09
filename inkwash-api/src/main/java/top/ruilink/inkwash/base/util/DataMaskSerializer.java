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
package top.ruilink.inkwash.base.util;

import java.lang.reflect.Method;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import top.ruilink.inkwash.base.annotation.DataMask;
import top.ruilink.inkwash.base.annotation.DataMask.MaskType;

/**
 * Jackson serializer masking sensitive string fields during output.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class DataMaskSerializer extends ValueSerializer<String> {

	private final MaskType maskType;
	private final String maskChar;
	private final int prefixLen;
	private final int suffixLen;
	private final boolean exemptForSelf;

	public DataMaskSerializer() {
		this(MaskType.PHONE, "*", 3, 4, false);
	}

	public DataMaskSerializer(MaskType maskType, String maskChar, int prefixLen, int suffixLen, boolean exemptForSelf) {
		this.maskType = maskType;
		this.maskChar = maskChar;
		this.prefixLen = prefixLen;
		this.suffixLen = suffixLen;
		this.exemptForSelf = exemptForSelf;
	}

	@Override
	public void serialize(String value, JsonGenerator gen, SerializationContext provider) {
		// Opt-in only: a field is exempt for the owner solely when
		// @DataMask(exemptForSelf = true)
		// says so. Previously any view exposing a `Long getId()` was silently exempt.
		if (exemptForSelf && isSelfView(gen)) {
			gen.writeString(value);
		} else {
			gen.writeString(mask(value));
		}
	}

	@Override
	public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
		if (property == null) {
			return this;
		}
		DataMask annotation = property.getAnnotation(DataMask.class);
		if (annotation == null) {
			return this;
		}
		return new DataMaskSerializer(annotation.value(), annotation.maskChar(), annotation.prefixLen(),
				annotation.suffixLen(), annotation.exemptForSelf());
	}

	/**
	 * Whether the record being serialized belongs to the currently logged-in user.
	 */
	private boolean isSelfView(JsonGenerator gen) {
		Long selfId = CurrentUserProvider.getCurrentUserIdOrNull();
		if (selfId == null) {
			return false;
		}
		Object current = gen.currentValue();
		if (current == null) {
			return false;
		}
		Long targetId = resolveId(current);
		return targetId != null && selfId.equals(targetId);
	}

	private Long resolveId(Object bean) {
		try {
			Method getId = bean.getClass().getMethod("getId");
			Object id = getId.invoke(bean);
			if (id instanceof Number number) {
				return number.longValue();
			}
		} catch (Exception ignored) {
			// Not a user object or no getId method, ignore
		}
		return null;
	}

	private String mask(String value) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		return switch (maskType) {
		case PHONE -> maskPhone(value);
		case EMAIL -> maskEmail(value);
		case ID_CARD -> maskIdCard(value);
		case NAME -> maskName(value);
		case ADDRESS -> maskAddress(value);
		case CUSTOM -> maskCustom(value);
		};
	}

	private String maskPhone(String phone) {
		if (phone == null || phone.length() < 7)
			return phone;
		return phone.substring(0, 3) + "****" + phone.substring(7);
	}

	private String maskEmail(String email) {
		if (email == null || !email.contains("@"))
			return email;
		int at = email.indexOf("@");
		String local = email.substring(0, at);
		String domain = email.substring(at);
		if (local.length() <= 1)
			return local + domain;
		return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
	}

	private String maskIdCard(String id) {
		if (id == null || id.length() < 10)
			return id;
		return id.substring(0, 6) + "********" + id.substring(id.length() - 4);
	}

	private String maskName(String name) {
		if (name == null || name.isEmpty())
			return name;
		if (name.length() == 1)
			return name;
		if (name.length() == 2)
			return name.charAt(0) + "*";
		return name.charAt(0) + repeat('*', name.length() - 2) + name.charAt(name.length() - 1);
	}

	private String maskAddress(String address) {
		if (address == null || address.length() < 6)
			return address;
		return address.substring(0, 6) + repeat('*', Math.min(6, address.length() - 6));
	}

	private String maskCustom(String value) {
		if (value == null || value.length() <= prefixLen + suffixLen)
			return value;
		return value.substring(0, prefixLen) + repeat(maskChar.charAt(0), value.length() - prefixLen - suffixLen)
				+ value.substring(value.length() - suffixLen);
	}

	private static String repeat(char c, int count) {
		if (count <= 0)
			return "";
		return String.valueOf(c).repeat(count);
	}
}
