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

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.util.SensitiveMatcher;
import top.ruilink.inkwash.cms.api.param.SensitiveParam;
import top.ruilink.inkwash.cms.api.query.SensitiveQuery;
import top.ruilink.inkwash.cms.api.view.SensitiveView;

/**
 * Sensitive word dictionary and matching service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface SensitiveService {
	SensitiveView createSensitive(SensitiveParam param);

	SensitiveView updateSensitive(Long id, SensitiveParam param);

	void deleteSensitive(Long id);

	SensitiveView getSensitive(Long id);

	PageResult<SensitiveView> listAll(SensitiveQuery query);

	List<String> getAllActiveWords();

	/**
	 * Get an Aho-Corasick matcher pre-built from all active sensitive words.
	 *
	 * <p>
	 * Both this and {@link #getAllActiveWords()} are served from the same
	 * {@code sensitive:words} cache and evicted together by the mutators'
	 * {@code @CacheEvict(allEntries = true)}. The matcher also carries the word
	 * list it was built from, so a caller must resolve {@code findMatches} indices
	 * through {@code SensitiveMatcher.wordAt(int)} rather than pairing them with a
	 * separately fetched list (ISS-015).
	 *
	 * <p>
	 * The explicit {@link #invalidateMatcher()} hook was removed: it existed only
	 * to clear a per-JVM field that {@code @CacheEvict} could not reach, which
	 * meant a multi-instance deployment kept serving stale matchers on every node
	 * but the one that mutated the word list (ISS-027).
	 */
	SensitiveMatcher getMatcher();
}
