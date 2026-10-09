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

import java.util.Map;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonView;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.enums.Gender;
import top.ruilink.inkwash.base.util.EnumUtil;
import top.ruilink.inkwash.system.enums.UserEducation;
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.system.api.param.AssignGroupParam;
import top.ruilink.inkwash.system.api.param.UpdateStatusParam;
import top.ruilink.inkwash.system.api.param.UserParam;
import top.ruilink.inkwash.system.api.query.UserQuery;
import top.ruilink.inkwash.system.api.view.ResetPasswordView;
import top.ruilink.inkwash.system.api.view.UserView;
import top.ruilink.inkwash.system.service.UserService;

/**
 * User management API served from /api/system/users.
 * 
 * Permission code rules: system:user:query lists users, system:user:detail
 * reads a user's details, system:user:create creates a user, system:user:update
 * edits a user, system:user:delete deletes a user, system:user:assign-group
 * assigns groups, system:user:reset-password resets a password and
 * system:user:update-status enables or disables a user.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@OperateTrace(module = "系统管理")
@RestController
@RequestMapping("/api/system/users")
@Validated
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * Lists users, requiring system:user:query.
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('system:user:query')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<PageResult<UserView>> listUsers(@Valid UserQuery query) {
		var result = userService.pageSearch(query);
		return ResponseEntity.ok(result);
	}

	/**
	 * Gets a user's details, requiring system:user:query.
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('system:user:detail')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<UserView> getUser(@PathVariable Long id) {
		UserView user = userService.getUserDetail(id);
		return ResponseEntity.ok(user);
	}

	/**
	 * Gets the total user count, requiring system:user:query.
	 */
	@GetMapping("/count")
	@PreAuthorize("hasAuthority('system:user:query')")
	public ResponseEntity<Map<String, Long>> getUserCount() {
		long count = userService.count();
		return ResponseEntity.ok(Map.of("count", count));
	}

	/**
	 * Creates a user, requiring system:user:create.
	 */
	@PostMapping
	@PreAuthorize("hasAuthority('system:user:create')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<UserView> createUser(@Validated(UserParam.Create.class) @RequestBody UserParam param) {
		log.info("创建用户, nickname={}", param.getNickname());
		UserView user = userService.createUser(param);
		return ResponseEntity.ok(user);
	}

	/**
	 * Updates a user, requiring system:user:update.
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('system:user:update')")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<UserView> updateUser(@PathVariable Long id,
			@Validated(UserParam.Update.class) @RequestBody UserParam param) {
		log.info("更新用户, userId={}", id);
		UserView user = userService.updateUser(id, param);
		return ResponseEntity.ok(user);
	}

	/**
	 * Deletes a user, requiring system:user:delete.
	 */
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('system:user:delete')")
	public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
		log.info("删除用户, userId={}", id);
		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Assigns groups, requiring system:user:assign-group.
	 */
	@PostMapping("/groups")
	@PreAuthorize("hasAuthority('system:user:assign-group')")
	public ResponseEntity<Void> assignGroups(@Valid @RequestBody AssignGroupParam param) {
		log.info("分配用户组, userId={}, groupIds={}", param.getUserId(), param.getGroupIds());
		userService.assignGroups(param.getUserId(), param.getGroupIds());
		return ResponseEntity.ok().build();
	}

	/**
	 * Resets a password, requiring system:user:reset-password.
	 */
	@PostMapping("/{id}/reset-password")
	@PreAuthorize("hasAuthority('system:user:reset-password')")
	public ResponseEntity<ResetPasswordView> resetPassword(@PathVariable Long id) {
		log.info("重置密码, userId={}", id);
		ResetPasswordView view = userService.resetPassword(id);
		return ResponseEntity.ok(view);
	}

	/**
	 * Enables or disables a user, requiring system:user:update-status.
	 */
	@PutMapping("/{id}/status")
	@PreAuthorize("hasAuthority('system:user:update-status')")
	public ResponseEntity<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusParam param) {
		log.info("更新用户状态, userId={}, enabled={}", id, param.getStatus());
		userService.updateStatus(id, param.getStatus());
		return ResponseEntity.ok().build();
	}

	@GetMapping("/gender")
	public ResponseEntity<Map<Object, Object>> listGenderOptions() {
		return ResponseEntity.ok(EnumUtil.listOptions(Gender.class, Gender::getCode, Gender::getName));
	}

	@GetMapping("/education")
	public ResponseEntity<Map<Object, Object>> listEducationOptions() {
		return ResponseEntity
				.ok(EnumUtil.listOptions(UserEducation.class, UserEducation::getCode, UserEducation::getName));
	}
}
