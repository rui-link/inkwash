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

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonView;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.system.api.param.PermissionParam;
import top.ruilink.inkwash.system.api.query.PermissionQuery;
import top.ruilink.inkwash.system.api.view.PermissionView;
import top.ruilink.inkwash.system.service.PermissionService;

/**
 * Permission management API served from /api/system/permissions.
 * 
 * Permission code rules: system:permission:query reads permissions,
 * system:permission:create creates a permission, system:permission:update edits
 * a permission and system:permission:delete deletes a permission.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@OperateTrace(module = "系统管理")
@RestController
@RequestMapping("/api/system/permissions")
@Validated
public class PermissionController {

	private final PermissionService permissionService;

	public PermissionController(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	/**
	 * Lists permissions, requiring system:permission:query.
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('system:permission:query')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<PageResult<PermissionView>> listPermissions(PermissionQuery query) {
		var result = permissionService.listPermissions(query);
		return ResponseEntity.ok(result);
	}

	/**
	 * Gets a permission's details, requiring system:permission:query.
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('system:permission:query')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<PermissionView> getPermission(@PathVariable Integer id) {
		PermissionView permission = permissionService.getPermissionDetail(id);
		return ResponseEntity.ok(permission);
	}

	/**
	 * Creates a permission, requiring system:permission:create.
	 */
	@PostMapping
	@PreAuthorize("hasAuthority('system:permission:create')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<PermissionView> createPermission(@Valid @RequestBody PermissionParam param) {
		log.info("创建权限, permissionCode={}", param);
		PermissionView permission = permissionService.createPermission(param);
		return ResponseEntity.ok(permission);
	}

	/**
	 * Updates a permission, requiring system:permission:update.
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('system:permission:update')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<PermissionView> updatePermission(@PathVariable Integer id,
			@Valid @RequestBody PermissionParam param) {
		log.info("更新权限, permissionId={}", id);
		PermissionView permission = permissionService.updatePermission(id, param);
		return ResponseEntity.ok(permission);
	}

	/**
	 * Deletes a permission, requiring system:permission:delete.
	 */
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('system:permission:delete')")
	public ResponseEntity<Void> deletePermission(@PathVariable Integer id) {
		log.info("删除权限, permissionId={}", id);
		permissionService.deletePermission(id);
		return ResponseEntity.noContent().build();
	}
}
