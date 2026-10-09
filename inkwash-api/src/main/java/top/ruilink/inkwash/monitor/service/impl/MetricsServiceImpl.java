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

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.context.ApplicationContext;

import top.ruilink.inkwash.monitor.api.view.SysHealthView;
import top.ruilink.inkwash.monitor.api.view.SysInfoView;
import top.ruilink.inkwash.monitor.api.view.SysMetricsView;
import top.ruilink.inkwash.monitor.service.MetricsService;
import org.springframework.stereotype.Service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import lombok.extern.slf4j.Slf4j;

/**
 * System metrics service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class MetricsServiceImpl implements MetricsService {

	private final ApplicationContext applicationContext;
	private final DataSource dataSource;

	public MetricsServiceImpl(ApplicationContext applicationContext, DataSource dataSource) {
		this.applicationContext = applicationContext;
		this.dataSource = dataSource;
	}

	@Override
	public SysHealthView getSystemHealth() {
		var environment = applicationContext.getEnvironment();
		return new SysHealthView("UP", environment.getActiveProfiles(), System.currentTimeMillis());
	}

	@Override
	public SysMetricsView getSystemMetrics() {
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		var heap = new SysMetricsView.MemoryUsage(memoryMXBean.getHeapMemoryUsage().getInit(),
				memoryMXBean.getHeapMemoryUsage().getUsed(), memoryMXBean.getHeapMemoryUsage().getMax(),
				memoryMXBean.getHeapMemoryUsage().getCommitted());
		var nonHeap = new SysMetricsView.MemoryUsage(memoryMXBean.getNonHeapMemoryUsage().getInit(),
				memoryMXBean.getNonHeapMemoryUsage().getUsed(), memoryMXBean.getNonHeapMemoryUsage().getMax(),
				memoryMXBean.getNonHeapMemoryUsage().getCommitted());

		OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
		var os = new SysMetricsView.OsInfo(osMXBean.getName(), osMXBean.getVersion(), osMXBean.getArch(),
				osMXBean.getAvailableProcessors(), osMXBean.getSystemLoadAverage());

		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		var runtime = new SysMetricsView.RuntimeInfo(runtimeMXBean.getUptime(), runtimeMXBean.getStartTime(),
				runtimeMXBean.getVmName(), runtimeMXBean.getVmVersion(), runtimeMXBean.getVmVendor());

		var threadMXBean = ManagementFactory.getThreadMXBean();
		var threads = new SysMetricsView.ThreadInfo(threadMXBean.getThreadCount(), threadMXBean.getPeakThreadCount(),
				threadMXBean.getDaemonThreadCount());

		long maxHeap = memoryMXBean.getHeapMemoryUsage().getMax();
		long usedHeap = memoryMXBean.getHeapMemoryUsage().getUsed();
		double heapUsage = maxHeap > 0 ? (double) usedHeap / maxHeap * 100 : 0;

		List<SysMetricsView.GcInfo> gcList = new ArrayList<>();
		for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
			gcList.add(new SysMetricsView.GcInfo(gc.getName(), gc.getCollectionCount(), gc.getCollectionTime()));
		}

		List<SysMetricsView.DiskInfo> diskList = new ArrayList<>();
		for (File root : File.listRoots()) {
			diskList.add(new SysMetricsView.DiskInfo(root.getPath(), root.getTotalSpace(), root.getFreeSpace(),
					root.getUsableSpace()));
		}

		return new SysMetricsView(heap, nonHeap, os, runtime, threads, Math.round(heapUsage * 100.0) / 100.0, gcList,
				diskList, getPoolMetrics(), System.currentTimeMillis());
	}

	private SysMetricsView.PoolInfo getPoolMetrics() {
		try {
			if (dataSource instanceof HikariDataSource hikariDS) {
				HikariPoolMXBean poolMXBean = hikariDS.getHikariPoolMXBean();
				return new SysMetricsView.PoolInfo(poolMXBean.getActiveConnections(), poolMXBean.getIdleConnections(),
						poolMXBean.getThreadsAwaitingConnection(), poolMXBean.getTotalConnections());
			}
		} catch (Exception e) {
			log.warn("获取连接池指标失败", e);
		}
		return new SysMetricsView.PoolInfo(0, 0, 0, 0);
	}

	@Override
	public SysInfoView getSystemInfo() {
		var environment = applicationContext.getEnvironment();
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();

		var app = new SysInfoView.AppInfo(environment.getProperty("spring.application.name", "inkwash"),
				String.join(",", environment.getActiveProfiles()));
		var java = new SysInfoView.JavaInfo(System.getProperty("java.version"), System.getProperty("java.vendor"),
				runtimeMXBean.getVmName());
		var system = new SysInfoView.SystemInfo(osMXBean.getName() + " " + osMXBean.getVersion(),
				osMXBean.getAvailableProcessors());
		return new SysInfoView(app, java, system);
	}
}
