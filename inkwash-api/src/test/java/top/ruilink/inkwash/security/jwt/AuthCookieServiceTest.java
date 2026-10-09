package top.ruilink.inkwash.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import top.ruilink.inkwash.security.config.AuthCookieConfig;

/**
 * Auth cookie attributes and browser-mode detection unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class AuthCookieServiceTest {

	private AuthCookieConfig properties;
	private JwtTokenConfig jwtConfig;
	private AuthCookieService cookieService;

	@BeforeEach
	void setUp() {
		properties = new AuthCookieConfig();
		jwtConfig = new JwtTokenConfig();
		jwtConfig.setAccessExpireTime(86_400_000L); // 24h in ms
		jwtConfig.setRefreshExpireTime(604_800_000L); // 7d in ms
		cookieService = new AuthCookieService(properties, jwtConfig);
	}

	private List<String> setCookies(MockHttpServletResponse response) {
		return response.getHeaders("Set-Cookie");
	}

	@Test
	@DisplayName("browser mode when X-Requested-With header matches")
	void isBrowserRequest_WithXhrHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Requested-With", "XMLHttpRequest");
		assertTrue(cookieService.isBrowserRequest(request));
	}

	@Test
	@DisplayName("browser mode when secure_access cookie present")
	void isBrowserRequest_WithAccessCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new jakarta.servlet.http.Cookie("secure_access", "jwt"));
		assertTrue(cookieService.isBrowserRequest(request));
	}

	@Test
	@DisplayName("browser mode when secure_refresh cookie present")
	void isBrowserRequest_WithRefreshCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new jakarta.servlet.http.Cookie("secure_refresh", "jwt"));
		assertTrue(cookieService.isBrowserRequest(request));
	}

	@Test
	@DisplayName("programmatic mode without xhr header or auth cookies")
	void notBrowserRequest_WithoutIndicators() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		assertEquals(false, cookieService.isBrowserRequest(request));
	}

	@Test
	@DisplayName("writeTokens sets httpOnly secure cookies with TTL-aligned max age")
	void writeTokens_SetsHttpOnlyCookies() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		cookieService.writeTokens(response, "access-jwt", "refresh-jwt");

		List<String> cookies = setCookies(response);
		assertEquals(2, cookies.size());

		String access = cookies.stream().filter(c -> c.startsWith("secure_access=")).findFirst().orElseThrow();
		String refresh = cookies.stream().filter(c -> c.startsWith("secure_refresh=")).findFirst().orElseThrow();

		assertTrue(access.contains("access-jwt"), "access cookie carries jwt");
		assertTrue(access.contains("Max-Age=86400"), "access cookie max age aligns access TTL");
		assertTrue(access.contains("HttpOnly"), "access cookie is httpOnly");
		assertTrue(access.contains("SameSite=Lax"), "access cookie has SameSite");
		assertTrue(access.contains("Path=/"), "access cookie path is /");

		assertTrue(refresh.contains("refresh-jwt"), "refresh cookie carries jwt");
		assertTrue(refresh.contains("Max-Age=604800"), "refresh cookie max age aligns refresh TTL");
		assertTrue(refresh.contains("HttpOnly"), "refresh cookie is httpOnly");
		assertTrue(refresh.contains("Path=/api/auth"), "refresh cookie path scoped to auth");
	}

	@Test
	@DisplayName("secure flag adds Secure attribute when enabled")
	void writeTokens_SecureWhenConfigured() {
		properties.setSecure(true);
		MockHttpServletResponse response = new MockHttpServletResponse();
		cookieService.writeTokens(response, "access-jwt", "refresh-jwt");

		assertTrue(setCookies(response).get(0).contains("Secure"));
	}

	@Test
	@DisplayName("explicit max age overrides jwt TTL")
	void writeTokens_HonorsExplicitMaxAge() {
		properties.setAccessMaxAge(120L);
		MockHttpServletResponse response = new MockHttpServletResponse();
		cookieService.writeTokens(response, "access-jwt", "refresh-jwt");

		assertTrue(setCookies(response).get(0).contains("Max-Age=120"));
	}

	@Test
	@DisplayName("clearTokens expires both cookies")
	void clearTokens_ExpiresBothCookies() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		cookieService.clearTokens(response);

		List<String> cookies = setCookies(response);
		assertEquals(2, cookies.size());
		assertTrue(cookies.stream().allMatch(c -> c.contains("Max-Age=0")));
		assertTrue(cookies.stream().anyMatch(c -> c.startsWith("secure_access=")));
		assertTrue(cookies.stream().anyMatch(c -> c.startsWith("secure_refresh=")));
	}

	@Test
	@DisplayName("disabled cookies write nothing")
	void disabled_WritesNothing() {
		properties.setEnabled(false);
		MockHttpServletResponse response = new MockHttpServletResponse();
		cookieService.writeTokens(response, "access-jwt", "refresh-jwt");
		assertEquals(0, setCookies(response).size());
	}
}