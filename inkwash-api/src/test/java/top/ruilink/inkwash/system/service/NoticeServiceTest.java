package top.ruilink.inkwash.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import top.ruilink.inkwash.base.util.SortValidator;
import top.ruilink.inkwash.system.mapper.NoticeMapper;
import top.ruilink.inkwash.system.service.impl.NoticeServiceImpl;

/**
 * Notice service unit tests for unread counts and read state delegation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

	@Mock
	private NoticeMapper noticeMapper;
	@Mock
	private SortValidator sortValidator;
	@InjectMocks
	private NoticeServiceImpl noticeService;

	@Test
	void unreadCount_delegatesToMapper() {
		when(noticeMapper.unreadCount(7L)).thenReturn(3L);
		assertEquals(3L, noticeService.unreadCount(7L));
	}

	@Test
	void markAllRead_delegatesToMapper() {
		noticeService.markAllRead(7L);
		verify(noticeMapper).markAllRead(7L);
	}

	@Test
	void markRead_skipsEmptyIds() {
		noticeService.markRead(List.of(), 7L);
		verify(noticeMapper, never()).markRead(any(), any());
	}

	@Test
	void deleteOwn_delegatesToMapper() {
		noticeService.deleteOwn(List.of(1L, 2L), 7L);
		verify(noticeMapper).deleteOwn(eq(List.of(1L, 2L)), eq(7L));
	}

	@Test
	void clearOwn_delegatesToMapper() {
		noticeService.clearOwn(7L);
		verify(noticeMapper).clearOwn(7L);
	}
}
