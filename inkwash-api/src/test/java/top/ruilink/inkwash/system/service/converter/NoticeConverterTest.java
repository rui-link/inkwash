package top.ruilink.inkwash.system.service.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.system.api.view.NoticeView;
import top.ruilink.inkwash.system.domain.SysNotice;
import top.ruilink.inkwash.system.enums.NoticeType;

/**
 * Notice converter unit tests for entity to view read status mapping.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class NoticeConverterTest {

	@Test
	void toNoticeView_mapsTypeAndUnreadRecipient() {
		SysNotice notice = new SysNotice();
		notice.setId(1L);
		notice.setTitle("待审核");
		notice.setType(NoticeType.INFORM);
		notice.setRecipientId(42L);
		notice.setReadTime(null);

		NoticeView view = NoticeConverter.toView(notice);

		assertEquals(NoticeType.INFORM, view.getType());
		assertEquals(Long.valueOf(42L), view.getRecipientId());
		assertEquals(Integer.valueOf(1), view.getReadStatus());
	}

	@Test
	void toNoticeView_readNoticeHasReadStatus2() {
		SysNotice notice = new SysNotice();
		notice.setType(NoticeType.INFORM);
		notice.setRecipientId(42L);
		notice.setReadTime(LocalDateTime.now());

		NoticeView view = NoticeConverter.toView(notice);

		assertEquals(Integer.valueOf(2), view.getReadStatus());
	}

	@Test
	void toNoticeView_broadcastHasNoReadStatus() {
		SysNotice notice = new SysNotice();
		notice.setType(NoticeType.NOTICE);
		notice.setRecipientId(null);

		NoticeView view = NoticeConverter.toView(notice);

		assertEquals(NoticeType.NOTICE, view.getType());
		assertNull(view.getReadStatus());
	}
}
