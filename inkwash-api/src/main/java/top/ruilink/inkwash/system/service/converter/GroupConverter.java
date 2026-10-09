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

import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.system.api.param.GroupParam;
import top.ruilink.inkwash.system.api.view.GroupView;
import top.ruilink.inkwash.system.domain.SysGroup;
import top.ruilink.inkwash.system.domain.SysRole;

/**
 * Group entity to view and group param to entity converter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class GroupConverter {
	private GroupConverter() {
	}

	public static GroupView toView(SysGroup group) {
		if (group == null)
			return null;
		GroupView view = new GroupView();
		view.setId(group.getId());
		view.setName(group.getName());
		view.setParentId(group.getParentId());
		view.setStatus(group.getStatus());
		view.setLevel(group.getLevel());
		view.setRemark(group.getRemark());
		view.setCreateTime(group.getCreateTime());
		return view;
	}

	public static GroupView toView(SysGroup group, Set<SysRole> roles) {
		if (group == null)
			return null;
		GroupView view = new GroupView();
		view.setId(group.getId());
		view.setName(group.getName());
		view.setParentId(group.getParentId());
		view.setStatus(group.getStatus());
		view.setLevel(group.getLevel());
		view.setRemark(group.getRemark());
		view.setCreateTime(group.getCreateTime());
		view.setUpdateTime(group.getUpdateTime());

		if (roles != null) {
			view.setRoles(roles.stream().map(RoleConverter::toRoleView).collect(Collectors.toSet()));
		} else {
			view.setRoles(Set.of());
		}
		return view;
	}

	public static SysGroup toEntity(GroupParam param) {
		if (param == null)
			return null;
		SysGroup group = new SysGroup();
		group.setName(param.getName());
		group.setParentId(param.getParentId());
		group.setStatus(param.getStatus() != null ? param.getStatus() : BaseStatus.ENABLE);
		group.setRemark(param.getRemark());
		group.setCreateTime(LocalDateTime.now());
		group.setUpdateTime(LocalDateTime.now());
		return group;
	}

	public static void updateEntity(SysGroup group, GroupParam param) {
		if (group == null || param == null)
			return;
		if (param.getName() != null)
			group.setName(param.getName());

		if (param.getParentId() != null)
			group.setParentId(param.getParentId().intValue());
		if (param.getStatus() != null)
			group.setStatus(param.getStatus());
		if (param.getRemark() != null)
			group.setRemark(param.getRemark());
		group.setUpdateTime(LocalDateTime.now());
	}
}
