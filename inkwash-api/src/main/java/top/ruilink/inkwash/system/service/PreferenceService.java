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
package top.ruilink.inkwash.system.service;

import top.ruilink.inkwash.security.api.param.PreferenceParam;
import top.ruilink.inkwash.security.api.view.PreferenceView;

/**
 * User preference service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface PreferenceService {

	/**
	 * Gets the current user's preferences, returning defaults when no record
	 * exists.
	 */
	PreferenceView getPreference();

	/**
	 * Saves the current user's preferences, updating an existing record and
	 * inserting a new one otherwise.
	 */
	void savePreference(PreferenceParam param);
}
