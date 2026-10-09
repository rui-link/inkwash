package top.ruilink.inkwash.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import top.ruilink.inkwash.security.adapter.SysUserDetails;
import top.ruilink.inkwash.security.adapter.SysUserDetailsService;
import top.ruilink.inkwash.security.service.TokenService;

/**
 * Bearer header and access cookie authentication with revocation rejection unit
 * tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class JwtAuthenticationFilterTest {

	private JwtTokenProvider tokenProvider;
	private SysUserDetailsService userDetailsService;
	private TokenService tokenService;
	private AuthCookieService cookieService;
	private JwtAuthenticationFilter filter;

	private static final class ProbeChain implements FilterChain {
		boolean invoked = false;

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) {
			invoked = true;
		}
	}

	@BeforeEach
	void setUp() {
		tokenProvider = mock(JwtTokenProvider.class);
		userDetailsService = mock(SysUserDetailsService.class);
		tokenService = mock(TokenService.class);
		cookieService = mock(AuthCookieService.class);
		filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService, tokenService, cookieService);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private SysUserDetails authenticatedUser() {
		SysUserDetails details = mock(SysUserDetails.class);
		when(details.getUsername()).thenReturn("1:admin");
		when(details.getAuthorities()).thenReturn(Collections.emptyList());
		return details;
	}

	@Test
	@DisplayName("Authorization header token is preferred over the access cookie")
	void headerTokenIsPreferred() throws Exception {
		when(tokenProvider.extractTokenFromRequest(any())).thenReturn("header-jwt");
		when(tokenProvider.validateToken("header-jwt")).thenReturn(true);
		when(tokenService.isTokenRevoked("header-jwt")).thenReturn(false);
		when(tokenProvider.extractUsername("header-jwt")).thenReturn("1:admin");
		SysUserDetails details = authenticatedUser();
		when(userDetailsService.loadUserByUsername("1:admin")).thenReturn(details);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/anything");
		ProbeChain chain = new ProbeChain();
		filter.doFilter(request, new MockHttpServletResponse(), chain);

		verify(cookieService, never()).readAccessToken(any());
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(auth);
		assertEquals(details, auth.getPrincipal());
	}

	@Test
	@DisplayName("falls back to the secure_access cookie when no header token")
	void cookieFallbackAuthenticates() throws Exception {
		when(tokenProvider.extractTokenFromRequest(any())).thenReturn(null);
		when(cookieService.readAccessToken(any())).thenReturn("cookie-jwt");
		when(tokenProvider.validateToken("cookie-jwt")).thenReturn(true);
		when(tokenService.isTokenRevoked("cookie-jwt")).thenReturn(false);
		when(tokenProvider.extractUsername("cookie-jwt")).thenReturn("1:admin");
		SysUserDetails details = authenticatedUser();
		when(userDetailsService.loadUserByUsername("1:admin")).thenReturn(details);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/anything");
		ProbeChain chain = new ProbeChain();
		filter.doFilter(request, new MockHttpServletResponse(), chain);

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(auth);
		assertEquals(details, auth.getPrincipal());
		assertEquals(true, chain.invoked);
	}

	@Test
	@DisplayName("no token anywhere leaves the context unauthenticated")
	void noTokenLeavesContextEmpty() throws Exception {
		when(tokenProvider.extractTokenFromRequest(any())).thenReturn(null);
		when(cookieService.readAccessToken(any())).thenReturn(null);

		ProbeChain chain = new ProbeChain();
		filter.doFilter(new MockHttpServletRequest("GET", "/api/anything"), new MockHttpServletResponse(), chain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertEquals(true, chain.invoked);
	}

	@Test
	@DisplayName("revoked cookie token is rejected with 401 and the chain stops")
	void revokedCookieTokenRejects401() throws Exception {
		when(tokenProvider.extractTokenFromRequest(any())).thenReturn(null);
		when(cookieService.readAccessToken(any())).thenReturn("cookie-jwt");
		when(tokenProvider.validateToken("cookie-jwt")).thenReturn(true);
		when(tokenService.isTokenRevoked("cookie-jwt")).thenReturn(true);

		MockHttpServletResponse response = new MockHttpServletResponse();
		ProbeChain chain = new ProbeChain();
		filter.doFilter(new MockHttpServletRequest("GET", "/api/anything"), response, chain);

		assertEquals(401, response.getStatus());
		assertEquals(false, chain.invoked);
	}
}
