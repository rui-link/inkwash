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
package top.ruilink.inkwash.cms.service.impl;

import static top.ruilink.inkwash.base.CacheConsts.SENSITIVE_WORDS_CACHE;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageImpl;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.base.util.SortValidator;
import top.ruilink.inkwash.base.util.SensitiveMatcher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.cms.api.param.SensitiveParam;
import top.ruilink.inkwash.cms.api.query.SensitiveQuery;
import top.ruilink.inkwash.cms.api.view.SensitiveView;
import top.ruilink.inkwash.cms.domain.Sensitive;
import top.ruilink.inkwash.cms.mapper.SensitiveMapper;
import top.ruilink.inkwash.cms.service.SensitiveService;
import top.ruilink.inkwash.cms.service.converter.SensitiveConverter;
import top.ruilink.inkwash.security.util.SecurityUtil;

/**
 * Sensitive word dictionary and matching service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Service
public class SensitiveServiceImpl implements SensitiveService {

	private final SortValidator sortValidator;
	private final SensitiveMapper sensitiveMapper;

	public SensitiveServiceImpl(SortValidator sortValidator, SensitiveMapper sensitiveMapper) {
		this.sortValidator = sortValidator;
		this.sensitiveMapper = sensitiveMapper;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = SENSITIVE_WORDS_CACHE, allEntries = true)
	public SensitiveView createSensitive(SensitiveParam param) {
		Sensitive entity = SensitiveConverter.toEntity(param);
		entity.setCreator(SecurityUtil.getCurrentUserId());
		entity.setUpdater(SecurityUtil.getCurrentUserId());
		sensitiveMapper.insert(entity);
		return SensitiveConverter.toView(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = SENSITIVE_WORDS_CACHE, allEntries = true)
	public SensitiveView updateSensitive(Long id, SensitiveParam param) {
		Sensitive entity = sensitiveMapper.selectById(id);
		if (entity == null) {
			throw new BusinessException("error.sensitive.not_found");
		}
		SensitiveConverter.updateEntity(entity, param);
		entity.setUpdater(SecurityUtil.getCurrentUserId());
		sensitiveMapper.update(entity);
		return SensitiveConverter.toView(entity);
	}

	@Override
	@CacheEvict(value = SENSITIVE_WORDS_CACHE, allEntries = true)
	@Transactional(rollbackFor = Exception.class)
	public void deleteSensitive(Long id) {
		sensitiveMapper.deleteById(id);
	}

	@Override
	public SensitiveView getSensitive(Long id) {
		Sensitive entity = sensitiveMapper.selectById(id);
		if (entity == null) {
			throw new BusinessException("error.sensitive.not_found");
		}
		return SensitiveConverter.toView(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<SensitiveView> listAll(SensitiveQuery query) {
		var sortSql = sortValidator.resolve(query.getSortField(), query.getSortOrder());
		var offset = (long) (query.getPage() - 1) * query.getSize();
		var total = sensitiveMapper.countSensitives(query);
		var list = sensitiveMapper.selectSensitiveList(query, offset, query.getSize(), sortSql);
		var views = list.stream().map(SensitiveConverter::toView).toList();
		var pageable = PageUtil.of(query.getPage(), query.getSize());
		var page = new PageImpl<>(views, pageable, total);
		return PageResult.of(page);
	}

	@Override
	@Cacheable(value = SENSITIVE_WORDS_CACHE, unless = "#result.isEmpty()")
	public List<String> getAllActiveWords() {
		return sensitiveMapper.selectAllActive().stream().map(Sensitive::getWord).toList();
	}

	/**
	 * Returns the automaton for the current word list, served from the cache.
	 *
	 * <p>
	 * Previously a hand-rolled {@code volatile} field guarded by
	 * {@code synchronized}, invalidated through a separate
	 * {@link #invalidateMatcher()} call. That created two independent caching
	 * mechanisms with different lifetimes: the word list went through the
	 * {@code CacheManager} (shared under Redis) while the automaton lived in a
	 * per-JVM field that {@code @CacheEvict} could not reach. In a multi-instance
	 * deployment an eviction on one node left every other node serving a stale
	 * automaton (ISS-027).
	 *
	 * <p>
	 * Caching the automaton through the same {@code CacheManager} gives both
	 * entries one TTL and one eviction path, and lets
	 * {@code @CacheEvict(allEntries = true)} on the mutators clear them together.
	 *
	 * <p>
	 * {@code SensitiveMatcher} is immutable and thread-safe, so the default
	 * {@code cacheManager.get(key, Callable)} path is safe.
	 */
	@Override
	@Cacheable(value = SENSITIVE_WORDS_CACHE, key = "'matcher'", unless = "#result == null || #result.size() == 0")
	public SensitiveMatcher getMatcher() {
		return SensitiveMatcher.build(getAllActiveWords());
	}
}
