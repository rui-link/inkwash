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
package top.ruilink.inkwash.system.domain.credential;

import top.ruilink.inkwash.base.enums.StringCodeEnum;

/**
 * Password credential state enumeration.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public enum PasswordStatus implements StringCodeEnum {
	ENABLE("ENABLE", "启用"), LOCKED("LOCKED", "锁定"), EXPIRED("EXPIRED", "已过期");

	private final String code;
	private final String name;

	PasswordStatus(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}
}
