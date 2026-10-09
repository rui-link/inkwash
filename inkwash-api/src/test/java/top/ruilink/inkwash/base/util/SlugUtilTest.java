package top.ruilink.inkwash.base.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Slug generation unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class SlugUtilTest {

	@Test
	@DisplayName("连续空白折叠为单个连字符")
	void generate_collapsesWhitespaceToSingleDash() {
		assertEquals("hello-world", SlugUtil.generate("Hello  World"));
	}

	@Test
	@DisplayName("字面加号不再被当作空白折成连字符，作为非法字符去除")
	void generate_literalPlus_isNotCollapsedAsWhitespace() {
		assertEquals("ab", SlugUtil.generate("a+b"));
		assertEquals("a-b", SlugUtil.generate("a b"));
	}

	@Test
	@DisplayName("首尾空白被去除")
	void generate_trimsLeadingTrailingWhitespace() {
		assertEquals("frontend", SlugUtil.generate("  Frontend  "));
	}

	@Test
	@DisplayName("保留中文与连字符")
	void generate_keepsHanAndDash() {
		assertEquals("前端-开发", SlugUtil.generate("前端 开发"));
	}

	@Test
	@DisplayName("null 直接返回 null")
	void generate_null_ReturnsNull() {
		assertEquals(null, SlugUtil.generate(null));
	}
}