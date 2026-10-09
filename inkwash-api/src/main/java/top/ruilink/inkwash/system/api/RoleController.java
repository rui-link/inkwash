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

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.system.api.param.AssignPermissionParam;
import top.ruilink.inkwash.system.api.param.RoleParam;
import top.ruilink.inkwash.system.api.query.RoleQuery;
import top.ruilink.inkwash.system.api.view.RoleView;
import top.ruilink.inkwash.system.service.RoleService;

/**
 * Role management API served from /api/system/roles.
 * 
 * Permission code rules: system:role:query lists roles, system:role:detail
 * reads a role's details, system:role:create creates a role, system:role:update
 * edits a role, system:role:delete deletes a role and
 * system:role:assign-permission assigns permissions.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@OperateTrace(module = "系统管理")
@Validated
@RestController
@RequestMapping("/api/system/roles")
public class RoleController {

	private final RoleService roleService;

	public RoleController(RoleService roleService) {
		this.roleService = roleService;
	}

	/**
	 * Lists roles, requiring system:role:query.
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('system:role:query')")
	public ResponseEntity<PageResult<RoleView>> listRoles(RoleQuery query) {
		var result = roleService.listRoles(query);
		return ResponseEntity.ok(result);
	}

	/**
	 * Gets a role's details, requiring system:role:detail.
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('system:role:detail')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<RoleView> getRole(@PathVariable Integer id) {
		RoleView role = roleService.getRoleDetail(id);
		return ResponseEntity.ok(role);
	}

	/**
	 * Creates a role, requiring system:role:create.
	 */
	@PostMapping
	@PreAuthorize("hasAuthority('system:role:create')")
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<RoleView> createRole(@Valid @RequestBody RoleParam param) {
		log.info("创建角色, name={}", param.getName());
		RoleView role = roleService.createRole(param);
		return ResponseEntity.ok(role);
	}

	/**
	 * Updates a role, requiring system:role:update.
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('system:role:update')")
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<RoleView> updateRole(@PathVariable Integer id, @Valid @RequestBody RoleParam param) {
		log.info("更新角色, roleId={}", id);
		RoleView role = roleService.updateRole(id, param);
		return ResponseEntity.ok(role);
	}

	/**
	 * Deletes a role, requiring system:role:delete.
	 */
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('system:role:delete')")
	public ResponseEntity<Void> deleteRole(@PathVariable Integer id) {
		log.info("删除角色, roleId={}", id);
		roleService.deleteRole(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Assigns permissions, requiring system:role:assign-permission.
	 */
	@PostMapping("/permissions")
	@PreAuthorize("hasAuthority('system:role:assign-permission')")
	public ResponseEntity<Void> assignPermissions(@Valid @RequestBody AssignPermissionParam param) {
		log.info("分配权限到角色, roleId={}, permissionIds={}", param.getRoleId(), param.getPermIds());
		roleService.assignPermissions(param.getRoleId(), param.getPermIds());
		return ResponseEntity.ok().build();
	}
}
