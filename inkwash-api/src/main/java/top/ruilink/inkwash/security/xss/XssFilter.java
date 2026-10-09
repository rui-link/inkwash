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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * XSS protection filter based on the OWASP enterprise library. Intercepts every
 * request and filters its parameters to prevent cross-site scripting. It
 * supports plain text escaping plus safe Markdown sanitisation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter extends OncePerRequestFilter {
	// Paths allowed through without XSS filtering.
	//
	// Every entry below was checked against the actual handler mappings (ISS-057).
	// Note that EXCLUDE_PATHS matches by prefix, so "/api/auth/register" also
	// covers
	// /register/password and /register/sms.
	//
	// Removed as non-existent:
	// /api/auth/password -> password endpoints live under /api/profile/**
	// /api/auth/reset-password -> ditto (/api/profile/password/reset)
	// /doc/ and /webjars/ -> no springdoc / knife4j dependency, so no API-doc UI
	private static final String[] EXCLUDE_PATHS = { "/api/auth/login", "/api/auth/register", "/api/auth/oauth2/token",
			"/static/", "/favicon.ico",
			// spring-boot-h2console is on the classpath but the console is not enabled in
			// application.yaml; keep the entry so enabling it later needs no filter change.
			"/h2-console/",
			// Spring Security OAuth2 flow: state, code and error parameters are opaque
			// server generated tokens,
			// so HTML escaping breaks comparison against session values and yields
			// authorization_request_not_found
			"/oauth2/", "/login", "/.well-known/" };

	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	// Fields rendered as Markdown/HTML and plain-text display fields (title,
	// summary, ...).
	// None of these may be HTML-entity-escaped on input: entity sanitising would
	// corrupt ordinary
	// text (e.g. 全角逗号 → &#xff0c;, & → &amp;, and <b> would be re-parsed into real
	// markup). They
	// are all rendered safely on the client (Vue text interpolation or a
	// DOMPurify-sanitised
	// markdown renderer), so the input side only applies the script-vector guard,
	// which strips
	// <script>, on* attributes and javascript: URLs while preserving literal
	// characters.
	private static final Set<String> MARKDOWN_FIELDS = Set.of("content");

	private static final Set<String> PLAIN_TEXT_FIELDS = Set.of("title", "subtitle", "summary", "description", "remark",
			"abstract", "keywords");

	// Credential, token and captcha fields always pass through untouched, since
	// entity escaping would break password hash and token comparison
	private static final Set<String> CREDENTIAL_FIELDS = Set.of("password", "oldPassword", "newPassword",
			"confirmPassword", "code", "smsCode", "emailCode", "captchaCode", "verifyCode", "token", "refreshToken",
			"accessToken", "secret");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String uri = request.getRequestURI();

		// Pass through paths needing no filtering
		if (isExcludePath(uri)) {
			setSecurityHeaders(response);
			filterChain.doFilter(request, response);
			return;
		}

		// XSS request wrapper
		XssRequestWrapper wrapper = new XssRequestWrapper(request);
		setSecurityHeaders(response);

		filterChain.doFilter(wrapper, response);
	}

	// Allowed path matching
	private boolean isExcludePath(String uri) {
		for (String path : EXCLUDE_PATHS) {
			if (uri.startsWith(path) || uri.equals(path)) {
				return true;
			}
		}
		return false;
	}

	// Security response headers, following modern browser standards
	private void setSecurityHeaders(HttpServletResponse response) {
		response.setHeader("X-XSS-Protection", "1; mode=block");
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
		response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
		response.setHeader("Content-Security-Policy",
				"default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; object-src 'none'; frame-ancestors 'none'");
	}

	/**
	 * XSS request wrapper.
	 */
	private static class XssRequestWrapper extends HttpServletRequestWrapper {

		private byte[] cachedBody;

		public XssRequestWrapper(HttpServletRequest request) {
			super(request);
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			if (cachedBody == null) {
				boolean json = getContentType() != null && getContentType().toLowerCase().contains("json");
				byte[] body = super.getInputStream().readAllBytes();
				if (json && body.length > 0) {
					String sanitized = sanitizeJsonBody(new String(body, StandardCharsets.UTF_8));
					cachedBody = sanitized.getBytes(StandardCharsets.UTF_8);
				} else {
					cachedBody = body;
				}
			}
			ByteArrayInputStream buffer = new ByteArrayInputStream(cachedBody);
			return new ServletInputStream() {
				@Override
				public int read() throws IOException {
					return buffer.read();
				}

				@Override
				public boolean isFinished() {
					return buffer.available() == 0;
				}

				@Override
				public boolean isReady() {
					return true;
				}

				@Override
				public void setReadListener(ReadListener readListener) {
				}
			};
		}

		@Override
		public BufferedReader getReader() throws IOException {
			return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
		}

		private String sanitizeJsonBody(String body) {
			try (JsonParser parser = JSON_FACTORY.createParser(ObjectReadContext.empty(), body);
					StringWriter writer = new StringWriter(body.length() + 16);
					JsonGenerator generator = JSON_FACTORY.createGenerator(ObjectWriteContext.empty(), writer)) {
				String currentField = null;
				while (parser.nextToken() != null) {
					JsonToken token = parser.currentToken();
					switch (token) {
					case START_OBJECT -> generator.writeStartObject();
					case END_OBJECT -> generator.writeEndObject();
					case START_ARRAY -> generator.writeStartArray();
					case END_ARRAY -> generator.writeEndArray();
					case PROPERTY_NAME -> {
						currentField = parser.currentName();
						generator.writeName(currentField);
					}
					case VALUE_STRING -> generator.writeString(sanitizeStringValue(currentField, parser.getString()));
					case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> generator.writeRawValue(parser.getString());
					case VALUE_TRUE -> generator.writeBoolean(true);
					case VALUE_FALSE -> generator.writeBoolean(false);
					case VALUE_NULL -> generator.writeNull();
					default -> generator.writeString(parser.getString());
					}
				}
				generator.flush();
				return writer.toString();
			} catch (IOException e) {
				XssFilter.log.warn("JSON请求体清洗失败，使用原始内容: {}", e.getMessage());
				return body;
			}
		}

		/**
		 * Field name aware string sanitisation: credential and other plain fields pass
		 * through untouched, while only markdown and plain-text rendering fields get
		 * the script-vector guard.
		 */
		private String sanitizeStringValue(String fieldName, String value) {
			if (value == null || value.isEmpty()) {
				return value;
			}
			if (fieldName == null || CREDENTIAL_FIELDS.contains(fieldName)) {
				return value;
			}
			if (MARKDOWN_FIELDS.contains(fieldName) || PLAIN_TEXT_FIELDS.contains(fieldName)) {
				return XssUtil.sanitizeMarkdown(value);
			}
			return value;
		}

		@Override
		public String getParameter(String name) {
			String val = super.getParameter(name);
			return clean(val, name);
		}

		@Override
		public String[] getParameterValues(String name) {
			String[] vals = super.getParameterValues(name);
			if (vals == null)
				return null;

			String[] result = new String[vals.length];
			for (int i = 0; i < vals.length; i++) {
				result[i] = clean(vals[i], name);
			}
			return result;
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			Map<String, String[]> original = super.getParameterMap();
			Map<String, String[]> cleaned = new HashMap<>(original.size());

			for (Map.Entry<String, String[]> entry : original.entrySet()) {
				String name = entry.getKey();
				String[] values = entry.getValue();

				if (values == null) {
					cleaned.put(name, null);
					continue;
				}

				String[] newValues = new String[values.length];
				for (int i = 0; i < values.length; i++) {
					newValues[i] = clean(values[i], name);
				}
				cleaned.put(name, newValues);
			}
			return cleaned;
		}

		/**
		 * Core sanitisation method with field name awareness: credential and token
		 * fields pass through untouched, only markdown and plain-text rendering fields
		 * get the script-vector guard, everything else passes through.
		 */
		private String clean(String value, String paramName) {
			if (value == null || value.isEmpty()) {
				return value;
			}
			if (CREDENTIAL_FIELDS.contains(paramName)) {
				return value;
			}
			// Article content fields and plain-text display fields use the script-vector
			// guard
			if (MARKDOWN_FIELDS.contains(paramName) || PLAIN_TEXT_FIELDS.contains(paramName)) {
				return XssUtil.sanitizeMarkdown(value);
			}
			// All other fields pass through unchanged, with no entity escaping on input
			return value;
		}
	}
}
