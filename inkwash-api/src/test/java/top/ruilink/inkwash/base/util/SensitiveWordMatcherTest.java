package top.ruilink.inkwash.base.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SensitiveMatcher CJK and ASCII word matching unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class SensitiveWordMatcherTest {

	@Test
	@DisplayName("纯中文敏感词能命中相同的中文文本")
	void cjkPatternMatchesIdenticalText() {
		SensitiveMatcher matcher = SensitiveMatcher.build(List.of("以德报怨"));
		assertTrue(matcher.containsAny("以德报怨"));
		assertEquals(List.of(0), matcher.findMatches("以德报怨"));
	}

	@Test
	@DisplayName("纯中文敏感词能命中句子中的中文词")
	void cjkPatternMatchesInsideSentence() {
		SensitiveMatcher matcher = SensitiveMatcher.build(List.of("以德报怨"));
		assertTrue(matcher.containsAny("子曰以德报怨可乎"));
		assertEquals(List.of(0), matcher.findMatches("子曰以德报怨可乎"));
	}

	@Test
	@DisplayName("纯中文敏感词不再误命中含ASCII字符的文本")
	void cjkPatternDoesNotMatchAsciiText() {
		SensitiveMatcher matcher = SensitiveMatcher.build(List.of("以德报怨", "全盘西化"));
		assertFalse(matcher.containsAny("E2E文章"));
		assertFalse(matcher.containsAny("## 正文"));
		assertTrue(matcher.findMatches("E2E文章").isEmpty());
		assertTrue(matcher.findMatches("## 正文").isEmpty());
	}

	@Test
	@DisplayName("文本不含敏感词时不命中")
	void noMatchWhenWordAbsent() {
		SensitiveMatcher matcher = SensitiveMatcher.build(List.of("以德报怨"));
		assertFalse(matcher.containsAny("全是正常内容"));
	}

	@Test
	@DisplayName("多个中文敏感词分别独立命中")
	void multipleCjkPatternsMatchIndependently() {
		SensitiveMatcher matcher = SensitiveMatcher.build(List.of("以德报怨", "全盘西化"));
		assertEquals(List.of(0), matcher.findMatches("以德报怨"));
		assertEquals(List.of(1), matcher.findMatches("全盘西化"));
		assertEquals(List.of(0, 1), matcher.findMatches("以德报怨和全盘西化"));
	}

	@Test
	@DisplayName("ASCII敏感词行为保持不变且大小写不敏感")
	void asciiPatternStillMatchesCaseInsensitive() {
		SensitiveMatcher matcher = SensitiveMatcher.build(List.of("abc"));
		assertTrue(matcher.containsAny("xxABCyy"));
		assertTrue(matcher.findMatches("xxABCyy").contains(0));
		assertEquals(List.of(0), matcher.findMatches("a-b-c"));
	}

	@Test
	@DisplayName("ASCII与中文敏感词混合时互不干扰")
	void mixedPatternsMatchCorrectly() {
		SensitiveMatcher matcher = SensitiveMatcher.build(List.of("以德报怨", "test"));
		assertEquals(List.of(0), matcher.findMatches("以德报怨"));
		assertEquals(List.of(1), matcher.findMatches("这是一段test内容"));
		assertEquals(List.of(0, 1), matcher.findMatches("以德报怨加test"));
	}

	@Test
	@DisplayName("空或null敏感词列表不匹配任何文本")
	void emptyPatternsNeverMatch() {
		SensitiveMatcher empty = SensitiveMatcher.build(List.of());
		SensitiveMatcher nullMatcher = SensitiveMatcher.build(null);
		assertFalse(empty.containsAny("abc"));
		assertTrue(empty.findMatches("以德报怨").isEmpty());
		assertFalse(nullMatcher.containsAny("abc"));
		assertTrue(nullMatcher.findMatches("E2E文章").isEmpty());
	}
}
