package top.ruilink.inkwash.cms.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import top.ruilink.inkwash.base.domain.StatisticEntry;
import top.ruilink.inkwash.base.enums.StatisticRange;
import top.ruilink.inkwash.base.enums.StatisticUnit;
import top.ruilink.inkwash.cms.api.view.DashboardView;
import top.ruilink.inkwash.cms.api.view.StatisticView;
import top.ruilink.inkwash.cms.domain.Category;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CategoryMapper;
import top.ruilink.inkwash.system.mapper.UserMapper;

/**
 * Dashboard statistics unit tests for time bucketing, category rollup, and role
 * gating.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class DashboardStatsServiceTest {

	@Mock
	private ArticleMapper articleMapper;
	@Mock
	private UserMapper userMapper;
	@Mock
	private CategoryMapper categoryMapper;

	private DashboardStatsServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new DashboardStatsServiceImpl(articleMapper, userMapper, categoryMapper);
	}

	private static StatisticEntry row(LocalDate day, Integer status, long cnt) {
		StatisticEntry r = new StatisticEntry();
		r.setStatDay(day);
		r.setStatus(status);
		r.setTotal(cnt);
		return r;
	}

	private static StatisticEntry published(LocalDate day, long cnt) {
		StatisticEntry r = new StatisticEntry();
		r.setStatDay(day);
		r.setTotal(cnt);
		return r;
	}

	private static StatisticEntry categoryRow(LocalDate day, Integer categoryId) {
		StatisticEntry r = new StatisticEntry();
		r.setStatDay(day);
		r.setCategoryId(categoryId);
		return r;
	}

	@Test
	@DisplayName("admin 一周文章统计：按天归桶，created/pending/approved/rejected/published")
	void articleStats_adminWeek() {
		LocalDate today = LocalDate.now();
		when(articleMapper.countArticleByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), any())).thenReturn(List
				.of(row(today, 3, 2), row(today, 2, 1), row(today.minusDays(1), 5, 1), row(today.minusDays(1), 4, 1)));
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), any()))
				.thenReturn(List.of(published(today, 3)));

		List<StatisticView.ArticleStatItem> items = service.getArticleStats(StatisticRange.LAST_7D, 42L);

		assertEquals(7, items.size());
		StatisticView.ArticleStatItem last = items.get(6);
		assertEquals(today.toString(), last.getTimeAxis());
		assertEquals(3, last.getCreated());
		assertEquals(1, last.getPending());
		assertEquals(2, last.getApproved());
		assertEquals(0, last.getRejected());
		assertEquals(2, last.getPendingPublish());
		assertEquals(3, last.getPublished());
		StatisticView.ArticleStatItem prev = items.get(5);
		assertEquals(2, prev.getCreated());
		assertEquals(1, prev.getRejected());
	}

	@Test
	@DisplayName("admin 近12月文章统计：按月归桶 12 桶")
	void articleStats_adminYear() {
		int ym = LocalDate.now().getYear() * 100 + LocalDate.now().getMonthValue();
		StatisticEntry create = new StatisticEntry();
		create.setYearMonth(ym);
		create.setStatus(5);
		create.setTotal(3);
		StatisticEntry approve = new StatisticEntry();
		approve.setYearMonth(ym);
		approve.setStatus(3);
		approve.setTotal(2);
		StatisticEntry pub = new StatisticEntry();
		pub.setYearMonth(ym);
		pub.setTotal(1);
		when(articleMapper.countArticleByRange(eq(StatisticUnit.MONTH), any(LocalDateTime.class), any()))
				.thenReturn(List.of(create, approve));
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.MONTH), any(LocalDateTime.class), any()))
				.thenReturn(List.of(pub));

		List<StatisticView.ArticleStatItem> items = service.getArticleStats(StatisticRange.LAST_12M, 42L);

		assertEquals(12, items.size());
		assertEquals(YearMonth.from(LocalDate.now()).minusMonths(11).toString(), items.get(0).getTimeAxis());
		assertEquals(YearMonth.from(LocalDate.now()).toString(), items.get(11).getTimeAxis());
		assertEquals(5, items.get(11).getCreated());
		assertEquals(2, items.get(11).getApproved());
		assertEquals(2, items.get(11).getPendingPublish());
		assertEquals(1, items.get(11).getPublished());
	}

	@Test
	@DisplayName("admin 近30天文章统计：按日归桶 30 桶")
	void articleStats_admin30Days() {
		LocalDate today = LocalDate.now();
		LocalDate start = today.minusDays(29);
		when(articleMapper.countArticleByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), any()))
				.thenReturn(List.of(row(today, 2, 5)));
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), any()))
				.thenReturn(List.of());

		List<StatisticView.ArticleStatItem> items = service.getArticleStats(StatisticRange.LAST_30D, 42L);

		assertEquals(30, items.size());
		assertEquals(start.toString(), items.get(0).getTimeAxis());
		assertEquals(today.toString(), items.get(29).getTimeAxis());
		assertEquals(5, items.get(29).getPending());
		assertEquals(5, items.get(29).getCreated());
	}

	@Test
	@DisplayName("editor 近30天文章统计：按日归桶 30 桶")
	void articleStats_editorMonth() {
		LocalDate today = LocalDate.now();
		LocalDate start = today.minusDays(29);
		when(articleMapper.countArticleByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), any()))
				.thenReturn(List.of(row(start.plusDays(2), 2, 4), row(today, 3, 1)));
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), any()))
				.thenReturn(List.of());

		List<StatisticView.ArticleStatItem> items = service.getArticleStats(StatisticRange.LAST_30D, 7L);

		assertEquals(30, items.size());
		assertEquals(start.toString(), items.get(0).getTimeAxis());
		assertEquals(today.toString(), items.get(29).getTimeAxis());
		assertEquals(4, items.get(2).getPending());
		assertEquals(4, items.get(2).getCreated());
		assertEquals(0, items.get(2).getApproved());
		assertEquals(1, items.get(29).getPendingPublish());
	}

	@Test
	@DisplayName("admin 一年用户统计：按月去重回桶")
	void userStats_adminYear() {
		int ym = LocalDate.now().getYear() * 100 + LocalDate.now().getMonthValue();
		StatisticEntry users = new StatisticEntry();
		users.setYearMonth(ym);
		users.setTotal(2);
		when(userMapper.countUsersByRange(eq(StatisticUnit.MONTH), any(LocalDateTime.class)))
				.thenReturn(List.of(users));
		StatisticEntry authors = new StatisticEntry();
		authors.setYearMonth(ym);
		authors.setTotal(1);
		when(articleMapper.countActiveAuthorsByRange(eq(StatisticUnit.MONTH), any(LocalDateTime.class)))
				.thenReturn(List.of(authors));

		List<StatisticView.UserStatItem> items = service.getUserStats(StatisticRange.LAST_12M, 42L);

		assertEquals(12, items.size());
		assertEquals(2, items.get(11).getNewUsers());
		assertEquals(1, items.get(11).getActiveAuthors());
	}

	@Test
	@DisplayName("admin 近30天发文去重：同日同一作者只计 1")
	void user_adminMonthDistinctAuthors() {
		LocalDate today = LocalDate.now();
		LocalDate start = today.minusDays(29);
		when(userMapper.countUsersByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class))).thenReturn(List.of());
		when(articleMapper.countActiveAuthorsByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class)))
				.thenReturn(List.of(row(today, null, 2), row(today.minusDays(1), null, 1)));

		List<StatisticView.UserStatItem> items = service.getUserStats(StatisticRange.LAST_30D, 42L);

		assertEquals(30, items.size());
		assertEquals(start.toString(), items.get(0).getTimeAxis());
		assertEquals(today.toString(), items.get(29).getTimeAxis());
		assertEquals(1, items.get(28).getActiveAuthors());
		assertEquals(2, items.get(29).getActiveAuthors());
	}

	@Test
	@DisplayName("admin 本月用户统计：同一周段内多日发文作者去重")
	void userStats_thisMonthWeekDistinctAuthors() {
		LocalDate today = LocalDate.now();
		LocalDate first = today.withDayOfMonth(1);
		when(userMapper.countUsersByRange(eq(StatisticUnit.WEEK), any(LocalDateTime.class))).thenReturn(List.of());
		StatisticEntry authors = row(first, null, 2);
		when(articleMapper.countActiveAuthorsByRange(eq(StatisticUnit.WEEK), any(LocalDateTime.class)))
				.thenReturn(List.of(authors));

		List<StatisticView.UserStatItem> items = service.getUserStats(StatisticRange.THIS_MONTH, 42L);

		assertEquals(2, items.get(0).getActiveAuthors());
		verify(articleMapper).countActiveAuthorsByRange(eq(StatisticUnit.WEEK), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("admin 一周文章类型统计：子分类归并到顶级分类，未分类排最后")
	void categoryStats_adminWeek() {
		Category tech = new Category();
		tech.setId(1);
		tech.setName("技术");
		Category sub = new Category();
		sub.setId(2);
		sub.setName("后端");
		sub.setParentId(1);
		Category deep = new Category();
		deep.setId(3);
		deep.setName("Java");
		deep.setParentId(2);
		when(categoryMapper.selectAll()).thenReturn(List.of(tech, sub, deep));
		LocalDate today = LocalDate.now();
		StatisticEntry cat3 = categoryRow(today, 3);
		cat3.setTotal(1);
		StatisticEntry cat4 = categoryRow(today, 4);
		cat4.setTotal(1);
		when(articleMapper.countCategoryByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class)))
				.thenReturn(List.of(cat3, cat4));

		List<StatisticView.CategoryStatItem> items = service.getCategoryStats(StatisticRange.LAST_7D, 42L);

		StatisticView.CategoryStatItem item = items.get(6);
		assertEquals(2, item.getCategories().size());
		StatisticView.CategoryCount first = item.getCategories().get(0);
		assertEquals(1, first.getCategoryId());
		assertEquals("技术", first.getCategoryName());
		assertEquals(1, first.getCount());
		StatisticView.CategoryCount second = item.getCategories().get(1);
		assertEquals(4, second.getCategoryId());
		assertEquals(null, second.getCategoryName());
		assertEquals(1, second.getCount());
	}

	@Test
	@DisplayName("普通用户一周文章统计：仅本人数据按天归桶")
	void myArticleStats_weekOnlyMine() {
		LocalDate today = LocalDate.now();
		when(articleMapper.countArticleByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of(row(today, 2, 1), row(today, 3, 2), row(today.minusDays(1), 4, 1)));
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of(published(today, 3)));

		StatisticView.MyArticleStats out = service.getMyArticleStats(StatisticRange.LAST_7D, 42L);
		List<StatisticView.ArticleStatItem> items = out.getItems();

		assertEquals(7, items.size());
		assertNull(out.getPrevCreated());
		StatisticView.ArticleStatItem last = items.get(6);
		assertEquals(today.toString(), last.getTimeAxis());
		assertEquals(1, last.getPending());
		assertEquals(2, last.getApproved());
		assertEquals(0, last.getRejected());
		assertEquals(2, last.getPendingPublish());
		assertEquals(3, last.getPublished());
		assertEquals(3, last.getCreated());
		assertEquals(1, items.get(5).getRejected());
		verify(articleMapper).countArticleByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), eq(42L));
	}

	@Test
	@DisplayName("普通用户近30天文章统计：按日归桶 30 桶")
	void myArticleStats_month() {
		LocalDate today = LocalDate.now();
		LocalDate start = today.minusDays(29);
		when(articleMapper.countArticleByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of(row(start.plusDays(2), 2, 4)));
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of());

		StatisticView.MyArticleStats out = service.getMyArticleStats(StatisticRange.LAST_30D, 42L);
		List<StatisticView.ArticleStatItem> items = out.getItems();

		assertEquals(30, items.size());
		assertNull(out.getPrevCreated());
		assertEquals(start.toString(), items.get(0).getTimeAxis());
		assertEquals(today.toString(), items.get(29).getTimeAxis());
		assertEquals(4, items.get(2).getPending());
		assertEquals(4, items.get(2).getCreated());
	}

	@Test
	@DisplayName("普通用户一年文章统计：按月归桶")
	void myArticleStats_year() {
		int ym = LocalDate.now().getYear() * 100 + LocalDate.now().getMonthValue();
		StatisticEntry s2 = new StatisticEntry();
		s2.setYearMonth(ym);
		s2.setStatus(2);
		s2.setTotal(1);
		StatisticEntry s4 = new StatisticEntry();
		s4.setYearMonth(ym);
		s4.setStatus(4);
		s4.setTotal(2);
		StatisticEntry pub = new StatisticEntry();
		pub.setYearMonth(ym);
		pub.setTotal(3);
		when(articleMapper.countArticleByRange(eq(StatisticUnit.MONTH), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of(s2, s4));
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.MONTH), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of(pub));

		StatisticView.MyArticleStats out = service.getMyArticleStats(StatisticRange.LAST_12M, 42L);
		List<StatisticView.ArticleStatItem> items = out.getItems();

		assertEquals(12, items.size());
		assertNull(out.getPrevCreated());
		StatisticView.ArticleStatItem last = items.get(11);
		assertEquals(1, last.getPending());
		assertEquals(2, last.getRejected());
		assertEquals(0, last.getApproved());
		assertEquals(3, last.getPublished());
		assertEquals(3, last.getCreated());
	}

	@Test
	@DisplayName("普通用户本月文章统计：本月1日起每7天一段，附上月环比")
	void myArticleStats_thisMonth() {
		LocalDate today = LocalDate.now();
		LocalDate first = today.withDayOfMonth(1);
		when(articleMapper.countArticleByRange(eq(StatisticUnit.WEEK), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of(row(today, 2, 3)));
		when(articleMapper.countCreatedByRange(first.minusMonths(1).atStartOfDay(), first.atStartOfDay(), 42L))
				.thenReturn(5L);
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.WEEK), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of());

		StatisticView.MyArticleStats out = service.getMyArticleStats(StatisticRange.THIS_MONTH, 42L);
		List<StatisticView.ArticleStatItem> items = out.getItems();

		int expectedBuckets = (today.getDayOfMonth() + 6) / 7;
		assertEquals(expectedBuckets, items.size());
		assertEquals(first.toString(), items.get(0).getTimeAxis());
		LocalDate lastStart = first.plusDays((long) (items.size() - 1) * 7);
		assertEquals(lastStart.toString(), items.get(items.size() - 1).getTimeAxis());
		assertEquals(3, items.get(items.size() - 1).getPending());
		assertEquals(3, items.get(items.size() - 1).getCreated());
		assertEquals(5L, out.getPrevCreated());
	}

	@Test
	@DisplayName("普通用户上月文章统计：整月每7天一段，附前月环比")
	void myArticleStats_lastMonth() {
		LocalDate today = LocalDate.now();
		LocalDate firstCur = today.withDayOfMonth(1);
		LocalDate firstLast = firstCur.minusMonths(1);
		LocalDate lastDay = firstCur.minusDays(1);
		when(articleMapper.countArticleByRange(eq(StatisticUnit.WEEK), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of(row(lastDay, 4, 2)));
		when(articleMapper.countCreatedByRange(firstCur.minusMonths(2).atStartOfDay(),
				firstCur.minusMonths(1).atStartOfDay(), 42L)).thenReturn(6L);
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.WEEK), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of());

		StatisticView.MyArticleStats out = service.getMyArticleStats(StatisticRange.LAST_MONTH, 42L);
		List<StatisticView.ArticleStatItem> items = out.getItems();

		int expectedBuckets = (YearMonth.from(firstLast).lengthOfMonth() + 6) / 7;
		assertEquals(expectedBuckets, items.size());
		assertEquals(firstLast.toString(), items.get(0).getTimeAxis());
		LocalDate lastStart = firstLast.plusDays((long) (items.size() - 1) * 7);
		assertEquals(lastStart.toString(), items.get(items.size() - 1).getTimeAxis());
		assertEquals(2, items.get(items.size() - 1).getRejected());
		assertEquals(6L, out.getPrevCreated());
	}

	@Test
	@DisplayName("上月统计：落在当前月的数据行被端排除，不污染上月桶合计与环比")
	void myArticleStats_lastMonth_excludesCurrentMonthRows() {
		LocalDate today = LocalDate.now();
		LocalDate firstCur = today.withDayOfMonth(1);
		LocalDate firstLast = firstCur.minusMonths(1);
		LocalDate lastDay = firstCur.minusDays(1);
		when(articleMapper.countArticleByRange(eq(StatisticUnit.WEEK), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of(row(lastDay, 2, 3), row(today, 2, 99)));
		when(articleMapper.countCreatedByRange(firstCur.minusMonths(2).atStartOfDay(),
				firstCur.minusMonths(1).atStartOfDay(), 42L)).thenReturn(6L);
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.WEEK), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of(published(today, 50)));

		StatisticView.MyArticleStats out = service.getMyArticleStats(StatisticRange.LAST_MONTH, 42L);
		List<StatisticView.ArticleStatItem> items = out.getItems();

		int expectedBuckets = (YearMonth.from(firstLast).lengthOfMonth() + 6) / 7;
		assertEquals(expectedBuckets, items.size());
		StatisticView.ArticleStatItem last = items.get(items.size() - 1);
		assertEquals(3, last.getCreated());
		assertEquals(3, last.getPending());
		assertEquals(0, last.getPublished());
		assertEquals(6L, out.getPrevCreated());
	}

	@Test
	@DisplayName("本月统计环比：prevCreated 走 SQL 聚合 COUNT，不再加载前月整桶日数据")
	void prevCreated_usesSqlAggregateNotDayBucket() {
		LocalDate today = LocalDate.now();
		LocalDate first = today.withDayOfMonth(1);
		when(articleMapper.countArticleByRange(eq(StatisticUnit.WEEK), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of());
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.WEEK), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of());
		when(articleMapper.countCreatedByRange(first.minusMonths(1).atStartOfDay(), first.atStartOfDay(), 42L))
				.thenReturn(8L);

		StatisticView.MyArticleStats out = service.getMyArticleStats(StatisticRange.THIS_MONTH, 42L);

		assertEquals(8L, out.getPrevCreated());
		verify(articleMapper).countCreatedByRange(first.minusMonths(1).atStartOfDay(), first.atStartOfDay(), 42L);
		verify(articleMapper, never()).countArticleByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), eq(42L));
	}

	@Test
	@DisplayName("近30天乱序行归桶：每桶独立累计，与行顺序无关")
	void articleStats_outOfOrderDaysBucketCorrectly() {
		LocalDate today = LocalDate.now();
		List<StatisticEntry> rows = new ArrayList<>();
		for (int i = 29; i >= 0; i--) {
			rows.add(row(today.minusDays(i), 3, i + 1L));
		}
		when(articleMapper.countArticleByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), any()))
				.thenReturn(rows);
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), any()))
				.thenReturn(List.of());

		List<StatisticView.ArticleStatItem> items = service.getArticleStats(StatisticRange.LAST_30D, 42L);

		assertEquals(30, items.size());
		for (int i = 0; i < 30; i++) {
			assertEquals(30 - i, items.get(i).getCreated());
		}
	}

	@Test
	@DisplayName("admin 去年用户统计：去年数据正确计桶，当前年数据行忽略且不抛异常")
	void userStats_lastYear_outOfWindowRowsIgnored() {
		int lastYm = (LocalDate.now().getYear() - 1) * 100 + LocalDate.now().getMonthValue();
		int curYm = LocalDate.now().getYear() * 100 + LocalDate.now().getMonthValue();
		StatisticEntry users = new StatisticEntry();
		users.setYearMonth(lastYm);
		users.setTotal(2);
		StatisticEntry curUsers = new StatisticEntry();
		curUsers.setYearMonth(curYm);
		curUsers.setTotal(9);
		when(userMapper.countUsersByRange(eq(StatisticUnit.MONTH), any(LocalDateTime.class)))
				.thenReturn(List.of(users, curUsers));
		StatisticEntry author = new StatisticEntry();
		author.setYearMonth(lastYm);
		author.setTotal(1);
		StatisticEntry curAuthor = new StatisticEntry();
		curAuthor.setYearMonth(curYm);
		curAuthor.setTotal(9);
		when(articleMapper.countActiveAuthorsByRange(eq(StatisticUnit.MONTH), any(LocalDateTime.class)))
				.thenReturn(List.of(author, curAuthor));

		List<StatisticView.UserStatItem> items = service.getUserStats(StatisticRange.LAST_YEAR, 42L);

		assertEquals(12, items.size());
		long totalUsers = items.stream().mapToLong(StatisticView.UserStatItem::getNewUsers).sum();
		long totalAuthors = items.stream().mapToLong(StatisticView.UserStatItem::getActiveAuthors).sum();
		assertEquals(2, totalUsers);
		assertEquals(1, totalAuthors);
	}

	@Test
	@DisplayName("普通用户无需角色即可获取个人统计（不检查角色）")
	void myArticleStats_noRoleCheck() {
		when(articleMapper.countArticleByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of());
		when(articleMapper.countPublishedByRange(eq(StatisticUnit.DAY), any(LocalDateTime.class), eq(42L)))
				.thenReturn(List.of());

		StatisticView.MyArticleStats out = service.getMyArticleStats(StatisticRange.LAST_7D, 42L);
		List<StatisticView.ArticleStatItem> items = out.getItems();

		assertEquals(7, items.size());
		assertNull(out.getPrevCreated());
		assertEquals(0, items.get(6).getPending());
	}

	@Test
	@DisplayName("summary 汇总各计数")
	void summary_aggregatesCounts() {
		when(articleMapper.count()).thenReturn(10L);
		when(articleMapper.countByStatus(eq(2))).thenReturn(5L);
		when(articleMapper.countByStatus(eq(3))).thenReturn(2L);
		when(articleMapper.countByStatus(eq(4))).thenReturn(1L);
		when(articleMapper.countByStatus(eq(5))).thenReturn(3L);
		when(userMapper.count()).thenReturn(8L);

		DashboardView.DashboardSummary s = service.getSummary(42L);

		assertEquals(10, s.getTotalArticles());
		assertEquals(5, s.getPendingReview());
		assertEquals(2, s.getApproved());
		assertEquals(1, s.getRejected());
		assertEquals(3, s.getPublished());
		assertEquals(8, s.getTotalUsers());
	}

	@Test
	@DisplayName("统计端点不再在服务层做角色判定，授权全部交给 @PreAuthorize")
	void roleGating_livesInPreAuthorizeNotHere() {
		// These calls were gated by ensureAdmin / ensureAdminOrEditor, which resolved
		// roles through a
		// second, independent query. That duplication is what let the editor dashboard
		// call an endpoint
		// its own view always rejected: /dashboard/article-stats allowed ROLE_EDITOR
		// while
		// /dashboard/category-stats denied it, so the editor view rendered a chart
		// backed by a
		// guaranteed 403. Access is now expressed once, as cms:dashboard:query (admin +
		// editor) and
		// cms:dashboard:user-stats (admin only) on the controller; PermissionSeedIT
		// locks the grants.
		when(articleMapper.count()).thenReturn(0L);
		when(articleMapper.countByStatus(anyInt())).thenReturn(0L);
		when(userMapper.count()).thenReturn(0L);

		assertDoesNotThrow(() -> service.getArticleStats(StatisticRange.LAST_7D, 9L));
		assertDoesNotThrow(() -> service.getSummary(9L));
		assertDoesNotThrow(() -> service.getCategoryStats(StatisticRange.LAST_7D, 9L));
		assertDoesNotThrow(() -> service.getUserStats(StatisticRange.LAST_7D, 9L));
	}
}
