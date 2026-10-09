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
package top.ruilink.inkwash.base.domain;

import java.time.LocalDate;
import lombok.Data;

/**
 * Statistics aggregation SQL row, keyed by either statDay or yearMonth
 * (mutually exclusive).
 *
 * <p>
 * The bucket column is aliased {@code stat_day} rather than {@code day} because
 * {@code DAY} is a reserved word in H2, which rejects a bare {@code AS day}.
 * {@code map-underscore-to-camel-case} turns {@code stat_day} into this
 * property automatically, so no explicit result mapping is needed.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class StatisticEntry {
	private LocalDate statDay;
	private Integer yearMonth;
	private Integer status;
	private Long authorId;
	private Integer categoryId;
	private long total;
}
