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
package top.ruilink.inkwash.monitor.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.monitor.api.query.JournalQuery;
import top.ruilink.inkwash.monitor.api.view.JournalView;
import top.ruilink.inkwash.monitor.domain.Journal;
import top.ruilink.inkwash.monitor.mapper.JournalMapper;
import top.ruilink.inkwash.monitor.service.JournalService;
import top.ruilink.inkwash.monitor.service.converter.JournalConverter;
import top.ruilink.inkwash.monitor.support.AuditLossMetrics;

/**
 * Operation journal service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class JournalServiceImpl implements JournalService {

	private final JournalMapper journalMapper;
	private final AuditLossMetrics auditLossMetrics;

	public JournalServiceImpl(JournalMapper journalMapper, AuditLossMetrics auditLossMetrics) {
		this.journalMapper = journalMapper;
		this.auditLossMetrics = auditLossMetrics;
	}

	/**
	 * Inserts the audit row on the {@code async-} pool.
	 *
	 * <p>
	 * The {@code try/catch} stays: {@code system-design.md} §6.2 requires the audit
	 * write to never affect the business outcome, and it must keep running outside
	 * the business transaction so the row survives a rollback. What was missing
	 * before ISS-042 was any machine-readable signal, so this method now also bumps
	 * {@link AuditLossMetrics} — an operator can alert on a non-zero
	 * {@code inkwash.audit.loss} instead of hoping someone reads the log.
	 *
	 * <p>
	 * The rejection policy is {@code CallerRunsPolicy}, so under saturation the
	 * calling request thread executes this insert inline. That is deliberate
	 * back-pressure — it keeps the audit row rather than dropping it — but it means
	 * a saturated pool also slows the endpoint down. Watch the executor's queue
	 * depth for that; see {@code InvokeInfoConfig}.
	 */
	@Async
	@Override
	public void save(Journal entity) {
		try {
			journalMapper.insert(entity);
		} catch (Exception e) {
			auditLossMetrics.recordWriteLoss();
			log.error("保存操作日志失败, method={}, userId={}", entity.getMethod(), entity.getUserId(), e);
		}
	}

	@Override
	public PageResult<JournalView> queryPage(JournalQuery param) {
		int offset = (param.getPage() - 1) * param.getSize();
		List<Journal> list = journalMapper.selectPage(param.getUsername(), param.getModule(), param.getOperation(),
				param.getResult(), param.getStartTime(), param.getEndTime(), offset, param.getSize());
		long total = journalMapper.count(param.getUsername(), param.getModule(), param.getOperation(),
				param.getResult(), param.getStartTime(), param.getEndTime());
		List<JournalView> views = list.stream().map(JournalConverter::toView).collect(Collectors.toList());
		Page<JournalView> page = PageUtil.toPage(views, param.getPage(), param.getSize(), total);
		return PageResult.of(page);
	}

	@Override
	public List<Journal> listRecent(int limit) {
		return journalMapper.selectRecent(limit);
	}
}
