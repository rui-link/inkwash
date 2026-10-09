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
package top.ruilink.inkwash.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the parity of {@code mysql-schema.sql} and {@code h2-schema.sql}.
 *
 * <p>
 * Both files are maintained by hand, which is what makes them readable — the
 * MySQL script carries the rationale for why a composite index exists and why a
 * single-column one was dropped. The cost of hand maintenance is drift:
 * {@code design/static} runs on H2 while production runs on MySQL, so a column
 * added to only one file fails in one environment and not the other, which is
 * close to impossible to reproduce locally.
 *
 * <p>
 * Column <em>types</em> are deliberately not compared. They must differ where
 * the engines differ — most visibly {@code BOOLEAN}, which MySQL stores as
 * {@code TINYINT(1)} and H2 as a native boolean. Table names, column names and
 * their order, and index definitions must match exactly, because those are what
 * the MyBatis mappers bind to.
 *
 * <p>
 * Discovery of the two redundant indexes this test now prevents
 * ({@code sys_role.code} and {@code sys_account(identity, auth_type)}) required
 * comparing the script text against a real MySQL server's
 * {@code information_schema}. Neither file looked wrong on its own.
 */
class SchemaParityTest {

	private static final Path DATABASE_DIR = Path.of("database");

	@Test
	@DisplayName("两个 schema 脚本声明完全相同的表")
	void tableSetsMatch() throws IOException {
		Schema mysql = parse(DATABASE_DIR.resolve("mysql-schema.sql"));
		Schema h2 = parse(DATABASE_DIR.resolve("h2-schema.sql"));

		assertEquals(mysql.tables.keySet(), h2.tables.keySet(),
				"表集合不一致：仅 mysql 有 " + difference(mysql.tables.keySet(), h2.tables.keySet()) + "；仅 h2 有 "
						+ difference(h2.tables.keySet(), mysql.tables.keySet()));
		assertEquals(24, mysql.tables.size(), "表数量应与设计文档 §7.1 一致");
	}

