package top.ruilink.inkwash.security.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.util.ClientInfoUtil;
import top.ruilink.inkwash.monitor.domain.LoginEvent;
import top.ruilink.inkwash.security.api.view.AccountView;
import top.ruilink.inkwash.security.service.OAuth2LoginService;
import top.ruilink.inkwash.security.service.TokenService;
import top.ruilink.inkwash.security.service.login.LoginResult;

/**
 * OAuth2 login success token issuance, redirect, and login event unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2登录成功处理器单元测试")
class OAuth2LoginSuccessHandlerTest {

	@Mock
	private OAuth2LoginService oauth2LoginService;

	@Mock
	private TokenService tokenService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@InjectMocks
	private OAuth2LoginSuccessHandler successHandler;

	@Captor
	private ArgumentCaptor<LoginEvent> loginEventCaptor;

	private AccountView accountView;

	@BeforeEach
	void setUp() throws Exception {
		accountView = new AccountView();
		accountView.setId(1L);
		accountView.setUserId(1L);
		accountView.setIdentity("github:12345");
		accountView.setCredential("access-token-value:refresh-token-value");
		accountView.setExpireTime(LocalDateTime.now().plusHours(8));

		lenient().when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
	}

	private OAuth2User githubUser(Map<String, Object> extraAttributes) {
		Map<String, Object> attributes = new java.util.HashMap<>();
		attributes.put("id", 12345);
		attributes.put("login", "github-user");
		attributes.put("name", "GitHub User");
		attributes.put("email", "github@example.com");
		attributes.putAll(extraAttributes);
		return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
	}

	@Nested
	@DisplayName("OAuth2登录成功测试")
	class OAuth2LoginSuccessTests {

		@AfterEach
		void resetTrustedProxies() {
			ClientInfoUtil.setTrustedProxies(Set.of());
		}

		@Test
		@DisplayName("调用OAuth2LoginService并生成令牌")
		void testOAuth2Login_DelegatesToServiceAndGeneratesToken() throws Exception {
			when(oauth2LoginService.login("github", "12345", "GitHub User", "github@example.com", "oauth-access-1",
					"oauth-refresh-1")).thenReturn(new LoginResult(1L, "github:12345", AuthType.OAUTH2));
			when(tokenService.generateTokens(1L, "github:12345", AuthType.OAUTH2.getCode())).thenReturn(accountView);

			OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(
					githubUser(Map.of("verified_email", "github@example.com", "oauth_access_token", "oauth-access-1",
							"oauth_refresh_token", "oauth-refresh-1")),
					List.of(new SimpleGrantedAuthority("ROLE_USER")), "github");

			successHandler.onAuthenticationSuccess(request, response, authToken);

			verify(oauth2LoginService).login("github", "12345", "GitHub User", "github@example.com", "oauth-access-1",
					"oauth-refresh-1");
			verify(tokenService).generateTokens(1L, "github:12345", AuthType.OAUTH2.getCode());
			verify(response).sendRedirect(argThat(
					url -> url.startsWith("/api/auth/oauth2/token?token=") && url.contains("access-token-value")));
		}

		@Test
		@DisplayName("无verified_email与token时传入null")
		void testOAuth2Login_NoEmailOrToken_PassesNull() throws Exception {
			when(oauth2LoginService.login("github", "12345", "GitHub User", null, null, null))
					.thenReturn(new LoginResult(1L, "github:12345", AuthType.OAUTH2));
			when(tokenService.generateTokens(1L, "github:12345", AuthType.OAUTH2.getCode())).thenReturn(accountView);

			OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(githubUser(Map.of()),
					List.of(new SimpleGrantedAuthority("ROLE_USER")), "github");

			successHandler.onAuthenticationSuccess(request, response, authToken);

			verify(oauth2LoginService).login("github", "12345", "GitHub User", null, null, null);
		}

		@Test
		@DisplayName("登录成功后发布LoginEvent")
		void testOAuth2Login_PublishesLoginEvent() throws Exception {
			when(oauth2LoginService.login("github", "12345", "GitHub User", null, null, null))
					.thenReturn(new LoginResult(1L, "github:12345", AuthType.OAUTH2));
			when(tokenService.generateTokens(1L, "github:12345", AuthType.OAUTH2.getCode())).thenReturn(accountView);
			ClientInfoUtil.setTrustedProxies(Set.of("10.0.0.10"));
			when(request.getRemoteAddr()).thenReturn("10.0.0.10");
			when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");
			when(request.getHeader("User-Agent")).thenReturn(
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

			OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(githubUser(Map.of()),
					List.of(new SimpleGrantedAuthority("ROLE_USER")), "github");

			successHandler.onAuthenticationSuccess(request, response, authToken);

			verify(eventPublisher).publishEvent(loginEventCaptor.capture());
			LoginEvent event = loginEventCaptor.getValue();
			assertEquals(1L, event.getContext().userId());
			assertEquals("github:12345", event.getContext().subject());
			assertEquals(AuthType.OAUTH2, event.getAuthType());
			assertTrue(event.success());
			assertEquals(authToken.getPrincipal(), event.principal());
			assertEquals("203.0.113.5", event.getContext().ip());
			assertTrue(event.getContext().userAgent().contains("Chrome"));
		}

		@Test
		@DisplayName("Google用户从sub字段提取openId")
		void testGoogleOAuth2_ExtractsSubAsOpenId() throws Exception {
			when(oauth2LoginService.login("google", "google-user-id-123", "Google User", null, null, null))
					.thenReturn(new LoginResult(1L, "google:google-user-id-123", AuthType.OAUTH2));
			when(tokenService.generateTokens(1L, "google:google-user-id-123", AuthType.OAUTH2.getCode()))
					.thenReturn(accountView);

			OAuth2User googleUser = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),
					Map.of("sub", "google-user-id-123", "name", "Google User", "email", "google@example.com"), "sub");
			OAuth2AuthenticationToken googleToken = new OAuth2AuthenticationToken(googleUser,
					googleUser.getAuthorities(), "google");

			successHandler.onAuthenticationSuccess(request, response, googleToken);

			verify(oauth2LoginService).login("google", "google-user-id-123", "Google User", null, null, null);
		}

		@Test
		@DisplayName("name缺失时以registrationId作为昵称")
		void testOAuth2Login_MissingName_UsesRegistrationId() throws Exception {
			when(oauth2LoginService.login("github", "12345", "github", null, null, null))
					.thenReturn(new LoginResult(1L, "github:12345", AuthType.OAUTH2));
			when(tokenService.generateTokens(1L, "github:12345", AuthType.OAUTH2.getCode())).thenReturn(accountView);

			OAuth2User userWithoutName = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),
					Map.of("id", 12345, "login", "github-user"), "id");
			OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(userWithoutName,
					userWithoutName.getAuthorities(), "github");

			successHandler.onAuthenticationSuccess(request, response, authToken);

			verify(oauth2LoginService).login("github", "12345", "github", null, null, null);
		}
	}

	@Nested
	@DisplayName("异常处理测试")
	class ExceptionHandlingTests {

		@Test
		@DisplayName("非OAuth2AuthenticationToken类型跳转到错误页")
		void testInvalidAuthType_RedirectsToError() throws Exception {
			var mockAuth = new TestingAuthenticationToken("user", "password", List.of());

			successHandler.onAuthenticationSuccess(request, response, mockAuth);

			verify(response).sendRedirect("/login?error=invalid_oauth");
		}
	}
}
