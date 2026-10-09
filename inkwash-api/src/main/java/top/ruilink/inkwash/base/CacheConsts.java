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
package top.ruilink.inkwash.base;

/**
 * Cache key constants.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface CacheConsts {

	String USER_PERMISSIONS_CACHE = "userPermissions";

	String ROLE_PERMISSIONS_CACHE = "rolePermissions";

	/**
	 * Assembled menu tree.
	 *
	 * <p>
	 * Previously spelled as a bare {@code "menuTree"} literal in two places inside
	 * {@code CacheConfig} — once per cache-manager branch. Two independent literals
	 * for one cache name are a silent-breakage risk: if only one branch were
	 * edited, Caffeine and Redis would end up registering caches under
	 * <em>different</em> names, and {@code @Cacheable("menuTree")} would resolve
	 * against whichever backend happened to be configured.
	 */
	String MENU_TREE_CACHE = "menuTree";

	/**
	 * Active sensitive words, and the automaton built from them.
	 *
	 * <p>
	 * Registered explicitly in both {@code CacheConfig} branches (ISS-016): a
	 * dynamically created Caffeine cache never expires while Redis falls back to
	 * {@code cacheDefaults}, so leaving the name unregistered made the effective
	 * TTL depend on {@code cache.type}.
	 */
	String SENSITIVE_WORDS_CACHE = "sensitive:words";
}
