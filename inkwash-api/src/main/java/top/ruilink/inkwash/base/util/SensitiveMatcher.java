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
package top.ruilink.inkwash.base.util;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Aho-Corasick automaton for multi-pattern string matching. Finds all
 * occurrences of multiple patterns in a text in O(n + m + z) time, where n =
 * text length, m = total pattern length, z = number of matches.
 *
 * Supported characters: a-z, A-Z (case-insensitive), 0-9, and any CJK character
 * that appears in a pattern. Other characters are treated as word boundaries
 * (skipped).
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class SensitiveMatcher implements Serializable {

	private final Map<Integer, Integer>[] transitions;
	private final int[] failure;
	private final int[][] outputs; // outputs[i] = pattern indices matched at state i
	private final int[] outputLen; // actual length of each output array
	private final int patternCount;
	private final Map<Character, Integer> charToIndex;
	/**
	 * The patterns this automaton was built from, in index order.
	 *
	 * <p>
	 * Retained so a caller can resolve the indices returned by
	 * {@link #findMatches(String)} back to the matched word. Callers must NOT pair
	 * those indices with a separately fetched word list: the automaton and that
	 * list may come from different snapshots, which yields the wrong word or an
	 * {@code IndexOutOfBoundsException} (ISS-015).
	 */
	private final List<String> patterns;

	private SensitiveMatcher(Map<Integer, Integer>[] transitions, int[] failure, int[][] outputs, int[] outputLen,
			int patternCount, Map<Character, Integer> charToIndex, List<String> patterns) {
		this.transitions = transitions;
		this.failure = failure;
		this.outputs = outputs;
		this.outputLen = outputLen;
		this.patternCount = patternCount;
		this.charToIndex = charToIndex;
		this.patterns = patterns;
	}

	/**
	 * Build an Aho-Corasick automaton from the given patterns.
	 *
	 * @param patterns list of patterns to match (e.g. sensitive words)
	 * @return a matcher, or an empty matcher if patterns is null/empty
	 */
	public static SensitiveMatcher build(List<String> patterns) {
		if (patterns == null || patterns.isEmpty()) {
			@SuppressWarnings("unchecked")
			Map<Integer, Integer>[] empty = new Map[0];
			return new SensitiveMatcher(empty, new int[0], new int[0][], new int[0], 0, new HashMap<>(), List.of());
		}

		// The alphabet is derived from the patterns: a-z, 0-9 always present,
		// plus every distinct CJK character used by any pattern.
		Map<Character, Integer> charToIndex = buildAlphabet(patterns);
		int alphabetSize = charToIndex.size();

		// Phase 1: Build trie
		int nodeCapacity = Math.max(8, patterns.size() * 4);
		int[][] trie = new int[nodeCapacity][alphabetSize];
		int[] fail = new int[nodeCapacity];
		int nodeCount = 1; // root = 0

		// Track which patterns end at each node
		List<List<Integer>> nodeOutputs = new ArrayList<>(nodeCapacity);
		nodeOutputs.add(new ArrayList<>()); // root

		for (int pid = 0; pid < patterns.size(); pid++) {
			String pattern = patterns.get(pid);
			int cur = 0;
			for (int i = 0; i < pattern.length(); i++) {
				int c = indexOf(pattern.charAt(i), charToIndex);
				if (c < 0)
					continue;
				if (trie[cur][c] == 0) {
					if (nodeCount == nodeCapacity) {
						nodeCapacity *= 2;
						int[][] bigger = new int[nodeCapacity][alphabetSize];
						for (int k = 0; k < trie.length; k++) {
							bigger[k] = trie[k];
						}
						trie = bigger;
						fail = Arrays.copyOf(fail, nodeCapacity);
						while (nodeOutputs.size() < nodeCapacity)
							nodeOutputs.add(new ArrayList<>());
					}
					trie[cur][c] = nodeCount++;
				}
				cur = trie[cur][c];
			}
			while (nodeOutputs.size() < nodeCount)
				nodeOutputs.add(new ArrayList<>());
			nodeOutputs.get(cur).add(pid);
		}

		// Phase 2: Build failure links via BFS
		Queue<Integer> queue = new ArrayDeque<>();
		for (int c = 0; c < alphabetSize; c++) {
			int next = trie[0][c];
			if (next != 0) {
				fail[next] = 0;
				queue.add(next);
			}
		}

		while (!queue.isEmpty()) {
			int r = queue.poll();
			for (int c = 0; c < alphabetSize; c++) {
				int next = trie[r][c];
				if (next != 0) {
					int f = fail[r];
					while (f != 0 && trie[f][c] == 0) {
						f = fail[f];
					}
					fail[next] = trie[f][c] != 0 ? trie[f][c] : 0;
					// Merge failure state's outputs into this state
					if (!nodeOutputs.get(fail[next]).isEmpty()) {
						nodeOutputs.get(next).addAll(nodeOutputs.get(fail[next]));
					}
					queue.add(next);
				}
			}
		}

		// Phase 3: Compact to final arrays. Only non-zero transitions are kept per
		// node (sparse maps), shrinking memory for CJK-heavy alphabets.
		@SuppressWarnings("unchecked")
		Map<Integer, Integer>[] compactTransitions = new Map[nodeCount];
		int[] compactFail = new int[nodeCount];
		int[][] compactOutputs = new int[nodeCount][];
		int[] compactOutputLen = new int[nodeCount];

		for (int i = 0; i < nodeCount; i++) {
			Map<Integer, Integer> row = new HashMap<>();
			for (int c = 0; c < alphabetSize; c++) {
				if (trie[i][c] != 0) {
					row.put(c, trie[i][c]);
				}
			}
			compactTransitions[i] = row;
			compactFail[i] = fail[i];
			List<Integer> out = nodeOutputs.get(i);
			compactOutputs[i] = new int[out.size()];
			for (int j = 0; j < out.size(); j++) {
				compactOutputs[i][j] = out.get(j);
			}
			compactOutputLen[i] = out.size();
		}

		return new SensitiveMatcher(compactTransitions, compactFail, compactOutputs, compactOutputLen, patterns.size(),
				charToIndex, List.copyOf(patterns));
	}

	/**
	 * Find all pattern indices that match anywhere in the text. Returns unique
	 * matched pattern indices (deduplicated).
	 */
	public List<Integer> findMatches(String text) {
		if (text == null || text.isEmpty() || transitions.length == 0) {
			return List.of();
		}

		boolean[] matched = new boolean[patternCount];
		int state = 0;

		for (int i = 0; i < text.length(); i++) {
			int c = indexOf(text.charAt(i));
			if (c < 0)
				continue;

			while (state != 0 && transitions[state].getOrDefault(c, 0) == 0) {
				state = failure[state];
			}
			state = transitions[state].getOrDefault(c, 0);

			int len = outputLen[state];
			if (len > 0) {
				int[] out = outputs[state];
				for (int j = 0; j < len; j++) {
					matched[out[j]] = true;
				}
			}
		}

		List<Integer> result = new ArrayList<>();
		for (int i = 0; i < patternCount; i++) {
			if (matched[i])
				result.add(i);
		}
		return result;
	}

	/**
	 * Check if any pattern exists in the text. Early exit on first match.
	 */

	/**
	 * Resolves a pattern index returned by {@link #findMatches(String)} to its
	 * word.
	 *
	 * @param patternIndex index as produced by {@code findMatches}
	 * @return the matched word
	 * @throws IndexOutOfBoundsException if the index did not come from this matcher
	 */
	public String wordAt(int patternIndex) {
		return patterns.get(patternIndex);
	}

	/** Number of patterns this automaton was built from. */
	public int size() {
		return patternCount;
	}

	public boolean containsAny(String text) {
		if (text == null || text.isEmpty() || transitions.length == 0) {
			return false;
		}

		int state = 0;
		for (int i = 0; i < text.length(); i++) {
			int c = indexOf(text.charAt(i));
			if (c < 0)
				continue;

			while (state != 0 && transitions[state].getOrDefault(c, 0) == 0) {
				state = failure[state];
			}
			state = transitions[state].getOrDefault(c, 0);

			if (outputLen[state] > 0) {
				return true;
			}
		}
		return false;
	}

	public int getPatternCount() {
		return patternCount;
	}

	/**
	 * Build the alphabet mapping from the given patterns. a-z -> 0-25, A-Z -> 0-25
	 * (case-insensitive), 0-9 -> 26-35, then every distinct CJK character found in
	 * any pattern.
	 */
	private static Map<Character, Integer> buildAlphabet(List<String> patterns) {
		Map<Character, Integer> map = new HashMap<>();
		for (char c = 'a'; c <= 'z'; c++) {
			map.put(c, map.size());
		}
		for (char c = '0'; c <= '9'; c++) {
			map.put(c, map.size());
		}
		for (String pattern : patterns) {
			for (int i = 0; i < pattern.length(); i++) {
				char ch = pattern.charAt(i);
				if (isCjk(ch) && !map.containsKey(ch)) {
					map.put(ch, map.size());
				}
			}
		}
		return map;
	}

	/**
	 * Map a character to its alphabet index, or -1 if unsupported.
	 */
	private int indexOf(char ch) {
		return indexOf(ch, charToIndex);
	}

	private static int indexOf(char ch, Map<Character, Integer> charToIndex) {
		Integer idx = charToIndex.get(normalize(ch));
		return idx == null ? -1 : idx;
	}

	/**
	 * Normalize a character: uppercase ASCII letters fold to lowercase.
	 */
	private static char normalize(char ch) {
		if (ch >= 'A' && ch <= 'Z') {
			return (char) (ch - 'A' + 'a');
		}
		return ch;
	}

	/**
	 * Check whether a BMP character is a CJK ideograph.
	 */
	private static boolean isCjk(char ch) {
		return (ch >= 0x4E00 && ch <= 0x9FFF) || (ch >= 0x3400 && ch <= 0x4DBF) || (ch >= 0xF900 && ch <= 0xFAFF);
	}
}
