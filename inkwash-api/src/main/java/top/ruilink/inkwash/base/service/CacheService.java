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
package top.ruilink.inkwash.base.service;

import java.time.Duration;

/**
 * Unified cache service interface, backed by Caffeine in dev and Redis in prod.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface CacheService {

	void put(String key, Object value, Duration ttl);

	void put(String key, Object value);

	<T> T get(String key, Class<T> type);

	Object get(String key);

	void evict(String key);

	boolean hasKey(String key);

	void increment(String key, long delta);

	Long getIncrement(String key);
}
