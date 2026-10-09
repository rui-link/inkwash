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
 * Article term entity.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class Term extends BaseEntity {
	private static final long serialVersionUID = 1L;

	/** Term ID */
	private Integer id;

	/** Term name */
	private String name;

	/** URL-friendly term slug */
	private String slug;

	/**
	 * Whether the term is enabled.
	 */
	private BaseStatus status;

	/** Term description */
	private String remark;

	/**
	 * Returns the term URL path used by frontend routing.
	 */
	public String getPath() {
		return "term/" + (slug != null ? slug : id);
	}
}
