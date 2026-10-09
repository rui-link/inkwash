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
package top.ruilink.inkwash.system.service;

import java.util.List;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.system.api.param.NoticeParam;
import top.ruilink.inkwash.system.api.query.NoticeQuery;
import top.ruilink.inkwash.system.api.view.NoticeView;
import top.ruilink.inkwash.system.domain.SysNotice;

/**
 * Notice and announcement service.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface NoticeService {

	/**
	 * Queries a page of notices.
	 */
	PageResult<NoticeView> pageSearch(NoticeQuery query);

	/**
	 * Creates an announcement.
	 */
	Long create(NoticeParam param);

	/**
	 * Looks up a notice by ID.
	 */
	SysNotice getById(Long id);

	/**
	 * Updates a notice.
	 */
	void update(NoticeParam param);

	/**
	 * Deletes a notice.
	 */
	void deleteById(Long id);

	/**
	 * Deletes notices in bulk.
	 */
	void deleteByIds(List<Long> ids);

	/**
	 * Gets every active notice and announcement.
	 */
	List<NoticeView> getActiveNotices();

	/**
	 * Gets the messages visible to the current user, both broadcast and targeted.
	 */
	List<NoticeView> listForUser(Long userId);

	/**
	 * The current user's unread message count.
	 */
	long unreadCount(Long userId);

	/**
	 * Marks a given message as read.
	 */
	void markRead(List<Long> ids, Long userId);

	/**
	 * Marks everything as read.
	 */
	void markAllRead(Long userId);

	/**
	 * Deletes a message belonging to the current user.
	 */
	void deleteOwn(List<Long> ids, Long userId);

	/**
	 * Clears every message belonging to the current user.
	 */
	void clearOwn(Long userId);
}
