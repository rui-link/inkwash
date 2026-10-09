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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.system.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.system.api.query.NoticeQuery;
import top.ruilink.inkwash.system.domain.SysNotice;

/**
 * Guards the empty-collection guard on
 * {@link NoticeMapper#createBatchIfAny(List)}.
 *
 * <p>
 * {@code createBatch} is built from annotation SQL whose {@code <foreach>}
 * emits one tuple per element. MyBatis opens the statement regardless of
 * whether the collection is empty, so an empty batch reaches the database as
 * {@code INSERT INTO sys_notice (...) VALUES } and fails with a syntax error.
 *
 * <p>
 * The delegation is verified against a hand-written stub rather than a Mockito
 * mock: Mockito intercepts interface default methods, so a mock would never
 * execute the guard and the test would be vacuous.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("通知批量插入空集合守卫")
class NoticeMapperBatchTest {

	private static final class RecordingNoticeMapper implements NoticeMapper {

		private final List<List<SysNotice>> batches = new ArrayList<>();

		@Override
		public int createBatch(List<SysNotice> notices) {
			batches.add(List.copyOf(notices));
			return notices.size();
		}

		@Override
		public int createBatchIfAny(List<SysNotice> notices) {
			return NoticeMapper.super.createBatchIfAny(notices);
		}

		@Override
		public int deleteOwn(List<Long> ids, Long userId) {
			return 0;
		}

		@Override
		public int markRead(List<Long> ids, Long userId) {
			return 0;
		}

		@Override
		public int markAllRead(Long userId) {
			return 0;
		}

		@Override
		public List<SysNotice> selectNoticeList(NoticeQuery query, long offset, int limit, String sortSql) {
			return List.of();
		}

		@Override
		public List<SysNotice> selectPage(String type, String status, int offset, int limit) {
			return List.of();
		}

		@Override
		public List<SysNotice> selectList(String type, String status) {
			return List.of();
		}

		@Override
		public SysNotice selectById(Long id) {
			return null;
		}

		@Override
		public Long create(SysNotice notice) {
			return null;
		}

		@Override
		public int update(SysNotice notice) {
			return 0;
		}

		@Override
		public int deleteById(Long id) {
			return 0;
		}

		@Override
		public int deleteByIds(List<Long> ids) {
			return 0;
		}

		@Override
		public List<SysNotice> selectForUser(Long userId) {
			return List.of();
		}

		@Override
		public long unreadCount(Long userId) {
			return 0;
		}

		@Override
		public long countNotices(NoticeQuery query) {
			return 0;
		}

		@Override
		public long count(String type, String status) {
			return 0;
		}

		@Override
		public int clearOwn(Long userId) {
			return 0;
		}
	}

	private static SysNotice notice(long recipientId) {
		SysNotice notice = new SysNotice();
		notice.setTitle("标题");
		notice.setContent("内容");
		notice.setRecipientId(recipientId);
		notice.setCreateTime(LocalDateTime.now());
		return notice;
	}

	@Test
	void emptyListIsRejectedWithoutTouchingTheDatabase() {
		RecordingNoticeMapper mapper = new RecordingNoticeMapper();

		assertEquals(0, mapper.createBatchIfAny(List.of()));
		assertTrue(mapper.batches.isEmpty(), "空批次不得下发 SQL");
	}

	@Test
	void nullListIsRejectedWithoutTouchingTheDatabase() {
		RecordingNoticeMapper mapper = new RecordingNoticeMapper();

		assertEquals(0, mapper.createBatchIfAny(null));
		assertTrue(mapper.batches.isEmpty(), "null 批次不得下发 SQL");
	}

	@Test
	void nonEmptyListIsForwardedIntact() {
		RecordingNoticeMapper mapper = new RecordingNoticeMapper();
		List<SysNotice> notices = List.of(notice(1L), notice(2L), notice(3L));

		assertEquals(3, mapper.createBatchIfAny(notices));
		assertEquals(1, mapper.batches.size(), "非空批次必须合并为一次 INSERT");
		assertEquals(List.of(1L, 2L, 3L), mapper.batches.getFirst().stream().map(SysNotice::getRecipientId).toList());
	}

	@Test
	void singleElementListIsForwarded() {
		RecordingNoticeMapper mapper = new RecordingNoticeMapper();

		assertEquals(1, mapper.createBatchIfAny(List.of(notice(7L))));
		assertEquals(1, mapper.batches.size());
	}
}