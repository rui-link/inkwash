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

/**
 * Base interface for enums whose code is a String (L-BN-8), unifying the
 * getCode()/fromCode() contract.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface StringCodeEnum {
	/**
	 * Returns the enum code.
	 */
	String getCode();

	/**
	 * Returns the enum constant for a code, or null when the code is null or blank.
	 */
	static <E extends Enum<E> & StringCodeEnum> E fromCode(Class<E> enumClass, String code) {
		if (code == null || code.isBlank()) {
			return null;
		}
		for (E enumConstant : enumClass.getEnumConstants()) {
			if (enumConstant.getCode().equals(code)) {
				return enumConstant;
			}
		}
		throw new IllegalArgumentException("无效的枚举编码：" + code + "，类型：" + enumClass.getSimpleName());
	}
}
