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
package top.ruilink.inkwash.cms.service;

import java.util.List;

import top.ruilink.inkwash.base.enums.StatisticRange;
import top.ruilink.inkwash.cms.api.view.DashboardView;
import top.ruilink.inkwash.cms.api.view.StatisticView;

/**
 * Dashboard statistics service producing per-role figures.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface DashboardStatsService {

	List<StatisticView.ArticleStatItem> getArticleStats(StatisticRange range, Long userId);

	StatisticView.MyArticleStats getMyArticleStats(StatisticRange range, Long userId);

	List<StatisticView.UserStatItem> getUserStats(StatisticRange range, Long userId);

	List<StatisticView.CategoryStatItem> getCategoryStats(StatisticRange range, Long userId);

	DashboardView.DashboardSummary getSummary(Long userId);
}
