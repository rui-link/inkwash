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

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.monitor.api.query.JournalQuery;
import top.ruilink.inkwash.monitor.api.view.JournalView;
import top.ruilink.inkwash.monitor.service.JournalService;

/**
 * Operation journal REST endpoints.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Validated
@RestController
@RequestMapping("/api/monitor/journals")
public class JournalController {

	private final JournalService journalService;

	public JournalController(JournalService journalService) {
		this.journalService = journalService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('monitor:journal:query')")
	public ResponseEntity<PageResult<JournalView>> queryPage(@Valid JournalQuery param) {
		return ResponseEntity.ok(journalService.queryPage(param));
	}
}
