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
package top.ruilink.inkwash.base.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Identity claim type, stored in sys_identity.identity_type.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public enum IdentityType implements BaseEnum {
	PHONE(1, "手机号"), EMAIL(2, "邮箱"), OIDC_SUB(3, "OIDC Subject");

	private final int code;
	private final String name;

	IdentityType(int code, String name) {
		this.code = code;
		this.name = name;
	}

	@JsonValue
	@Override
	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	@JsonCreator
	public static IdentityType fromCode(int code) {
		for (IdentityType type : values()) {
			if (type.getCode() == code) {
				return type;
			}
		}
		throw new IllegalArgumentException("Invalid code: " + code);
	}
}
