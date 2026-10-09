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
package top.ruilink.inkwash.monitor.api.view;

import java.util.List;

/**
 * System runtime metrics response view.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public record SysMetricsView(MemoryUsage heap, MemoryUsage nonHeap, OsInfo os, RuntimeInfo runtime, ThreadInfo threads,
		double heapUsagePercent, List<GcInfo> gc, List<DiskInfo> disk, PoolInfo pool, long timestamp) {
	public record MemoryUsage(long init, long used, long max, long committed) {
	}

	public record OsInfo(String name, String version, String arch, int availableProcessors, double systemLoadAverage) {
	}

	public record RuntimeInfo(long uptime, long startTime, String vmName, String vmVersion, String vmVendor) {
	}

	public record ThreadInfo(int count, int peak, int daemon) {
	}

	public record GcInfo(String name, long collectionCount, long collectionTime) {
	}

	public record DiskInfo(String path, long total, long free, long usable) {
	}

	public record PoolInfo(int active, int idle, int pending, int total) {
	}
}
