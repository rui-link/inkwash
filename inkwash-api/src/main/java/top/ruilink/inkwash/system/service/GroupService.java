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
import top.ruilink.inkwash.system.api.param.GroupParam;
import top.ruilink.inkwash.system.api.query.GroupQuery;
import top.ruilink.inkwash.system.api.view.GroupView;
import top.ruilink.inkwash.system.domain.SysGroup;

/**
 * User group service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface GroupService {

	/**
	 * Looks up a user group by ID.
	 */
	SysGroup getById(Integer groupId);

	/**
	 * Gets a page of user groups.
	 */
	PageResult<GroupView> listGroups(GroupQuery query);

	/**
	 * Gets the user group tree.
	 */
	List<GroupView> getGroupTree();

	/**
	 * Gets a user group's details.
	 */
	GroupView getGroupDetail(Integer groupId);

	/**
	 * Creates a user group.
	 */
	GroupView createGroup(GroupParam param);

	/**
	 * Updates a user group.
	 */
	GroupView updateGroup(Integer groupId, GroupParam param);

	/**
	 * Deletes a user group.
	 */
	void deleteGroup(Integer groupId);

	/**
	 * Assigns roles.
	 */
	void assignRoles(Integer groupId, List<Integer> roleIds);
}
