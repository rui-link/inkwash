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

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import top.ruilink.inkwash.base.enums.StatisticRange;
import top.ruilink.inkwash.cms.api.view.DashboardView;
import top.ruilink.inkwash.cms.api.view.StatisticView;
import top.ruilink.inkwash.cms.service.DashboardService;
import top.ruilink.inkwash.cms.service.DashboardStatsService;
import top.ruilink.inkwash.security.util.SecurityUtil;

/**
 * Dashboard statistics REST endpoints.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@RestController
@RequestMapping("/api/cms/dashboard")
@Validated
public class DashboardController {
	private final DashboardService dashboardService;
	private final DashboardStatsService dashboardStatsService;

	public DashboardController(DashboardService dashboardService, DashboardStatsService dashboardStatsService) {
		this.dashboardService = dashboardService;
		this.dashboardStatsService = dashboardStatsService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('cms:article:query')")
	public ResponseEntity<?> getDashboard() {
		if (SecurityUtil.isAdmin()) {
			return ResponseEntity.ok(dashboardService.getAdminDashboard());
		}
		if (SecurityUtil.isEditor()) {
			return ResponseEntity.ok(dashboardService.getEditorDashboard());
		}
		return ResponseEntity.ok(dashboardService.getUserDashboard(SecurityUtil.getCurrentUserId()));
	}

	@GetMapping("/article-stats")
	@PreAuthorize("hasAuthority('cms:dashboard:query')")
	public ResponseEntity<List<StatisticView.ArticleStatItem>> getArticleStats(
			@RequestParam(value = "range", defaultValue = "last7d") String range) {
		return ResponseEntity.ok(dashboardStatsService.getArticleStats(StatisticRange.fromValue(range),
				SecurityUtil.getCurrentUserId()));
	}

	@GetMapping("/user-stats")
	@PreAuthorize("hasAuthority('cms:dashboard:user-stats')")
	public ResponseEntity<List<StatisticView.UserStatItem>> getUserStats(
			@RequestParam(value = "range", defaultValue = "last7d") String range) {
		return ResponseEntity.ok(
				dashboardStatsService.getUserStats(StatisticRange.fromValue(range), SecurityUtil.getCurrentUserId()));
	}

	@GetMapping("/category-stats")
	@PreAuthorize("hasAuthority('cms:dashboard:query')")
	public ResponseEntity<List<StatisticView.CategoryStatItem>> getCategoryStats(
			@RequestParam(value = "range", defaultValue = "last7d") String range) {
		return ResponseEntity.ok(dashboardStatsService.getCategoryStats(StatisticRange.fromValue(range),
				SecurityUtil.getCurrentUserId()));
	}

	@GetMapping("/summary")
	@PreAuthorize("hasAuthority('cms:dashboard:query')")
	public ResponseEntity<DashboardView.DashboardSummary> getSummary() {
		return ResponseEntity.ok(dashboardStatsService.getSummary(SecurityUtil.getCurrentUserId()));
	}

	@GetMapping("/my-article-stats")
	@PreAuthorize("hasAuthority('cms:article:query')")
	public ResponseEntity<StatisticView.MyArticleStats> getMyArticleStats(
			@RequestParam(value = "range", defaultValue = "thisMonth") String range) {
		return ResponseEntity.ok(dashboardStatsService.getMyArticleStats(StatisticRange.fromValue(range),
				SecurityUtil.getCurrentUserId()));
	}
}
