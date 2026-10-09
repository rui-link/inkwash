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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import top.ruilink.inkwash.monitor.api.view.AuditView;
import top.ruilink.inkwash.monitor.service.AuditService;
import top.ruilink.inkwash.monitor.service.JournalService;
import top.ruilink.inkwash.monitor.service.LoginInfoService;

/**
 * Audit dashboard service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Service
public class AuditServiceImpl implements AuditService {

	private final ApplicationContext applicationContext;
	private final LoginInfoService loginInfoService;
	private final JournalService journalService;

	public AuditServiceImpl(ApplicationContext applicationContext, LoginInfoService loginInfoService,
			JournalService journalService) {
		this.applicationContext = applicationContext;
		this.loginInfoService = loginInfoService;
		this.journalService = journalService;
	}

	@Override
	public AuditView getAuditDashboard() {
		AuditView view = new AuditView();
		view.setSystemHealth("UP");
		view.setActiveProfiles(List.of(applicationContext.getEnvironment().getActiveProfiles()));
		view.setUptime(ManagementFactory.getRuntimeMXBean().getUptime());

		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		var heap = new AuditView.MemoryUsage();
		heap.setUsed(memoryBean.getHeapMemoryUsage().getUsed());
		heap.setMax(memoryBean.getHeapMemoryUsage().getMax());
		heap.setCommitted(memoryBean.getHeapMemoryUsage().getCommitted());
		view.setHeapMemory(heap);

		view.setRecentLogins(loginInfoService.listRecent(5).stream().map(li -> {
			var s = new AuditView.LoginSummary();
			s.setId(li.getId());
			s.setIdentity(li.getIdentity());
			s.setStatus(li.getStatus() != null ? li.getStatus().name() : null);
			s.setLoginTime(li.getLoginTime());
			return s;
		}).toList());

		view.setRecentOperations(journalService.listRecent(5).stream().map(j -> {
			var s = new AuditView.JournalSummary();
			s.setId(j.getId());
			s.setOperation(j.getOperation());
			s.setUrl(j.getUrl());
			s.setResult(j.getResult());
			s.setDuration(j.getDuration());
			s.setCreateTime(j.getCreateTime());
			return s;
		}).toList());

		return view;
	}
}
