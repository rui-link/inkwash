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
package top.ruilink.inkwash.cms.api.view;

import java.util.List;
import lombok.Data;

/**
 * Time-bucketed dashboard statistic response views.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class StatisticView {

	@Data
	public static class ArticleStatItem {
		private String timeAxis;
		private long created;
		private long pending;
		private long approved;
		private long rejected;
		private long pendingPublish;
		private long published;
	}

	@Data
	public static class UserStatItem {
		private String timeAxis;
		private long newUsers;
		private long activeAuthors;
	}

	@Data
	public static class CategoryStatItem {
		private String timeAxis;
		private List<CategoryCount> categories;
	}

	@Data
	public static class CategoryCount {
		private Integer categoryId;
		private String categoryName;
		private long count;
	}

	@Data
	public static class MyArticleStats {
		private List<ArticleStatItem> items;
		private Long prevCreated;
	}
}
