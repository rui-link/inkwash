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
package top.ruilink.inkwash.system.service.converter;

import java.time.LocalDateTime;

import top.ruilink.inkwash.system.api.param.NoticeParam;
import top.ruilink.inkwash.system.api.view.NoticeView;
import top.ruilink.inkwash.system.domain.SysNotice;
import top.ruilink.inkwash.system.enums.NoticeStatus;
import top.ruilink.inkwash.system.enums.NoticeType;
import top.ruilink.inkwash.security.util.SecurityUtil;

/**
 * Notice entity to view and notice param to entity converter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class NoticeConverter {
	private NoticeConverter() {
	}

	public static NoticeView toView(SysNotice notice) {
		if (notice == null)
			return null;
		NoticeView view = new NoticeView();
		view.setId(notice.getId());
		view.setTitle(notice.getTitle());
		view.setType(notice.getType());
		view.setContent(notice.getContent());
		view.setStatus(notice.getStatus());
		view.setRecipientId(notice.getRecipientId());
		view.setReadTime(notice.getReadTime());
		if (notice.getRecipientId() != null) {
			view.setReadStatus(
					notice.getReadTime() != null ? NoticeStatus.READ.getCode() : NoticeStatus.UNREAD.getCode());
		}
		view.setCreator(notice.getCreator());
		view.setUpdater(notice.getUpdater());
		view.setCreateTime(notice.getCreateTime());
		view.setUpdateTime(notice.getUpdateTime());
		return view;
	}

	public static SysNotice toEntity(NoticeParam param) {
		if (param == null)
			return null;
		SysNotice notice = new SysNotice();
		notice.setTitle(param.getTitle());
		notice.setType(param.getType() != null && !param.getType().isBlank()
				? NoticeType.fromCode(Integer.parseInt(param.getType()))
				: null);
		notice.setContent(param.getContent());
		notice.setStatus(param.getStatus());
		notice.setCreator(SecurityUtil.getCurrentUserId());
		notice.setUpdater(SecurityUtil.getCurrentUserId());
		notice.setCreateTime(LocalDateTime.now());
		notice.setUpdateTime(LocalDateTime.now());
		return notice;
	}

	public static void updateEntity(SysNotice notice, NoticeParam param) {
		if (notice == null || param == null)
			return;
		if (param.getTitle() != null)
			notice.setTitle(param.getTitle());
		if (param.getContent() != null)
			notice.setContent(param.getContent());
		if (param.getStatus() != null)
			notice.setStatus(param.getStatus());
		notice.setUpdateTime(LocalDateTime.now());
	}
}
