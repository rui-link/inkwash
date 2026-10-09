package top.ruilink.inkwash.cms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.cms.domain.Comment;
import top.ruilink.inkwash.cms.domain.CommentInteraction;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CommentInteractionMapper;
import top.ruilink.inkwash.cms.mapper.CommentMapper;
import top.ruilink.inkwash.cms.service.impl.CommentServiceImpl;
import top.ruilink.inkwash.security.util.SecurityUtil;
import top.ruilink.inkwash.system.mapper.UserMapper;

/**
 * Comment like unit tests — {@code cms_comment.agree_count} used to be a column
 * nothing ever wrote (ISS-034 / D-06).
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("评论点赞单元测试")
class CommentInteractionServiceTest {

	@Mock
	private ArticleMapper articleMapper;

	@Mock
	private CommentMapper commentMapper;

	@Mock
	private CommentInteractionMapper commentInteractionMapper;

	@Mock
	private UserMapper userMapper;

	@InjectMocks
	private CommentServiceImpl commentService;

	private static final Long COMMENT_ID = 500L;
	private static final Long ACTOR_ID = 77L;

	private Comment existingComment() {
		Comment comment = new Comment();
		comment.setId(COMMENT_ID);
		comment.setArticleId(9L);
		comment.setCommenterId(3L);
		return comment;
	}

	@Nested
	@DisplayName("点赞与取消")
	class Toggle {

		@Test
		@DisplayName("首次点赞应写入互动行并增加计数")
		void firstAgreeCreatesRowAndIncrements() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(ACTOR_ID);
				when(commentMapper.selectById(COMMENT_ID)).thenReturn(existingComment());
				when(commentInteractionMapper.selectByCommentAndUser(COMMENT_ID, ACTOR_ID)).thenReturn(null);

				commentService.agreeComment(COMMENT_ID);

				ArgumentCaptor<CommentInteraction> captor = ArgumentCaptor.forClass(CommentInteraction.class);
				verify(commentInteractionMapper).insert(captor.capture());
				assertEquals(COMMENT_ID, captor.getValue().getCommentId());
				assertEquals(ACTOR_ID, captor.getValue().getActorId());
				assertTrue(captor.getValue().getAgree());
				verify(commentMapper).incrementAgreeCount(COMMENT_ID);
			}
		}

		@Test
		@DisplayName("重复点赞不应重复计数")
		void repeatedAgreeDoesNotIncrementTwice() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(ACTOR_ID);
				when(commentMapper.selectById(COMMENT_ID)).thenReturn(existingComment());

				CommentInteraction existing = new CommentInteraction();
				existing.setCommentId(COMMENT_ID);
				existing.setActorId(ACTOR_ID);
				existing.setAgree(true);
				when(commentInteractionMapper.selectByCommentAndUser(COMMENT_ID, ACTOR_ID)).thenReturn(existing);

				commentService.agreeComment(COMMENT_ID);
				commentService.agreeComment(COMMENT_ID);

				verify(commentMapper, never()).incrementAgreeCount(COMMENT_ID);
			}
		}

		@Test
		@DisplayName("取消点赞应清除标志位并递减计数")
		void unAgreeClearsAndDecrements() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(ACTOR_ID);
				when(commentMapper.selectById(COMMENT_ID)).thenReturn(existingComment());

				CommentInteraction existing = new CommentInteraction();
				existing.setCommentId(COMMENT_ID);
				existing.setActorId(ACTOR_ID);
				existing.setAgree(true);
				when(commentInteractionMapper.selectByCommentAndUser(COMMENT_ID, ACTOR_ID)).thenReturn(existing);

				commentService.unagreeComment(COMMENT_ID);

				ArgumentCaptor<CommentInteraction> captor = ArgumentCaptor.forClass(CommentInteraction.class);
				verify(commentInteractionMapper).update(captor.capture());
				assertFalse(captor.getValue().getAgree());
				verify(commentMapper).decrementAgreeCount(COMMENT_ID);
			}
		}

		@Test
		@DisplayName("未点赞时取消应无操作")
		void unAgreeWithoutAgreeIsNoop() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(ACTOR_ID);
				when(commentMapper.selectById(COMMENT_ID)).thenReturn(existingComment());

				CommentInteraction existing = new CommentInteraction();
				existing.setCommentId(COMMENT_ID);
				existing.setActorId(ACTOR_ID);
				existing.setAgree(false);
				when(commentInteractionMapper.selectByCommentAndUser(COMMENT_ID, ACTOR_ID)).thenReturn(existing);

				commentService.unagreeComment(COMMENT_ID);

				verify(commentMapper, never()).decrementAgreeCount(COMMENT_ID);
				verify(commentInteractionMapper, never()).update(any());
			}
		}
	}

	@Nested
	@DisplayName("边界")
	class Guards {

		@Test
		@DisplayName("评论不存在时应失败且不写互动行")
		void missingCommentFails() {
			when(commentMapper.selectById(COMMENT_ID)).thenReturn(null);

			assertThrows(BusinessException.class, () -> commentService.agreeComment(COMMENT_ID));
			assertThrows(BusinessException.class, () -> commentService.unagreeComment(COMMENT_ID));
			verify(commentInteractionMapper, never()).insert(any());
		}

		@Test
		@DisplayName("匿名用户视为未点赞")
		void anonymousIsNotAgreed() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::isAuthenticated).thenReturn(false);

				assertFalse(commentService.isCommentAgreed(COMMENT_ID));
				verify(commentInteractionMapper, never()).selectByCommentAndUser(any(), any());
			}
		}

		@Test
		@DisplayName("删除评论应连带清除所有用户的点赞行")
		void deletingCommentClearsAllLikes() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(ACTOR_ID);
				securityUtil.when(SecurityUtil::isAdmin).thenReturn(true);
				when(commentMapper.selectById(COMMENT_ID)).thenReturn(existingComment());
				when(articleMapper.selectById(9L)).thenReturn(null);

				commentService.deleteComment(COMMENT_ID);

				verify(commentInteractionMapper).deleteByComment(COMMENT_ID);
				verify(commentInteractionMapper, never()).delete(any(), any());
			}
		}
	}
}