	@Test
	@DisplayName("同名表的列名与列序完全一致")
	void columnNamesAndOrderMatch() throws IOException {
		Schema mysql = parse(DATABASE_DIR.resolve("mysql-schema.sql"));
		Schema h2 = parse(DATABASE_DIR.resolve("h2-schema.sql"));

		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : mysql.tables.entrySet()) {
			String table = entry.getKey();
			List<String> mine = entry.getValue();
			List<String> theirs = h2.tables.get(table);
			if (!mine.equals(theirs)) {
				problems.add(table + ": mysql=" + mine + " h2=" + theirs);
			}
		}
		assertTrue(problems.isEmpty(), "以下表的列不一致（MyBatis 按列名绑定，漏一列即运行期报错）：" + problems);
	}

	@Test
	@DisplayName("索引定义完全一致")
	void indexDefinitionsMatch() throws IOException {
		Schema mysql = parse(DATABASE_DIR.resolve("mysql-schema.sql"));
		Schema h2 = parse(DATABASE_DIR.resolve("h2-schema.sql"));

		assertEquals(mysql.indexes.keySet(), h2.indexes.keySet(),
				"索引集合不一致：仅 mysql 有 " + difference(mysql.indexes.keySet(), h2.indexes.keySet()) + "；仅 h2 有 "
						+ difference(h2.indexes.keySet(), mysql.indexes.keySet()));
		for (Map.Entry<String, String> entry : mysql.indexes.entrySet()) {
			assertEquals(entry.getValue(), h2.indexes.get(entry.getKey()), "索引 " + entry.getKey() + " 的列不一致");
		}
	}

	@Test
	@DisplayName("不存在覆盖相同列的重复索引")
	void noDuplicateIndexColumns() throws IOException {
		for (String file : List.of("mysql-schema.sql", "h2-schema.sql")) {
			Schema schema = parse(DATABASE_DIR.resolve(file));
			Map<String, String> seen = new LinkedHashMap<>();
			for (Map.Entry<String, String> index : schema.indexes.entrySet()) {
				String signature = schema.indexTable.get(index.getKey()) + "->" + index.getValue();
				assertFalse(seen.containsKey(signature), file + ": 索引 " + index.getKey() + " 与 " + seen.get(signature)
						+ " 覆盖完全相同的列，其中一个必然冗余（唯一索引已可服务同列查询）");
				seen.put(signature, index.getKey());
			}
		}
	}

	@Test
	@DisplayName("建表与建索引语句可被解析出内容（防止格式被破坏而静默失效）")
	void bothScriptsParse() throws IOException {
		Schema mysql = parse(DATABASE_DIR.resolve("mysql-schema.sql"));
		Schema h2 = parse(DATABASE_DIR.resolve("h2-schema.sql"));

		assertFalse(mysql.tables.isEmpty(), "mysql-schema.sql 未解析出任何表");
		assertFalse(h2.tables.isEmpty(), "h2-schema.sql 未解析出任何表");
		assertTrue(mysql.indexes.size() >= 19, "索引数量异常偏少，解析可能已失效：" + mysql.indexes.size());
	}

	@Test
	@DisplayName("迁移脚本目录已移除，基线脚本是唯一事实来源")
	void migrationDirectoryIsGone() {
		Path migrations = DATABASE_DIR.resolve("migration");
		assertFalse(Files.exists(migrations), "database/migration/ 已被删除：基线脚本已含全部变更，保留迁移脚本只会在两处重复描述同一状态并再次漂移");
	}

	private static Set<String> difference(Set<String> left, Set<String> right) {
		Set<String> only = new LinkedHashSet<>(left);
		only.removeAll(right);
		return only;
	}

	private static final Pattern CREATE_TABLE = Pattern.compile("(?i)^CREATE TABLE (?:IF NOT EXISTS )?(\\w+)\\s*\\($");
	private static final Pattern CREATE_INDEX = Pattern
			.compile("(?i)^CREATE (UNIQUE )?INDEX (\\w+) ON (\\w+)\\s*\\(([^)]*)\\)\\s*;?\\s*$");
	private static final Pattern INLINE_INDEX = Pattern
			.compile("(?i)^(UNIQUE KEY|UNIQUE INDEX|KEY|INDEX) (\\w+)\\s*\\(([^)]*)\\)");
	/**
	 * H2 spells a named unique constraint {@code CONSTRAINT <name> UNIQUE (cols)}.
	 */
	private static final Pattern NAMED_CONSTRAINT = Pattern
			.compile("(?i)^CONSTRAINT (\\w+)\\s+UNIQUE\\s*\\(([^)]*)\\)");
	private static final Pattern COLUMN = Pattern.compile("(?i)^`?(\\w+)`?\\s+([A-Za-z]+)(?:\\([^)]*\\))?\\s*(.*?),?$");

	private static final class Schema {
		private final Map<String, List<String>> tables = new LinkedHashMap<>();
		private final Map<String, String> indexes = new LinkedHashMap<>();
		private final Map<String, String> indexTable = new LinkedHashMap<>();
	}

	private static Schema parse(Path file) throws IOException {
		Schema schema = new Schema();
		String table = null;
		for (String raw : Files.readAllLines(file, java.nio.charset.StandardCharsets.UTF_8)) {
			String line = raw.trim();
			if (line.isEmpty() || line.startsWith("--")) {
				continue;
			}

			Matcher createTable = CREATE_TABLE.matcher(line);
			if (createTable.matches()) {
				table = createTable.group(1);
				schema.tables.put(table, new ArrayList<>());
				continue;
			}

			Matcher createIndex = CREATE_INDEX.matcher(line);
			if (createIndex.matches()) {
				String name = createIndex.group(2);
				schema.indexes.put(name, normalizeColumns(createIndex.group(4)));
				schema.indexTable.put(name, createIndex.group(3));
				continue;
			}

			if (table == null) {
				continue;
			}
			if (line.startsWith(")")) {
				table = null;
				continue;
			}

			Matcher inline = INLINE_INDEX.matcher(line);
			if (inline.find()) {
				schema.indexes.put(inline.group(2), normalizeColumns(inline.group(3)));
				schema.indexTable.put(inline.group(2), table);
				continue;
			}

			Matcher named = NAMED_CONSTRAINT.matcher(line);
			if (named.find()) {
				schema.indexes.put(named.group(1), normalizeColumns(named.group(2)));
				schema.indexTable.put(named.group(1), table);
				continue;
			}

			Matcher column = COLUMN.matcher(line);
			if (column.matches() && !isConstraintKeyword(column.group(1))) {
				schema.tables.get(table).add(column.group(1));
			}
		}
		return schema;
	}

	private static String normalizeColumns(String columns) {
		List<String> parts = new ArrayList<>();
		for (String part : columns.split(",")) {
			parts.add(part.trim().replace("`", "").toLowerCase());
		}
		return String.join(",", parts);
	}

	private static boolean isConstraintKeyword(String token) {
		return switch (token.toUpperCase()) {
		case "PRIMARY", "UNIQUE", "KEY", "INDEX", "CONSTRAINT", "FOREIGN", "CHECK" -> true;
		default -> false;
		};
	}
}