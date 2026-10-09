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
package top.ruilink.inkwash.monitor.support;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Makes lost audit rows alertable (ISS-042).
 *
 * <p>
 * {@code system-design.md} §6.2 mandates two things that together produce a
 * blind spot: the {@code @OperateTrace} aspect must never fail the business
 * request ("永不失败"), and {@code JournalServiceImpl.save} runs on a separate
 * thread via {@code @Async}. Both are deliberate. The consequence is that a
 * lost {@code mon_journal} row leaves no signal a machine can act on — only a
 * log line nobody is guaranteed to read, and the endpoint is explicitly not
 * allowed to react.
 *
 * <p>
 * This class supplies that signal as a monotonic counter, exported through the
 * actuator's metrics registry like any other Micrometer meter, so an operator
 * can alert on {@code inkwash.audit.loss} instead of grepping logs. The
 * in-memory {@link AtomicLong} mirrors exist so the value stays readable even
 * when no {@link MeterRegistry} is present (plain unit tests, or a deployment
 * that strips actuator).
 *
 * <p>
 * Two stages are counted separately, because they fail for different reasons:
 * <ul>
 * <li>{@code submit} — the aspect could not even hand the row to the
 * executor;</li>
 * <li>{@code write} — the asynchronous insert itself failed.</li>
 * </ul>
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Component
public class AuditLossMetrics {

	/** Stage tag: the aspect failed to dispatch the audit row. */
	public static final String STAGE_SUBMIT = "submit";

	/** Stage tag: the asynchronous database insert failed. */
	public static final String STAGE_WRITE = "write";

	static final String METRIC_NAME = "inkwash.audit.loss";

	private final ObjectProvider<MeterRegistry> registryProvider;
	private final AtomicLong submitLosses = new AtomicLong();
	private final AtomicLong writeLosses = new AtomicLong();

	public AuditLossMetrics(ObjectProvider<MeterRegistry> registryProvider) {
		this.registryProvider = registryProvider;
	}

	public void recordSubmitLoss() {
		submitLosses.incrementAndGet();
		increment(STAGE_SUBMIT);
	}

	public void recordWriteLoss() {
		writeLosses.incrementAndGet();
		increment(STAGE_WRITE);
	}

	/** Total audit rows lost since startup, across both stages. */
	public long totalLosses() {
		return submitLosses.get() + writeLosses.get();
	}

	public long submitLosses() {
		return submitLosses.get();
	}

	public long writeLosses() {
		return writeLosses.get();
	}

	/**
	 * Resets the counters. Used by tests; production code must never clear an audit
	 * signal.
	 */
	void reset() {
		submitLosses.set(0);
		writeLosses.set(0);
	}

	private void increment(String stage) {
		MeterRegistry registry = registryProvider.getIfAvailable();
		if (registry == null) {
			return;
		}
		Counter.builder(METRIC_NAME).tag("stage", stage).description("丢失的操作审计行数").register(registry).increment();
	}
}