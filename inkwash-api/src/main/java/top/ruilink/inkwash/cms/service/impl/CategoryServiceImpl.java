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
package top.ruilink.inkwash.cms.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.base.util.SortValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.util.SlugUtil;
import top.ruilink.inkwash.base.exception.NotFoundException;
import top.ruilink.inkwash.cms.api.param.CategoryParam;
import top.ruilink.inkwash.cms.api.query.CategoryQuery;
import top.ruilink.inkwash.cms.api.view.CategoryView;
import top.ruilink.inkwash.cms.domain.Category;
import top.ruilink.inkwash.cms.service.converter.CategoryConverter;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CategoryMapper;
import top.ruilink.inkwash.cms.service.CategoryService;

/**
 * Category management service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Service
public class CategoryServiceImpl implements CategoryService {

	private final CategoryMapper categoryMapper;
	private final ArticleMapper articleMapper;
	private final SortValidator sortValidator;

	public CategoryServiceImpl(CategoryMapper categoryMapper, ArticleMapper articleMapper,
			SortValidator sortValidator) {
		this.categoryMapper = categoryMapper;
		this.articleMapper = articleMapper;
		this.sortValidator = sortValidator;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<CategoryView> listCategories(CategoryQuery query) {
		var sortSql = sortValidator.resolve(query.getSortField(), query.getSortOrder());
		var offset = (long) (query.getPage() - 1) * query.getSize();
		var total = categoryMapper.countCategories(query);
		var list = categoryMapper.selectCategoryList(query, offset, query.getSize(), sortSql);
		var views = list.stream().map(CategoryConverter::toView).toList();
		var pageable = PageUtil.of(query.getPage(), query.getSize());
		var page = new PageImpl<>(views, pageable, total);
		return PageResult.of(page);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CategoryView> listCategoryTree() {
		List<Category> all = categoryMapper.selectAll();
		return buildTree(all);
	}

	private List<CategoryView> buildTree(List<Category> categories) {
		Map<Integer, List<Category>> childrenMap = categories.stream().filter(c -> c.getParentId() != null)
				.collect(Collectors.groupingBy(Category::getParentId));
		List<CategoryView> roots = new ArrayList<>();
		for (Category cat : categories) {
			if (cat.isTopLevel()) {
				roots.add(toTreeView(cat, childrenMap));
			}
		}
		return roots;
	}

	private CategoryView toTreeView(Category node, Map<Integer, List<Category>> childrenMap) {
		CategoryView view = CategoryConverter.toView(node);
		List<Category> children = childrenMap.getOrDefault(node.getId(), List.of());
		if (!children.isEmpty()) {
			view.setChildren(children.stream().map(c -> toTreeView(c, childrenMap)).toList());
		}
		return view;
	}

	@Override
	@Transactional(readOnly = true)
	public CategoryView getCategory(Long id) {
		Category category = categoryMapper.selectById(id);
		return CategoryConverter.toView(category);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public CategoryView createCategory(CategoryParam param) {
		applySlug(param);
		Category entity = CategoryConverter.toEntity(param);
		entity.setLevel(deriveLevel(entity.getParentId()));
		categoryMapper.insert(entity);
		return CategoryConverter.toView(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public CategoryView updateCategory(Long id, CategoryParam param) {
		Category entity = categoryMapper.selectById(id);
		if (entity == null)
			throw new NotFoundException("分类不存在");
		applySlug(param);
		CategoryConverter.updateEntity(entity, param);
		// Re-derive on update too: changing parent_id moves the node in the tree.
		entity.setLevel(deriveLevel(entity.getParentId()));
		categoryMapper.update(entity);
		return CategoryConverter.toView(entity);
	}

	/**
	 * Depth of a category in the tree: roots are 1, each nesting level adds 1.
	 *
	 * <p>
	 * Mirrors {@code GroupServiceImpl}'s derivation so the two hierarchies behave
	 * the same. An unknown or self-referencing parent falls back to 1 rather than
	 * throwing, matching the group behaviour.
	 */
	private Integer deriveLevel(Integer parentId) {
		if (parentId == null || parentId <= 0) {
			return 1;
		}
		Category parent = categoryMapper.selectById(parentId.longValue());
		if (parent == null) {
			return 1;
		}
		return (parent.getLevel() == null ? 1 : parent.getLevel()) + 1;
	}

	private void applySlug(CategoryParam param) {
		if (StringUtils.hasText(param.getSlug())) {
			param.setSlug(param.getSlug().trim());
		} else {
			String slug = SlugUtil.generate(param.getName());
			param.setSlug(slug.length() > 60 ? slug.substring(0, 60) : slug);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteCategory(Long id) {
		if (categoryMapper.countByParentId(id) > 0) {
			throw new BusinessException("error.category.has_children");
		}
		if (articleMapper.countByCategoryId(id.intValue()) > 0) {
			throw new BusinessException("error.category.has_articles");
		}
		categoryMapper.deleteById(id);
	}
}
