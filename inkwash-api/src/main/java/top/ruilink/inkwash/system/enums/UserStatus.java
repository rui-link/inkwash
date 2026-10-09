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
package top.ruilink.inkwash.system.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import top.ruilink.inkwash.base.enums.BaseEnum;

/**
 * User account status enumeration.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public enum UserStatus implements BaseEnum {
	DISABLE(0, "未激活"), ENABLE(1, "启用"), LOCKED(2, "锁定"), EXPIRE(3, "过期"), PENDING(4, "待审核"), DELETED(5, "已删除");

	private final int code;
	private final String name;

	UserStatus(int code, String name) {
		this.code = code;
		this.name = name;
	}

	@JsonValue
	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	@JsonCreator
	public static UserStatus fromCode(int code) {
		for (UserStatus status : values()) {
			if (status.getCode() == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("Invalid code: " + code);
	}
}
