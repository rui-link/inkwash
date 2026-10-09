package top.ruilink.inkwash.cms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.cms.api.param.ArticleParam;
import top.ruilink.inkwash.cms.api.param.CommentParam;
import top.ruilink.inkwash.cms.api.query.ArticleQuery;
import top.ruilink.inkwash.cms.api.view.ArticleView;
import top.ruilink.inkwash.cms.api.view.CommentView;
import top.ruilink.inkwash.cms.api.view.TallyView;
import top.ruilink.inkwash.cms.domain.ArticleTally;
import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.cms.service.ArticleService;
import top.ruilink.inkwash.cms.service.CommentService;
import top.ruilink.inkwash.cms.service.InteractionService;

/**
 * Article REST endpoint unit tests for the draft-to-publish workflow, comments,
 * and tallies.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

	@Mock
	private ArticleService articleService;

	@Mock
	private CommentService commentService;

	@Mock
	private InteractionService interactionService;

	@InjectMocks
	private ArticleController articleController;

	private ArticleView publishedArticleView;
	private ArticleView draftArticleView;
	private ArticleView pendingArticleView;
	private CommentView testCommentView;

	@BeforeEach
	void setUp() {
		publishedArticleView = new ArticleView();
		publishedArticleView.setId(1L);
		publishedArticleView.setTitle("已发布的文章");
		publishedArticleView.setContent("这是一篇已发布的文章内容");
		publishedArticleView.setAuthorId(100L);
		publishedArticleView.setAuthor("alice");
		publishedArticleView.setStatus(ArticleStatus.PUBLISHED);
		publishedArticleView.setTally(new ArticleTally());
		publishedArticleView.setCreateTime(LocalDateTime.now().minusDays(5));
		publishedArticleView.setUpdateTime(LocalDateTime.now());

		draftArticleView = new ArticleView();
		draftArticleView.setId(2L);
		draftArticleView.setTitle("草稿文章");
		draftArticleView.setContent("这是一篇草稿文章");
		draftArticleView.setAuthorId(100L);
		draftArticleView.setAuthor("alice");
		draftArticleView.setStatus(ArticleStatus.DRAFT);
		draftArticleView.setCreateTime(LocalDateTime.now().minusDays(1));
		draftArticleView.setUpdateTime(LocalDateTime.now());

		pendingArticleView = new ArticleView();
		pendingArticleView.setId(3L);
		pendingArticleView.setTitle("待审核文章");
		pendingArticleView.setContent("这是一篇待审核的文章");
		pendingArticleView.setAuthorId(100L);
		pendingArticleView.setAuthor("alice");
		pendingArticleView.setStatus(ArticleStatus.PENDING);
		pendingArticleView.setCreateTime(LocalDateTime.now().minusDays(2));
		pendingArticleView.setUpdateTime(LocalDateTime.now());

		testCommentView = new CommentView();
		testCommentView.setId(1L);
		testCommentView.setArticleId(1L);
		testCommentView.setContent("这是一条测试评论");
		testCommentView.setNickname("testuser");
		testCommentView.setCommenterId(100L);
		testCommentView.setCreateTime(LocalDateTime.now());
	}

	@Nested
	@DisplayName("创建文章测试")
	class CreateArticleTests {

		@Test
		@DisplayName("创建文章成功")
		void createArticle_Success() {
			ArticleParam param = new ArticleParam();
			param.setTitle("新文章");
			param.setContent("新文章内容");

			when(articleService.createArticle(any(ArticleParam.class))).thenReturn(draftArticleView);

			ResponseEntity<ArticleView> response = articleController.createArticle(param);

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
			assertEquals(2L, response.getBody().getId());
			verify(articleService).createArticle(any(ArticleParam.class));
		}
	}

	@Nested
	@DisplayName("修改文章测试")
	class UpdateArticleTests {

		@Test
		@DisplayName("作者编辑自己的草稿成功")
		void updateArticle_AuthorDraft_Success() {
			ArticleParam param = new ArticleParam();
			param.setTitle("更新的标题");

			when(articleService.updateArticle(eq(2L), any(ArticleParam.class))).thenReturn(draftArticleView);

			ResponseEntity<ArticleView> response = articleController.updateArticle(2L, param);

			assertEquals(200, response.getStatusCode().value());
		}
	}

	@Nested
	@DisplayName("删除文章测试")
	class DeleteArticleTests {

		@Test
		@DisplayName("删除文章成功")
		void deleteArticle_Success() {
			ResponseEntity<Void> response = articleController.deleteArticle(2L);

			assertEquals(204, response.getStatusCode().value());
			verify(articleService).deleteArticle(2L);
		}
	}

	@Nested
	@DisplayName("提交审核测试")
	class SubmitForReviewTests {

		@Test
		@DisplayName("作者提交草稿审核成功")
		void submitForReview_AuthorDraft_Success() {
			when(articleService.commitArticle(2L)).thenReturn(pendingArticleView);

			ResponseEntity<ArticleView> response = articleController.commitArticle(2L);

			assertEquals(200, response.getStatusCode().value());
			assertEquals(ArticleStatus.PENDING, response.getBody().getStatus());
		}
	}

	@Nested
	@DisplayName("审核文章测试")
	class AuditArticleTests {

		@Test
		@DisplayName("审核通过待审核文章成功")
		void auditArticle_Approve_Success() {
			ArticleView approvedView = new ArticleView();
			approvedView.setId(3L);
			approvedView.setStatus(ArticleStatus.PUBLISHED);
			when(articleService.reviewArticle(3L, true, "")).thenReturn(approvedView);

			ResponseEntity<ArticleView> response = articleController.reviewArticle(3L, true, "");

			assertEquals(200, response.getStatusCode().value());
			assertEquals(ArticleStatus.PUBLISHED, response.getBody().getStatus());
		}
	}

	@Nested
	@DisplayName("发布文章测试")
	class PublishArticleTests {

		@Test
		@DisplayName("发布待审核文章成功")
		void publishArticle_Pending_Success() {
			when(articleService.publishArticle(3L)).thenReturn(publishedArticleView);

			ResponseEntity<ArticleView> response = articleController.publishArticle(3L);

			assertEquals(200, response.getStatusCode().value());
			assertEquals(ArticleStatus.PUBLISHED, response.getBody().getStatus());
		}
	}

	@Nested
	@DisplayName("获取文章测试")
	class GetArticleTests {

		@Test
		@DisplayName("获取文章详情成功")
		void getArticle_Success() {
			when(articleService.getArticle(1L)).thenReturn(publishedArticleView);

			ResponseEntity<ArticleView> response = articleController.getArticle(1L);

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
			assertEquals(1L, response.getBody().getId());
			assertEquals("已发布的文章", response.getBody().getTitle());
		}
	}

	@Nested
	@DisplayName("获取文章列表测试")
	class ListPublishedArticlesTests {

		@Test
		@DisplayName("获取已发布文章列表成功")
		void listPublishedArticles_Success() {
			ArticleView articleView = new ArticleView();
			articleView.setId(1L);
			articleView.setTitle("已发布的文章");
			articleView.setStatus(ArticleStatus.PUBLISHED);

			var springPage = new PageImpl<>(Arrays.asList(articleView));
			PageResult<ArticleView> pageResult = PageResult.of(springPage);
			ArticleQuery query = new ArticleQuery();
			when(articleService.listAll(any(ArticleQuery.class))).thenReturn(pageResult);

			ResponseEntity<PageResult<ArticleView>> response = articleController.listAll(query);

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
			assertEquals(1, response.getBody().getList().size());
			assertEquals(1L, response.getBody().getList().get(0).getId());
		}
	}

	@Nested
	@DisplayName("评论功能测试")
	class CommentTests {

		@Test
		@DisplayName("添加评论成功")
		void addComment_Success() {
			CommentParam param = new CommentParam();
			param.setContent("测试评论内容");

			when(commentService.addComment(eq(1L), any(CommentParam.class))).thenReturn(testCommentView);

			ResponseEntity<CommentView> response = articleController.addComment(1L, param);

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
			assertEquals(1L, response.getBody().getId());
		}

		@Test
		@DisplayName("获取文章评论列表成功")
		void getComments_Success() {
			List<CommentView> comments = Arrays.asList(testCommentView);
			when(commentService.getComments(1L)).thenReturn(comments);

			ResponseEntity<List<CommentView>> response = articleController.getComments(1L);

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
			assertEquals(1, response.getBody().size());
			assertEquals(1L, response.getBody().get(0).getId());
		}
	}

	@Nested
	@DisplayName("文章交互状态测试")
	class TallyTests {

		@Test
		@DisplayName("getTally 返回 TallyView DTO 而非 Map")
		void getTally_ReturnsTallyView() {
			when(interactionService.isAgree(1L)).thenReturn(true);
			when(interactionService.isFavorited(1L)).thenReturn(false);

			ResponseEntity<TallyView> response = articleController.getTally(1L);

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
			assertTrue(response.getBody().isAgreed());
			assertFalse(response.getBody().isFavorited());
		}
	}
}
