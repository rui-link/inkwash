package top.ruilink.inkwash.cms.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import top.ruilink.inkwash.base.config.MybatisConfig;
import top.ruilink.inkwash.base.domain.StatisticEntry;
import top.ruilink.inkwash.base.enums.StatisticUnit;
import top.ruilink.inkwash.base.util.SortValidator;
import top.ruilink.inkwash.cms.api.query.ArticleQuery;
import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.domain.ArticleTally;
import top.ruilink.inkwash.cms.enums.ArticleStatus;

/**
 * ArticleMapper integration test verifying aggregation, pagination and
 * ${sortSql} ordering against H2 in MySQL mode.
 *
 * <p>
 * The datasource URL deliberately does <em>not</em> carry
 * {@code NON_KEYWORDS=DAY}: the application runs a stock H2 URL, so relaxing
 * the keywords here would let a reserved-word regression in the {@code AS day}
 * aliases pass unnoticed.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@MybatisTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:inkwash_it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
		"spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
		"spring.datasource.password=" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@Import(MybatisConfig.class)
@Sql(scripts = "cms-article-schema.sql")
class ArticleMapperIT {

	@Autowired
	private ArticleMapper articleMapper;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	private Article newArticle(LocalDateTime createTime, long authorId, Integer categoryId, ArticleStatus status) {
		Article article = new Article();
		article.setTitle("标题-" + createTime.toLocalDate() + "-" + System.nanoTime());
		article.setSlug("slug-" + System.nanoTime());
		article.setContentType("markdown");
		article.setAuthorId(authorId);
		article.setCategoryId(categoryId);
		article.setStatus(status);
		ArticleTally tally = new ArticleTally();
		tally.setViewCount(3L);
		tally.setCommentCount(1L);
		article.setTally(tally);
		article.setCreator(authorId);
		article.setCreateTime(createTime);
		article.setUpdateTime(createTime);
		if (status == ArticleStatus.PUBLISHED) {
			article.setPublishTime(createTime);
		}
		return article;
	}

	private Long insertArticle(LocalDateTime createTime, long authorId, Integer categoryId, ArticleStatus status) {
		Article article = newArticle(createTime, authorId, categoryId, status);
		articleMapper.insert(article);
		if (status == ArticleStatus.PUBLISHED) {
			// insert() does not write publish_time; publication-time is written by the
			// transition update, so route the fixture through it the same way the service
			// does.
			articleMapper.updateStatus(article, ArticleStatus.PUBLISHED.getCode());
		}
		return article.getId();
	}

	@Test
	void countActiveAuthorsByRange_distinctAuthorsPerDay() {
		LocalDate d1 = LocalDate.now().minusDays(2);
		LocalDate d2 = LocalDate.now().minusDays(1);
		LocalDate d3 = LocalDate.now();
		insertArticle(d1.atTime(9, 0), 1L, null, ArticleStatus.DRAFT);
		insertArticle(d1.atTime(10, 0), 1L, null, ArticleStatus.DRAFT);
		insertArticle(d1.atTime(11, 0), 2L, null, ArticleStatus.DRAFT);
		insertArticle(d2.atTime(9, 0), 1L, null, ArticleStatus.DRAFT);
		insertArticle(d2.atTime(10, 0), 2L, null, ArticleStatus.DRAFT);
		insertArticle(d2.atTime(11, 0), 3L, null, ArticleStatus.DRAFT);
		insertArticle(d3.atTime(9, 0), 1L, null, ArticleStatus.DRAFT);

		List<StatisticEntry> rows = articleMapper.countActiveAuthorsByRange(StatisticUnit.DAY, d1.atStartOfDay());

		assertEquals(3, rows.size());
		Map<LocalDate, Long> byDay = new HashMap<>();
		for (StatisticEntry row : rows) {
			byDay.put(row.getStatDay(), row.getTotal());
		}
		assertEquals(2L, byDay.get(d1));
		assertEquals(3L, byDay.get(d2));
		assertEquals(1L, byDay.get(d3));
	}

	@Test
	void countCategoryByRange_groupsByDayAndCategory() {
		LocalDate d = LocalDate.now();
		insertArticle(d.atTime(10, 0), 1L, 3, ArticleStatus.PUBLISHED);
		insertArticle(d.atTime(11, 0), 1L, 3, ArticleStatus.PUBLISHED);
		insertArticle(d.atTime(12, 0), 2L, 4, ArticleStatus.PUBLISHED);
		insertArticle(d.atTime(13, 0), 3L, 4, ArticleStatus.PUBLISHED);

		List<StatisticEntry> rows = articleMapper.countCategoryByRange(StatisticUnit.DAY, d.atStartOfDay());

		assertEquals(2, rows.size());
		Map<Integer, Long> byCategory = new HashMap<>();
		for (StatisticEntry row : rows) {
			byCategory.put(row.getCategoryId(), row.getTotal());
		}
		assertEquals(2L, byCategory.get(3));
		assertEquals(2L, byCategory.get(4));
	}

	@Test
	void countCategoryByRange_monthBucketsAcrossMonths() {
		LocalDate today = LocalDate.now();
		YearMonth curMonth = YearMonth.from(today);
		YearMonth prevMonth = curMonth.minusMonths(1);
		insertArticle(prevMonth.atDay(15).atTime(10, 0), 1L, 3, ArticleStatus.PUBLISHED);
		insertArticle(prevMonth.atDay(20).atTime(10, 0), 1L, 3, ArticleStatus.PUBLISHED);
		insertArticle(today.withDayOfMonth(5).atTime(10, 0), 1L, 4, ArticleStatus.PUBLISHED);

		List<StatisticEntry> rows = articleMapper.countCategoryByRange(StatisticUnit.MONTH,
				prevMonth.atDay(1).atStartOfDay());

		assertEquals(2, rows.size());
		Map<String, Long> byKey = new HashMap<>();
		for (StatisticEntry row : rows) {
			byKey.put(row.getYearMonth() + "/" + row.getCategoryId(), row.getTotal());
		}
		assertEquals(2L, byKey.get((prevMonth.getYear() * 100 + prevMonth.getMonthValue()) + "/3"));
		assertEquals(1L, byKey.get((curMonth.getYear() * 100 + curMonth.getMonthValue()) + "/4"));
	}

	@Test
	void countCreatedByRange_countsOnlyRowsInsideExclusiveWindow() {
		LocalDate first = LocalDate.now().withDayOfMonth(1);
		insertArticle(first.minusMonths(1).withDayOfMonth(15).atTime(9, 0), 1L, null, ArticleStatus.DRAFT);
		insertArticle(first.minusMonths(1).withDayOfMonth(20).atTime(9, 0), 2L, null, ArticleStatus.DRAFT);
		insertArticle(first.withDayOfMonth(3).atTime(9, 0), 1L, null, ArticleStatus.DRAFT);

		long total = articleMapper.countCreatedByRange(first.minusMonths(1).atStartOfDay(), first.atStartOfDay(), null);
		long mine = articleMapper.countCreatedByRange(first.minusMonths(1).atStartOfDay(), first.atStartOfDay(), 1L);

		assertEquals(2L, total);
		assertEquals(1L, mine);
	}

	@Test
	void countActiveAuthorsByRange_weekBucketsAreSevenDaysAnchoredOnStart() {
		LocalDate start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		insertArticle(start.atTime(9, 0), 1L, null, ArticleStatus.DRAFT);
		insertArticle(start.plusDays(3).atTime(9, 0), 2L, null, ArticleStatus.DRAFT);
		insertArticle(start.plusDays(6).atTime(9, 0), 3L, null, ArticleStatus.DRAFT);
		insertArticle(start.plusDays(7).atTime(9, 0), 1L, null, ArticleStatus.DRAFT);

		List<StatisticEntry> rows = articleMapper.countActiveAuthorsByRange(StatisticUnit.WEEK, start.atStartOfDay());

		assertEquals(2, rows.size());
		Map<LocalDate, Long> byBucket = new HashMap<>();
		for (StatisticEntry row : rows) {
			byBucket.put(row.getStatDay(), row.getTotal());
		}
		assertEquals(3L, byBucket.get(start));
		assertEquals(1L, byBucket.get(start.plusDays(7)));
		// Buckets are 7 days apart, each anchored on the bind parameter. They are NOT
		// calendar
		// weeks: for THIS_MONTH the anchor is the 1st of the month, so the bucket can
		// begin on any
		// weekday. The anchor here happens to be a Monday only because this test picked
		// one, which
		// is why the congruence check below is expressed against `start` and not
		// against MONDAY.
		for (LocalDate bucket : byBucket.keySet()) {
			assertEquals(0L, Math.floorMod(bucket.toEpochDay() - start.toEpochDay(), 7L),
					"week bucket must be a multiple of 7 days from the range start: " + bucket);
		}
	}

	@Test
	void countActiveAuthorsByRange_weekBucketMatchesJavaLocalDateArithmetic() {
		LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate start = monday.plusDays(1);
		LocalDate[] dates = { start, start.plusDays(2), start.plusDays(6), start.plusDays(7), start.plusDays(19) };

		Map<LocalDate, Long> expected = new HashMap<>();
		for (LocalDate date : dates) {
			insertArticle(date.atTime(9, 0), (long) date.getDayOfMonth(), null, ArticleStatus.DRAFT);
			LocalDate bucket = date.minusDays((int) (date.toEpochDay() - start.toEpochDay()) % 7);
			expected.merge(bucket, 1L, Long::sum);
		}

		List<StatisticEntry> rows = articleMapper.countActiveAuthorsByRange(StatisticUnit.WEEK, start.atStartOfDay());

		assertEquals(expected.size(), rows.size());
		Map<LocalDate, Long> byBucket = new HashMap<>();
		for (StatisticEntry row : rows) {
			byBucket.put(row.getStatDay(), row.getTotal());
		}
		assertEquals(expected, byBucket);
	}

	@Test
	void countArticleByRange_groupsByDayAndStatus() {
		LocalDate d = LocalDate.now();
		insertArticle(d.atTime(10, 0), 1L, null, ArticleStatus.DRAFT);
		insertArticle(d.atTime(11, 0), 1L, null, ArticleStatus.PENDING);
		insertArticle(d.atTime(12, 0), 2L, null, ArticleStatus.DRAFT);

		List<StatisticEntry> rows = articleMapper.countArticleByRange(StatisticUnit.DAY, d.atStartOfDay(), null);

		assertEquals(2, rows.size());
		Map<String, Long> byStatus = new HashMap<>();
		for (StatisticEntry row : rows) {
			assertEquals(d, row.getStatDay());
			byStatus.put(row.getStatus() + "/" + row.getStatDay(), row.getTotal());
		}
		assertEquals(2L, byStatus.get(ArticleStatus.DRAFT.getCode() + "/" + d));
		assertEquals(1L, byStatus.get(ArticleStatus.PENDING.getCode() + "/" + d));
	}

	@Test
	void countPublishedByRange_ignoresNonPublishedRows() {
		LocalDate d = LocalDate.now();
		insertArticle(d.atTime(10, 0), 1L, null, ArticleStatus.PUBLISHED);
		insertArticle(d.atTime(11, 0), 1L, null, ArticleStatus.PUBLISHED);
		insertArticle(d.atTime(12, 0), 2L, null, ArticleStatus.DRAFT);

		List<StatisticEntry> rows = articleMapper.countPublishedByRange(StatisticUnit.DAY, d.atStartOfDay(), null);

		assertEquals(1, rows.size());
		assertEquals(d, rows.get(0).getStatDay());
		assertEquals(2L, rows.get(0).getTotal());
	}

	@Test
	void selectArticleList_paginationShapeWithFilters() {
		LocalDate today = LocalDate.now();
		Long firstId = insertArticle(today.atTime(1, 0), 1L, 1, ArticleStatus.PENDING);
		Long secondId = insertArticle(today.atTime(2, 0), 1L, 1, ArticleStatus.PENDING);
		insertArticle(today.atTime(3, 0), 2L, 2, ArticleStatus.PUBLISHED);
		Long fourthId = insertArticle(today.atTime(4, 0), 1L, 1, ArticleStatus.PENDING);

		ArticleQuery query = new ArticleQuery();
		query.setStatus(ArticleStatus.PENDING);
		query.setAuthorId(1L);

		List<Article> page = articleMapper.selectArticleList(query, 0L, 2, "id DESC");

		assertEquals(3L, articleMapper.countArticles(query));
		assertEquals(2, page.size());
		assertEquals(fourthId, page.get(0).getId());
		assertEquals(secondId, page.get(1).getId());
	}

	@Test
	void selectArticleList_ordersByWhitelistedSortColumn() {
		SortValidator validator = new SortValidator();
		String sortSql = validator.resolve("updateTime", "desc");
		assertEquals("update_time DESC", sortSql);
		LocalDate d = LocalDate.now();
		insertArticle(d.atTime(1, 0), 1L, null, ArticleStatus.PUBLISHED);
		insertArticle(d.atTime(2, 0), 2L, null, ArticleStatus.PUBLISHED);
		insertArticle(d.atTime(3, 0), 3L, null, ArticleStatus.PUBLISHED);

		List<Article> rows = articleMapper.selectArticleList(new ArticleQuery(), 0L, 10, sortSql);

		assertEquals(3, rows.size());
		for (int i = 1; i < rows.size(); i++) {
			assertTrue(rows.get(i - 1).getUpdateTime().isAfter(rows.get(i).getUpdateTime()));
		}
	}

	/**
	 * selectById and selectByIds are the only two statements here that use
	 * {@code SELECT *}, so they are the only ones whose result set actually
	 * contains {@code averse_count}. Every other query projects LIST_COLUMNS, which
	 * omits it, so a broken mapping for that column stays invisible until one of
	 * these two runs.
	 */
	@Test
	void selectById_mapsAverseCountFromTheStarProjection() {
		LocalDate d = LocalDate.now();
		Long id = insertArticle(d.atTime(9, 0), 1L, null, ArticleStatus.PUBLISHED);
		jdbcTemplate.update("UPDATE cms_article SET averse_count = 7 WHERE id = ?", id);

		Article row = articleMapper.selectById(id);

		assertNotNull(row);
		row.initTally();
		assertEquals(7L, row.getTally().getAverseCount());
	}

	@Test
	void selectByIds_mapsAverseCountFromTheStarProjection() {
		LocalDate d = LocalDate.now();
		Long id = insertArticle(d.atTime(9, 0), 1L, null, ArticleStatus.PUBLISHED);
		jdbcTemplate.update("UPDATE cms_article SET averse_count = 4 WHERE id = ?", id);

		List<Article> rows = articleMapper.selectByIds(List.of(id));

		assertEquals(1, rows.size());
		rows.get(0).initTally();
		assertEquals(4L, rows.get(0).getTally().getAverseCount());
	}
}