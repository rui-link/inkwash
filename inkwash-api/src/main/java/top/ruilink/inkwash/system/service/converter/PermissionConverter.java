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

import top.ruilink.inkwash.system.api.param.PermissionParam;
import top.ruilink.inkwash.system.api.view.PermissionView;
import top.ruilink.inkwash.system.domain.SysPermission;

/**
 * Permission entity to view and permission param to entity converter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class PermissionConverter {
	private PermissionConverter() {
	}

	public static PermissionView toView(SysPermission permission) {
		if (permission == null)
			return null;
		PermissionView view = new PermissionView();
		view.setId(permission.getId());
		view.setName(permission.getName());
		view.setType(permission.getType());
		view.setAuthority(permission.getAuthority());
		view.setModule(permission.getModule());
		view.setResource(permission.getResource());
		view.setAction(permission.getAction());
		view.setStatus(permission.getStatus());
		view.setRemark(permission.getRemark());
		view.setCreateTime(permission.getCreateTime());
		return view;
	}

	public static SysPermission toEntity(PermissionParam param) {
		if (param == null)
			return null;
		SysPermission permission = new SysPermission();
		permission.setName(param.getName());
		permission.setType(param.getType());
		permission.setModule(param.getModule());
		permission.setResource(param.getResource());
		permission.setAction(param.getAction());
		permission.setRemark(param.getRemark());
		permission.setCreateTime(LocalDateTime.now());
		permission.setUpdateTime(LocalDateTime.now());
		return permission;
	}

	public static void updateEntity(SysPermission permission, PermissionParam param) {
		if (permission == null || param == null)
			return;
		if (param.getName() != null)
			permission.setName(param.getName());
		if (param.getType() != null)
			permission.setType(param.getType());
		if (param.getModule() != null)
			permission.setModule(param.getModule());
		if (param.getResource() != null)
			permission.setResource(param.getResource());
		if (param.getAction() != null)
			permission.setAction(param.getAction());
		if (param.getRemark() != null)
			permission.setRemark(param.getRemark());
		permission.setUpdateTime(LocalDateTime.now());
	}
}
