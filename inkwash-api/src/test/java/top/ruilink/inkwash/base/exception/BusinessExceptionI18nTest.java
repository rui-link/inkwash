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
package top.ruilink.inkwash.base.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import top.ruilink.inkwash.base.util.LocaleUtil;

/**
 * Locks the {@code messageKey} contract introduced by ISS-018 / ISS-041 /
 * ISS-049.
 *
 * <p>
 * Three regressions are guarded here:
 * <ol>
 * <li>a {@code BusinessException} must expose the raw key via
 * {@link #getMessageKey()} while {@link #getMessage()} resolves it for the
 * request locale;</li>
 * <li>{@code {0}} placeholders must be substituted for parameterized
 * messages;</li>
 * <li>every {@code error.*} key referenced from source must exist in
 * <em>both</em> bundles — {@code ResourceBundleMessageSource} silently falls
 * back to the base bundle, so a missing {@code zh_CN} entry is invisible at
 * runtime and only shows up as an English string in a Chinese UI.</li>
 * </ol>
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("异常文案 messageKey 契约")
class BusinessExceptionI18nTest {

	private static final Path BASE_BUNDLE = Path.of("src/main/resources/i18n/messages.properties");
	private static final Path ZH_BUNDLE = Path.of("src/main/resources/i18n/messages_zh_CN.properties");

	@BeforeEach
	void installMessageSource() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("i18n/messages");
		source.setDefaultEncoding(StandardCharsets.UTF_8.name());
		// Must mirror spring.messages.fallback-to-system-locale=false. Without this,
		// java.util.ResourceBundle silently falls back to the *default locale* bundle
		// before the base bundle, so an English request on a zh_CN JVM returns Chinese
		// and the English assertions below would pass for the wrong reason.
		source.setFallbackToSystemLocale(false);
		new LocaleUtil(source);
		LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
	}

	@AfterEach
	void resetLocale() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void keyResolvesToChineseWhileMessageKeyStaysRaw() {
		BusinessException ex = new BusinessException("error.article.not_found");

		assertEquals("文章不存在", ex.getMessage());
		assertEquals("error.article.not_found", ex.getMessageKey(), "getMessageKey 必须返回原始 key，供日志与测试断言使用");
	}

	@Test
	void keyResolvesToEnglishForEnglishLocale() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);

		assertEquals("Article not found", new BusinessException("error.article.not_found").getMessage());
	}

	@Test
	void placeholderIsSubstituted() {
		BusinessException ex = new BusinessException("error.sort.field_invalid", "title");

		assertEquals("非法的排序字段: title", ex.getMessage());
		assertEquals("error.sort.field_invalid", ex.getMessageKey());
	}

	@Test
	void placeholderIsSubstitutedForEnglishLocale() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);

		assertEquals("Unsupported sort field: title",
				new BusinessException("error.sort.field_invalid", "title").getMessage());
	}

	@Test
	void collectionArgumentIsRendered() {
		BusinessException ex = new BusinessException("error.sensitive.content_blocked",
				String.join(", ", List.of("spam", "ad")));

		assertEquals("内容包含敏感词: spam, ad", ex.getMessage());
	}

	@Test
	void unknownKeyFallsBackToItself() {
		assertEquals("error.definitely.missing", new BusinessException("error.definitely.missing").getMessage());
	}

	@Test
	void httpStatusAndCodeArePreserved() {
		BusinessException ex = new BusinessException(org.springframework.http.HttpStatus.NOT_FOUND, 42,
				"error.article.not_found");

		assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getHttpStatus());
		assertEquals(42, ex.getCode());
	}

	@Test
	void bothBundlesDeclareIdenticalKeySets() throws IOException {
		Set<String> base = keysOf(BASE_BUNDLE);
		Set<String> zh = keysOf(ZH_BUNDLE);

		Set<String> missingInZh = new TreeSet<>(base);
		missingInZh.removeAll(zh);
		Set<String> missingInBase = new TreeSet<>(zh);
		missingInBase.removeAll(base);

		assertTrue(missingInZh.isEmpty(), () -> "messages_zh_CN.properties 缺少: " + missingInZh);
		assertTrue(missingInBase.isEmpty(), () -> "messages.properties 缺少: " + missingInBase);
	}

	@Test
	void everyReferencedErrorKeyExistsInBothBundles() throws IOException {
		Set<String> referenced = referencedErrorKeys();
		Set<String> base = keysOf(BASE_BUNDLE);
		Set<String> zh = keysOf(ZH_BUNDLE);

		Set<String> missingInBase = new TreeSet<>(referenced);
		missingInBase.removeAll(base);
		Set<String> missingInZh = new TreeSet<>(referenced);
		missingInZh.removeAll(zh);

		assertTrue(missingInBase.isEmpty(), () -> "messages.properties 缺少被引用的 key: " + missingInBase);
		assertTrue(missingInZh.isEmpty(), () -> "messages_zh_CN.properties 缺少被引用的 key: " + missingInZh);
	}

	@Test
	void noBusinessExceptionStillCarriesAChineseLiteral() throws IOException {
		Pattern chineseThrow = Pattern.compile("new BusinessException\\([^)]*[\\u4e00-\\u9fff]");
		List<String> offenders;
		try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
			offenders = files.filter(p -> p.toString().endsWith(".java")).filter(p -> {
				try {
					return chineseThrow.matcher(Files.readString(p, StandardCharsets.UTF_8)).find();
				} catch (IOException e) {
					throw new UncheckedIo(e);
				}
			}).map(Path::toString).toList();
		}

		assertTrue(offenders.isEmpty(), () -> "以下文件仍直接抛出中文文案，应改用 error.* key: " + offenders);
	}

	private static final class UncheckedIo extends RuntimeException {
		private static final long serialVersionUID = 1L;

		UncheckedIo(IOException cause) {
			super(cause);
		}
	}

	private static Set<String> referencedErrorKeys() throws IOException {
		// Tolerant of fully-qualified constructor calls such as
		// `new top.ruilink.inkwash.base.exception.BusinessException("...")`, which a
		// stricter `new BusinessException\(` pattern silently misses.
		Pattern call = Pattern.compile("BusinessException\\(.{0,160}?\"(error\\.[A-Za-z0-9_.]+)\"");
		Set<String> keys = new LinkedHashSet<>();
		try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
			for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
				Matcher matcher = call.matcher(Files.readString(p, StandardCharsets.UTF_8));
				while (matcher.find()) {
					keys.add(matcher.group(1));
				}
			}
		}
		// Canary: if this drops, the regex has stopped matching and the parity test
		// above would pass vacuously. Raise it when new error keys are introduced.
		assertTrue(keys.size() >= 90, "扫描到的 error.* key 数量异常减少，正则可能失效: " + keys.size());
		return keys;
	}

	private static Set<String> keysOf(Path bundle) throws IOException {
		Properties properties = new Properties();
		try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(bundle), StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		return new LinkedHashSet<>(properties.stringPropertyNames());
	}
}