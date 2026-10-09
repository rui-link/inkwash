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
package top.ruilink.inkwash.system.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonView;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.system.api.param.MenuParam;
import top.ruilink.inkwash.system.api.query.MenuQuery;
import top.ruilink.inkwash.system.api.view.MenuTreeView;
import top.ruilink.inkwash.system.api.view.MenuView;
import top.ruilink.inkwash.system.service.MenuService;

/**
 * Menu management API served from /api/system/menus.
 * 
 * Permission code rules: system:menu:query reads menus, system:menu:create
 * creates a menu, system:menu:update edits a menu and system:menu:delete
 * deletes a menu.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@OperateTrace(module = "系统管理")
@RestController
@RequestMapping("/api/system/menus")
@Validated
public class MenuController {

	private final MenuService menuService;

	public MenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	/**
	 * Lists menus with pagination, requiring system:menu:query.
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('system:menu:query')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<PageResult<MenuView>> listMenus(MenuQuery query) {
		var result = menuService.listMenus(query);
		return ResponseEntity.ok(result);
	}

	/**
	 * Gets the menu tree, requiring system:menu:query.
	 */
	@GetMapping("/tree")
	@PreAuthorize("hasAuthority('system:menu:query')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<List<MenuTreeView>> getMenuTree() {
		List<MenuTreeView> tree = menuService.getMenuTree();
		return ResponseEntity.ok(tree);
	}

	/**
	 * Gets a menu's details, requiring system:menu:query.
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('system:menu:detail')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<MenuView> getMenu(@PathVariable Long id) {
		MenuView menu = menuService.getMenuDetail(id);
		return ResponseEntity.ok(menu);
	}

	/**
	 * Gets the menus visible to the current user based on their permissions; any
	 * authenticated user may call it.
	 */
	@GetMapping("/user")
	@PreAuthorize("isAuthenticated()")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<List<MenuTreeView>> getUserMenus() {
		List<MenuTreeView> menus = menuService.getUserMenus();
		return ResponseEntity.ok(menus);
	}

	/**
	 * Creates a menu, requiring system:menu:create.
	 */
	@PostMapping
	@PreAuthorize("hasAuthority('system:menu:create')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<MenuView> createMenu(@Valid @RequestBody MenuParam param) {
		log.info("创建菜单, name={}", param.getName());
		MenuView menu = menuService.createMenu(param);
		return ResponseEntity.ok(menu);
	}

	/**
	 * Updates a menu, requiring system:menu:update.
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('system:menu:update')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<MenuView> updateMenu(@PathVariable Long id, @Valid @RequestBody MenuParam param) {
		log.info("更新菜单, menuId={}", id);
		MenuView menu = menuService.updateMenu(id, param);
		return ResponseEntity.ok(menu);
	}

	/**
	 * Deletes a menu, requiring system:menu:delete.
	 */
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('system:menu:delete')")
	public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
		log.info("删除菜单, menuId={}", id);
		menuService.deleteMenu(id);
		return ResponseEntity.noContent().build();
	}
}
