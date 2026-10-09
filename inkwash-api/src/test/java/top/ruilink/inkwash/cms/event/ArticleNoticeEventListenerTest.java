package top.ruilink.inkwash.cms.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.cms.event.ArticleNoticeEvent;
import top.ruilink.inkwash.system.domain.SysNotice;
import top.ruilink.inkwash.system.mapper.NoticeMapper;
import top.ruilink.inkwash.system.mapper.UserMapper;

/**
 * Article notice event listener unit tests for submission, rejection, and
 * publication notices.
 *
 * <p>
 * Submission fans out to every editor in a single {@code createBatch} call
 * (D-10 / ISS-022); rejection and publication address a single recipient and
 * still use {@code create}.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class ArticleNoticeEventListenerTest {

	@Mock
	private NoticeMapper noticeMapper;
	@Mock
	private UserMapper userMapper;
	@InjectMocks
	private ArticleNoticeEventListener listener;

	/**
	 * Captures the single batch the listener hands to the mapper.
	 *
	 * <p>
	 * {@code createBatchIfAny} is an interface default method, so Mockito
	 * intercepts the call instead of delegating to the real implementation; the
	 * delegated {@code createBatch} is therefore never reached in a unit test.
	 * Asserting the argument passed to {@code createBatchIfAny} is what actually
	 * pins the fan-out.
	 */
	@SuppressWarnings("unchecked")
	private List<SysNotice> captureBatch() {
		org.mockito.ArgumentCaptor<List<SysNotice>> captor = org.mockito.ArgumentCaptor.forClass((Class) List.class);
		verify(noticeMapper).createBatchIfAny(captor.capture());
		return captor.getValue();
	}

	@Test
	void submit_notifiesEveryEditorExceptOperatorInOneBatch() {
		when(userMapper.selectUserIdsByRoleCode("ROLE_EDITOR")).thenReturn(List.of(1L, 2L, 3L));

		listener.handle(new ArticleNoticeEvent(10L, "标题", 1L, ArticleStatus.PENDING.getCode(), 1L, "作者甲", null));

		List<SysNotice> notices = captureBatch();
		assertEquals(2, notices.size(), "运营者本人不应收到通知，其余两位编辑各一条");
		assertEquals(List.of(2L, 3L), notices.stream().map(SysNotice::getRecipientId).toList());
		assertTrue(notices.stream().allMatch(n -> n.getTitle().contains("待审核")));
		verify(noticeMapper, never()).create(any(SysNotice.class));
	}

	@Test
	void submit_withoutEditorsSendsNothing() {
		when(userMapper.selectUserIdsByRoleCode("ROLE_EDITOR")).thenReturn(List.of());

		listener.handle(new ArticleNoticeEvent(10L, "标题", 1L, ArticleStatus.PENDING.getCode(), 1L, "作者甲", null));

		verify(noticeMapper, never()).createBatch(any());
	}

	@Test
	void submit_whenOperatorIsTheOnlyEditorSkipsEmptyInsert() {
		when(userMapper.selectUserIdsByRoleCode("ROLE_EDITOR")).thenReturn(List.of(1L));

		listener.handle(new ArticleNoticeEvent(10L, "标题", 1L, ArticleStatus.PENDING.getCode(), 1L, "作者甲", null));

		assertTrue(captureBatch().isEmpty(), "只剩运营者一人时批次为空，必须由 createBatchIfAny 拦下");
		verify(noticeMapper, never()).createBatch(any());
	}

	@Test
	void reject_notifiesAuthorWithOpinion() {
		listener.handle(new ArticleNoticeEvent(10L, "标题", 9L, ArticleStatus.REJECTED.getCode(), 2L, "编辑乙", "图片不清晰"));
		verify(noticeMapper).create(argThat(n -> n.getRecipientId().equals(9L) && n.getContent().contains("图片不清晰")));
	}

	@Test
	void publish_notifiesAuthor() {
		listener.handle(new ArticleNoticeEvent(10L, "标题", 9L, ArticleStatus.PUBLISHED.getCode(), null, null, null));
		verify(noticeMapper).create(argThat(n -> n.getRecipientId().equals(9L) && n.getTitle().contains("已发布")));
	}

	@Test
	void nullAuthor_isIgnored() {
		listener.handle(new ArticleNoticeEvent(10L, "标题", null, ArticleStatus.APPROVED.getCode(), 2L, "编辑乙", null));
		verify(noticeMapper, never()).create(any(SysNotice.class));
	}
}
