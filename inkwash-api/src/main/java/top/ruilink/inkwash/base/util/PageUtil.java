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

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongSupplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import top.ruilink.inkwash.base.domain.PageResult;

/**
 * Offset pagination and page result construction helper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class PageUtil {
	private PageUtil() {
	}

	public static Pageable of(int page, int size) {
		return PageRequest.of(page - 1, size);
	}

	public static <T> Page<T> toPage(List<T> content, int page, int size, long total) {
		return new PageImpl<>(content, PageRequest.of(page - 1, size), total);
	}

	public static <T> Page<T> empty(int page, int size) {
		return new PageImpl<>(List.of(), PageRequest.of(page - 1, size), 0);
	}

	/**
	 * Generic paginated query running count, select, conversion and PageResult
	 * wrapping.
	 *
	 * @param page      1-based page number
	 * @param size      page size
	 * @param countFunc function counting rows, given the offset
	 * @param queryFunc function querying rows, given offset and size
	 * @param converter function converting an entity to a view
	 * @return PageResult<View>
	 */
	public static <Entity, View> PageResult<View> queryPage(int page, int size, LongSupplier countFunc,
			BiFunction<Long, Integer, List<Entity>> queryFunc, Function<Entity, View> converter) {

		long offset = (long) (page - 1) * size;
		long total = countFunc.getAsLong();
		List<Entity> list = queryFunc.apply(offset, size);
		List<View> views = list.stream().map(converter).toList();
		Pageable pageable = PageRequest.of(page - 1, size);
		return PageResult.of(new PageImpl<>(views, pageable, total));
	}
}
