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
package top.ruilink.inkwash.cms.domain;

import lombok.Getter;
import lombok.Setter;
import top.ruilink.inkwash.base.domain.BaseEntity;
import top.ruilink.inkwash.base.enums.BaseStatus;

/**
 * Article category entity supporting a hierarchy.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class Category extends BaseEntity {
	private static final long serialVersionUID = 1L;

	/** Category ID */
	private Integer id;

	/** Category name */
	private String name;

	/** URL-friendly category slug */
	private String slug;

	/** Parent category ID, 0 for a top-level category */
	private Integer parentId;
	/**
	 * Category depth level.
	 */
	private Integer level;
	/**
	 * Whether the category is enabled.
	 */
	private BaseStatus status;

	/** Category description */
	private String remark;

	/** Whether this is a top-level category */
	public boolean isTopLevel() {
		return parentId == null || parentId == 0;
	}
}
