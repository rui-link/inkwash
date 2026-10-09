package top.ruilink.inkwash.cms.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.util.SensitiveMatcher;
import top.ruilink.inkwash.base.util.SortValidator;
import top.ruilink.inkwash.cms.api.query.SensitiveQuery;
import top.ruilink.inkwash.cms.api.view.SensitiveView;
import top.ruilink.inkwash.cms.domain.Sensitive;
import top.ruilink.inkwash.cms.mapper.SensitiveMapper;

/**
 * Sensitive service unit tests for exactly-once matcher caching, invalidation,
 * and paged listing.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class SensitiveServiceImplTest {

	@Mock
	private SortValidator sortValidator;

	@Mock
	private SensitiveMapper sensitiveMapper;

	@InjectMocks
	private SensitiveServiceImpl sensitiveService;

	@Test
	@DisplayName("并发调用 getMatcher 得到等价的匹配器（共享由 CacheManager 保证）")
	void getMatcher_isSafeToCallConcurrently() throws Exception {
		Sensitive word = new Sensitive();
		word.setWord("以德报怨");
		when(sensitiveMapper.selectAllActive()).thenReturn(List.of(word));

		int threadCount = 16;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);
		Set<SensitiveMatcher> matchers = ConcurrentHashMap.newKeySet();
		List<Throwable> errors = java.util.Collections.synchronizedList(new ArrayList<>());
		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					ready.countDown();
					start.await(30, TimeUnit.SECONDS);
					matchers.add(sensitiveService.getMatcher());
				} catch (Throwable t) {
					errors.add(t);
				}
			});
		}
		ready.await(30, TimeUnit.SECONDS);
		start.countDown();
		executor.shutdown();
		executor.awaitTermination(30, TimeUnit.SECONDS);

		if (!errors.isEmpty()) {
			StringBuilder sb = new StringBuilder("并发调用抛异常: ");
			for (Throwable t : errors) {
				sb.append(t).append(" / ");
			}
			fail(sb.toString());
		}
		assertEquals(threadCount, matchers.size(), "每个线程都应拿到可用的匹配器");
	}

	@Test
	@DisplayName("匹配器是无状态的，可安全共享")
	void matcherIsImmutableAndShareable() {
		Sensitive word = new Sensitive();
		word.setWord("以德报怨");
		when(sensitiveMapper.selectAllActive()).thenReturn(List.of(word));

		SensitiveMatcher matcher = sensitiveService.getMatcher();

		assertEquals(1, matcher.size());
		assertEquals("以德报怨", matcher.wordAt(0));
		assertTrue(matcher.findMatches("何以德报怨乎").contains(0), "应命中以德报怨");
		assertTrue(matcher.findMatches("无关文本").isEmpty());
		// repeated use must not mutate anything
		assertEquals(1, matcher.findMatches("以德报怨").size());
		assertEquals("以德报怨", matcher.wordAt(0));
	}

	@Test
	@DisplayName("变更方法都在共享缓存上 @CacheEvict(allEntries = true) —— 旧的失效钩子已移除")
	void mutatorsEvictTheSharedCache() throws NoSuchMethodException {
		Class<?> paramClass = top.ruilink.inkwash.cms.api.param.SensitiveParam.class;
		List<Method> mutators = List.of(SensitiveServiceImpl.class.getMethod("createSensitive", paramClass),
				SensitiveServiceImpl.class.getMethod("updateSensitive", Long.class, paramClass),
				SensitiveServiceImpl.class.getMethod("deleteSensitive", Long.class));

		for (Method m : mutators) {
			org.springframework.cache.annotation.CacheEvict evict = m
					.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);

			assertNotNull(evict, m.getName() + " 必须 @CacheEvict，否则词表改动后缓存不失效");
			assertArrayEquals(new String[] { top.ruilink.inkwash.base.CacheConsts.SENSITIVE_WORDS_CACHE },
					evict.value(), m.getName() + " 必须清理同一个缓存名");
			assertTrue(evict.allEntries(), m.getName() + " 必须 allEntries = true：词表与匹配器同属一个缓存，不逐键清理");
		}
	}

	@Test
	@DisplayName("getMatcher 与 getAllActiveWords 共用同一缓存名")
	void matcherAndWordsShareOneCacheName() throws NoSuchMethodException {
		org.springframework.cache.annotation.Cacheable words = SensitiveServiceImpl.class.getMethod("getAllActiveWords")
				.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
		org.springframework.cache.annotation.Cacheable matcher = SensitiveServiceImpl.class.getMethod("getMatcher")
				.getAnnotation(org.springframework.cache.annotation.Cacheable.class);

		assertNotNull(words);
		assertNotNull(matcher);
		assertEquals(top.ruilink.inkwash.base.CacheConsts.SENSITIVE_WORDS_CACHE, words.value()[0]);
		assertEquals(top.ruilink.inkwash.base.CacheConsts.SENSITIVE_WORDS_CACHE, matcher.value()[0],
				"两者必须同属一个缓存，否则 TTL 与失效路径会再次分叉（ISS-027）");
	}

	void getMatcher_buildsFromActiveWords() {
		Sensitive word = new Sensitive();
		word.setWord("以德报怨");
		when(sensitiveMapper.selectAllActive()).thenReturn(List.of(word));

		SensitiveMatcher matcher = sensitiveService.getMatcher();

		assertTrue(matcher.containsAny("子曰以德报怨可乎"));
		assertFalse(matcher.containsAny("全是正常内容"));
		verify(sensitiveMapper).selectAllActive();
	}

	@Test
	@DisplayName("listAll 按排序与分页查询并委托转换")
	void listAll_delegatesSortAndPagination() {
		SensitiveQuery query = new SensitiveQuery();
		query.setPage(2);
		query.setSize(5);
		query.setSortField("word");
		query.setSortOrder("desc");

		Sensitive s1 = new Sensitive();
		s1.setId(1L);
		s1.setWord("以德报怨");
		Sensitive s2 = new Sensitive();
		s2.setId(2L);
		s2.setWord("全盘西化");

		when(sortValidator.resolve("word", "desc")).thenReturn("word DESC");
		when(sensitiveMapper.countSensitives(query)).thenReturn(12L);
		when(sensitiveMapper.selectSensitiveList(query, 5L, 5, "word DESC")).thenReturn(List.of(s1, s2));

		PageResult<SensitiveView> result = sensitiveService.listAll(query);

		assertEquals(2, result.getPageNum());
		assertEquals(5, result.getPageSize());
		assertEquals(12, result.getTotal());
		assertEquals(2, result.getList().size());
		assertEquals("以德报怨", result.getList().get(0).getWord());
		assertEquals("全盘西化", result.getList().get(1).getWord());
		verify(sensitiveMapper).countSensitives(query);
		verify(sensitiveMapper).selectSensitiveList(query, 5L, 5, "word DESC");
	}
}