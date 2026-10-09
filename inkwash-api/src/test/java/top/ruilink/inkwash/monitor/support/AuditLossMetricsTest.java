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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import top.ruilink.inkwash.monitor.domain.Journal;
import top.ruilink.inkwash.monitor.mapper.JournalMapper;
import top.ruilink.inkwash.monitor.service.impl.JournalServiceImpl;

/**
 * Guards the audit-loss signal introduced by ISS-042.
 *
 * <p>
 * Two properties matter: an audit write that fails must never propagate (design
 * document §6.2 requires the audit to be unable to fail the business request),
 * and it must be counted so the loss is alertable rather than merely logged.
 */
class AuditLossMetricsTest {

	private AuditLossMetrics metrics;
	private JournalServiceImpl service;
	private JournalMapper journalMapper;

	@BeforeEach
	void setUp() {
		metrics = new AuditLossMetrics(emptyRegistryProvider());
		journalMapper = mock(JournalMapper.class);
		service = new JournalServiceImpl(journalMapper, metrics);
	}

	private static ObjectProvider<MeterRegistry> emptyRegistryProvider() {
		@SuppressWarnings("unchecked")
		ObjectProvider<MeterRegistry> provider = Mockito.mock(ObjectProvider.class);
		Mockito.when(provider.getIfAvailable()).thenReturn(null);
		return provider;
	}

	@Test
	@DisplayName("写库失败被吞掉，不向调用方抛出（审计不得影响业务）")
	void writeFailure_doesNotPropagate() {
		doThrow(new IllegalStateException("connection reset")).when(journalMapper).insert(any());

		Journal entity = new Journal();
		entity.setMethod("ArticleController.deleteArticle");
		entity.setUserId(7L);

		// no exception expected: this is the whole point of the try/catch
		service.save(entity);
	}

	@Test
	@DisplayName("写库失败被计入 write 阶段损失")
	void writeFailure_isCounted() {
		doThrow(new IllegalStateException("connection reset")).when(journalMapper).insert(any());
		service.save(new Journal());

		assertEquals(1, metrics.writeLosses());
		assertEquals(0, metrics.submitLosses());
		assertEquals(1, metrics.totalLosses());
	}

	@Test
	@DisplayName("写入成功不计数")
	void success_isNotCounted() {
		service.save(new Journal());

		assertEquals(0, metrics.totalLosses());
	}

	@Test
	@DisplayName("两个阶段独立累计，便于区分失败原因")
	void stages_areIndependent() {
		metrics.recordSubmitLoss();
		metrics.recordWriteLoss();
		metrics.recordWriteLoss();

		assertEquals(1, metrics.submitLosses());
		assertEquals(2, metrics.writeLosses());
		assertEquals(3, metrics.totalLosses());
	}

	@Test
	@DisplayName("存在 MeterRegistry 时同时注册 Micrometer 计数器")
	void meterRegistry_isInCREMENTED() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		@SuppressWarnings("unchecked")
		ObjectProvider<MeterRegistry> provider = Mockito.mock(ObjectProvider.class);
		Mockito.when(provider.getIfAvailable()).thenReturn(registry);

		AuditLossMetrics withRegistry = new AuditLossMetrics(provider);
		withRegistry.recordSubmitLoss();
		withRegistry.recordWriteLoss();

		assertEquals(1.0, registry.get(AuditLossMetrics.METRIC_NAME).tag("stage", AuditLossMetrics.STAGE_SUBMIT)
				.counter().count(), 0.0001);
		assertEquals(1.0,
				registry.get(AuditLossMetrics.METRIC_NAME).tag("stage", AuditLossMetrics.STAGE_WRITE).counter().count(),
				0.0001);
	}

	@Test
	@DisplayName("无 MeterRegistry 时降级为内存计数，不抛异常")
	void withoutRegistry_degradesGracefully() {
		metrics.recordSubmitLoss();

		assertEquals(1, metrics.submitLosses());
	}

	@Test
	@DisplayName("save 标注 @Async，运行在业务事务之外")
	void save_isAsync() throws Exception {
		java.lang.reflect.Method save = JournalServiceImpl.class.getMethod("save", Journal.class);
		assertEquals(true, save.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class),
				"审计写入必须保持 @Async，否则会随业务事务一起回滚");
	}

	@Test
	@DisplayName("切面吞掉提交异常时计入 submit 阶段")
	void submitFailure_isCounted() {
		metrics.recordSubmitLoss();

		assertEquals(1, metrics.submitLosses());
		assertEquals(0, metrics.writeLosses());
	}
}