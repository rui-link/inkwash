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
package top.ruilink.inkwash.security.adapter;

import org.springframework.security.core.GrantedAuthority;

import top.ruilink.inkwash.system.domain.SysPermission;

/**
 * Authority adapter exposing a permission as a Spring Security
 * GrantedAuthority.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class PermissionAuthority implements GrantedAuthority {
	private static final long serialVersionUID = -5606371332234983967L;
	private final SysPermission permission;

	public PermissionAuthority(SysPermission permission) {
		this.permission = permission;
	}

	// Core: return the permission key (module:resource:action)
	@Override
	public String getAuthority() {
		return permission.getAuthority();
	}

	// Returns the underlying domain object
	public SysPermission getPermission() {
		return permission;
	}
}
