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
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageImpl;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.base.util.SortValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.CharConsts;
import top.ruilink.inkwash.base.CacheConsts;
import top.ruilink.inkwash.system.api.param.PermissionParam;
import top.ruilink.inkwash.system.api.query.PermissionQuery;
import top.ruilink.inkwash.system.api.view.PermissionView;
import top.ruilink.inkwash.system.domain.SysPermission;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.service.converter.PermissionConverter;
import top.ruilink.inkwash.system.mapper.PermissionMapper;
import top.ruilink.inkwash.system.service.PermissionService;

/**
 * Permission service implementation.
 * 
 * Responsibilities: business logic, transaction control and business
 * validation.
 * 
 * Conversion is delegated to PermissionConverter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {

	private final PermissionMapper permissionMapper;
	private final SortValidator sortValidator;

	public PermissionServiceImpl(PermissionMapper permissionMapper, SortValidator sortValidator) {
		this.permissionMapper = permissionMapper;
		this.sortValidator = sortValidator;
	}

	@EventListener(ContextRefreshedEvent.class)
	@Caching(evict = { @CacheEvict(value = CacheConsts.USER_PERMISSIONS_CACHE, allEntries = true),
			@CacheEvict(value = CacheConsts.ROLE_PERMISSIONS_CACHE, allEntries = true) })
	public void evictPermissionCacheOnStartup() {
		log.info("应用启动，清除权限缓存");
	}

	// ========== Query operations ==========

	@Override
	@Cacheable(value = CacheConsts.USER_PERMISSIONS_CACHE, key = "#sysUser.id")
	public Set<SysPermission> findByUser(SysUser sysUser) {
		log.debug("从数据库加载用户权限, userId={}", sysUser.getId());
		return permissionMapper.selectByUserId(sysUser.getId());
	}

	@Override
	@Cacheable(value = CacheConsts.USER_PERMISSIONS_CACHE, key = "#userId")
	public Set<SysPermission> findByUserId(Long userId) {
		log.debug("从数据库加载用户权限, userId={}", userId);
		return permissionMapper.selectByUserId(userId);
	}

	@Override
	public SysPermission getById(Integer permissionId) {
		return permissionMapper.selectById(permissionId);
	}

	@Override
	public PageResult<PermissionView> listPermissions(PermissionQuery query) {
		var sortSql = sortValidator.resolve(query.getSortField(), query.getSortOrder());
		var offset = (long) (query.getPage() - 1) * query.getSize();
		var total = permissionMapper.countPermissions(query);
		var list = permissionMapper.selectPermissionList(query, offset, query.getSize(), sortSql);
		var views = list.stream().map(PermissionConverter::toView).toList();
		var pageable = PageUtil.of(query.getPage(), query.getSize());
		var page = new PageImpl<>(views, pageable, total);
		return PageResult.of(page);
	}

	@Override
	public PermissionView getPermissionDetail(Integer permissionId) {
		SysPermission permission = permissionMapper.selectById(permissionId);
		if (permission == null) {
			throw new BusinessException("error.permission.not_found");
		}
		return PermissionConverter.toView(permission);
	}

	// ========== Write operations ==========

	@Override
	@Transactional(rollbackFor = Exception.class)
	@Caching(evict = { @CacheEvict(value = CacheConsts.USER_PERMISSIONS_CACHE, allEntries = true),
			@CacheEvict(value = CacheConsts.ROLE_PERMISSIONS_CACHE, allEntries = true) })
	public PermissionView createPermission(PermissionParam param) {
		// Business validation: check whether the permission code already exists
		if (permissionMapper.selectByCode(param.getModule(), param.getResource(), param.getAction()) != null) {
			throw new BusinessException("error.permission.code_exists");
		}

		// Create the entity through the converter
		SysPermission permission = PermissionConverter.toEntity(param);

		// Persist, with MyBatis writing the generated key back into the entity id
		permissionMapper.create(permission);

		log.info("创建权限成功, permissionId={}, permissionCode={}", permission.getId(), param);
		return PermissionConverter.toView(permission);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@Caching(evict = { @CacheEvict(value = CacheConsts.USER_PERMISSIONS_CACHE, allEntries = true),
			@CacheEvict(value = CacheConsts.ROLE_PERMISSIONS_CACHE, allEntries = true) })
	public PermissionView updatePermission(Integer permissionId, PermissionParam param) {
		// Load the existing permission
		SysPermission permission = permissionMapper.selectById(permissionId);
		if (permission == null) {
			throw new BusinessException("error.permission.not_found");
		}

		// Business validation: check whether the permission code is taken by another
		// permission
		if (!(param.getModule() + CharConsts.COLON + param.getResource() + CharConsts.COLON + param.getAction())
				.equals(permission.getAuthority())) {
			SysPermission existing = permissionMapper.selectByCode(param.getModule(), param.getResource(),
					param.getAction());
			if (existing != null && !existing.getId().equals(permissionId)) {
				throw new BusinessException("error.permission.code_taken");
			}
		}

		// Update the entity through the converter
		PermissionConverter.updateEntity(permission, param);

		// Persist
		permissionMapper.update(permission);

		log.info("更新权限成功, permissionId={}", permissionId);
		return PermissionConverter.toView(permission);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@Caching(evict = { @CacheEvict(value = CacheConsts.USER_PERMISSIONS_CACHE, allEntries = true),
			@CacheEvict(value = CacheConsts.ROLE_PERMISSIONS_CACHE, allEntries = true) })
	public void deletePermission(Integer permissionId) {
		SysPermission permission = permissionMapper.selectById(permissionId);
		if (permission == null) {
			throw new BusinessException("error.permission.not_found");
		}

		permissionMapper.delete(permissionId);
		log.info("删除权限成功, permissionId={}", permissionId);
	}

	@Override
	public List<String> findRoleCodesByUserId(Long userId) {
		return permissionMapper.selectRoleCodesByUserId(userId);
	}
}
