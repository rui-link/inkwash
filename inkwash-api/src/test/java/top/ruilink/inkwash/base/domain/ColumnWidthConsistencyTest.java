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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.base.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards that declared validation bounds never exceed the physical column width
 * (ISS-059).
 *
 * <p>
 * A {@code @Size(max = 120)} on a field mapped to {@code VARCHAR(60)} looks
 * harmless but inverts the failure mode: instead of a 400 with a field-level
 * message, the request reaches the database and comes back as a truncation
 * error with no usable diagnostics. The original defect was
 * {@code cms_category.slug} and {@code cms_term.slug}, both bounded at 120
 * against a 60-character column.
 *
 * <p>
 * Stricter validation than the column is fine and expected, so only the
 * "validation wider than column" direction fails.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("校验长度不得超过列宽（ISS-059）")
class ColumnWidthConsistencyTest {

	private static final Path SCHEMA = Path.of("database/mysql-schema.sql");
	private static final Path PARAMS = Path.of("src/main/java");

	/**
	 * Param simple name -> table name, for the params that map 1:1 onto a table.
	 */
	private static final Map<String, String> PARAM_TO_TABLE = new LinkedHashMap<>();

	static {
		PARAM_TO_TABLE.put("ArticleParam", "cms_article");
		PARAM_TO_TABLE.put("CategoryParam", "cms_category");
		PARAM_TO_TABLE.put("TermParam", "cms_term");
		PARAM_TO_TABLE.put("CommentParam", "cms_comment");
		PARAM_TO_TABLE.put("SensitiveParam", "cms_sensitive");
		PARAM_TO_TABLE.put("NoticeParam", "sys_notice");
		PARAM_TO_TABLE.put("MenuParam", "sys_menu");
		PARAM_TO_TABLE.put("RoleParam", "sys_role");
		PARAM_TO_TABLE.put("GroupParam", "sys_group");
		PARAM_TO_TABLE.put("PermissionParam", "sys_permission");
		PARAM_TO_TABLE.put("UserParam", "sys_user");
	}

	@Test
	@DisplayName("@Size(max) 不得超过对应列的 VARCHAR 宽度")
	void validationNeverExceedsColumnWidth() throws IOException {
		String schema = Files.readString(SCHEMA, StandardCharsets.UTF_8);
		List<String> violations = new ArrayList<>();

		for (Map.Entry<String, String> e : PARAM_TO_TABLE.entrySet()) {
			Path param = findParam(e.getKey());
			if (param == null) {
				continue;
			}
			Map<String, Integer> columns = columnWidths(schema, e.getValue());
			if (columns.isEmpty()) {
				continue;
			}
			for (Map.Entry<String, Integer> bound : sizeBounds(param).entrySet()) {
				Integer width = columns.get(bound.getKey().toLowerCase());
				if (width == null) {
					continue;
				}
				if (bound.getValue() > width) {
					violations.add(String.format("%s.%s: @Size(max=%d) 超过 %s 的 VARCHAR(%d)", e.getKey(), bound.getKey(),
							bound.getValue(), e.getValue(), width));
				}
			}
		}

		assertTrue(violations.isEmpty(), () -> "以下字段的校验上限大于列宽，超长会触发数据库截断错误而非 400: " + violations);
	}

	@Test
	@DisplayName("schema 文件可解析出列宽（防止测试因解析失败而空转）")
	void schemaParsesColumnWidths() throws IOException {
		String schema = Files.readString(SCHEMA, StandardCharsets.UTF_8);
		Map<String, Integer> article = columnWidths(schema, "cms_article");

		assertTrue(article.containsKey("title"), "cms_article.title 列宽解析失败");
		assertTrue(article.containsKey("opinion"), "cms_article.opinion 列宽解析失败");
		assertTrue(article.containsKey("slug"), "cms_article.slug 列宽解析失败");
	}

	@Test
	@DisplayName("已知回归点：category/term 的 slug 上限为 60，与列宽一致")
	void slugBoundsMatchColumnWidth() throws IOException {
		String schema = Files.readString(SCHEMA, StandardCharsets.UTF_8);
		for (String param : List.of("CategoryParam", "TermParam")) {
			Path file = findParam(param);
			assertTrue(file != null, param + " 未找到");
			Integer bound = sizeBounds(file).get("slug");
			Integer width = columnWidths(schema, "cms_category").get("slug");

			assertTrue(bound != null, param + ".slug 缺少 @Size");
			assertTrue(width != null, "cms_category.slug 列宽解析失败");
			assertTrue(bound <= width, param + ".slug 上限 " + bound + " 超过列宽 " + width);
		}
	}

	private static Path findParam(String simpleName) throws IOException {
		try (Stream<Path> files = Files.walk(PARAMS)) {
			for (Path p : files.filter(f -> f.getFileName().toString().equals(simpleName + ".java")).toList()) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Maps lower-cased column name to its VARCHAR width for one CREATE TABLE block.
	 */
	private static Map<String, Integer> columnWidths(String schema, String table) {
		Map<String, Integer> widths = new LinkedHashMap<>();
		Matcher block = Pattern
				.compile("CREATE TABLE " + table + "\\s*\\((.*?)\\n\\s*\\)\\s*(ENGINE|;)", Pattern.DOTALL)
				.matcher(schema);
		if (!block.find()) {
			return widths;
		}
		Matcher col = Pattern.compile("(?m)^\\s*`?(\\w+)`?\\s+VARCHAR\\((\\d+)\\)").matcher(block.group(1));
		while (col.find()) {
			widths.put(col.group(1).toLowerCase(), Integer.parseInt(col.group(2)));
		}
		return widths;
	}

	/**
	 * Maps field name to the {@code max} of the {@code @Size} declared immediately
	 * above it.
	 */
	private static Map<String, Integer> sizeBounds(Path param) throws IOException {
		Map<String, Integer> bounds = new LinkedHashMap<>();
		String src = Files.readString(param, StandardCharsets.UTF_8);
		Matcher m = Pattern
				.compile("@Size\\(([^)]*)\\)\\s*(?:@[\\w.]+(?:\\([^)]*\\))?\\s*)*private\\s+String\\s+(\\w+)\\s*;",
						Pattern.DOTALL)
				.matcher(src);
		while (m.find()) {
			Matcher max = Pattern.compile("max\\s*=\\s*(\\d+)").matcher(m.group(1));
			if (max.find()) {
				bounds.put(m.group(2), Integer.parseInt(max.group(1)));
			}
		}
		return bounds;
	}
}