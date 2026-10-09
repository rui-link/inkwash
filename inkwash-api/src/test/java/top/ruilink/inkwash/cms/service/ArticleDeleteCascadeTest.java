package top.ruilink.inkwash.cms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CategoryMapper;
import top.ruilink.inkwash.cms.mapper.CommentInteractionMapper;
import top.ruilink.inkwash.cms.mapper.CommentMapper;
import top.ruilink.inkwash.cms.mapper.InteractionMapper;
import top.ruilink.inkwash.cms.mapper.TermMapper;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.support.file.service.FileUploadService;
import top.ruilink.inkwash.base.util.SortValidator;
import top.ruilink.inkwash.cms.service.impl.ArticleServiceImpl;
import top.ruilink.inkwash.cms.service.ArticleViewResolver;

/**
 * Locks the cascading cleanup performed by
 * {@code ArticleServiceImpl.deleteArticle} (ISS-012).
 *
 * <p>
 * Before the fix the method deleted only the article row and its term links,
 * leaving one orphan row per reader in {@code cms_interaction} and every
 * comment thread intact. Those orphans then surfaced through the "my favourites
 * / likes / comments" queries as references to articles that no longer existed.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("删除文章的级联清理（ISS-012）")
class ArticleDeleteCascadeTest {

	private static final Long ARTICLE_ID = 77L;
	private static final Long AUTHOR_ID = 5L;

	@Mock
	private ArticleMapper articleMapper;
	@Mock
	private CommentMapper commentMapper;
	@Mock
	private CommentInteractionMapper commentInteractionMapper;
	@Mock
	private InteractionMapper interactionMapper;
	@Mock
	private CategoryMapper categoryMapper;
	@Mock
	private TermMapper termMapper;
	@Mock
	private ArticleViewResolver articleViewResolver;
	@Mock
	private SensitiveService sensitiveService;
	@Mock
	private SortValidator sortValidator;
	@Mock
	private FileUploadService fileUploadService;
	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ArticleServiceImpl articleService;

	private Article deletableArticle() {
		Article article = new Article();
		article.setId(ARTICLE_ID);
		article.setAuthorId(AUTHOR_ID);
		article.setStatus(ArticleStatus.DRAFT);
		article.setCoverUrl(null);
		return article;
	}

	@Nested
	@DisplayName("级联删除")
	class Cascade {

		@Test
		@DisplayName("应同时清理评论、评论互动与文章互动")
		void deletesAllDependentTables() {
			when(articleMapper.selectById(ARTICLE_ID)).thenReturn(deletableArticle());

			try (org.mockito.MockedStatic<top.ruilink.inkwash.security.util.SecurityUtil> securityUtil = org.mockito.Mockito
					.mockStatic(top.ruilink.inkwash.security.util.SecurityUtil.class)) {
				securityUtil.when(top.ruilink.inkwash.security.util.SecurityUtil::getCurrentUserId)
						.thenReturn(AUTHOR_ID);
				securityUtil.when(top.ruilink.inkwash.security.util.SecurityUtil::isAdmin).thenReturn(true);

				articleService.deleteArticle(ARTICLE_ID);
			}

			verify(commentInteractionMapper).deleteByArticleId(ARTICLE_ID);
			verify(commentMapper).deleteByArticleId(ARTICLE_ID);
			verify(interactionMapper).deleteByArticleId(ARTICLE_ID);
			verify(articleMapper).deleteById(ARTICLE_ID);
			verify(articleMapper).deleteTermsByArticleId(ARTICLE_ID);
		}

		@Test
		@DisplayName("评论互动必须先于评论删除 —— 否则 comment_id 无法解析")
		void commentInteractionsAreRemovedBeforeComments() {
			when(articleMapper.selectById(ARTICLE_ID)).thenReturn(deletableArticle());

			try (org.mockito.MockedStatic<top.ruilink.inkwash.security.util.SecurityUtil> securityUtil = org.mockito.Mockito
					.mockStatic(top.ruilink.inkwash.security.util.SecurityUtil.class)) {
				securityUtil.when(top.ruilink.inkwash.security.util.SecurityUtil::getCurrentUserId)
						.thenReturn(AUTHOR_ID);
				securityUtil.when(top.ruilink.inkwash.security.util.SecurityUtil::isAdmin).thenReturn(true);

				articleService.deleteArticle(ARTICLE_ID);
			}

			InOrder order = inOrder(commentInteractionMapper, commentMapper, interactionMapper, articleMapper);
			order.verify(commentInteractionMapper).deleteByArticleId(ARTICLE_ID);
			order.verify(commentMapper).deleteByArticleId(ARTICLE_ID);
			order.verify(interactionMapper).deleteByArticleId(ARTICLE_ID);
			order.verify(articleMapper).deleteById(ARTICLE_ID);
		}

		@Test
		@DisplayName("文章不存在时不得执行任何级联删除")
		void doesNotCascadeWhenArticleMissing() {
			when(articleMapper.selectById(ARTICLE_ID)).thenReturn(null);

			assertThrows(BusinessException.class, () -> articleService.deleteArticle(ARTICLE_ID));

			verify(commentInteractionMapper, never()).deleteByArticleId(eq(ARTICLE_ID));
			verify(commentMapper, never()).deleteByArticleId(eq(ARTICLE_ID));
			verify(interactionMapper, never()).deleteByArticleId(eq(ARTICLE_ID));
			verify(articleMapper, never()).deleteById(eq(ARTICLE_ID));
		}
	}

	@Nested
	@DisplayName("Mapper SQL 契约")
	class MapperSql {

		@Test
		@DisplayName("三个级联语句都以 article_id 为条件")
		void cascadeStatementsAreScopedToTheArticle() {
			assertEquals("DELETE FROM cms_comment WHERE article_id = #{articleId}", deleteSql(CommentMapper.class));
			assertEquals("DELETE FROM cms_interaction WHERE article_id = #{articleId}",
					deleteSql(InteractionMapper.class));
			assertEquals(
					"DELETE FROM cms_comment_interaction WHERE comment_id IN "
							+ "(SELECT id FROM cms_comment WHERE article_id = #{articleId})",
					deleteSql(CommentInteractionMapper.class));
		}

		@Test
		@DisplayName("删除单条评论的语句不受影响（ISS-034 的级联清理仍然有效）")
		void singleCommentDeleteIsUnchanged() {
			assertEquals("DELETE FROM cms_comment_interaction WHERE comment_id = #{commentId}",
					deleteSql(CommentInteractionMapper.class, "deleteByComment"));
		}

		private String deleteSql(Class<?> mapper) {
			return deleteSql(mapper, "deleteByArticleId");
		}

		private String deleteSql(Class<?> mapper, String method) {
			try {
				org.apache.ibatis.annotations.Delete d = mapper.getMethod(method, Long.class)
						.getAnnotation(org.apache.ibatis.annotations.Delete.class);
				assertNotNull(d, mapper.getSimpleName() + "#" + method + " 缺少 @Delete");
				return String.join(" ", d.value()).replaceAll("\\s+", " ").trim();
			} catch (NoSuchMethodException e) {
				throw new AssertionError(mapper.getSimpleName() + "#" + method + " 不存在", e);
			}
		}
	}
}