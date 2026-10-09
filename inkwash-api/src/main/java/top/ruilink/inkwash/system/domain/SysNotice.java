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
package top.ruilink.inkwash.system.domain;

import lombok.Getter;
import lombok.Setter;
import top.ruilink.inkwash.base.domain.BaseEntity;
import top.ruilink.inkwash.system.enums.NoticeType;

/**
 * Announcement and notification domain entity.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class SysNotice extends BaseEntity {
	private static final long serialVersionUID = 4247794133337997900L;

	private Long id;

	/** Notice title */
	private String title;

	/** Notice type, where 1 is a notification and 2 is an announcement */
	private NoticeType type;

	/** Notice content */
	private String content;

	/** Notice status, where 0 is normal and 1 is closed */
	private String status;

	/** Recipient user ID, where null means a broadcast announcement */
	private Long recipientId;

	/** Read time, where null means unread and only targeted messages use it */
	private java.time.LocalDateTime readTime;

}
