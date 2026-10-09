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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.base.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks the match-index contract of {@link SensitiveMatcher} (ISS-015).
 *
 * <p>
 * {@code findMatches} returns indices into the pattern list the automaton was
 * built from. Callers used to pair those indices with a <em>separately
 * fetched</em> word list ({@code ArticleServiceImpl} called both
 * {@code getAllActiveWords()} and {@code getMatcher()}). When the cache was
 * evicted between the two reads, the two snapshots differed and the caller
 * either reported the wrong word or hit an {@code IndexOutOfBoundsException}.
 *
 * <p>
 * The matcher now owns its source list, so {@link #wordAt(int)} always resolves
 * an index produced by the same instance.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("敏感词匹配索引一致性（ISS-015）")
class SensitiveMatcherIndexTest {

	private static final List<String> WORDS = List.of("以德报怨", "忍无可忍", "tencent");

	@Test
	@DisplayName("findMatches 的每个索引都能被 wordAt 解析")
	void everyMatchIndexResolvesToItsWord() {
		SensitiveMatcher matcher = SensitiveMatcher.build(WORDS);
		String text = "此人以德报怨，实乃忍无可忍，tencent 亦然";

		List<Integer> indices = matcher.findMatches(text);

		assertEquals(3, indices.size(), "应命中三个词: " + indices);
		for (int idx : indices) {
			assertNotNull(matcher.wordAt(idx), "索引 " + idx + " 必须能解析出对应词");
		}
		assertEquals(List.of("以德报怨", "忍无可忍", "tencent"), indices.stream().map(matcher::wordAt).toList(),
				"解析结果必须与词表顺序一致");
	}

	@Test
	@DisplayName("wordAt 与外部词表在同一次快照下等价")
	void wordAtMatchesExternalListForTheSameSnapshot() {
		SensitiveMatcher matcher = SensitiveMatcher.build(WORDS);

		for (int i = 0; i < WORDS.size(); i++) {
			assertEquals(WORDS.get(i), matcher.wordAt(i));
		}
		assertEquals(WORDS.size(), matcher.size());
	}

	@Test
	@DisplayName("越界索引抛 IndexOutOfBoundsException，而不是静默返回 null")
	void outOfRangeIndexThrows() {
		SensitiveMatcher matcher = SensitiveMatcher.build(WORDS);

		assertThrows(IndexOutOfBoundsException.class, () -> matcher.wordAt(WORDS.size()));
		assertThrows(IndexOutOfBoundsException.class, () -> matcher.wordAt(-1));
	}

	@Test
	@DisplayName("空词表构建的 matcher 不返回任何匹配")
	void emptyMatcherMatchesNothing() {
		SensitiveMatcher matcher = SensitiveMatcher.build(List.of());

		assertEquals(0, matcher.size());
		assertTrue(matcher.findMatches("任意文本").isEmpty());
		assertThrows(IndexOutOfBoundsException.class, () -> matcher.wordAt(0));
	}

	@Test
	@DisplayName("null 词表按空处理")
	void nullPatternsTreatedAsEmpty() {
		SensitiveMatcher matcher = SensitiveMatcher.build(null);

		assertEquals(0, matcher.size());
		assertTrue(matcher.findMatches("任意文本").isEmpty());
	}

	@Test
	@DisplayName("重复词不会让索引解析错位")
	void duplicatePatternsStillResolve() {
		List<String> withDup = List.of("词", "词", "另一个");
		SensitiveMatcher matcher = SensitiveMatcher.build(withDup);

		List<Integer> indices = matcher.findMatches("词");

		assertEquals("词", matcher.wordAt(indices.get(0)));
		assertEquals("另一个", matcher.wordAt(2));
	}

	@Test
	@DisplayName("wordAt 的结果不受调用顺序影响（不可变快照）")
	void matcherIsImmutableAcrossCalls() {
		SensitiveMatcher matcher = SensitiveMatcher.build(WORDS);

		List<Integer> first = matcher.findMatches("以德报怨");
		List<Integer> second = matcher.findMatches("忍无可忍");

		assertEquals(WORDS.get(first.get(0)), matcher.wordAt(first.get(0)));
		assertEquals(WORDS.get(second.get(0)), matcher.wordAt(second.get(0)));
		assertEquals(first, matcher.findMatches("以德报怨"), "重复调用必须返回相同索引");
	}
}