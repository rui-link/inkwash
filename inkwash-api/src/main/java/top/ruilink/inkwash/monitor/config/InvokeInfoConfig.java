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
package top.ruilink.inkwash.monitor.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async task executor configuration.
 *
 * <p>
 * Currently used only by the operation-journal write path.
 *
 * <p>
 * <b>Back-pressure note (ISS-042).</b> The rejection policy is
 * {@link ThreadPoolExecutor.CallerRunsPolicy}: when both {@code maxPoolSize}
 * and {@code queueCapacity} are exhausted, the task runs on the
 * <em>calling</em> thread. For audit that is the desired trade-off — an
 * overloaded system slows down instead of losing rows — but it also means a
 * saturated pool degrades the endpoint into synchronous behaviour. Watch this
 * executor's queue depth; the counter {@code inkwash.audit.loss} is the signal
 * for rows that were lost outright.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "invoke")
public class InvokeInfoConfig {

	private int corePoolSize = 2;
	private int maxPoolSize = 8;
	private int queueCapacity = 1000;

	public int getCorePoolSize() {
		return corePoolSize;
	}

	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public int getQueueCapacity() {
		return queueCapacity;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	@Bean("taskExecutor")
	public Executor asyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("async-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}
}
