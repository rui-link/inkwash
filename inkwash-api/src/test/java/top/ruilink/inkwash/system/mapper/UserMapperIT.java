package top.ruilink.inkwash.system.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import top.ruilink.inkwash.base.config.MybatisConfig;
import top.ruilink.inkwash.base.domain.StatisticEntry;
import top.ruilink.inkwash.base.enums.StatisticUnit;
import top.ruilink.inkwash.system.SystemConsts;

/**
 * UserMapper integration test for the dashboard user-trend aggregation against
 * H2 in MySQL mode.
 *
 * <p>
 * This mapper had no database-backed test, which is why its {@code AS day}
 * alias survived after the same alias was fixed in {@code ArticleMapper}: H2
 * rejects {@code DAY} as a reserved word, and only a real H2 round trip catches
 * that. The datasource URL therefore keeps stock H2 semantics (no
 * {@code NON_KEYWORDS} escape hatch).
 *
 * @author Dyllon
 * @since 0.5.1
 */
@MybatisTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:inkwash_user_it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
		"spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
		"spring.datasource.password=" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@Import(MybatisConfig.class)
@Sql(scripts = "sys-user-schema.sql")
class UserMapperIT {

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private void insertUser(LocalDateTime createTime, int status) {
		jdbcTemplate.update("INSERT INTO sys_user (nickname, status, create_time, update_time) VALUES (?, ?, ?, ?)",
				"u-" + createTime.toLocalDate() + "-" + System.nanoTime(), status, createTime, createTime);
	}

	@Test
	void countUsersByRange_groupsByDayAndExcludesDeleted() {
		LocalDate d1 = LocalDate.now().minusDays(1);
		LocalDate today = LocalDate.now();
		insertUser(d1.atTime(9, 0), 1);
		insertUser(d1.atTime(10, 0), 1);
		insertUser(today.atTime(9, 0), 1);
		insertUser(today.atTime(10, 0), SystemConsts.USER_STATUS_DELETED);

		List<StatisticEntry> rows = userMapper.countUsersByRange(StatisticUnit.DAY, d1.atStartOfDay());

		Map<LocalDate, Long> byDay = new HashMap<>();
		for (StatisticEntry row : rows) {
			byDay.put(row.getStatDay(), row.getTotal());
		}
		assertEquals(2, byDay.size());
		assertEquals(2L, byDay.get(d1));
		assertEquals(1L, byDay.get(today));
	}

	@Test
	void countUsersByRange_monthBucketsAcrossMonths() {
		LocalDate today = LocalDate.now();
		LocalDate prevMonth = today.minusMonths(1);
		insertUser(prevMonth.withDayOfMonth(10).atTime(9, 0), 1);
		insertUser(prevMonth.withDayOfMonth(20).atTime(9, 0), 1);
		insertUser(today.withDayOfMonth(5).atTime(9, 0), 1);

		List<StatisticEntry> rows = userMapper.countUsersByRange(StatisticUnit.MONTH,
				prevMonth.withDayOfMonth(1).atStartOfDay());

		Map<Integer, Long> byMonth = new HashMap<>();
		for (StatisticEntry row : rows) {
			byMonth.put(row.getYearMonth(), row.getTotal());
		}
		assertEquals(2, byMonth.size());
		assertEquals(2L, byMonth.get(prevMonth.getYear() * 100 + prevMonth.getMonthValue()));
		assertEquals(1L, byMonth.get(today.getYear() * 100 + today.getMonthValue()));
	}

	/**
	 * Records that a WEEK request returns daily rows from this mapper, while
	 * {@code ArticleMapper.countActiveAuthorsByRange} returns week-bucketed rows
	 * for the same unit.
	 *
	 * <p>
	 * This difference is invisible in the HTTP response. The service folds mapper
	 * rows into the same 7-day buckets regardless of the shape it receives, so a
	 * {@code THIS_MONTH} request yields weekly points either way. The asymmetry is
	 * therefore intentional, not a defect: commit {@code 3e3999b} made
	 * {@code THIS_MONTH} and {@code LAST_MONTH} use 7-day buckets anchored at the
	 * month's first day, and the chart deliberately omits a "week" label because a
	 * month-anchored bucket is not a calendar week.
	 *
	 * <p>
	 * This test pins the mapper-level shape so that any future change to it is a
	 * conscious decision. Do not "fix" the asymmetry without reading commit
	 * {@code 3e3999b} first.
	 */
	@Test
	void countUsersByRange_weekUnitReturnsDailyRows() {
		LocalDate start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		// Two dates 2 days apart, so both fall inside the FIRST 7-day window anchored
		// on `start`.
		// Day-level bucketing therefore yields two buckets of 1; 7-day bucketing
		// anchored on `start`
		// (what ArticleMapper.countActiveAuthorsByRange does) would yield a single
		// bucket of 2.
		// Observing two buckets of 1 therefore proves this mapper buckets by day. Note
		// this says
		// nothing about calendar weeks: no query in this codebase buckets to Monday
		// boundaries.
		insertUser(start.atTime(9, 0), 1);
		insertUser(start.plusDays(2).atTime(9, 0), 1);

		List<StatisticEntry> rows = userMapper.countUsersByRange(StatisticUnit.WEEK, start.atStartOfDay());

		Map<LocalDate, Long> byBucket = new HashMap<>();
		for (StatisticEntry row : rows) {
			byBucket.put(row.getStatDay(), row.getTotal());
		}
		assertEquals(2, byBucket.size(), "WEEK is bucketed by day here, not collapsed into 7-day windows");
		assertEquals(1L, byBucket.get(start));
		assertEquals(1L, byBucket.get(start.plusDays(2)));
	}

	@Test
	void countUsersByRange_returnsNoRowsWithoutUsers() {
		assertFalse(
				userMapper.countUsersByRange(StatisticUnit.DAY, LocalDate.now().atStartOfDay()).iterator().hasNext());
	}
}
