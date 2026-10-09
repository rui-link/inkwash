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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.validation.annotation.Validated;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.monitor.api.view.SysHealthView;
import top.ruilink.inkwash.monitor.api.view.SysInfoView;
import top.ruilink.inkwash.monitor.api.view.SysMetricsView;
import top.ruilink.inkwash.monitor.service.MetricsService;

/**
 * System monitoring REST endpoints.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/monitor/metrics")
public class MetricsController {

	private final MetricsService metricsService;

	public MetricsController(MetricsService metricsService) {
		this.metricsService = metricsService;
	}

	@GetMapping("/health")
	@PreAuthorize("hasAuthority('monitor:system:query')")
	public ResponseEntity<SysHealthView> health() {
		return ResponseEntity.ok(metricsService.getSystemHealth());
	}

	@GetMapping("/metrics")
	@PreAuthorize("hasAuthority('monitor:system:query')")
	public ResponseEntity<SysMetricsView> metrics() {
		return ResponseEntity.ok(metricsService.getSystemMetrics());
	}

	@GetMapping("/info")
	@PreAuthorize("hasAuthority('monitor:system:query')")
	public ResponseEntity<SysInfoView> info() {
		return ResponseEntity.ok(metricsService.getSystemInfo());
	}
}
