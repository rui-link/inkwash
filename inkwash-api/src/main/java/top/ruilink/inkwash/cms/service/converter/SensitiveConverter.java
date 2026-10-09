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

import top.ruilink.inkwash.cms.api.param.SensitiveParam;
import top.ruilink.inkwash.cms.api.view.SensitiveView;
import top.ruilink.inkwash.cms.domain.Sensitive;

/**
 * Sensitive word param to entity and entity to view converter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class SensitiveConverter {
	private SensitiveConverter() {
	}

	public static SensitiveView toView(Sensitive entity) {
		if (entity == null)
			return null;
		SensitiveView view = new SensitiveView();
		view.setId(entity.getId());
		view.setWord(entity.getWord());
		view.setType(entity.getType());
		view.setStatus(entity.getStatus());
		view.setRemark(entity.getRemark());
		view.setCreateTime(entity.getCreateTime());
		view.setUpdateTime(entity.getUpdateTime());
		return view;
	}

	public static Sensitive toEntity(SensitiveParam param) {
		if (param == null)
			return null;
		Sensitive entity = new Sensitive();
		entity.setWord(param.getWord());
		entity.setType(param.getType());
		entity.setStatus(param.getStatus());
		entity.setRemark(param.getRemark());
		return entity;
	}

	public static void updateEntity(Sensitive entity, SensitiveParam param) {
		if (param == null || entity == null)
			return;
		if (param.getWord() != null)
			entity.setWord(param.getWord());
		if (param.getType() != null)
			entity.setType(param.getType());
		if (param.getStatus() != null)
			entity.setStatus(param.getStatus());
		if (param.getRemark() != null)
			entity.setRemark(param.getRemark());
	}
}
