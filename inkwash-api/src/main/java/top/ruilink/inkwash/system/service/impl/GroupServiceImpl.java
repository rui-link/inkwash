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
package top.ruilink.inkwash.system.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageImpl;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.base.util.SortValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.CacheConsts;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.system.api.param.GroupParam;
import top.ruilink.inkwash.system.api.query.GroupQuery;
import top.ruilink.inkwash.system.api.view.GroupView;
import top.ruilink.inkwash.system.domain.SysGroup;
import top.ruilink.inkwash.system.domain.SysRole;
import top.ruilink.inkwash.system.mapper.GroupMapper;
import top.ruilink.inkwash.system.mapper.RoleMapper;
import top.ruilink.inkwash.system.service.GroupService;
import top.ruilink.inkwash.system.service.converter.GroupConverter;

/**
 * User group service implementation.
 * 
 * Responsibilities: business logic, transaction control and business
 * validation.
 * 
 * Conversion is delegated to GroupConverter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

	private final GroupMapper groupMapper;
	private final RoleMapper roleMapper;
	private final SortValidator sortValidator;

	public GroupServiceImpl(GroupMapper groupMapper, RoleMapper roleMapper, SortValidator sortValidator) {
		this.groupMapper = groupMapper;
		this.roleMapper = roleMapper;
		this.sortValidator = sortValidator;
	}

	// ========== Query operations ==========

	@Override
	public SysGroup getById(Integer groupId) {
		SysGroup group = groupMapper.selectById(Math.toIntExact(groupId));
		if (group == null) {
			throw new BusinessException("error.group.not_found");
		}
		return group;
	}

	@Override
	public PageResult<GroupView> listGroups(GroupQuery query) {
		var sortSql = sortValidator.resolve(query.getSortField(), query.getSortOrder());
		var offset = (long) (query.getPage() - 1) * query.getSize();
		var total = groupMapper.countGroups(query);
		var list = groupMapper.selectGroupList(query, offset, query.getSize(), sortSql);
		var views = list.stream().map(GroupConverter::toView).toList();
		var pageable = PageUtil.of(query.getPage(), query.getSize());
		var page = new PageImpl<>(views, pageable, total);
		return PageResult.of(page);
	}

	@Override
	public List<GroupView> getGroupTree() {
		List<SysGroup> allGroups = groupMapper.selectAll();

		// Batch load the roles of every group in one query instead of N queries
		List<Integer> allGroupIds = allGroups.stream().map(SysGroup::getId).toList();
		Map<Integer, Set<SysRole>> groupRoleMap = allGroupIds.isEmpty() ? Map.of()
				: roleMapper.selectByGroupIds(allGroupIds).stream()
						.collect(Collectors.groupingBy(SysRole::getGroupId, Collectors.toSet()));

		return buildGroupTree(allGroups, 0, groupRoleMap);
	}

	/**
	 * Builds the user group tree recursively.
	 */
	private List<GroupView> buildGroupTree(List<SysGroup> allGroups, Integer parentId,
			Map<Integer, Set<SysRole>> groupRoleMap) {
		return allGroups.stream().filter(g -> {
			Integer pid = g.getParentId();
			if (parentId == null || parentId == 0) {
				return pid == null || pid == 0;
			}
			return parentId.equals(pid);
		}).map(g -> {
			Set<SysRole> roleSet = groupRoleMap.getOrDefault(g.getId(), Set.of());
			return GroupConverter.toView(g, roleSet);
		}).toList();
	}

	@Override
	public GroupView getGroupDetail(Integer groupId) {
		SysGroup group = groupMapper.selectById(Math.toIntExact(groupId));
		if (group == null) {
			throw new BusinessException("error.group.not_found");
		}

		// Load the associated roles
		var roles = roleMapper.selectByGroupId(group.getId());
		// Convert the List to a Set
		Set<SysRole> roleSet = roles != null ? new HashSet<>(roles) : Set.of();

		return GroupConverter.toView(group, roleSet);
	}

	// ========== Write operations ==========

	@Override
	@Transactional(rollbackFor = Exception.class)
	public GroupView createGroup(GroupParam param) {
		// Create the entity through the converter
		SysGroup group = GroupConverter.toEntity(param);

		// Derive the level from the parent, where the root sits at level 1
		Integer level = 1;
		if (group.getParentId() != null && group.getParentId() > 0) {
			SysGroup parent = groupMapper.selectById(group.getParentId());
			if (parent != null) {
				level = (parent.getLevel() == null ? 1 : parent.getLevel()) + 1;
			}
		}
		group.setLevel(level);

		// Persist, with MyBatis writing the generated key back into the entity id
		groupMapper.create(group);

		log.info("创建用户组成功, groupId={}", group.getId());
		return GroupConverter.toView(group);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public GroupView updateGroup(Integer groupId, GroupParam param) {
		// Load the existing group
		SysGroup group = groupMapper.selectById(Math.toIntExact(groupId));
		if (group == null) {
			throw new BusinessException("error.group.not_found");
		}

		// Update the entity through the converter
		GroupConverter.updateEntity(group, param);

		// Persist
		groupMapper.update(group);

		log.info("更新用户组成功, groupId={}", groupId);
		return GroupConverter.toView(group);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteGroup(Integer groupId) {
		SysGroup group = groupMapper.selectById(Math.toIntExact(groupId));
		if (group == null) {
			throw new BusinessException("error.group.not_found");
		}

		deleteGroupWithChildren(groupId);
		log.info("删除用户组成功, groupId={}", groupId);
	}

	private void deleteGroupWithChildren(Integer groupId) {
		List<SysGroup> children = groupMapper.selectChildren(groupId);
		for (SysGroup child : children) {
			deleteGroupWithChildren(child.getId());
		}
		groupMapper.deleteByParentId(groupId);
		groupMapper.delete(groupId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CacheConsts.USER_PERMISSIONS_CACHE, allEntries = true)
	public void assignRoles(Integer groupId, List<Integer> roleIds) {
		// Business validation
		SysGroup group = groupMapper.selectById(Math.toIntExact(groupId));
		if (group == null) {
			throw new BusinessException("error.group.not_found");
		}

		Integer groupIdInt = Math.toIntExact(groupId);

		// Assign the roles
		roleMapper.removeGroupRoles(groupIdInt);
		if (roleIds != null && !roleIds.isEmpty()) {
			roleMapper.assignGroupRolesBatch(groupIdInt, roleIds);
		}

		log.info("分配角色成功, groupId={}, roleIds={}", groupId, roleIds);
	}
}
