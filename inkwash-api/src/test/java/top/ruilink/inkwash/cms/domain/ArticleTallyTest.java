package top.ruilink.inkwash.cms.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Article tally counter unit tests verifying no lost updates under concurrent
 * increments.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class ArticleTallyTest {

	@Test
	void concurrentIncrements_NeverLoseUpdates() throws Exception {
		ArticleTally tally = new ArticleTally();
		int threads = 8;
		int bumps = 1000;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			CountDownLatch start = new CountDownLatch(1);
			List<Future<?>> futures = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				futures.add(pool.submit(() -> {
					start.await(30, TimeUnit.SECONDS);
					for (int j = 0; j < bumps; j++) {
						tally.incrementView();
						tally.incrementComment();
						tally.incrementAgree();
						tally.incrementFavorite();
						tally.incrementShare();
					}
					return null;
				}));
			}
			start.countDown();
			for (Future<?> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		} finally {
			pool.shutdownNow();
		}
		long expected = (long) threads * bumps;
		assertEquals(expected, tally.getViewCount());
		assertEquals(expected, tally.getCommentCount());
		assertEquals(expected, tally.getAgreeCount());
		assertEquals(expected, tally.getFavoriteCount());
		assertEquals(expected, tally.getShareCount());
	}
}