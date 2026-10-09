package top.ruilink.inkwash.security.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * VerificationCodeStore unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class VerificationCodeStoreTest {

	private VerifyCodeStore store;

	@BeforeEach
	void setUp() {
		store = new VerifyCodeStore();
	}

	@AfterEach
	void tearDown() {
		// cleanup thread is daemon; nothing to stop explicitly
	}

	@Test
	@DisplayName("save + verify 成功即消费（二次 verify 失败）")
	void verifySuccess_ConsumesCode() {
		store.save("phone-1", "123456", 5);

		assertTrue(store.verify("phone-1", "123456"));
		assertFalse(store.verify("phone-1", "123456"), "验证码一次性消费，二次验证应失败");
	}

	@Test
	@DisplayName("过期验证码失效")
	void verifyExpired_ReturnsFalse() {
		store.save("phone-1", "123456", 0);
		sleep(30);
		assertFalse(store.verify("phone-1", "123456"));
	}

	@Test
	@DisplayName("错误次数达上限后移除")
	void verifyWrongAttempts_RemovesCode() {
		store.save("phone-1", "123456", 5);

		for (int i = 0; i < 5; i++) {
			assertFalse(store.verify("phone-1", "wrong"));
		}
		assertFalse(store.verify("phone-1", "123456"), "错误次数达上限后验证码被移除");
	}

	@Test
	@DisplayName("save 覆盖旧值")
	void saveOverwrites_OldValue() {
		store.save("phone-1", "111111", 5);
		store.save("phone-1", "222222", 5);

		assertTrue(store.verify("phone-1", "222222"));
		assertFalse(store.verify("phone-1", "111111"), "旧验证码应被新值覆盖");
	}

	@Test
	@DisplayName("null key/code 直接返回 false")
	void nullKeyOrCode_ReturnsFalse() {
		assertFalse(store.verify(null, "123456"));
		assertFalse(store.verify("phone-1", null));
		assertFalse(store.verify(null, null));
	}

	@Test
	@DisplayName("clear 移除验证码")
	void clear_RemovesCode() {
		store.save("phone-1", "123456", 5);
		store.clear("phone-1");
		assertFalse(store.verify("phone-1", "123456"));
	}

	@Test
	@DisplayName("并发 verify/save 不抛异常")
	void concurrentVerifyAndSave_NoException() throws Exception {
		int threads = 8;
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Thread> workers = new ArrayList<>();

		for (int i = 0; i < threads; i++) {
			final int idx = i;
			Thread t = new Thread(() -> {
				ready.countDown();
				try {
					start.await();
					for (int j = 0; j < 200; j++) {
						String key = "k-" + idx;
						store.save(key, "code" + j, 1);
						store.verify(key, "code" + j);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			t.start();
			workers.add(t);
		}

		assertTrue(ready.await(5, TimeUnit.SECONDS), "线程应在 5 秒内就绪");
		start.countDown();
		for (Thread t : workers) {
			t.join(10000);
		}
		assertFalse(workers.stream().anyMatch(Thread::isAlive), "所有线程应已完成");
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
