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
import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.system.enums.MenuType;

/**
 * Menu entity.
 * 
 * @author Dyllon
 * @date 2026-01-01
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class SysMenu extends BaseEntity {
	private static final long serialVersionUID = 8198592289192283592L;

	/**
	 * Menu ID.
	 */
	private Integer id;

	/**
	 * Parent menu ID.
	 */
	private Integer parentId;

	/**
	 * Menu name.
	 */
	private String name;

	/**
	 * Menu display name.
	 */
	private String title;

	/**
	 * Menu type.
	 */
	private MenuType type;

	/**
	 * Route path, the address bar path.
	 */
	private String path;

	/**
	 * Component path, the full page route path.
	 */
	private String component;

	/**
	 * Visibility status.
	 */
	private Boolean visible;

	/**
	 * Redirect path.
	 */
	private String redirect;

	/**
	 * Tree path.
	 */
	private String treePath;

	/**
	 * Whether page caching is enabled.
	 */
	private Boolean keepAlive;

	/**
	 * Sort order.
	 */
	private Integer sort;

	/**
	 * Menu status.
	 */
	private BaseStatus status;

	/**
	 * Permission code.
	 */
	private String authority;

	/**
	 * Menu icon.
	 */
	private String icon;

	/**
	 * User remark.
	 */
	private String remark;
}
