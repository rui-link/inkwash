package top.ruilink.inkwash.system.api.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import top.ruilink.inkwash.system.enums.NoticeType;

/**
 * Notice view unit tests covering the notice type enum property.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class NoticeViewTest {

	@Test
	@DisplayName("NoticeView.type 是 NoticeType 枚举类型")
	void noticeView_TypeIsNoticeTypeEnum() {
		NoticeView view = new NoticeView();
		view.setType(NoticeType.NOTICE);
		assertNotNull(view.getType());
		assertEquals(NoticeType.NOTICE, view.getType());
	}

	@Test
	@DisplayName("NoticeView.type 序列化为枚举code值")
	void noticeView_TypeSerializesAsCode() {
		NoticeView view = new NoticeView();
		view.setType(NoticeType.INFORM);
		assertEquals(1, view.getType().getCode());
		view.setType(NoticeType.WARNING);
		assertEquals(3, view.getType().getCode());
	}
}
