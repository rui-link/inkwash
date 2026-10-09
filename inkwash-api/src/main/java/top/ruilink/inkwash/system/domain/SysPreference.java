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

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * User preference domain object, in a one-to-one relation with sys_user.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class SysPreference {
	/** User ID, the primary key */
	private Long userId;
	/** Theme ID, such as sky-blue */
	private String theme;
	/** Locale, such as zh-CN */
	private String language;
	/** Menu style, either left or top */
	private String menuStyle;
	/** Additional options as JSON, such as {"showTabs":true,...} */
	private String options;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;
}
