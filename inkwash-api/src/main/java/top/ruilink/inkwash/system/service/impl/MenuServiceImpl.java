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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.base.util.SortValidator;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import static top.ruilink.inkwash.base.CacheConsts.MENU_TREE_CACHE;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.util.SecurityUtil;
import top.ruilink.inkwash.system.api.param.MenuParam;
import top.ruilink.inkwash.system.api.query.MenuQuery;
import top.ruilink.inkwash.system.api.view.MenuTreeView;
import top.ruilink.inkwash.system.api.view.MenuView;
import top.ruilink.inkwash.system.domain.SysMenu;
import top.ruilink.inkwash.system.service.converter.MenuConverter;
import top.ruilink.inkwash.system.mapper.MenuMapper;
import top.ruilink.inkwash.system.service.MenuService;

/**
 * Menu service implementation.
 * 
 * Responsibilities: business logic, transaction control and business
 * validation.
 * 
 * Conversion is delegated to MenuConverter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class MenuServiceImpl implements MenuService {

	private final MenuMapper menuMapper;
	private final SortValidator sortValidator;

	public MenuServiceImpl(MenuMapper menuMapper, SortValidator sortValidator) {
		this.menuMapper = menuMapper;
		this.sortValidator = sortValidator;
	}

	// ========== Query operations ==========

	@Override
	public SysMenu getById(Long menuId) {
		return menuMapper.selectById(menuId);
	}

	@Override
	public PageResult<MenuView> listMenus(MenuQuery query) {
		var sortSql = sortValidator.resolve(query.getSortField(), query.getSortOrder());
		var offset = (long) (query.getPage() - 1) * query.getSize();
		var total = menuMapper.countMenus(query);
		var list = menuMapper.selectMenuList(query, offset, query.getSize(), sortSql);
		var views = list.stream().map(MenuConverter::toMenuView).toList();
		var pageable = PageUtil.of(query.getPage(), query.getSize());
		var page = new PageImpl<>(views, pageable, total);
		return PageResult.of(page);
	}

	@Override
	@Cacheable(MENU_TREE_CACHE)
	public List<MenuTreeView> getMenuTree() {
		List<SysMenu> allMenus = menuMapper.selectAll();
		return buildMenuTree(allMenus, 0);
	}

	@Override
	public MenuView getMenuDetail(Long menuId) {
		SysMenu menu = menuMapper.selectById(menuId);
		if (menu == null) {
			throw new BusinessException("error.menu.not_found");
		}
		return MenuConverter.toMenuView(menu);
	}

	@Override
	public List<MenuTreeView> getUserMenus() {
		Long userId = SecurityUtil.getCurrentUserId();

		if (userId == null) {
			return List.of();
		}

		List<SysMenu> allMenus = menuMapper.selectAll();
		Set<String> authorities = new HashSet<>(menuMapper.selectUserAuthorities(userId));

		List<SysMenu> menus = allMenus.stream()
				.filter(m -> m.getAuthority() == null || authorities.contains(m.getAuthority())).toList();

		log.debug("[getUserMenus] userId={}, allMenus={}, authorities={}, filteredMenus={}", userId, allMenus.size(),
				authorities, menus.stream().map(m -> m.getId() + ":" + m.getName()).toList());

		List<MenuTreeView> tree = buildMenuTree(menus, 0);
		log.debug("[getUserMenus] treeSize={}", tree.size());
		return pruneDeadGroups(tree);
	}

	/**
	 * Removes directory nodes left with no accessible children after filtering,
	 * meaning no component and no children, so an authenticated user never sees
	 * menu entries they cannot access.
	 */
	private List<MenuTreeView> pruneDeadGroups(List<MenuTreeView> nodes) {
		if (nodes == null || nodes.isEmpty()) {
			return nodes;
		}
		List<MenuTreeView> result = new ArrayList<>(nodes.size());
		for (MenuTreeView node : nodes) {
			List<MenuTreeView> children = pruneDeadGroups(node.getChildren());
			node.setChildren(children);
			boolean isGroup = node.getComponent() == null || node.getComponent().isBlank();
			if (isGroup && (children == null || children.isEmpty())) {
				log.debug("[pruneDeadGroups] drop dead group menu, id={}, name={}", node.getId(), node.getName());
				continue;
			}
			result.add(node);
		}
		return result;
	}

	// ========== Write operations ==========

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = MENU_TREE_CACHE, allEntries = true)
	public MenuView createMenu(MenuParam param) {
		// Create the entity through the converter
		SysMenu menu = MenuConverter.toMenuEntity(param);

		// Persist, with MyBatis writing the generated key back into the entity id
		menuMapper.create(menu);

		log.info("创建菜单成功, name={}", param.getName());
		return MenuConverter.toMenuView(menu);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = MENU_TREE_CACHE, allEntries = true)
	public MenuView updateMenu(Long menuId, MenuParam param) {
		// Load the existing menu
		SysMenu menu = menuMapper.selectById(menuId);
		if (menu == null) {
			throw new BusinessException("error.menu.not_found");
		}

		// Update the entity through the converter
		MenuConverter.updateMenuEntity(menu, param);

		// Persist
		menuMapper.update(menu);

		log.info("更新菜单成功, menuId={}", menuId);
		return MenuConverter.toMenuView(menu);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = MENU_TREE_CACHE, allEntries = true)
	public void deleteMenu(Long menuId) {
		SysMenu menu = menuMapper.selectById(menuId);
		if (menu == null) {
			throw new BusinessException("error.menu.not_found");
		}

		deleteMenuWithChildren(menuId);
		log.info("删除菜单成功, menuId={}", menuId);
	}

	private void deleteMenuWithChildren(Long menuId) {
		List<SysMenu> children = menuMapper.selectByParentId(menuId.intValue());
		for (SysMenu child : children) {
			deleteMenuWithChildren(child.getId().longValue());
		}
		menuMapper.deleteByParentId(menuId.intValue());
		menuMapper.delete(menuId);
	}

	/**
	 * Builds the menu tree.
	 */
	private List<MenuTreeView> buildMenuTree(List<SysMenu> allMenus, int parentId) {
		Map<Integer, List<SysMenu>> childrenMap = allMenus.stream().filter(menu -> menu.getParentId() != null)
				.collect(Collectors.groupingBy(SysMenu::getParentId));
		log.debug("[buildMenuTree] childrenMap keys={}, each child count={}", childrenMap.keySet(),
				childrenMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
						e -> e.getValue().stream().map(SysMenu::getName).toList())));
		return buildMenuTreeLevel(allMenus, childrenMap, parentId);
	}

	private List<MenuTreeView> buildMenuTreeLevel(List<SysMenu> allMenus, Map<Integer, List<SysMenu>> childrenMap,
			int parentId) {
		List<SysMenu> children;
		if (parentId == 0) {
			children = allMenus.stream().filter(menu -> menu.getParentId() == null || menu.getParentId() == 0).toList();
		} else {
			children = childrenMap.getOrDefault(parentId, List.of());
		}
		return children.stream().map(menu -> {
			List<MenuTreeView> subChildren = buildMenuTreeLevel(allMenus, childrenMap, menu.getId().intValue());
			return MenuConverter.toMenuTreeView(menu, subChildren);
		}).collect(Collectors.toList());
	}
}
