package top.ruilink.inkwash.cms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.cms.api.view.ArticleView;
import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.domain.Interaction;
import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CommentMapper;
import top.ruilink.inkwash.cms.mapper.InteractionMapper;
import top.ruilink.inkwash.cms.service.impl.InteractionServiceImpl;
import top.ruilink.inkwash.security.util.SecurityUtil;

/**
 * Interaction service unit tests for paged favorites, agrees, and commented
 * article lists.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

	@Mock
	private ArticleMapper articleMapper;
	@Mock
	private InteractionMapper interactionMapper;
	@Mock
	private CommentMapper commentMapper;
	@Mock
	private ArticleViewResolver articleViewResolver;

	@InjectMocks
	private InteractionServiceImpl interactionService;

	private static final Long USER_ID = 100L;

	@BeforeEach
	void setUp() {
	}

	private Article createArticle(Long id) {
		Article article = new Article();
		article.setId(id);
		article.setTitle("Article " + id);
		article.setSlug("article-" + id);
		article.setSummary("Summary " + id);
		article.setCoverUrl("https://example.com/cover" + id + ".jpg");
		article.setContent("Content " + id);
		article.setContentType("markdown");
		article.setAuthorId(1L);
		article.setCategoryId(1);
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setViewCount(10L);
		article.setCommentCount(2L);
		article.setAgreeCount(5L);
		article.setFavoriteCount(3L);
		article.setShareCount(1L);
		article.setPublishTime(LocalDateTime.now().minusDays(1));
		article.setCreateTime(LocalDateTime.now().minusDays(2));
		article.setUpdateTime(LocalDateTime.now().minusDays(1));
		return article;
	}

	@Nested
	@DisplayName("getFavorites")
	class GetFavoritesTests {

		@Test
		@DisplayName("should return empty page when user has no favorites")
		void shouldReturnEmptyPage_whenUserHasNoFavorites() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(interactionMapper.selectFavoriteArticleIdsPaged(eq(USER_ID), eq(10), eq(0)))
						.thenReturn(Collections.emptyList());

				PageResult<ArticleView> result = interactionService.getFavorites(1, 10);

				assertNotNull(result);
				assertEquals(0, result.getList().size());
				assertEquals(0, result.getTotal());
			}
		}

		@Test
		@DisplayName("should return first page with correct total when user has favorites")
		void shouldReturnFirstPage_whenUserHasMultipleFavorites() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				List<Long> pageIds = List.of(30L, 20L, 10L, 8L, 5L, 3L, 2L, 1L);
				when(interactionMapper.selectFavoriteArticleIdsPaged(eq(USER_ID), eq(10), eq(0))).thenReturn(pageIds);
				when(interactionMapper.countFavorites(USER_ID)).thenReturn(25L);

				List<Article> articles = pageIds.stream().map(id -> createArticle(id)).toList();
				when(articleMapper.selectByIds(pageIds)).thenReturn(articles);
				when(articleViewResolver.toViews(articles)).thenAnswer(invocation -> {
					List<Article> list = invocation.getArgument(0);
					return list.stream().map(a -> {
						ArticleView view = new ArticleView();
						view.setId(a.getId());
						return view;
					}).toList();
				});

				PageResult<ArticleView> result = interactionService.getFavorites(1, 10);

				assertNotNull(result);
				assertEquals(8, result.getList().size());
				assertEquals(25, result.getTotal());
				assertEquals(1, result.getPageNum());
				assertEquals(10, result.getPageSize());
			}
		}

		@Test
		@DisplayName("should return empty page when page exceeds total")
		void shouldReturnEmptyPage_whenPageExceedsTotal() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(interactionMapper.selectFavoriteArticleIdsPaged(eq(USER_ID), eq(10), eq(90)))
						.thenReturn(Collections.emptyList());

				PageResult<ArticleView> result = interactionService.getFavorites(10, 10);

				assertNotNull(result);
				assertEquals(0, result.getList().size());
			}
		}

		@Test
		@DisplayName("should filter out deleted articles")
		void shouldFilterDeletedArticles() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);

				List<Long> pageIds = List.of(10L, 20L, 30L);
				when(interactionMapper.selectFavoriteArticleIdsPaged(eq(USER_ID), eq(10), eq(0))).thenReturn(pageIds);
				when(interactionMapper.countFavorites(USER_ID)).thenReturn(3L);

				// Article 20 was deleted (not returned by mapper)
				Article article10 = createArticle(10L);
				Article article30 = createArticle(30L);
				List<Article> found = List.of(article10, article30);
				when(articleMapper.selectByIds(pageIds)).thenReturn(found);
				when(articleViewResolver.toViews(found)).thenAnswer(invocation -> {
					List<Article> list = invocation.getArgument(0);
					return list.stream().map(a -> {
						ArticleView view = new ArticleView();
						view.setId(a.getId());
						return view;
					}).toList();
				});

				PageResult<ArticleView> result = interactionService.getFavorites(1, 10);

				assertNotNull(result);
				assertEquals(2, result.getList().size());
				assertTrue(result.getList().stream().noneMatch(v -> v.getId().equals(20L)));
			}
		}
	}

	@Nested
	@DisplayName("getAgrees")
	class GetAgreesTests {

		@Test
		@DisplayName("should return empty page when user has no agrees")
		void shouldReturnEmptyPage_whenUserHasNoAgrees() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(interactionMapper.selectAgreeArticleIdsPaged(eq(USER_ID), eq(10), eq(0)))
						.thenReturn(Collections.emptyList());

				PageResult<ArticleView> result = interactionService.getAgrees(1, 10);

				assertNotNull(result);
				assertEquals(0, result.getList().size());
			}
		}
	}

	@Nested
	@DisplayName("getCommentedArticles")
	class GetCommentedArticlesTests {

		@Test
		@DisplayName("should return empty page when user has no comments")
		void shouldReturnEmptyPage_whenUserHasNoComments() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(commentMapper.selectDistinctArticleIdsByCommenterPaged(eq(USER_ID), eq(10), eq(0)))
						.thenReturn(Collections.emptyList());

				PageResult<ArticleView> result = interactionService.getCommentedArticles(1, 10);

				assertNotNull(result);
				assertEquals(0, result.getList().size());
			}
		}
	}

	@Nested
	@DisplayName("share / unshare")
	class ShareTests {

		private static final Long ARTICLE_ID = 200L;

		@Test
		@DisplayName("首次分享应写入互动行并增加计数")
		void shouldCreateInteractionAndIncrement_whenFirstShare() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(null);

				interactionService.shareArticle(ARTICLE_ID);

				ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
				verify(interactionMapper).insert(captor.capture());
				assertTrue(captor.getValue().getShare());
				verify(articleMapper).incrementShareCount(ARTICLE_ID);
			}
		}

		@Test
		@DisplayName("重复分享不应重复计数 —— ISS-011 的核心回归")
		void shouldNotIncrementAgain_whenAlreadyShared() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));

				Interaction existing = new Interaction();
				existing.setArticleId(ARTICLE_ID);
				existing.setActorId(USER_ID);
				existing.setAgree(false);
				existing.setFavorite(false);
				existing.setShare(true);
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(existing);

				interactionService.shareArticle(ARTICLE_ID);
				interactionService.shareArticle(ARTICLE_ID);

				verify(articleMapper, never()).incrementShareCount(ARTICLE_ID);
				verify(interactionMapper, never()).insert(any());
			}
		}

		@Test
		@DisplayName("取消分享应清除分享位并递减计数")
		void shouldClearFlagAndDecrement_whenUnshare() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));

				Interaction existing = new Interaction();
				existing.setArticleId(ARTICLE_ID);
				existing.setActorId(USER_ID);
				existing.setAgree(false);
				existing.setFavorite(false);
				existing.setShare(true);
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(existing);

				interactionService.unshareArticle(ARTICLE_ID);

				ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
				verify(interactionMapper).update(captor.capture());
				assertFalse(captor.getValue().getShare());
				verify(articleMapper).decrementShareCount(ARTICLE_ID);
			}
		}

		@Test
		@DisplayName("未分享时取消分享应为无操作")
		void shouldDoNothing_whenUnshareWithoutShare() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));

				Interaction existing = new Interaction();
				existing.setArticleId(ARTICLE_ID);
				existing.setActorId(USER_ID);
				existing.setShare(false);
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(existing);

				interactionService.unshareArticle(ARTICLE_ID);

				verify(articleMapper, never()).decrementShareCount(ARTICLE_ID);
				verify(interactionMapper, never()).update(any());
			}
		}

		@Test
		@DisplayName("isShared 应读取互动行的分享位")
		void shouldReadShareFlag() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::isAuthenticated).thenReturn(true);
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);

				Interaction shared = new Interaction();
				shared.setShare(true);
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(shared);
				assertTrue(interactionService.isShared(ARTICLE_ID));

				Interaction notShared = new Interaction();
				notShared.setShare(false);
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(notShared);
				assertFalse(interactionService.isShared(ARTICLE_ID));
			}
		}
	}

	@Nested
	@DisplayName("点踩 / averse —— ISS-009 / D-05")
	class AverseTests {

		private static final Long ARTICLE_ID = 300L;

		@Test
		@DisplayName("首次点踩应写入互动行并增加计数")
		void firstAverseCreatesRowAndIncrements() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(null);

				interactionService.averseArticle(ARTICLE_ID);

				ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
				verify(interactionMapper).insert(captor.capture());
				assertTrue(captor.getValue().getAverse());
				assertFalse(captor.getValue().getAgree(), "新建互动行时其余标志位必须显式初始化为 false，否则会写入 NULL");
				verify(articleMapper).incrementAverseCount(ARTICLE_ID);
			}
		}

		@Test
		@DisplayName("重复点踩不应重复计数")
		void repeatedAverseDoesNotIncrementTwice() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));

				Interaction existing = new Interaction();
				existing.setArticleId(ARTICLE_ID);
				existing.setActorId(USER_ID);
				existing.setAverse(true);
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(existing);

				interactionService.averseArticle(ARTICLE_ID);
				interactionService.averseArticle(ARTICLE_ID);

				verify(articleMapper, never()).incrementAverseCount(ARTICLE_ID);
			}
		}

		@Test
		@DisplayName("取消点踩应清除标志位并递减计数")
		void unAverseClearsFlagAndDecrements() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));

				Interaction existing = new Interaction();
				existing.setArticleId(ARTICLE_ID);
				existing.setActorId(USER_ID);
				existing.setAverse(true);
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(existing);

				interactionService.unaverseArticle(ARTICLE_ID);

				ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
				verify(interactionMapper).update(captor.capture());
				assertFalse(captor.getValue().getAverse());
				verify(articleMapper).decrementAverseCount(ARTICLE_ID);
			}
		}

		@Test
		@DisplayName("点踩与点赞互不影响")
		void averseDoesNotDisturbAgree() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));

				Interaction existing = new Interaction();
				existing.setArticleId(ARTICLE_ID);
				existing.setActorId(USER_ID);
				existing.setAgree(true);
				existing.setAverse(false);
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(existing);

				interactionService.averseArticle(ARTICLE_ID);

				ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
				verify(interactionMapper).update(captor.capture());
				assertTrue(captor.getValue().getAgree(), "点赞位不应被点踩操作改写");
				assertTrue(captor.getValue().getAverse());
			}
		}
	}

	@Nested
	@DisplayName("互动时间列 —— D-16")
	class CreateTimeTests {

		private static final Long ARTICLE_ID = 300L;

		@Test
		@DisplayName("首次互动应写入 create_time")
		void firstInteractionStampsCreateTime() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(null);

				interactionService.favoriteArticle(ARTICLE_ID);

				ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
				verify(interactionMapper).insert(captor.capture());
				assertNotNull(captor.getValue().getCreateTime(), "排序依据的 create_time 必须在插入时写入，否则收藏列表无法按时间排序");
			}
		}

		@Test
		@DisplayName("开关互动不得刷新 create_time —— 它记录的是首次互动时间")
		void togglingMustNotRefreshCreateTime() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));

				LocalDateTime firstInteraction = LocalDateTime.of(2026, 1, 1, 9, 0, 0);
				Interaction existing = new Interaction();
				existing.setArticleId(ARTICLE_ID);
				existing.setActorId(USER_ID);
				existing.setAgree(false);
				existing.setFavorite(false);
				existing.setShare(false);
				existing.setAverse(false);
				existing.setCreateTime(firstInteraction);
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(existing);

				interactionService.favoriteArticle(ARTICLE_ID);

				ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
				verify(interactionMapper).update(captor.capture());
				assertEquals(firstInteraction, captor.getValue().getCreateTime(),
						"若在 update 时刷新 create_time，'最近互动'会退化为'最后被改动的标志位'");
			}
		}

		@Test
		@DisplayName("取消收藏同样不得改动 create_time")
		void untogglingMustNotTouchCreateTime() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));

				LocalDateTime firstInteraction = LocalDateTime.of(2026, 1, 1, 9, 0, 0);
				Interaction existing = new Interaction();
				existing.setArticleId(ARTICLE_ID);
				existing.setActorId(USER_ID);
				existing.setFavorite(true);
				existing.setCreateTime(firstInteraction);
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(existing);

				interactionService.unfavoriteArticle(ARTICLE_ID);

				ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
				verify(interactionMapper).update(captor.capture());
				assertEquals(firstInteraction, captor.getValue().getCreateTime());
			}
		}

		@Test
		@DisplayName("重新收藏沿用原 create_time —— 排序不因再次互动而置顶")
		void refavouritingKeepsOriginalTimestamp() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(articleMapper.selectById(ARTICLE_ID)).thenReturn(createArticle(ARTICLE_ID));

				LocalDateTime firstInteraction = LocalDateTime.of(2026, 1, 1, 9, 0, 0);
				Interaction existing = new Interaction();
				existing.setArticleId(ARTICLE_ID);
				existing.setActorId(USER_ID);
				existing.setFavorite(false);
				existing.setCreateTime(firstInteraction);
				when(interactionMapper.selectByArticleAndUser(ARTICLE_ID, USER_ID)).thenReturn(existing);

				interactionService.favoriteArticle(ARTICLE_ID);

				ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
				verify(interactionMapper).update(captor.capture());
				assertEquals(firstInteraction, captor.getValue().getCreateTime(),
						"create_time 表示首次互动，因此取消后再收藏不会把文章移回列表顶部");
			}
		}

		@Test
		@DisplayName("三个分页查询均按 create_time 倒序，并以 id 兜底同秒并列")
		void pagedQueriesSortByCreateTimeThenId() throws NoSuchMethodException {
			for (String method : List.of("selectFavoriteArticleIdsPaged", "selectAverseArticleIdsPaged",
					"selectAgreeArticleIdsPaged")) {
				org.apache.ibatis.annotations.Select select = InteractionMapper.class
						.getMethod(method, Long.class, int.class, int.class)
						.getAnnotation(org.apache.ibatis.annotations.Select.class);

				assertNotNull(select, method + " 缺少 @Select");
				String sql = String.join(" ", select.value());
				assertTrue(sql.contains("ORDER BY create_time DESC, id DESC"),
						method + " 必须按 create_time 倒序；id 仅作同秒并列的兜底，实际顺序: " + sql);
				assertFalse(sql.contains("ORDER BY id DESC LIMIT"), method + " 仍在仅按 id 排序，等于没有实现 D-16");
			}
		}
	}

	@Nested
	@DisplayName("我的点踩列表 —— ISS-009 / D-05")
	class AverseListTests {

		@Test
		@DisplayName("应按 averse 标志位分页查询")
		void listsAversedArticles() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
				when(interactionMapper.selectAverseArticleIdsPaged(eq(USER_ID), eq(10), eq(0)))
						.thenReturn(Collections.emptyList());

				PageResult<ArticleView> result = interactionService.getAverses(1, 10);

				assertNotNull(result);
				assertEquals(0, result.getList().size());
				assertEquals(0, result.getTotal());
				verify(interactionMapper).selectAverseArticleIdsPaged(eq(USER_ID), eq(10), eq(0));
			}
		}
	}
}
