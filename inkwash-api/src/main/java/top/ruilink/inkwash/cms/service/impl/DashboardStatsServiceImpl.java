/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.cms.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import top.ruilink.inkwash.base.domain.StatisticEntry;
import top.ruilink.inkwash.base.enums.StatisticRange;
import top.ruilink.inkwash.base.enums.StatisticUnit;
import top.ruilink.inkwash.cms.api.view.DashboardView;
import top.ruilink.inkwash.cms.api.view.StatisticView;
import top.ruilink.inkwash.cms.domain.Category;
import top.ruilink.inkwash.cms.enums.ArticleStatus;
import top.ruilink.inkwash.cms.mapper.ArticleMapper;
import top.ruilink.inkwash.cms.mapper.CategoryMapper;
import top.ruilink.inkwash.cms.service.DashboardStatsService;
import top.ruilink.inkwash.system.mapper.UserMapper;

/**
 * Role-based dashboard statistics service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Service
public class DashboardStatsServiceImpl implements DashboardStatsService {

	private final ArticleMapper articleMapper;
	private final UserMapper userMapper;
	private final CategoryMapper categoryMapper;

	public DashboardStatsServiceImpl(ArticleMapper articleMapper, UserMapper userMapper,
			CategoryMapper categoryMapper) {
		this.articleMapper = articleMapper;
		this.userMapper = userMapper;
		this.categoryMapper = categoryMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<StatisticView.ArticleStatItem> getArticleStats(StatisticRange range, Long userId) {

		RangeSpec spec = resolveRange(range);
		List<Bucket> buckets = buildBuckets(spec);
		Map<String, long[]> statuses = statusCounts(range, buckets, spec);
		Map<String, long[]> published = publishedCounts(range, buckets, spec);
		return buckets.stream().map(b -> {
			StatisticView.ArticleStatItem item = new StatisticView.ArticleStatItem();
			item.setTimeAxis(b.period);
			item.setCreated(statuses.get(b.period)[0]);
			item.setPending(statuses.get(b.period)[1]);
			item.setApproved(statuses.get(b.period)[2]);
			item.setRejected(statuses.get(b.period)[3]);
			item.setPendingPublish(statuses.get(b.period)[2]);
			item.setPublished(published.get(b.period)[0]);
			return item;
		}).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StatisticView.UserStatItem> getUserStats(StatisticRange range, Long userId) {

		RangeSpec spec = resolveRange(range);
		List<Bucket> buckets = buildBuckets(spec);
		Map<String, long[]> users = userCounts(range, buckets, spec);
		Map<String, long[]> authors = authorCounts(range, buckets, spec);
		return buckets.stream().map(b -> {
			StatisticView.UserStatItem item = new StatisticView.UserStatItem();
			item.setTimeAxis(b.period);
			item.setNewUsers(users.get(b.period)[0]);
			item.setActiveAuthors(authors.get(b.period)[0]);
			return item;
		}).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StatisticView.CategoryStatItem> getCategoryStats(StatisticRange range, Long userId) {

		Map<Integer, Category> byId = buildCategoryIndex();
		Map<Integer, Integer> topIds = new HashMap<>();
		Map<Integer, String> topNames = new HashMap<>();
		for (Category category : byId.values()) {
			Category top = topOf(category, byId);
			topIds.put(category.getId(), top.getId());
			topNames.put(category.getId(), top.getName());
		}
		RangeSpec spec = resolveRange(range);
		List<Bucket> buckets = buildBuckets(spec);
		Map<String, Map<Integer, Long>> perBucket = new HashMap<>();
		Map<LocalDate, String> index = dayIndex(buckets);
		LocalDateTime start = spec.start().atStartOfDay();
		for (StatisticEntry row : articleMapper.countCategoryByRange(spec.unit(), start)) {
			String key = keyOf(row, index);
			if (key == null) {
				continue;
			}
			Integer foldId = topIds.getOrDefault(row.getCategoryId(), row.getCategoryId());
			perBucket.computeIfAbsent(key, _ -> new HashMap<>()).merge(foldId, row.getTotal(), Long::sum);
		}
		return buckets.stream().map(b -> {
			StatisticView.CategoryStatItem item = new StatisticView.CategoryStatItem();
			item.setTimeAxis(b.period);
			List<StatisticView.CategoryCount> list = new ArrayList<>();
			perBucket.getOrDefault(b.period, Map.of()).forEach((catId, cnt) -> {
				StatisticView.CategoryCount cc = new StatisticView.CategoryCount();
				cc.setCategoryId(catId);
				cc.setCategoryName(topNames.get(catId));
				cc.setCount(cnt);
				list.add(cc);
			});
			list.sort((a, c) -> {
				if (a.getCategoryId() == null && c.getCategoryId() == null) {
					return 0;
				}
				if (a.getCategoryId() == null) {
					return 1;
				}
				if (c.getCategoryId() == null) {
					return -1;
				}
				return Integer.compare(a.getCategoryId(), c.getCategoryId());
			});
			item.setCategories(list);
			return item;
		}).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public StatisticView.MyArticleStats getMyArticleStats(StatisticRange range, Long userId) {
		RangeSpec spec = resolveRange(range);
		List<Bucket> buckets = buildBuckets(spec);
		Map<String, long[]> counts = myStatusCounts(range, buckets, spec, userId);
		Map<String, long[]> published = myPublishedCounts(range, buckets, spec, userId);
		StatisticView.MyArticleStats out = new StatisticView.MyArticleStats();
		out.setItems(buckets.stream().map(b -> {
			StatisticView.ArticleStatItem item = new StatisticView.ArticleStatItem();
			item.setTimeAxis(b.period);
			item.setCreated(counts.get(b.period)[3]);
			item.setPending(counts.get(b.period)[0]);
			item.setApproved(counts.get(b.period)[1]);
			item.setRejected(counts.get(b.period)[2]);
			item.setPendingPublish(counts.get(b.period)[1]);
			item.setPublished(published.get(b.period)[0]);
			return item;
		}).toList());
		if (spec.monthAligned()) {
			out.setPrevCreated(prevCreated(range, userId));
		}
		return out;
	}

	@Override
	@Transactional(readOnly = true)
	public DashboardView.DashboardSummary getSummary(Long userId) {

		DashboardView.DashboardSummary summary = new DashboardView.DashboardSummary();
		summary.setTotalArticles(articleMapper.count());
		summary.setPendingReview(articleMapper.countByStatus(ArticleStatus.PENDING.getCode()));
		summary.setApproved(articleMapper.countByStatus(ArticleStatus.APPROVED.getCode()));
		summary.setRejected(articleMapper.countByStatus(ArticleStatus.REJECTED.getCode()));
		summary.setPublished(articleMapper.countByStatus(ArticleStatus.PUBLISHED.getCode()));
		summary.setTotalUsers(userMapper.count());
		return summary;
	}

	// ========== Assembly helpers ==========

	private Map<String, long[]> statusCounts(StatisticRange range, List<Bucket> buckets, RangeSpec spec) {
		Map<String, long[]> map = emptyCounts(buckets, 4);
		Map<LocalDate, String> index = dayIndex(buckets);
		LocalDateTime start = spec.start().atStartOfDay();
		for (StatisticEntry row : articleMapper.countArticleByRange(spec.unit(), start, null)) {
			long[] arr = map.get(keyOf(row, index));
			if (arr == null) {
				continue;
			}
			arr[0] += row.getTotal();
			if (row.getStatus() == ArticleStatus.PENDING.getCode()) {
				arr[1] += row.getTotal();
			} else if (row.getStatus() == ArticleStatus.APPROVED.getCode()) {
				arr[2] += row.getTotal();
			} else if (row.getStatus() == ArticleStatus.REJECTED.getCode()) {
				arr[3] += row.getTotal();
			}
		}
		return map;
	}

	private Map<String, long[]> publishedCounts(StatisticRange range, List<Bucket> buckets, RangeSpec spec) {
		Map<String, long[]> map = emptyCounts(buckets, 1);
		Map<LocalDate, String> index = dayIndex(buckets);
		LocalDateTime start = spec.start().atStartOfDay();
		for (StatisticEntry row : articleMapper.countPublishedByRange(spec.unit(), start, null)) {
			long[] arr = map.get(keyOf(row, index));
			if (arr == null) {
				continue;
			}
			arr[0] += row.getTotal();
		}
		return map;
	}

	private Map<String, long[]> myStatusCounts(StatisticRange range, List<Bucket> buckets, RangeSpec spec,
			Long authorId) {
		Map<String, long[]> map = emptyCounts(buckets, 4);
		Map<LocalDate, String> index = dayIndex(buckets);
		LocalDateTime start = spec.start().atStartOfDay();
		for (StatisticEntry row : articleMapper.countArticleByRange(spec.unit(), start, authorId)) {
			long[] arr = map.get(keyOf(row, index));
			if (arr == null) {
				continue;
			}
			arr[3] += row.getTotal();
			if (row.getStatus() == ArticleStatus.PENDING.getCode()) {
				arr[0] += row.getTotal();
			} else if (row.getStatus() == ArticleStatus.APPROVED.getCode()) {
				arr[1] += row.getTotal();
			} else if (row.getStatus() == ArticleStatus.REJECTED.getCode()) {
				arr[2] += row.getTotal();
			}
		}
		return map;
	}

	private Map<String, long[]> myPublishedCounts(StatisticRange range, List<Bucket> buckets, RangeSpec spec,
			Long authorId) {
		Map<String, long[]> map = emptyCounts(buckets, 1);
		Map<LocalDate, String> index = dayIndex(buckets);
		LocalDateTime start = spec.start().atStartOfDay();
		for (StatisticEntry row : articleMapper.countPublishedByRange(spec.unit(), start, authorId)) {
			long[] arr = map.get(keyOf(row, index));
			if (arr == null) {
				continue;
			}
			arr[0] += row.getTotal();
		}
		return map;
	}

	private Long prevCreated(StatisticRange range, Long authorId) {
		LocalDate firstCur = LocalDate.now().withDayOfMonth(1);
		LocalDate prevStart = (range == StatisticRange.LAST_MONTH) ? firstCur.minusMonths(2) : firstCur.minusMonths(1);
		return articleMapper.countCreatedByRange(prevStart.atStartOfDay(), prevStart.plusMonths(1).atStartOfDay(),
				authorId);
	}

	private Map<String, long[]> userCounts(StatisticRange range, List<Bucket> buckets, RangeSpec spec) {
		Map<String, long[]> map = emptyCounts(buckets, 1);
		Map<LocalDate, String> index = dayIndex(buckets);
		LocalDateTime start = spec.start().atStartOfDay();
		for (StatisticEntry row : userMapper.countUsersByRange(spec.unit(), start)) {
			long[] arr = map.get(keyOf(row, index));
			if (arr == null) {
				continue;
			}
			arr[0] += row.getTotal();
		}
		return map;
	}

	private Map<String, long[]> authorCounts(StatisticRange range, List<Bucket> buckets, RangeSpec spec) {
		Map<String, long[]> map = emptyCounts(buckets, 1);
		Map<LocalDate, String> index = dayIndex(buckets);
		LocalDateTime start = spec.start().atStartOfDay();
		for (StatisticEntry row : articleMapper.countActiveAuthorsByRange(spec.unit(), start)) {
			long[] arr = map.get(keyOf(row, index));
			if (arr == null) {
				continue;
			}
			arr[0] += row.getTotal();
		}
		return map;
	}

	private Map<Integer, Category> buildCategoryIndex() {
		Map<Integer, Category> byId = new HashMap<>();
		for (Category category : categoryMapper.selectAll()) {
			byId.put(category.getId(), category);
		}
		return byId;
	}

	private static Category topOf(Category category, Map<Integer, Category> byId) {
		Category top = category;
		while (!top.isTopLevel()) {
			Category parent = byId.get(top.getParentId());
			if (parent == null) {
				break;
			}
			top = parent;
		}
		return top;
	}

	private RangeSpec resolveRange(StatisticRange range) {
		LocalDate today = LocalDate.now();
		LocalDate start = today;
		LocalDate endExclusive = today;
		switch (range) {
		case LAST_7D -> {
			start = today.minusDays(6);
			endExclusive = today.plusDays(1);
		}
		case LAST_30D -> {
			start = today.minusDays(29);
			endExclusive = today.plusDays(1);
		}
		case LAST_12M -> {
			start = today.minusMonths(11).withDayOfMonth(1);
			endExclusive = today.plusDays(1);
		}
		case THIS_MONTH -> {
			start = today.withDayOfMonth(1);
			endExclusive = today.plusDays(1);
		}
		case LAST_MONTH -> {
			LocalDate firstCur = today.withDayOfMonth(1);
			start = firstCur.minusMonths(1);
			endExclusive = firstCur;
		}
		case THIS_YEAR -> {
			start = today.withDayOfMonth(1).minusMonths(today.getMonthValue() - 1);
			endExclusive = today.plusDays(1);
		}
		case LAST_YEAR -> {
			start = LocalDate.of(today.getYear() - 1, 1, 1);
			endExclusive = LocalDate.of(today.getYear(), 1, 1);
		}
		}
		boolean monthAligned = (range == StatisticRange.THIS_MONTH || range == StatisticRange.LAST_MONTH);
		return new RangeSpec(start, endExclusive, range.getUnit(),
				range == StatisticRange.LAST_MONTH || range == StatisticRange.LAST_YEAR, monthAligned);
	}

	private List<Bucket> buildBuckets(RangeSpec spec) {
		List<Bucket> buckets = new ArrayList<>();
		switch (spec.unit()) {
		case DAY -> {
			LocalDate cursor = spec.start();
			while (cursor.isBefore(spec.endExclusive())) {
				buckets.add(new Bucket(cursor.toString(), cursor, cursor.plusDays(1)));
				cursor = cursor.plusDays(1);
			}
		}
		case MONTH -> {
			YearMonth cursor = YearMonth.from(spec.start());
			YearMonth last = YearMonth.from(spec.endExclusive().minusDays(1));
			while (!cursor.isAfter(last)) {
				buckets.add(new Bucket(cursor.toString(), cursor.atDay(1), cursor.plusMonths(1).atDay(1)));
				cursor = cursor.plusMonths(1);
			}
		}
		case WEEK -> {
			LocalDate cursor = spec.start();
			LocalDate bound = spec.endExclusive();
			while (cursor.isBefore(bound)) {
				LocalDate end = cursor.plusDays(7);
				if (end.isAfter(bound)) {
					end = bound;
				}
				buckets.add(new Bucket(cursor.toString(), cursor, end));
				cursor = cursor.plusDays(7);
			}
		}
		}
		return buckets;
	}

	private static Map<String, long[]> emptyCounts(List<Bucket> buckets, int width) {
		Map<String, long[]> map = new HashMap<>();
		for (Bucket bucket : buckets) {
			map.put(bucket.period, new long[width]);
		}
		return map;
	}

	private static String keyOf(StatisticEntry row, Map<LocalDate, String> index) {
		if (row.getYearMonth() != null) {
			int y = row.getYearMonth() / 100;
			int m = row.getYearMonth() % 100;
			return index.get(LocalDate.of(y, m, 1));
		}
		return index.get(row.getStatDay());
	}

	private static Map<LocalDate, String> dayIndex(List<Bucket> buckets) {
		Map<LocalDate, String> index = new HashMap<>();
		for (Bucket bucket : buckets) {
			LocalDate cursor = bucket.start;
			while (cursor.isBefore(bucket.endExclusive)) {
				index.put(cursor, bucket.period);
				cursor = cursor.plusDays(1);
			}
		}
		return index;
	}

	// ========== Role checks ==========

	private record RangeSpec(LocalDate start, LocalDate endExclusive, StatisticUnit unit, boolean closed,
			boolean monthAligned) {
	}

	private record Bucket(String period, LocalDate start, LocalDate endExclusive) {
	}
}
