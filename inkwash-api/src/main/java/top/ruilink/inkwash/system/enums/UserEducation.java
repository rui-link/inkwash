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
 * Education level enumeration.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public enum UserEducation implements BaseEnum {
	UNKNOWN(0, "未知"), COMMONER(1, "未入学"), PRIMARY(2, "小学"), JUNIOR(3, "初中"), SENIOR(4, "高中"), COLLEGE(5, "专科"),
	BACHELOR(6, "本科"), MASTER(7, "硕士"), DOCTOR(8, "博士"), EXPERT(9, "博士后");

	private final int code;
	private final String name;

	UserEducation(int code, String name) {
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
	public static UserEducation fromCode(int code) {
		for (UserEducation type : values()) {
			if (type.getCode() == code) {
				return type;
			}
		}
		throw new IllegalArgumentException("Invalid code: " + code);
	}
}
