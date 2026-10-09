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
package top.ruilink.inkwash.monitor.api.view;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Operation journal response view.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class JournalView {
	private Long id;
	private Long userId;
	private String userName;
	private String module;
	private String operation;
	private String url;
	private String method;
	private String param;
	private Integer result;
	private String failReason;
	private String ip;
	private Long duration;
	private LocalDateTime createTime;
}
