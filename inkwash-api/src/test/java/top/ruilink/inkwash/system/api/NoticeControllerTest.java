package top.ruilink.inkwash.system.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import top.ruilink.inkwash.security.util.SecurityUtil;
import top.ruilink.inkwash.system.api.param.NoticeReadParam;
import top.ruilink.inkwash.system.service.NoticeService;

/**
 * Notice read-marking endpoint unit tests for per-id and mark-all requests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class NoticeControllerTest {

	@Mock
	private NoticeService noticeService;

	@InjectMocks
	private NoticeController noticeController;

	private static final Long USER_ID = 1L;

	@Test
	@DisplayName("markRead - 无 ids 且非 all 时返回400")
	void markRead_emptyWithoutAll_Returns400() {
		try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
			securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
			NoticeReadParam param = new NoticeReadParam();
			param.setAll(false);

			ResponseEntity<Void> response = noticeController.markRead(param);

			assertEquals(400, response.getStatusCode().value());
			verify(noticeService, never()).markRead(any(), any());
			verify(noticeService, never()).markAllRead(any());
		}
	}

	@Test
	@DisplayName("markRead - 指定 ids 成功时幂等返回204")
	void markRead_ids_SucceedsWith204() {
		try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
			securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
			NoticeReadParam param = new NoticeReadParam();
			param.setIds(List.of(10L, 20L));

			ResponseEntity<Void> response = noticeController.markRead(param);

			assertEquals(204, response.getStatusCode().value());
			verify(noticeService).markRead(List.of(10L, 20L), USER_ID);
			verify(noticeService, never()).markAllRead(USER_ID);
		}
	}

	@Test
	@DisplayName("markRead - all=true 标记全部并返回204")
	void markRead_all_SucceedsWith204() {
		try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
			securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
			NoticeReadParam param = new NoticeReadParam();
			param.setAll(true);

			ResponseEntity<Void> response = noticeController.markRead(param);

			assertEquals(204, response.getStatusCode().value());
			verify(noticeService).markAllRead(USER_ID);
			verify(noticeService, never()).markRead(any(), any());
		}
	}
}