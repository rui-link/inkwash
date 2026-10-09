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
package top.ruilink.inkwash.monitor.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.monitor.api.query.LoginInfoQuery;
import top.ruilink.inkwash.monitor.api.view.LoginInfoView;
import top.ruilink.inkwash.monitor.service.LoginInfoService;

/**
 * REST controller for login logs.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@OperateTrace(module = "监控")
@Validated
@RestController
@RequestMapping("/api/monitor/logins")
public class LoginInfoController {

	private final LoginInfoService loginInfoService;

	public LoginInfoController(LoginInfoService loginInfoService) {
		this.loginInfoService = loginInfoService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('monitor:login:query')")
	public ResponseEntity<PageResult<LoginInfoView>> list(LoginInfoQuery query) {
		return ResponseEntity.ok(loginInfoService.listLoginInfos(query));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('monitor:login:query')")
	public ResponseEntity<LoginInfoView> getById(@PathVariable Long id) {
		var entity = loginInfoService.getById(id);
		return ResponseEntity.ok(top.ruilink.inkwash.monitor.service.converter.LoginInfoConverter.toView(entity));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('monitor:login:manage')")
	public ResponseEntity<Void> deleteById(@PathVariable Long id) {
		loginInfoService.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/delete/batch")
	@PreAuthorize("hasAuthority('monitor:login:manage')")
	public ResponseEntity<Void> deleteByIds(@RequestBody List<Long> ids) {
		loginInfoService.deleteByIds(ids);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/clear")
	@PreAuthorize("hasAuthority('monitor:login:manage')")
	public ResponseEntity<Void> clear() {
		loginInfoService.deleteAll();
		return ResponseEntity.noContent().build();
	}
}
