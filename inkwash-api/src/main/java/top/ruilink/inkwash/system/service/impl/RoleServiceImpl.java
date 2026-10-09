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

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageImpl;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.base.util.SortValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.CacheConsts;
import top.ruilink.inkwash.system.api.param.RoleParam;
import top.ruilink.inkwash.system.api.query.RoleQuery;
import top.ruilink.inkwash.system.api.view.RoleView;
import top.ruilink.inkwash.system.domain.SysRole;
import top.ruilink.inkwash.system.service.converter.RoleConverter;
import top.ruilink.inkwash.system.mapper.PermissionMapper;
import top.ruilink.inkwash.system.mapper.RoleMapper;
import top.ruilink.inkwash.system.service.RoleService;

/**
 * Role service implementation.
 * 
 * Responsibilities: business logic, transaction control and business
 * validation.
 * 
 * Conversion is delegated to RoleConverter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

	private final RoleMapper roleMapper;
	private final PermissionMapper permissionMapper;
	private final SortValidator sortValidator;

	public RoleServiceImpl(RoleMapper roleMapper, PermissionMapper permissionMapper, SortValidator sortValidator) {
		this.roleMapper = roleMapper;
		this.permissionMapper = permissionMapper;
		this.sortValidator = sortValidator;
	}

	// ========== Query operations ==========

	@Override
	public SysRole getById(Integer roleId) {
		return roleMapper.selectById(roleId);
	}

	@Override
	public PageResult<RoleView> listRoles(RoleQuery query) {
		var sortSql = sortValidator.resolve(query.getSortField(), query.getSortOrder());
		var offset = (long) (query.getPage() - 1) * query.getSize();
		var total = roleMapper.countRoles(query);
		var list = roleMapper.selectRoleList(query, offset, query.getSize(), sortSql);
		var views = list.stream().map(RoleConverter::toRoleView).toList();
		var pageable = PageUtil.of(query.getPage(), query.getSize());
		var page = new PageImpl<>(views, pageable, total);
		return PageResult.of(page);
	}

	@Override
	public RoleView getRoleDetail(Integer roleId) {
		SysRole role = roleMapper.selectById(roleId);
		if (role == null) {
			throw new BusinessException("error.role.not_found");
		}

		// Load the associated permissions
		var permissions = permissionMapper.selectByRoleId(roleId);

		return RoleConverter.toRoleDetailView(role, permissions);
	}

	// ========== Write operations ==========

	@Override
	@Transactional(rollbackFor = Exception.class)
	public RoleView createRole(RoleParam param) {
		validateRoleParam(param);

		// Create the entity through the converter
		SysRole role = RoleConverter.toRoleEntity(param);

		// Persist, with MyBatis writing the generated key back into the entity id
		roleMapper.create(role);

		log.info("创建角色成功, roleId={}", role.getId());
		return RoleConverter.toRoleView(role);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public RoleView updateRole(Integer roleId, RoleParam param) {
		// Load the existing role
		SysRole role = roleMapper.selectById(roleId);
		if (role == null) {
			throw new BusinessException("error.role.not_found");
		}

		validateRoleParam(param);

		// Update the entity through the converter
		RoleConverter.updateRoleEntity(role, param);

		// Persist
		roleMapper.update(role);

		log.info("更新角色成功, roleId={}", roleId);
		return RoleConverter.toRoleView(role);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteRole(Integer roleId) {
		SysRole role = roleMapper.selectById(roleId);
		if (role == null) {
			throw new BusinessException("error.role.not_found");
		}

		roleMapper.delete(roleId);
		log.info("删除角色成功, roleId={}", roleId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@Caching(evict = { @CacheEvict(value = CacheConsts.USER_PERMISSIONS_CACHE, allEntries = true),
			@CacheEvict(value = CacheConsts.ROLE_PERMISSIONS_CACHE, allEntries = true) })
	public void assignPermissions(Integer roleId, List<Long> permissionIds) {
		// Business validation
		SysRole role = roleMapper.selectById(roleId);
		if (role == null) {
			throw new BusinessException("error.role.not_found");
		}

		// Assign the permissions
		roleMapper.removePermissions(roleId);
		if (permissionIds != null && !permissionIds.isEmpty()) {
			roleMapper.assignPermissions(roleId, permissionIds);
		}

		log.info("分配权限成功, roleId={}, permissionIds={}", roleId, permissionIds);
	}

	private void validateRoleParam(RoleParam param) {
		if (!StringUtils.hasText(param.getName())) {
			throw new BusinessException("error.role.name_required");
		}
		if (!StringUtils.hasText(param.getCode())) {
			throw new BusinessException("error.role.code_required");
		}
		if (!param.getCode().startsWith("ROLE_")) {
			throw new BusinessException("error.role.code_prefix_required");
		}
	}
}
