package top.ruilink.inkwash.cms.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.cms.enums.ArticleStatus;

/**
 * Article entity unit tests for status transitions and author or admin
 * permission predicates.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class ArticleDomainTest {

	// ========== State machine transition tests ==========

	@Test
	void submit_FromDraft_TransitionsToPending() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		article.submit();
		assertEquals(ArticleStatus.PENDING, article.getStatus());
		assertNotNull(article.getUpdateTime());
	}

	@Test
	void submit_FromPublished_Throws() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		assertThrows(BusinessException.class, article::submit);
	}

	@Test
	void submit_FromPending_Throws() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PENDING);
		assertThrows(BusinessException.class, article::submit);
	}

	@Test
	void submit_FromRejected_Throws_UseResubmitInstead() {
		Article article = new Article();
		article.setStatus(ArticleStatus.REJECTED);
		assertThrows(BusinessException.class, article::submit);
	}

	@Test
	void approve_FromPending_SetsApproved() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PENDING);
		article.approve(42L);
		assertEquals(ArticleStatus.APPROVED, article.getStatus());
		assertEquals(42L, article.getReviewerId());
		assertNotNull(article.getUpdateTime());
	}

	@Test
	void approve_FromDraft_Throws() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		assertThrows(BusinessException.class, () -> article.approve(1L));
	}

	@Test
	void reject_FromPending_SetsRejected() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PENDING);
		article.reject(42L, "too short");
		assertEquals(ArticleStatus.REJECTED, article.getStatus());
		assertEquals(42L, article.getReviewerId());
		assertEquals("too short", article.getOpinion());
		assertNotNull(article.getUpdateTime());
	}

	@Test
	void reject_FromDraft_Throws() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		assertThrows(BusinessException.class, () -> article.reject(1L, "no"));
	}

	@Test
	void publish_FromApproved_SetsPublished() {
		Article article = new Article();
		article.setStatus(ArticleStatus.APPROVED);
		article.publish();
		assertEquals(ArticleStatus.PUBLISHED, article.getStatus());
		assertNotNull(article.getPublishTime());
		assertNotNull(article.getUpdateTime());
	}

	@Test
	void publish_FromDraft_Throws() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		assertThrows(BusinessException.class, article::publish);
	}

	@Test
	void retract_FromPublished_ByAuthor_SetsRetracted() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setAuthorId(1L);
		article.retract(1L, false);
		assertEquals(ArticleStatus.RETRACTED, article.getStatus());
	}

	@Test
	void retract_FromPublished_ByAdmin_SetsRetracted() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setAuthorId(1L);
		article.retract(2L, true);
		assertEquals(ArticleStatus.RETRACTED, article.getStatus());
	}

	@Test
	void retract_FromDraft_ByAuthor_Throws() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		article.setAuthorId(1L);
		assertThrows(BusinessException.class, () -> article.retract(1L, false));
	}

	@Test
	void retract_FromPublished_ByNonAuthor_Throws() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setAuthorId(1L);
		assertThrows(BusinessException.class, () -> article.retract(2L, false));
	}

	@Test
	void resubmit_FromRejected_TransitionsToPending() {
		Article article = new Article();
		article.setStatus(ArticleStatus.REJECTED);
		article.setOpinion("证据不足");
		article.resubmit();
		assertEquals(ArticleStatus.PENDING, article.getStatus());
		assertNull(article.getOpinion());
		assertNotNull(article.getUpdateTime());
	}

	@Test
	void resubmit_FromDraft_Throws() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		assertThrows(BusinessException.class, article::resubmit);
	}

	@Test
	void resubmit_FromPublished_Throws() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		assertThrows(BusinessException.class, article::resubmit);
	}

	// ========== Query method tests ==========

	@Test
	void canEdit_ByAuthorOnDraft_ReturnsTrue() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		article.setAuthorId(1L);
		assertTrue(article.canEdit(1L));
	}

	@Test
	void canEdit_OnPublished_ReturnsFalse() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setAuthorId(1L);
		assertFalse(article.canEdit(1L));
	}

	@Test
	void canEdit_ByNonAuthor_ReturnsFalse() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		article.setAuthorId(1L);
		assertFalse(article.canEdit(2L));
	}

	@Test
	void canDelete_ByAuthorOnDraft_ReturnsTrue() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		article.setAuthorId(1L);
		assertTrue(article.canDelete(1L, false));
	}

	@Test
	void canDelete_ByAdminOnAny_ReturnsTrue() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setAuthorId(1L);
		assertTrue(article.canDelete(2L, true));
	}

	@Test
	void canDelete_ByNonAuthorNonAdminOnDraft_ReturnsFalse() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		article.setAuthorId(1L);
		assertFalse(article.canDelete(2L, false));
	}

	@Test
	void canReview_OnPending_ReturnsTrue() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PENDING);
		assertTrue(article.canReview());
	}

	@Test
	void canReview_OnDraft_ReturnsFalse() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		assertFalse(article.canReview());
	}

	@Test
	void canPublish_OnApproved_ReturnsTrue() {
		Article article = new Article();
		article.setStatus(ArticleStatus.APPROVED);
		assertTrue(article.canPublish());
	}

	@Test
	void canPublish_OnPending_ReturnsFalse() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PENDING);
		assertFalse(article.canPublish());
	}

	@Test
	void canView_OnPublished_ReturnsTrue() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		assertTrue(article.canView());
	}

	@Test
	void canView_OnDraft_ReturnsFalse() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		assertFalse(article.canView());
	}

	@Test
	void canReply_OnPublished_ReturnsTrue() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		assertTrue(article.canReply());
	}

	@Test
	void canReply_OnDraft_ReturnsFalse() {
		Article article = new Article();
		article.setStatus(ArticleStatus.DRAFT);
		assertFalse(article.canReply());
	}

	@Test
	void canRetract_ByAuthorOnPublished_ReturnsTrue() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setAuthorId(1L);
		assertTrue(article.canRetract(1L, false));
	}

	@Test
	void canRetract_ByAdminOnPublished_ReturnsTrue() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setAuthorId(1L);
		assertTrue(article.canRetract(2L, true));
	}

	@Test
	void canRetract_ByNonAuthorOnPublished_ReturnsFalse() {
		Article article = new Article();
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setAuthorId(1L);
		assertFalse(article.canRetract(2L, false));
	}

	@Nested
	@DisplayName("越权失败的语义 —— ISS-014 / D-02")
	class UnauthorizedSemantics {

		@Test
		@DisplayName("撤回他人文章应抛 403 并携带 i18n messageKey")
		void retract_ByNonOwner_Throws403WithMessageKey() {
			Article article = new Article();
			article.setStatus(ArticleStatus.PUBLISHED);
			article.setAuthorId(1L);

			BusinessException e = assertThrows(BusinessException.class, () -> article.retract(2L, false));

			assertEquals(HttpStatus.FORBIDDEN, e.getHttpStatus(), "越权必须是 403；默认的 400 会让前端无法与参数错误区分");
			assertEquals("error.article.not_retractable", e.getMessageKey(), "messageKey 必须是 i18n key，不能是给人看的中文原文");
		}

		@Test
		@DisplayName("状态守卫仍为 400 —— 它不是越权")
		void submit_FromPublished_StillThrows400() {
			Article article = new Article();
			article.setStatus(ArticleStatus.PUBLISHED);

			BusinessException e = assertThrows(BusinessException.class, article::submit);

			assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus(), "状态不允许属业务规则违反，不是越权，不应改成 403");
		}
	}

}
