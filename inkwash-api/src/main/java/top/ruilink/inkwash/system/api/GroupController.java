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

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.system.api.param.GroupParam;
import top.ruilink.inkwash.system.api.query.GroupQuery;
import top.ruilink.inkwash.system.api.view.GroupView;
import top.ruilink.inkwash.system.service.GroupService;

/**
 * User group management API served from /api/system/groups.
 * 
 * Permission code rules: system:group:query lists groups, system:group:detail
 * reads a group's details, system:group:create creates a group,
 * system:group:update edits a group, system:group:delete deletes a group and
 * system:group:assign-role assigns roles.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@OperateTrace(module = "系统管理")
@RestController
@RequestMapping("/api/system/groups")
@Validated
public class GroupController {

	private final GroupService groupService;

	public GroupController(GroupService groupService) {
		this.groupService = groupService;
	}

	/**
	 * Lists groups, requiring system:group:query.
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('system:group:query')")
	public ResponseEntity<PageResult<GroupView>> listGroups(GroupQuery query) {
		var result = groupService.listGroups(query);
		return ResponseEntity.ok(result);
	}

	/**
	 * Gets the group tree, requiring system:group:query.
	 */
	@GetMapping("/tree")
	@PreAuthorize("hasAuthority('system:group:query')")
	public ResponseEntity<List<GroupView>> getGroupTree() {
		List<GroupView> tree = groupService.getGroupTree();
		return ResponseEntity.ok(tree);
	}

	/**
	 * Gets a group's details, requiring system:group:detail.
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('system:group:detail')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<GroupView> getGroup(@PathVariable Integer id) {
		GroupView group = groupService.getGroupDetail(id);
		return ResponseEntity.ok(group);
	}

	/**
	 * Creates a group, requiring system:group:create.
	 */
	@PostMapping
	@PreAuthorize("hasAuthority('system:group:create')")
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<GroupView> createGroup(@Valid @RequestBody GroupParam param) {
		log.info("创建用户组, name={}", param.getName());
		GroupView group = groupService.createGroup(param);
		return ResponseEntity.ok(group);
	}

	/**
	 * Updates a group, requiring system:group:update.
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('system:group:update')")
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<GroupView> updateGroup(@PathVariable Integer id, @Valid @RequestBody GroupParam param) {
		log.info("更新用户组, groupId={}", id);
		GroupView group = groupService.updateGroup(id, param);
		return ResponseEntity.ok(group);
	}

	/**
	 * Deletes a group, requiring system:group:delete.
	 */
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('system:group:delete')")
	public ResponseEntity<Void> deleteGroup(@PathVariable Integer id) {
		log.info("删除用户组, groupId={}", id);
		groupService.deleteGroup(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Assigns roles, requiring system:group:assign-role.
	 */
	@PostMapping("/roles")
	@PreAuthorize("hasAuthority('system:group:assign-role')")
	public ResponseEntity<Void> assignRoles(@Valid @RequestBody AssignRoleParam param) {
		log.info("分配角色到用户组, groupId={}, roleIds={}", param.groupId(), param.roleIds());
		groupService.assignRoles(param.groupId(), param.roleIds());
		return ResponseEntity.ok().build();
	}

	public record AssignRoleParam(@NotNull Integer groupId, @NotNull List<Integer> roleIds) {
	}
}
