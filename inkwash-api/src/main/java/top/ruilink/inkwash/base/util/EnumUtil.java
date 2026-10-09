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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration code to label option map helper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class EnumUtil {
	/**
	 * Converts an enum type into a Map of code to name.
	 * 
	 * @param enumClass     the enum class
	 * @param codeExtractor function extracting the code
	 * @param nameExtractor function extracting the name
	 * @return a Map from enum code to name
	 * 
	 *         Map<Object, Object> genderOptions =
	 *         EnumUtil.listOptions(Gender.class, Gender::getCode, Gender::getName);
	 */
	public static <T> Map<Object, Object> listOptions(Class<T> enumClass, Function<T, Object> codeExtractor,
			Function<T, Object> nameExtractor) {
		return Arrays.stream(enumClass.getEnumConstants())
				.collect(Collectors.toMap(codeExtractor::apply, nameExtractor::apply, (u, _) -> u, LinkedHashMap::new));
	}
}
