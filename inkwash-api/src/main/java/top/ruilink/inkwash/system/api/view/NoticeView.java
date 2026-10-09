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
package top.ruilink.inkwash.system.api.view;

import java.time.LocalDateTime;
import lombok.Data;
import top.ruilink.inkwash.system.enums.NoticeType;

@Data

/**
 * System notice response view including per-recipient read state.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class NoticeView {

	private Long id;

	private String title;

	private NoticeType type;

	private String content;

	private String status;

	private Long recipientId;

	private java.time.LocalDateTime readTime;

	private Integer readStatus;

	private Long creator;

	private Long updater;

	private LocalDateTime createTime;

	private LocalDateTime updateTime;
}
