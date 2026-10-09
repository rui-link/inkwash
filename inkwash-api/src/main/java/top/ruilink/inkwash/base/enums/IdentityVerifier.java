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
 * Verification source of an identity claim, stored in sys_identity.verified_by.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public enum IdentityVerifier implements BaseEnum {
	USER(1, "用户自验证"), SMS_CODE(2, "短信验证码"), OAUTH2(3, "OAuth2"), ADMIN(4, "管理员");

	private final int code;
	private final String name;

	IdentityVerifier(int code, String name) {
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
	public static IdentityVerifier fromCode(int code) {
		for (IdentityVerifier by : values()) {
			if (by.getCode() == code) {
				return by;
			}
		}
		throw new IllegalArgumentException("Invalid code: " + code);
	}
}
