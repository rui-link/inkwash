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
package top.ruilink.inkwash.system.service.impl;

import java.util.List;

import org.springframework.data.domain.PageImpl;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.base.util.SortValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.system.api.param.NoticeParam;
import top.ruilink.inkwash.system.api.query.NoticeQuery;
import top.ruilink.inkwash.system.api.view.NoticeView;
import top.ruilink.inkwash.system.domain.SysNotice;
import top.ruilink.inkwash.system.mapper.NoticeMapper;
import top.ruilink.inkwash.system.service.NoticeService;
import top.ruilink.inkwash.system.service.converter.NoticeConverter;
import top.ruilink.inkwash.security.util.SecurityUtil;

/**
 * Announcement and per-user message service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class NoticeServiceImpl implements NoticeService {

	private final NoticeMapper noticeMapper;
	private final SortValidator sortValidator;

	public NoticeServiceImpl(NoticeMapper noticeMapper, SortValidator sortValidator) {
		this.noticeMapper = noticeMapper;
		this.sortValidator = sortValidator;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long create(NoticeParam param) {
		SysNotice notice = NoticeConverter.toEntity(param);
		if (notice.getStatus() == null) {
			notice.setStatus("0");
		}
		return noticeMapper.create(notice);
	}

	@Override
	public SysNotice getById(Long id) {
		return noticeMapper.selectById(id);
	}

	@Override
	public PageResult<NoticeView> pageSearch(NoticeQuery query) {
		var sortSql = sortValidator.resolve(query.getSortField(), query.getSortOrder());
		var offset = (long) (query.getPage() - 1) * query.getSize();
		var total = noticeMapper.countNotices(query);
		var list = noticeMapper.selectNoticeList(query, offset, query.getSize(), sortSql);
		var views = list.stream().map(NoticeConverter::toView).toList();
		var pageable = PageUtil.of(query.getPage(), query.getSize());
		var page = new PageImpl<>(views, pageable, total);
		return PageResult.of(page);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(NoticeParam param) {
		SysNotice notice = noticeMapper.selectById(param.getId());
		if (notice == null) {
			throw new BusinessException("error.notice.not_found");
		}
		NoticeConverter.updateEntity(notice, param);
		noticeMapper.update(notice);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteById(Long id) {
		noticeMapper.deleteById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByIds(List<Long> ids) {
		noticeMapper.deleteByIds(ids);
	}

	@Override
	@Transactional(readOnly = true)
	public List<NoticeView> getActiveNotices() {
		Long userId = null;
		try {
			userId = SecurityUtil.getCurrentUserId();
		} catch (BusinessException e) {
			// Not authenticated: broadcasts only
		}
		return noticeMapper.selectForUser(userId).stream().map(NoticeConverter::toView).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<NoticeView> listForUser(Long userId) {
		return noticeMapper.selectForUser(userId).stream().map(NoticeConverter::toView).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public long unreadCount(Long userId) {
		return noticeMapper.unreadCount(userId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void markRead(List<Long> ids, Long userId) {
		if (ids != null && !ids.isEmpty()) {
			noticeMapper.markRead(ids, userId);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void markAllRead(Long userId) {
		noticeMapper.markAllRead(userId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteOwn(List<Long> ids, Long userId) {
		if (ids != null && !ids.isEmpty()) {
			noticeMapper.deleteOwn(ids, userId);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void clearOwn(Long userId) {
		noticeMapper.clearOwn(userId);
	}
}
