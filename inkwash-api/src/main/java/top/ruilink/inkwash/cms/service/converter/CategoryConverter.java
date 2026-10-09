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
package top.ruilink.inkwash.cms.service.converter;

import top.ruilink.inkwash.cms.api.param.CategoryParam;
import top.ruilink.inkwash.cms.api.view.CategoryView;
import top.ruilink.inkwash.cms.domain.Category;

/**
 * Category param to entity and entity to view converter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class CategoryConverter {
	private CategoryConverter() {
	}

	public static CategoryView toView(Category category) {
		if (category == null)
			return null;
		CategoryView view = new CategoryView();
		view.setId(category.getId());
		view.setName(category.getName());
		view.setSlug(category.getSlug());
		view.setParentId(category.getParentId());
		view.setLevel(category.getLevel());
		view.setStatus(category.getStatus());
		view.setRemark(category.getRemark());
		view.setCreateTime(category.getCreateTime());
		view.setUpdateTime(category.getUpdateTime());
		return view;
	}

	public static Category toEntity(CategoryParam param) {
		if (param == null)
			return null;
		Category entity = new Category();
		entity.setName(param.getName());
		entity.setSlug(param.getSlug());
		entity.setParentId(param.getParentId());
		entity.setStatus(param.getStatus());
		entity.setRemark(param.getRemark());
		return entity;
	}

	public static void updateEntity(Category entity, CategoryParam param) {
		if (entity == null || param == null)
			return;
		if (param.getName() != null)
			entity.setName(param.getName());
		if (param.getSlug() != null)
			entity.setSlug(param.getSlug());
		if (param.getParentId() != null)
			entity.setParentId(param.getParentId());
		if (param.getStatus() != null)
			entity.setStatus(param.getStatus());
		if (param.getRemark() != null)
			entity.setRemark(param.getRemark());
	}
}
