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
package top.ruilink.inkwash.cms.api;

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
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.cms.api.param.SensitiveParam;
import top.ruilink.inkwash.cms.api.query.SensitiveQuery;
import top.ruilink.inkwash.cms.api.view.SensitiveView;
import top.ruilink.inkwash.cms.service.SensitiveService;

/**
 * Sensitive word REST endpoints.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@OperateTrace(module = "内容管理")
@RestController
@RequestMapping("/api/cms/sensitive")
@Validated
public class SensitiveController {

	private final SensitiveService sensitiveService;

	public SensitiveController(SensitiveService sensitiveService) {
		this.sensitiveService = sensitiveService;
	}

	@PostMapping
	@PreAuthorize("hasAuthority('cms:sensitive:create')")
	public ResponseEntity<SensitiveView> create(@Valid @RequestBody SensitiveParam param) {
		return ResponseEntity.ok(sensitiveService.createSensitive(param));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('cms:sensitive:update')")
	public ResponseEntity<SensitiveView> update(@PathVariable Long id, @Valid @RequestBody SensitiveParam param) {
		return ResponseEntity.ok(sensitiveService.updateSensitive(id, param));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('cms:sensitive:delete')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		sensitiveService.deleteSensitive(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('cms:sensitive:detail')")
	public ResponseEntity<SensitiveView> detail(@PathVariable Long id) {
		return ResponseEntity.ok(sensitiveService.getSensitive(id));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('cms:sensitive:query')")
	public ResponseEntity<PageResult<SensitiveView>> listAll(@Valid SensitiveQuery query) {
		return ResponseEntity.ok(sensitiveService.listAll(query));
	}
}
