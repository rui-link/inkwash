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
package top.ruilink.inkwash.security.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.security.api.param.PreferenceParam;
import top.ruilink.inkwash.security.api.view.PreferenceView;
import top.ruilink.inkwash.system.service.PreferenceService;

/**
 * User preference controller.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/preference")
public class PreferenceController {
	private final PreferenceService preferenceService;

	public PreferenceController(PreferenceService preferenceService) {
		this.preferenceService = preferenceService;
	}

	@GetMapping
	public ResponseEntity<PreferenceView> getPreference() {
		return ResponseEntity.ok(preferenceService.getPreference());
	}

	@PutMapping
	public ResponseEntity<Void> savePreference(@Valid @RequestBody PreferenceParam param) {
		preferenceService.savePreference(param);
		return ResponseEntity.ok().build();
	}
}
