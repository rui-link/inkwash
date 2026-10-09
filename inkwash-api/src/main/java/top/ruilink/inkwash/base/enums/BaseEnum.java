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
 * Base interface for enums, standardising the getCode() method across all of
 * them.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface BaseEnum {
	/**
	 * Returns the enum code, which is the value persisted in the database.
	 */
	int getCode();

	/**
	 * Default implementation of getCode() (L-BN-7).
	 */
	default int code() {
		return getCode();
	}

	/**
	 * Returns the enum constant for a code when reading from the database
	 * (L-BN-11).
	 */
	static <E extends Enum<E> & BaseEnum> E fromCode(Class<E> enumClass, int code) {
		for (E enumConstant : enumClass.getEnumConstants()) {
			if (enumConstant.code() == code) {
				return enumConstant;
			}
		}
		throw new IllegalArgumentException("无效的枚举编码：" + code + "，类型：" + enumClass.getSimpleName());
	}
}
