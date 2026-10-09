package top.ruilink.inkwash.cms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import top.ruilink.inkwash.cms.api.view.ArticleView;
import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.api.view.ArticleTermView;
import top.ruilink.inkwash.cms.domain.Category;
import top.ruilink.inkwash.cms.domain.Term;
import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CategoryMapper;
import top.ruilink.inkwash.cms.mapper.TermMapper;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.mapper.UserMapper;

/**
 * Article view resolver unit tests for batched association loading without
 * per-row queries.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class ArticleViewResolverTest {

	@Mock
	private ArticleMapper articleMapper;
	@Mock
	private CategoryMapper categoryMapper;
	@Mock
	private TermMapper termMapper;
	@Mock
	private UserMapper userMapper;

	private ArticleViewResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new ArticleViewResolver(articleMapper, categoryMapper, termMapper, userMapper);
	}

	private Article createArticle(Long id, Long authorId, Integer categoryId) {
		Article article = new Article();
		article.setId(id);
		article.setTitle("Article " + id);
		article.setSlug("article-" + id);
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setAuthorId(authorId);
		article.setCategoryId(categoryId);
		return article;
	}

	private SysUser user(Long id, String nickname) {
		SysUser user = new SysUser();
		user.setId(id);
		user.setNickname(nickname);
		return user;
	}

	@Test
	@DisplayName("批量解析作者只执行一次 IN 查询，不做逐行 selectById")
	void resolvesAuthorsWithSingleBatchQuery() {
		var article1 = createArticle(1L, 10L, 1);
		var article2 = createArticle(2L, 20L, 2);
		when(userMapper.selectByIds(anyList())).thenReturn(List.of(user(10L, "alice"), user(20L, "bob")));
		when(categoryMapper.selectByIds(anyList())).thenReturn(List.of());
		when(articleMapper.selectArticleTermsByArticleIds(anyList())).thenReturn(List.of());

		List<ArticleView> views = resolver.toViews(List.of(article1, article2));

		assertEquals(2, views.size());
		assertEquals("alice", views.get(0).getAuthor());
		assertEquals("bob", views.get(1).getAuthor());
		verify(userMapper, times(1)).selectByIds(anyList());
		verify(userMapper, never()).selectById(any());
	}

	@Test
	@DisplayName("同一作者多篇文章去重后只查一次")
	void deduplicatesSharedAuthors() {
		var article1 = createArticle(1L, 10L, 1);
		var article2 = createArticle(2L, 10L, 1);
		when(userMapper.selectByIds(List.of(10L))).thenReturn(List.of(user(10L, "alice")));
		when(categoryMapper.selectByIds(anyList())).thenReturn(List.of());
		when(articleMapper.selectArticleTermsByArticleIds(anyList())).thenReturn(List.of());

		List<ArticleView> views = resolver.toViews(List.of(article1, article2));

		assertEquals("alice", views.get(0).getAuthor());
		assertEquals("alice", views.get(1).getAuthor());
		verify(userMapper, times(1)).selectByIds(List.of(10L));
		verify(userMapper, never()).selectById(any());
	}

	@Test
	@DisplayName("单篇 toView 内部也走批量查询填充作者/分类/标签")
	void singleToViewFillsAssociationsViaBatchQueries() {
		var article = createArticle(1L, 10L, 1);
		Category category = new Category();
		category.setId(1);
		category.setName("Tech");
		when(userMapper.selectByIds(List.of(10L))).thenReturn(List.of(user(10L, "alice")));
		when(categoryMapper.selectByIds(List.of(1L))).thenReturn(List.of(category));
		when(articleMapper.selectArticleTermsByArticleIds(List.of(1L))).thenReturn(List.of(ofTerm(1L, 100)));
		when(termMapper.selectByIds(List.of(100))).thenReturn(List.of(term(100, "Java")));

		ArticleView view = resolver.toView(article);

		assertNotNull(view);
		assertEquals("alice", view.getAuthor());
		assertEquals("Tech", view.getCategory().getName());
		assertEquals(1, view.getTerms().size());
		assertEquals("Java", view.getTerms().get(0).getName());
		verify(userMapper, times(1)).selectByIds(List.of(10L));
		verify(userMapper, never()).selectById(any());
	}

	private ArticleTermView ofTerm(Long articleId, Integer termId) {
		ArticleTermView row = new ArticleTermView();
		row.setArticleId(articleId);
		row.setTermId(termId);
		return row;
	}

	private Term term(Integer id, String name) {
		Term term = new Term();
		term.setId(id);
		term.setName(name);
		return term;
	}
}