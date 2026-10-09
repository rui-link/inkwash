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
import top.ruilink.inkwash.system.api.param.RoleParam;
import top.ruilink.inkwash.system.api.query.RoleQuery;
import top.ruilink.inkwash.system.api.view.RoleView;
import top.ruilink.inkwash.system.domain.SysRole;

/**
 * Role service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface RoleService {

	/**
	 * Looks up a role by ID.
	 */
	SysRole getById(Integer roleId);

	/**
	 * Gets a page of roles.
	 */
	PageResult<RoleView> listRoles(RoleQuery query);

	/**
	 * Gets a role's details.
	 */
	RoleView getRoleDetail(Integer roleId);

	/**
	 * Creates a role.
	 */
	RoleView createRole(RoleParam param);

	/**
	 * Updates a role.
	 */
	RoleView updateRole(Integer roleId, RoleParam param);

	/**
	 * Deletes a role.
	 */
	void deleteRole(Integer roleId);

	/**
	 * Assigns permissions.
	 */
	void assignPermissions(Integer roleId, List<Long> permissionIds);
}
