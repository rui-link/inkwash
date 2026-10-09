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

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import top.ruilink.inkwash.base.exception.BusinessException;

/**
 * Sort expression validator. Whitelists allowed column names and validates the
 * final SQL expression. Always returns a non-null, safe ORDER BY expression or
 * rejects invalid input.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Component
public class SortValidator {

	private static final Map<String, String> SORT_FIELDS = Map.ofEntries(Map.entry("createTime", "create_time"),
			Map.entry("updateTime", "update_time"), Map.entry("nickname", "nickname"), Map.entry("id", "id"),
			Map.entry("status", "status"), Map.entry("name", "name"), Map.entry("module", "module"),
			Map.entry("resource", "resource"), Map.entry("action", "action"), Map.entry("code", "code"),
			Map.entry("parentId", "parent_id"), Map.entry("level", "level"), Map.entry("type", "type"),
			Map.entry("sort", "sort"), Map.entry("title", "title"), Map.entry("slug", "slug"),
			Map.entry("word", "word"), Map.entry("categoryId", "category_id"), Map.entry("authorId", "author_id"),
			Map.entry("publishTime", "publish_time"), Map.entry("userId", "user_id"), Map.entry("identity", "identity"),
			Map.entry("loginType", "login_type"), Map.entry("address", "address"), Map.entry("location", "location"),
			Map.entry("device", "device"), Map.entry("browser", "browser"), Map.entry("ostype", "ostype"),
			Map.entry("loginTime", "login_time"));

	private static final Pattern SAFE_SQL = Pattern
			.compile("^[a-zA-Z_][a-zA-Z0-9_]* (ASC|DESC)(, [a-zA-Z_][a-zA-Z0-9_]* (ASC|DESC))*$");

	private static final String DEFAULT_SORT = "id ASC";

	/**
	 * Resolve sort field and order into a safe SQL ORDER BY expression. Throws
	 * {@link BusinessException} (HTTP 400) when a non-blank field or order is not
	 * whitelisted; falls back for missing input only.
	 */
	public String resolve(String sortField, String sortOrder) {
		return resolve(sortField, sortOrder, DEFAULT_SORT);
	}

	/**
	 * Resolve sort field and order into a safe SQL ORDER BY expression, using a
	 * caller-provided default for missing input only. Invalid whitelist-violating
	 * input is rejected rather than substituted.
	 */
	public String resolve(String sortField, String sortOrder, String defaultSort) {
		if (sortField == null || sortField.isBlank()) {
			return (defaultSort != null && SAFE_SQL.matcher(defaultSort).matches()) ? defaultSort : DEFAULT_SORT;
		}
		var column = SORT_FIELDS.get(sortField);
		if (column == null) {
			throw new BusinessException("error.sort.field_invalid", sortField);
		}
		var dir = "ASC";
		if (sortOrder != null && !sortOrder.isBlank()) {
			if ("desc".equalsIgnoreCase(sortOrder)) {
				dir = "DESC";
			} else if (!"asc".equalsIgnoreCase(sortOrder)) {
				throw new BusinessException("error.sort.direction_invalid", sortOrder);
			}
		}
		var result = column + " " + dir;
		if (!SAFE_SQL.matcher(result).matches()) {
			throw new IllegalStateException("排序字段白名单包含非法列名: " + column);
		}
		return result;
	}
}
