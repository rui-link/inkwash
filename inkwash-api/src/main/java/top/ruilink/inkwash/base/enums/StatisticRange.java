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
package top.ruilink.inkwash.base.enums;

import top.ruilink.inkwash.base.exception.BusinessException;

/**
 * Statistics time range with unambiguous semantics, rolling for admin and
 * natural for user.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public enum StatisticRange {
	LAST_7D("last7d", StatisticUnit.DAY), LAST_30D("last30d", StatisticUnit.DAY),
	LAST_12M("last12m", StatisticUnit.MONTH), THIS_MONTH("thisMonth", StatisticUnit.WEEK),
	LAST_MONTH("lastMonth", StatisticUnit.WEEK), THIS_YEAR("thisYear", StatisticUnit.MONTH),
	LAST_YEAR("lastYear", StatisticUnit.MONTH);

	private final String value;
	private final StatisticUnit unit;

	StatisticRange(String value, StatisticUnit unit) {
		this.value = value;
		this.unit = unit;
	}

	public String getValue() {
		return value;
	}

	public StatisticUnit getUnit() {
		return unit;
	}

	public static StatisticRange fromValue(String value) {
		for (StatisticRange range : values()) {
			if (range.value.equalsIgnoreCase(value)) {
				return range;
			}
		}
		throw new BusinessException("error.range.invalid", value);
	}
}
