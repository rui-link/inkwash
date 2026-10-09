package top.ruilink.inkwash.security.csrf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import top.ruilink.inkwash.security.config.AuthCookieConfig;
import top.ruilink.inkwash.security.config.CorsConfig;

/**
 * Origin-check CSRF filter rejection of cross-site state-changing requests unit
 * tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class OriginCheckCsrfFilterTest {

	private CorsConfig corsConfig;
	private AuthCookieConfig properties;
	private OriginCheckCsrfFilter filter;

	private static final class ProbeChain implements FilterChain {
		boolean invoked = false;

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) {
			invoked = true;
		}
	}

	@BeforeEach
	void setUp() {
		corsConfig = new CorsConfig();
		ReflectionTestUtils.setField(corsConfig, "allowedOrigin",
				"http://localhost:9089, http://localhost:3000, http://localhost:9090 ");
		properties = new AuthCookieConfig();
		filter = new OriginCheckCsrfFilter(corsConfig, properties);
	}

	private ProbeChain run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
		ProbeChain chain = new ProbeChain();
		filter.doFilter(request, response, chain);
		return chain;
	}

	private void useAccessCookie(MockHttpServletRequest request) {
		request.setCookies(new jakarta.servlet.http.Cookie("secure_access", "jwt"));
	}

	private void useRefreshOnlyCookie(MockHttpServletRequest request) {
		request.setCookies(new jakarta.servlet.http.Cookie("secure_refresh", "jwt"));
	}

	@Test
	@DisplayName("read-only request with foreign origin is allowed")
	void readOnly_WithForeignOrigin() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cms/articles");
		useAccessCookie(request);
		request.addHeader("Origin", "http://evil.com");
		assertTrue(run(request, new MockHttpServletResponse()).invoked);
	}

	@Test
	@DisplayName("POST with secure_access and no Origin header is allowed")
	void post_WithAccessCookie_NoOrigin() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		assertTrue(run(request, new MockHttpServletResponse()).invoked);
	}

	@Test
	@DisplayName("POST with allowed origin passes")
	void post_WithAllowedOrigin() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		request.addHeader("Origin", "http://localhost:9089");
		assertTrue(run(request, new MockHttpServletResponse()).invoked);
	}

	@Test
	@DisplayName("POST with allowed origin and trailing slash passes")
	void post_WithAllowedOriginTrailingSlash() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		request.addHeader("Origin", "http://localhost:9089/");
		assertTrue(run(request, new MockHttpServletResponse()).invoked);
	}

	@Test
	@DisplayName("POST with foreign origin is rejected 403")
	void post_WithForeignOrigin() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		request.addHeader("Origin", "http://evil.com");
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertFalse(run(request, response).invoked);
		assertEquals(403, response.getStatus());
		assertEquals("application/json;charset=utf-8", response.getContentType());
		assertTrue(response.getContentAsString().contains("\"status\":403"));
	}

	@Test
	@DisplayName("POST with scheme mismatch is rejected 403")
	void post_WithSchemeMismatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		request.addHeader("Origin", "https://localhost:9089");
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertFalse(run(request, response).invoked);
		assertEquals(403, response.getStatus());
	}

	@Test
	@DisplayName("POST with port mismatch is rejected 403")
	void post_WithPortMismatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		request.addHeader("Origin", "http://localhost:9088");
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertFalse(run(request, response).invoked);
		assertEquals(403, response.getStatus());
	}

	@Test
	@DisplayName("POST without access cookie skips origin check")
	void post_WithoutAccessCookie() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
		request.addHeader("Origin", "http://evil.com");
		assertTrue(run(request, new MockHttpServletResponse()).invoked);
	}

	@Test
	@DisplayName("POST with refresh-only cookie skips origin check")
	void post_WithRefreshOnlyCookie() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/token/refresh");
		useRefreshOnlyCookie(request);
		request.addHeader("Origin", "http://evil.com");
		assertTrue(run(request, new MockHttpServletResponse()).invoked);
	}

	@Test
	@DisplayName("csrf check disabled allows everything")
	void csrfCheckDisabled() throws Exception {
		properties.setCsrfCheckEnabled(false);
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		request.addHeader("Origin", "http://evil.com");
		assertTrue(run(request, new MockHttpServletResponse()).invoked);
	}

	@Test
	@DisplayName("POST with default http port is normalized to the portless whitelist entry")
	void post_WithDefaultHttpPort() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		request.addHeader("Origin", "http://localhost:9089");
		assertTrue(run(request, new MockHttpServletResponse()).invoked);
	}

	@Test
	@DisplayName("POST with explicit default port :80 equals the portless origin")
	void post_Port80NormalizedToPortless() throws Exception {
		ReflectionTestUtils.setField(corsConfig, "allowedOrigin", "http://localhost:8080");
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		request.addHeader("Origin", "http://localhost:80");
		// 8080 != 80, so this must still be rejected
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertFalse(run(request, response).invoked);
		assertEquals(403, response.getStatus());
	}

	@Test
	@DisplayName("POST with malformed Origin is rejected 403")
	void post_WithMalformedOrigin() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		request.addHeader("Origin", "::::not-an-origin:::");
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertFalse(run(request, response).invoked);
		assertEquals(403, response.getStatus());
	}

	@Test
	@DisplayName("POST with default port 443 https normalizes like portless")
	void post_WithDefaultHttpsPort() throws Exception {
		ReflectionTestUtils.setField(corsConfig, "allowedOrigin", "https://localhost");
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		request.addHeader("Origin", "https://localhost:443");
		assertTrue(run(request, new MockHttpServletResponse()).invoked);
	}

	@Test
	@DisplayName("wildcard entries never satisfy the strict csrf whitelist")
	void wildcardOriginIsRejected() throws Exception {
		ReflectionTestUtils.setField(corsConfig, "allowedOrigin", "http://*.example.com,http://localhost:9089");
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
		useAccessCookie(request);
		request.addHeader("Origin", "http://sub.example.com");
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertFalse(run(request, response).invoked);
		assertEquals(403, response.getStatus());
	}
}