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
import java.util.List;

import top.ruilink.inkwash.system.api.param.MenuParam;
import top.ruilink.inkwash.system.api.view.MenuTreeView;
import top.ruilink.inkwash.system.api.view.MenuView;
import top.ruilink.inkwash.system.domain.SysMenu;

/**
 * Menu entity to view or tree, menu param to entity converter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class MenuConverter {
	private MenuConverter() {
	}

	public static MenuView toMenuView(SysMenu menu) {
		if (menu == null)
			return null;
		MenuView view = new MenuView();
		view.setId(menu.getId());
		view.setName(menu.getName());
		view.setParentId(menu.getParentId());
		view.setIcon(menu.getIcon());
		view.setPath(menu.getPath());
		view.setComponent(menu.getComponent());
		view.setRemark(menu.getRemark());
		view.setCreateTime(menu.getCreateTime());
		view.setSort(menu.getSort());
		view.setStatus(menu.getStatus());
		view.setType(menu.getType());
		return view;
	}

	public static MenuTreeView toMenuTreeView(SysMenu menu, List<MenuTreeView> children) {
		if (menu == null)
			return null;
		MenuTreeView view = new MenuTreeView();
		view.setId(menu.getId());
		view.setName(menu.getName());
		view.setParentId(menu.getParentId());
		view.setIcon(menu.getIcon());
		view.setPath(menu.getPath());
		view.setComponent(menu.getComponent());
		view.setSort(menu.getSort());
		view.setType(menu.getType());
		view.setStatus(menu.getStatus());
		view.setVisible(menu.getVisible() != null ? menu.getVisible() : true);
		view.setAuthority(menu.getAuthority());
		view.setChildren(children);
		return view;
	}

	public static SysMenu toMenuEntity(MenuParam param) {
		if (param == null)
			return null;
		SysMenu menu = new SysMenu();
		menu.setName(param.getName());
		menu.setTitle(param.getTitle());
		menu.setType(param.getType());
		menu.setStatus(param.getStatus());
		menu.setParentId(param.getParentId());
		menu.setIcon(param.getIcon());
		menu.setPath(param.getPath());
		menu.setComponent(param.getComponent());
		menu.setSort(param.getSort());
		menu.setAuthority(param.getAuthority());
		menu.setVisible(param.getVisible());
		menu.setKeepAlive(param.getKeepAlive());
		menu.setRedirect(param.getRedirect());
		menu.setRemark(param.getRemark());
		menu.setCreateTime(LocalDateTime.now());
		menu.setUpdateTime(LocalDateTime.now());
		return menu;
	}

	public static void updateMenuEntity(SysMenu menu, MenuParam param) {
		if (menu == null || param == null)
			return;
		if (param.getName() != null)
			menu.setName(param.getName());
		if (param.getTitle() != null)
			menu.setTitle(param.getTitle());
		if (param.getType() != null)
			menu.setType(param.getType());
		if (param.getStatus() != null)
			menu.setStatus(param.getStatus());
		if (param.getParentId() != null)
			menu.setParentId(param.getParentId().intValue());
		if (param.getIcon() != null)
			menu.setIcon(param.getIcon());
		if (param.getPath() != null)
			menu.setPath(param.getPath());
		if (param.getComponent() != null)
			menu.setComponent(param.getComponent());
		if (param.getSort() != null)
			menu.setSort(param.getSort());
		if (param.getAuthority() != null)
			menu.setAuthority(param.getAuthority());
		if (param.getVisible() != null)
			menu.setVisible(param.getVisible());
		if (param.getKeepAlive() != null)
			menu.setKeepAlive(param.getKeepAlive());
		if (param.getRedirect() != null)
			menu.setRedirect(param.getRedirect());
		if (param.getRemark() != null)
			menu.setRemark(param.getRemark());
		menu.setUpdateTime(LocalDateTime.now());
	}
}
