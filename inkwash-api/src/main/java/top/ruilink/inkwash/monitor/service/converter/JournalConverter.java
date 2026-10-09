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
package top.ruilink.inkwash.monitor.service.converter;

import top.ruilink.inkwash.monitor.api.view.JournalView;
import top.ruilink.inkwash.monitor.domain.Journal;

/**
 * Operation journal entity to response view converter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class JournalConverter {
	private JournalConverter() {
	}

	public static JournalView toView(Journal entity) {
		if (entity == null) {
			return null;
		}
		JournalView view = new JournalView();
		view.setId(entity.getId());
		view.setUserId(entity.getUserId());
		view.setUserName(entity.getUserName());
		view.setModule(entity.getModule());
		view.setOperation(entity.getOperation());
		view.setUrl(entity.getUrl());
		view.setMethod(entity.getMethod());
		view.setParam(entity.getParam());
		view.setResult(entity.getResult());
		view.setFailReason(entity.getFailReason());
		view.setIp(entity.getIp());
		view.setDuration(entity.getDuration());
		view.setCreateTime(entity.getCreateTime());
		return view;
	}
}
