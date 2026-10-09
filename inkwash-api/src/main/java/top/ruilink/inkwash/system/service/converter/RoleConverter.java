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
package top.ruilink.inkwash.system.service.converter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import top.ruilink.inkwash.system.api.param.RoleParam;
import top.ruilink.inkwash.system.api.view.RoleView;
import top.ruilink.inkwash.system.domain.SysPermission;
import top.ruilink.inkwash.system.domain.SysRole;

/**
 * Role entity to view and role param to entity converter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class RoleConverter {

	private RoleConverter() {
	}

	public static RoleView toRoleView(SysRole role) {
		if (role == null)
			return null;
		RoleView view = new RoleView();
		view.setId(role.getId());
		view.setName(role.getName());
		view.setCode(role.getCode());
		view.setStatus(role.getStatus());
		view.setRemark(role.getRemark());
		view.setCreateTime(role.getCreateTime());
		view.setUpdateTime(role.getUpdateTime());
		return view;
	}

	public static RoleView toRoleDetailView(SysRole role, Set<SysPermission> permissions) {
		if (role == null)
			return null;
		RoleView view = new RoleView();
		view.setId(role.getId());
		view.setName(role.getName());
		view.setCode(role.getCode());
		view.setStatus(role.getStatus());
		view.setRemark(role.getRemark());
		view.setCreateTime(role.getCreateTime());
		view.setUpdateTime(role.getUpdateTime());

		if (permissions != null) {
			view.setPermissions(permissions.stream().map(PermissionConverter::toView).collect(Collectors.toSet()));
		} else {
			view.setPermissions(Set.of());
		}
		return view;
	}

	public static SysRole toRoleEntity(RoleParam param) {
		if (param == null)
			return null;
		SysRole role = new SysRole();
		role.setName(param.getName());
		role.setCode(param.getCode());
		role.setStatus(param.getStatus());
		role.setRemark(param.getRemark());
		role.setCreateTime(LocalDateTime.now());
		role.setUpdateTime(LocalDateTime.now());
		return role;
	}

	public static void updateRoleEntity(SysRole role, RoleParam param) {
		if (role == null || param == null)
			return;
		if (param.getName() != null)
			role.setName(param.getName());
		if (param.getCode() != null)
			role.setCode(param.getCode());
		if (param.getStatus() != null)
			role.setStatus(param.getStatus());
		if (param.getRemark() != null)
			role.setRemark(param.getRemark());
		role.setUpdateTime(LocalDateTime.now());
	}
}
