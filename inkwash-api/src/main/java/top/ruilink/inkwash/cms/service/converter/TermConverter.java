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

import top.ruilink.inkwash.cms.api.param.TermParam;
import top.ruilink.inkwash.cms.api.view.TermView;
import top.ruilink.inkwash.cms.domain.Term;

/**
 * Article tag param to entity and entity to view converter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class TermConverter {
	private TermConverter() {
	}

	public static TermView toView(Term term) {
		if (term == null)
			return null;
		TermView view = new TermView();
		view.setId(term.getId());
		view.setName(term.getName());
		view.setSlug(term.getSlug());
		view.setStatus(term.getStatus());
		view.setRemark(term.getRemark());
		view.setCreateTime(term.getCreateTime());
		view.setUpdateTime(term.getUpdateTime());
		return view;
	}

	public static Term toEntity(TermParam param) {
		if (param == null)
			return null;
		Term entity = new Term();
		entity.setName(param.getName());
		entity.setSlug(param.getSlug());
		entity.setStatus(param.getStatus());
		entity.setRemark(param.getRemark());
		return entity;
	}

	public static void updateEntity(Term entity, TermParam param) {
		if (entity == null || param == null)
			return;
		if (param.getName() != null)
			entity.setName(param.getName());
		if (param.getSlug() != null)
			entity.setSlug(param.getSlug());
		if (param.getStatus() != null)
			entity.setStatus(param.getStatus());
		if (param.getRemark() != null)
			entity.setRemark(param.getRemark());
	}
}
