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
import java.util.Set;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.system.api.param.PermissionParam;
import top.ruilink.inkwash.system.api.query.PermissionQuery;
import top.ruilink.inkwash.system.api.view.PermissionView;
import top.ruilink.inkwash.system.domain.SysPermission;
import top.ruilink.inkwash.system.domain.SysUser;

/**
 * Permission service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface PermissionService {

	/**
	 * Aggregates all of a user's permissions along the user to group to role to
	 * permission path.
	 */
	Set<SysPermission> findByUser(SysUser sysUser);

	/**
	 * Queries permissions by user ID.
	 */
	Set<SysPermission> findByUserId(Long userId);

	/**
	 * Looks up a permission by ID.
	 */
	SysPermission getById(Integer permissionId);

	/**
	 * Gets a page of permissions.
	 */
	PageResult<PermissionView> listPermissions(PermissionQuery query);

	/**
	 * Gets a permission's details.
	 */
	PermissionView getPermissionDetail(Integer permissionId);

	/**
	 * Creates a permission.
	 */
	PermissionView createPermission(PermissionParam param);

	/**
	 * Updates a permission.
	 */
	PermissionView updatePermission(Integer permissionId, PermissionParam param);

	/**
	 * Deletes a permission.
	 */
	void deletePermission(Integer permissionId);

	/**
	 * Queries all of a user's role codes along the user to group to role path.
	 */
	List<String> findRoleCodesByUserId(Long userId);
}
