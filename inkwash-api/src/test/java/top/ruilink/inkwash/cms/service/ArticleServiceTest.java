package top.ruilink.inkwash.cms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.util.SensitiveMatcher;
import top.ruilink.inkwash.cms.api.param.ArticleParam;
import top.ruilink.inkwash.cms.api.view.ArticleView;
import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.domain.ArticleTally;
import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CategoryMapper;
import top.ruilink.inkwash.cms.mapper.TermMapper;
import top.ruilink.inkwash.cms.service.impl.ArticleServiceImpl;
import top.ruilink.inkwash.security.adapter.SysUserDetails;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.mapper.UserMapper;

/**
 * Article service unit tests for workflow transitions, author permissions, and
 * server-controlled status.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleServiceTest {

	@Mock
	private ArticleMapper articleMapper;

	@Mock
	private UserMapper userMapper;

	@Mock
	private SensitiveService sensitiveService;

	@Mock
	private SecurityContext securityContext;

	@Mock
	private Authentication authentication;

	@Mock
	private org.springframework.context.ApplicationEventPublisher eventPublisher;

	@Mock
	private CategoryMapper categoryMapper;

	@Mock
	private TermMapper termMapper;

	@Mock
	private ArticleViewResolver articleViewResolver;

	@Mock
	private top.ruilink.inkwash.cms.mapper.CommentMapper commentMapper;

	@Mock
	private top.ruilink.inkwash.cms.mapper.CommentInteractionMapper commentInteractionMapper;

	@Mock
	private top.ruilink.inkwash.cms.mapper.InteractionMapper interactionMapper;

	@InjectMocks
	private ArticleServiceImpl articleService;

	private Article draftArticle;
	private Article pendingArticle;
	private Article publishedArticle;
	private Article approveArticle;
	private SysUser mockUser;
	private SysUserDetails mockUserDetails;

	@BeforeEach
	void setUp() {
		mockUser = new SysUser();
		mockUser.setId(1L);
		mockUser.setNickname("testuser");

		mockUserDetails = new SysUserDetails(mockUser, null, List.of(new SimpleGrantedAuthority("cms:article:create")));

		draftArticle = new Article();
		draftArticle.setId(1L);
		draftArticle.setTitle("Test Draft Article");
		draftArticle.setContent("Draft Content");
		draftArticle.setAuthorId(1L);
		draftArticle.setStatus(ArticleStatus.DRAFT);
		draftArticle.setTally(new ArticleTally());

		pendingArticle = new Article();
		pendingArticle.setId(2L);
		pendingArticle.setTitle("Test Pending Article");
		pendingArticle.setContent("Pending Content");
		pendingArticle.setAuthorId(2L);
		pendingArticle.setStatus(ArticleStatus.PENDING);
		pendingArticle.setTally(new ArticleTally());

		publishedArticle = new Article();
		publishedArticle.setId(3L);
		publishedArticle.setTitle("Test Published Article");
		publishedArticle.setContent("Published Content");
		publishedArticle.setAuthorId(1L);
		publishedArticle.setStatus(ArticleStatus.PUBLISHED);
		publishedArticle.setTally(new ArticleTally());
		publishedArticle.setPublishTime(LocalDateTime.now());

		approveArticle = new Article();
		approveArticle.setId(4L);
		approveArticle.setTitle("Test Pending Article");
		approveArticle.setContent("Pending Content");
		approveArticle.setAuthorId(2L);
		approveArticle.setStatus(ArticleStatus.APPROVED);
		approveArticle.setTally(new ArticleTally());

		when(userMapper.selectById(1L)).thenReturn(mockUser);

		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(mockUserDetails);
		doReturn(List.of(new SimpleGrantedAuthority("cms:article:create"), new SimpleGrantedAuthority("ROLE_ADMIN")))
				.when(authentication).getAuthorities();
		SecurityContextHolder.setContext(securityContext);

		when(sensitiveService.getAllActiveWords()).thenReturn(List.of());
		when(sensitiveService.getMatcher()).thenReturn(SensitiveMatcher.build(List.of()));

		when(articleViewResolver.toView(any(Article.class))).thenAnswer(invocation -> {
			Article article = invocation.getArgument(0);
			ArticleView view = new ArticleView();
			view.setId(article.getId());
			view.setAuthorId(article.getAuthorId());
			view.setStatus(article.getStatus());
			view.setPublishTime(article.getPublishTime());
			return view;
		});
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("创建文章测试")
	class CreateArticleTests {

		@Test
		@DisplayName("创建文章成功")
		void createArticle_Success() {
			ArticleParam param = new ArticleParam();
			param.setTitle("New Article");
			param.setContent("Article Content");
			when(userMapper.selectById(1L)).thenReturn(mockUser);
			when(articleMapper.insert(any(Article.class))).thenAnswer(invocation -> {
				Article article = invocation.getArgument(0);
				article.setId(100L);
				return 1L;
			});

			ArticleView result = articleService.createArticle(param);

			assertNotNull(result);
			assertEquals(100L, result.getId());
			assertEquals(ArticleStatus.DRAFT, result.getStatus());
			assertEquals(1L, result.getAuthorId());
			verify(articleMapper).insert(any(Article.class));
		}

		@Test
		@DisplayName("文章状态仅服务端可控：ArticleParam 不暴露 status")
		void articleParam_hasNoStatusSetter() {
			assertThrows(NoSuchMethodException.class,
					() -> ArticleParam.class.getMethod("setStatus", ArticleStatus.class));
		}
	}

	@Nested
	@DisplayName("修改文章测试")
	class UpdateArticleTests {

		@Test
		@DisplayName("修改驳回文章不改变状态，离开驳回态只能走 resubmit")
		void updateArticle_Rejected_KeepsStatus() {
			Article rejectedArticle = new Article();
			rejectedArticle.setId(5L);
			rejectedArticle.setTitle("Rejected Article");
			rejectedArticle.setContent("Rejected Content");
			rejectedArticle.setAuthorId(1L);
			rejectedArticle.setStatus(ArticleStatus.REJECTED);
			rejectedArticle.setTally(new ArticleTally());
			when(articleMapper.selectById(5L)).thenReturn(rejectedArticle);
			when(articleMapper.update(any(Article.class))).thenReturn(1);

			ArticleParam param = new ArticleParam();
			param.setTitle("Updated Title");

			ArticleView result = articleService.updateArticle(5L, param);

			assertEquals(ArticleStatus.REJECTED, result.getStatus());
		}
	}

	@Nested
	@DisplayName("提交审核测试")
	class SubmitForReviewTests {

		@Test
		@DisplayName("提交草稿文章审核成功")
		void submitForReview_Success() {
			when(articleMapper.selectById(1L)).thenReturn(draftArticle);
			when(articleMapper.updateStatus(any(Article.class), anyInt())).thenReturn(1);

			ArticleView result = articleService.commitArticle(1L);

			assertEquals(ArticleStatus.PENDING, result.getStatus());
			verify(articleMapper).updateStatus(any(Article.class), anyInt());
		}

		@Test
		@DisplayName("审核自己的文章应被拒绝（职责分离）")
		void auditArticle_SelfReview_Forbidden() {
			Article selfPending = new Article();
			selfPending.setId(8L);
			selfPending.setTitle("Self Pending");
			selfPending.setAuthorId(1L);
			selfPending.setStatus(ArticleStatus.PENDING);
			selfPending.setTally(new ArticleTally());
			when(articleMapper.selectById(8L)).thenReturn(selfPending);

			assertThrows(BusinessException.class, () -> articleService.reviewArticle(8L, true, ""));
			verify(articleMapper, never()).updateStatus(any(Article.class), anyInt());
		}

		@Test
		@DisplayName("状态被并发修改后应放弃本次迁移")
		void commitArticle_ConcurrentStatusChange_Aborts() {
			when(articleMapper.selectById(1L)).thenReturn(draftArticle);
			when(articleMapper.updateStatus(any(Article.class), anyInt())).thenReturn(0);

			assertThrows(BusinessException.class, () -> articleService.commitArticle(1L));
		}

		@Test
		@DisplayName("提交不存在的文章应抛出异常")
		void submitForReview_ArticleNotFound() {
			when(articleMapper.selectById(999L)).thenReturn(null);

			assertThrows(RuntimeException.class, () -> articleService.commitArticle(999L));
		}

		@Test
		@DisplayName("提交已发布的文章应抛出异常")
		void submitForReview_PublishedArticle_Fails() {
			when(articleMapper.selectById(3L)).thenReturn(publishedArticle);

			assertThrows(RuntimeException.class, () -> articleService.commitArticle(3L));
		}
	}

	@Nested
	@DisplayName("审核文章测试")
	class AuditArticleTests {

		@Test
		@DisplayName("审核通过待审核文章成功")
		void auditArticle_Approve_Success() {
			when(articleMapper.selectById(2L)).thenReturn(pendingArticle);
			when(articleMapper.updateStatus(any(Article.class), anyInt())).thenReturn(1);

			ArticleView result = articleService.reviewArticle(2L, true, "");

			assertNotNull(result);
			assertEquals(ArticleStatus.APPROVED, result.getStatus());
			verify(articleMapper).updateStatus(any(Article.class), anyInt());
		}

		@Test
		@DisplayName("审核不通过待审核文章成功")
		void auditArticle_Reject_Success() {
			when(articleMapper.selectById(2L)).thenReturn(pendingArticle);
			when(articleMapper.updateStatus(any(Article.class), anyInt())).thenReturn(1);

			ArticleView result = articleService.reviewArticle(2L, false, "");

			assertEquals(ArticleStatus.REJECTED, result.getStatus());
			verify(articleMapper).updateStatus(any(Article.class), anyInt());
		}

		@Test
		@DisplayName("审核草稿文章应抛出异常")
		void auditArticle_DraftArticle_Fails() {
			when(articleMapper.selectById(1L)).thenReturn(draftArticle);

			assertThrows(RuntimeException.class, () -> articleService.reviewArticle(1L, true, ""));
		}
	}

	@Nested
	@DisplayName("发布文章测试")
	class PublishArticleTests {

		@Test
		@DisplayName("发布待审核文章成功")
		void publishArticle_Success() {
			when(articleMapper.selectById(4L)).thenReturn(approveArticle);
			when(articleMapper.updateStatus(any(Article.class), anyInt())).thenReturn(1);

			ArticleView result = articleService.publishArticle(4L);

			assertEquals(ArticleStatus.PUBLISHED, result.getStatus());
			assertNotNull(result.getPublishTime());
			verify(articleMapper).updateStatus(any(Article.class), anyInt());
		}

		@Test
		@DisplayName("发布自己的文章应被拒绝（职责分离）")
		void publishArticle_SelfPublish_Forbidden() {
			Article selfApproved = new Article();
			selfApproved.setId(9L);
			selfApproved.setTitle("Self Approved");
			selfApproved.setAuthorId(1L);
			selfApproved.setStatus(ArticleStatus.APPROVED);
			selfApproved.setTally(new ArticleTally());
			when(articleMapper.selectById(9L)).thenReturn(selfApproved);

			assertThrows(BusinessException.class, () -> articleService.publishArticle(9L));
			verify(articleMapper, never()).updateStatus(any(Article.class), anyInt());
		}

		@Test
		@DisplayName("发布草稿文章应抛出异常")
		void publishArticle_DraftArticle_Fails() {
			when(articleMapper.selectById(1L)).thenReturn(draftArticle);

			assertThrows(RuntimeException.class, () -> articleService.publishArticle(1L));
		}
	}

	@Nested
	@DisplayName("获取文章测试")
	class GetArticleTests {

		@Test
		@DisplayName("获取已发布文章成功，增加阅读数")
		void getArticle_PublishedArticle_IncreasesViewCount() {
			when(articleMapper.selectById(3L)).thenReturn(publishedArticle);
			when(articleMapper.incrementViewCount(3L)).thenReturn(1);

			ArticleView result = articleService.getArticle(3L);

			assertNotNull(result);
			assertEquals(3L, result.getId());
			assertEquals(ArticleStatus.PUBLISHED, result.getStatus());
			verify(articleMapper).incrementViewCount(3L);
		}

		@Test
		@DisplayName("获取不存在的文章应抛出异常")
		void getArticle_NotFound() {
			when(articleMapper.selectById(999L)).thenReturn(null);

			assertThrows(RuntimeException.class, () -> articleService.getArticle(999L));
		}

		@Test
		@DisplayName("非作者持 review 权限可查看未发布文章")
		void getArticle_ReviewerWithoutAdmin_Allowed() {
			SysUser reviewer = new SysUser();
			reviewer.setId(2L);
			SysUserDetails reviewerDetails = new SysUserDetails(reviewer, null,
					List.of(new SimpleGrantedAuthority("cms:article:review")));
			doReturn(List.of(new SimpleGrantedAuthority("cms:article:review"))).when(authentication).getAuthorities();
			when(authentication.getPrincipal()).thenReturn(reviewerDetails);
			when(articleMapper.selectById(2L)).thenReturn(pendingArticle);

			ArticleView result = articleService.getArticle(2L);

			assertNotNull(result);
			assertEquals(2L, result.getId());
		}

		@Test
		@DisplayName("无权限的非作者查看未发布文章应返回 403")
		void getArticle_NoPermission_ReturnsForbidden() {
			SysUser other = new SysUser();
			other.setId(2L);
			SysUserDetails otherDetails = new SysUserDetails(other, null, List.of());
			// 文章作者是 1L，而当前登录用户是 2L 且无任何权限
			Article someoneElsesPending = new Article();
			someoneElsesPending.setId(2L);
			someoneElsesPending.setTitle("Someone Else Pending");
			someoneElsesPending.setAuthorId(1L);
			someoneElsesPending.setStatus(ArticleStatus.PENDING);
			someoneElsesPending.setTally(new ArticleTally());
			doReturn(List.of()).when(authentication).getAuthorities();
			when(authentication.getPrincipal()).thenReturn(otherDetails);
			when(articleMapper.selectById(2L)).thenReturn(someoneElsesPending);

			BusinessException ex = assertThrows(BusinessException.class, () -> articleService.getArticle(2L));

			assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
		}
	}

	@Nested
	@DisplayName("文章实体业务方法测试")
	class ArticleBusinessMethodsTests {

		@Test
		@DisplayName("canEdit - 已发布文章不能编辑")
		void canEdit_PublishedArticle_ReturnsFalse() {
			assertFalse(publishedArticle.canEdit(1L));
		}

		@Test
		@DisplayName("canEdit - 作者可以编辑自己的草稿")
		void canEdit_OwnDraftArticle_ReturnsTrue() {
			assertTrue(draftArticle.canEdit(1L));
		}

		@Test
		@DisplayName("canEdit - 非作者不能编辑他人文章")
		void canEdit_OthersArticle_ReturnsFalse() {
			assertFalse(draftArticle.canEdit(2L));
		}

		@Test
		@DisplayName("canDelete - 作者可以删除自己的草稿")
		void canDelete_OwnDraft_ReturnsTrue() {
			assertTrue(draftArticle.canDelete(1L, false));
		}

		@Test
		@DisplayName("canDelete - 管理员可以删除任何文章")
		void canDelete_Admin_ReturnsTrue() {
			assertTrue(draftArticle.canDelete(2L, true));
			assertTrue(publishedArticle.canDelete(2L, true));
		}

		@Test
		@DisplayName("canAudit - 只有待审核文章可以审核")
		void canAudit_ReturnsCorrectStatus() {
			assertTrue(pendingArticle.canReview());
			assertFalse(draftArticle.canReview());
			assertFalse(publishedArticle.canReview());
		}

		@Test
		@DisplayName("canRetract - 只有已发布文章可以撤回")
		void canRetract_ReturnsCorrectStatus() {
			assertTrue(publishedArticle.canRetract(1L, false));
			assertFalse(draftArticle.canRetract(1L, false));
		}

		@Test
		@DisplayName("canView - 只有已发布文章可以公开查看")
		void canView_ReturnsCorrectStatus() {
			assertTrue(publishedArticle.canView());
			assertFalse(draftArticle.canView());
			assertFalse(pendingArticle.canView());
		}
	}
}
