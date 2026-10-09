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
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import top.ruilink.inkwash.base.annotation.OperateTrace;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.cms.api.param.TermParam;
import top.ruilink.inkwash.cms.api.query.TermQuery;
import top.ruilink.inkwash.cms.api.view.TermView;
import top.ruilink.inkwash.cms.service.TermService;

/**
 * Tag REST endpoints.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@OperateTrace(module = "内容管理")
@RestController
@RequestMapping("/api/cms/terms")
@Validated
public class TermController {

	private final TermService termService;

	public TermController(TermService termService) {
		this.termService = termService;
	}

	@GetMapping
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<PageResult<TermView>> list(@Valid TermQuery query) {
		PageResult<TermView> result = termService.listTerms(query);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('cms:term:detail')")
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<TermView> detail(@PathVariable Long id) {
		TermView result = termService.getTerm(id);
		return ResponseEntity.ok(result);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('cms:term:create')")
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<TermView> create(@Valid @RequestBody TermParam param) {
		TermView result = termService.createTerm(param);
		return ResponseEntity.ok(result);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('cms:term:update')")
	@JsonView(ResultView.Basic.class)
	public ResponseEntity<TermView> update(@PathVariable Long id, @Valid @RequestBody TermParam param) {
		TermView result = termService.updateTerm(id, param);
		return ResponseEntity.ok(result);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('cms:term:delete')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		termService.deleteTerm(id);
		return ResponseEntity.noContent().build();
	}
}
