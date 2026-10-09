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
package top.ruilink.inkwash.cms.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import top.ruilink.inkwash.cms.api.param.CategoryParam;
import top.ruilink.inkwash.cms.domain.Category;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CategoryMapper;

/**
 * Locks the {@code cms_category.level} contract introduced by ISS-032.
 *
 * <p>
 * The entity declared a {@code level} field while the table had a {@code sort}
 * column that no query ever selected, so {@code level} was permanently null and
 * {@code CategoryView.level} was always null too. {@code sort} is gone and
 * {@code level} is now a real column, derived from {@code parent_id} on both
 * create and update — mirroring {@code GroupServiceImpl}.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("分类层级 level 派生（ISS-032）")
class CategoryLevelTest {

	@Mock
	private CategoryMapper categoryMapper;
	@Mock
	private ArticleMapper articleMapper;
	@Mock
	private top.ruilink.inkwash.base.util.SortValidator sortValidator;

	@InjectMocks
	private CategoryServiceImpl categoryService;

	@Captor
	private ArgumentCaptor<Category> captor;

	private static CategoryParam param(Integer parentId) {
		CategoryParam param = new CategoryParam();
		param.setName("分类名");
		param.setSlug("slug");
		param.setParentId(parentId);
		return param;
	}

	private Category category(Integer id, Integer parentId, Integer level) {
		Category c = new Category();
		c.setId(id);
		c.setParentId(parentId);
		c.setLevel(level);
		return c;
	}

	@Test
	@DisplayName("根分类的 level 为 1")
	void rootCategoryIsLevelOne() {
		categoryService.createCategory(param(null));

		verify(categoryMapper).insert(captor.capture());
		assertEquals(1, captor.getValue().getLevel());
		verify(categoryMapper, never()).selectById(eq(1L));
	}

	@Test
	@DisplayName("子分类的 level = 父级 + 1")
	void childLevelIsParentPlusOne() {
		when(categoryMapper.selectById(7L)).thenReturn(category(7, null, 1));

		categoryService.createCategory(param(7));

		verify(categoryMapper).insert(captor.capture());
		assertEquals(2, captor.getValue().getLevel());
	}

	@Test
	@DisplayName("多级嵌套逐层累加")
	void nestedLevelsAccumulate() {
		when(categoryMapper.selectById(7L)).thenReturn(category(7, 1, 1));
		when(categoryMapper.selectById(8L)).thenReturn(category(8, 7, 2));

		categoryService.createCategory(param(8));

		verify(categoryMapper).insert(captor.capture());
		assertEquals(3, captor.getValue().getLevel());
	}

	@Test
	@DisplayName("父级 level 为 null 时按 1 处理")
	void nullParentLevelTreatedAsOne() {
		when(categoryMapper.selectById(7L)).thenReturn(category(7, null, null));

		categoryService.createCategory(param(7));

		verify(categoryMapper).insert(captor.capture());
		assertEquals(2, captor.getValue().getLevel());
	}

	@Test
	@DisplayName("父级不存在时回退为 1，不抛异常")
	void unknownParentFallsBackToOne() {
		when(categoryMapper.selectById(99L)).thenReturn(null);

		categoryService.createCategory(param(99));

		verify(categoryMapper).insert(captor.capture());
		assertEquals(1, captor.getValue().getLevel());
	}

	@Test
	@DisplayName("parentId = 0 视为根，不查库")
	void zeroParentIsRoot() {
		categoryService.createCategory(param(0));

		verify(categoryMapper).insert(captor.capture());
		assertEquals(1, captor.getValue().getLevel());
		verify(categoryMapper, never()).selectById(any());
	}

	@Test
	@DisplayName("更新时改父级需重新派生 level")
	void updateRederivesLevelWhenParentChanges() {
		when(categoryMapper.selectById(5L)).thenReturn(category(5, 1, 1));
		when(categoryMapper.selectById(7L)).thenReturn(category(7, null, 1));

		categoryService.updateCategory(5L, param(7));

		verify(categoryMapper).update(captor.capture());
		assertEquals(2, captor.getValue().getLevel(), "换父级后 level 必须重算，否则层级陈旧");
	}

	@Test
	@DisplayName("Mapper 的 insert/update 都持久化 level")
	void mapperPersistsLevel() throws NoSuchMethodException {
		String insert = String.join(" ", CategoryMapper.class.getMethod("insert", Category.class)
				.getAnnotation(org.apache.ibatis.annotations.Insert.class).value());
		String update = String.join(" ", CategoryMapper.class.getMethod("update", Category.class)
				.getAnnotation(org.apache.ibatis.annotations.Update.class).value());

		org.junit.jupiter.api.Assertions.assertTrue(insert.contains("level"), "INSERT 必须写入 level: " + insert);
		org.junit.jupiter.api.Assertions.assertTrue(insert.contains("#{level}"), insert);
		org.junit.jupiter.api.Assertions.assertTrue(update.contains("level=#{level}"), "UPDATE 必须写入 level: " + update);
	}

	@Test
	@DisplayName("Mapper 的读取列含 level，且不再有 sort")
	void mapperSelectsLevelAndNotSort() throws Exception {
		Method selectById = CategoryMapper.class.getMethod("selectById", Long.class);
		String sql = String.join(" ", selectById.getAnnotation(org.apache.ibatis.annotations.Select.class).value());

		org.junit.jupiter.api.Assertions.assertTrue(sql.contains(CategoryMapper.COLUMNS), sql);
		org.junit.jupiter.api.Assertions.assertTrue(CategoryMapper.COLUMNS.contains("level"), "COLUMNS 常量必须含 level");
		org.junit.jupiter.api.Assertions.assertFalse(CategoryMapper.COLUMNS.contains("sort"),
				"sort 列已删除，不应再出现在 COLUMNS 中");
	}
}