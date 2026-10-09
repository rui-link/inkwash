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
import top.ruilink.inkwash.system.api.param.MenuParam;
import top.ruilink.inkwash.system.api.query.MenuQuery;
import top.ruilink.inkwash.system.api.view.MenuTreeView;
import top.ruilink.inkwash.system.api.view.MenuView;
import top.ruilink.inkwash.system.domain.SysMenu;

/**
 * Menu service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface MenuService {

	/**
	 * Looks up a menu by ID.
	 */
	SysMenu getById(Long menuId);

	/**
	 * Gets a page of menus.
	 */
	PageResult<MenuView> listMenus(MenuQuery query);

	/**
	 * Gets the menu tree.
	 */
	List<MenuTreeView> getMenuTree();

	/**
	 * Gets a menu's details.
	 */
	MenuView getMenuDetail(Long menuId);

	/**
	 * Gets the menus a user may see, based on their permissions.
	 */
	List<MenuTreeView> getUserMenus();

	/**
	 * Creates a menu.
	 */
	MenuView createMenu(MenuParam param);

	/**
	 * Updates a menu.
	 */
	MenuView updateMenu(Long menuId, MenuParam param);

	/**
	 * Deletes a menu.
	 */
	void deleteMenu(Long menuId);
}
