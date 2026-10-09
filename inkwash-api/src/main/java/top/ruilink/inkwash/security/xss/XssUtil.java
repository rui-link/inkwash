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
package top.ruilink.inkwash.security.xss;

import java.net.URI;
import java.util.regex.Pattern;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.util.StringUtils;

/**
 * Rich text XSS protection utility based on the official OWASP library,
 * defending against all known XSS vectors.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class XssUtil {

	private XssUtil() {
	}

	private static final PolicyFactory PLAIN_TEXT_POLICY = Sanitizers.FORMATTING;

	private static final PolicyFactory SAFE_HTML_POLICY = new HtmlPolicyBuilder().allowCommonInlineFormattingElements()
			.allowCommonBlockElements().allowStyling()
			.allowElements("table", "tr", "td", "th", "thead", "tbody", "tfoot")
			.allowAttributes("border", "cellpadding", "cellspacing", "width", "height", "class", "style")
			.onElements("table", "tr", "td", "th").allowElements("a").allowAttributes("href", "target", "rel")
			.onElements("a").allowElements("img").allowAttributes("src").onElements("img")
			.allowElements("video", "source").allowAttributes("controls", "poster", "width", "height", "class", "style")
			.onElements("video").allowAttributes("src", "type").onElements("source")
			.allowAttributes("class", "style", "title", "alt").onElements("*").allowStandardUrlProtocols().toFactory();

	private static final String[] MEDIA_ALLOWED_DOMAINS = { "localhost:8080" };

	/**
	 * Rich text XSS sanitisation that keeps safe HTML styling.
	 */
	public static String sanitizeRichText(String input) {
		if (!StringUtils.hasText(input)) {
			return input;
		}
		return SAFE_HTML_POLICY.sanitize(input);
	}

	/**
	 * Plain text XSS sanitisation that keeps no HTML at all.
	 */
	public static String sanitizePlainText(String input) {
		if (!StringUtils.hasText(input)) {
			return input;
		}
		return PLAIN_TEXT_POLICY.sanitize(input);
	}

	// Matches script tags and their variants, case insensitively
	private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("<script[^>]*>.*?</script>",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	// Matches HTML event handler attributes such as onclick, onload and onerror
	private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile("\\son\\w+\\s*=", Pattern.CASE_INSENSITIVE);

	// Matches the javascript: scheme
	private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN = Pattern.compile("javascript:\\s*",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Markdown XSS sanitisation that only removes executable scripts and leaves
	 * everything else alone, since Markdown itself cannot run JS, so this only
	 * strips embedded raw HTML script tags and event handlers.
	 */
	public static String sanitizeMarkdown(String input) {
		if (!StringUtils.hasText(input)) {
			return input;
		}
		String result = SCRIPT_TAG_PATTERN.matcher(input).replaceAll("");
		result = EVENT_HANDLER_PATTERN.matcher(result).replaceAll(" disabled-");
		result = JAVASCRIPT_PROTOCOL_PATTERN.matcher(result).replaceAll("disabled:");
		return result;
	}

	/**
	 * Checks whether the content is safe, meaning free of dangerous scripts.
	 */
	public static boolean isSafeContent(String input) {
		if (!StringUtils.hasText(input)) {
			return true;
		}
		return input.equals(sanitizeRichText(input));
	}

	/**
	 * Validates that a media URL is on the allowlist.
	 */
	public static boolean isValidMediaUrl(String url) {
		if (!StringUtils.hasText(url)) {
			return false;
		}
		try {
			String host = new URI(url).getHost();
			for (String domain : MEDIA_ALLOWED_DOMAINS) {
				if (host.equals(domain) || host.endsWith("." + domain)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}
}
