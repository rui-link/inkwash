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

import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;
import top.ruilink.inkwash.base.CharConsts;
import top.ruilink.inkwash.base.domain.BaseEntity;
import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.system.enums.MenuType;

/**
 * Permission domain object.
 * 
 * Permission checks use @PreAuthorize, for example
 * hasAuthority('system:user:update').
 * 
 * @author Dyllon
 * @date 2025-10-01
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class SysPermission extends BaseEntity {
	private static final long serialVersionUID = 6324859835978381680L;
	/**
	 * Permission ID.
	 */
	private Integer id;
	/**
	 * Permission name.
	 */
	private String name;
	/**
	 * Permission type.
	 */
	private MenuType type;
	/**
	 * Permission module.
	 */
	private String module;
	/**
	 * Permission resource, optional.
	 */
	private String resource;
	/**
	 * Permission action, optional.
	 */
	private String action;
	/**
	 * Permission status.
	 */
	private BaseStatus status;
	/**
	 * Permission description.
	 */
	private String remark;

	/**
	 * Gets the permission code, generated from module:resource:action.
	 * 
	 * @return the permission code string
	 */
	public String getAuthority() {
		// Generated from module:resource:action
		if (StringUtils.hasText(module) && StringUtils.hasText(resource) && StringUtils.hasText(action)) {
			return module.toLowerCase() + CharConsts.COLON + resource.toLowerCase() + CharConsts.COLON
					+ action.toLowerCase();
		}
		// Returns null or throws when none of them is set
		return null;
	}

	/**
	 * Setting the permission code parses module, resource and action automatically,
	 * so for example system:user:update sets module=system, resource=user and
	 * action=edit.
	 */
	public void setAuthority(String authority) {

		// Parse automatically
		if (StringUtils.hasText(authority) && authority.contains(CharConsts.COLON)) {
			String[] parts = authority.split(CharConsts.COLON);
			if (parts.length >= 3) {
				this.module = parts[0];
				this.resource = parts[1];
				this.action = parts[2];
			} else if (parts.length == 2) {
				this.module = parts[0];
				this.resource = parts[1];
			}
		}
	}
}
