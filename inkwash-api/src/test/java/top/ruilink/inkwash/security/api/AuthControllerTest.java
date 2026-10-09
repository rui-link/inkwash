package top.ruilink.inkwash.security.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.Cookie;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.adapter.SysUserDetails;
import top.ruilink.inkwash.security.api.param.PasswordLoginParam;
import top.ruilink.inkwash.security.api.param.PasswordRegisterParam;
import top.ruilink.inkwash.security.api.param.QrTicketParam;
import top.ruilink.inkwash.security.api.param.RefreshTokenParam;
import top.ruilink.inkwash.security.api.param.SmsCodeParam;
import top.ruilink.inkwash.security.api.view.AccountView;
import top.ruilink.inkwash.security.api.view.CaptchaView;
import top.ruilink.inkwash.security.api.view.QrCodeStatusView;
import top.ruilink.inkwash.security.api.view.QrLoginResultView;
import top.ruilink.inkwash.security.api.view.RegisterResultView;
import top.ruilink.inkwash.security.config.AuthCookieConfig;
import top.ruilink.inkwash.security.jwt.AuthCookieService;
import top.ruilink.inkwash.security.jwt.JwtTokenConfig;
import top.ruilink.inkwash.security.jwt.JwtTokenProvider;
import top.ruilink.inkwash.security.service.AuthService;
import top.ruilink.inkwash.security.service.impl.QrTicketService;
import top.ruilink.inkwash.system.domain.SysUser;

/**
 * AuthController unit tests. Covers all endpoints after the H-FS-3 cookie-auth
 * contract: browser mode sets HttpOnly cookies with credential=null,
 * programmatic mode keeps the old contract.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	@Mock
	private AuthService authService;

	@Mock
	private JwtTokenProvider tokenProvider;

	private AuthCookieService cookieService;
	private QrTicketService qrTicketService;
	private AuthController authController;

	private CaptchaView testCaptcha;
	private AccountView testAccountView;

	@BeforeEach
	void setUp() {
		AuthCookieConfig properties = new AuthCookieConfig();
		JwtTokenConfig jwtConfig = new JwtTokenConfig();
		jwtConfig.setAccessExpireTime(86_400_000L);
		jwtConfig.setRefreshExpireTime(604_800_000L);
		cookieService = new AuthCookieService(properties, jwtConfig);
		qrTicketService = new QrTicketService();
		authController = new AuthController(authService, cookieService, qrTicketService, tokenProvider);

		testCaptcha = new CaptchaView();
		testCaptcha.setCaptchaId("test-captcha-id");
		testCaptcha.setCaptchaImage("data:image/png;base64,test");

		testAccountView = new AccountView();
		testAccountView.setId(1L);
		testAccountView.setUserId(1L);
		testAccountView.setIdentity("admin");
		testAccountView.setCredential("eyJhbGciOiJIUzUxMiJ9.access:eyJhbGciOiJIUzUxMiJ9.refresh");
		testAccountView.setExpireTime(LocalDateTime.now().plusHours(4));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private MockHttpServletRequest browserRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Requested-With", "XMLHttpRequest");
		return request;
	}

	private MockHttpServletRequest programmaticRequest() {
		return new MockHttpServletRequest();
	}

	@Nested
	@DisplayName("Captcha Tests")
	class CaptchaTests {

		@Test
		@DisplayName("get captcha success")
		void getCaptcha_Success() {
			when(authService.getCaptcha()).thenReturn(testCaptcha);

			ResponseEntity<CaptchaView> response = authController.getCaptcha();

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
			assertEquals("test-captcha-id", response.getBody().getCaptchaId());
		}
	}

	@Nested
	@DisplayName("SMS Code Tests")
	class SmsCodeTests {

		@Test
		@DisplayName("send sms code success")
		void sendSmsCode_Success() {
			SmsCodeParam param = new SmsCodeParam("13800138000", "test-id", "ABCD");

			ResponseEntity<Void> response = authController.sendSmsCode(param);

			assertEquals(200, response.getStatusCode().value());
		}
	}

	@Nested
	@DisplayName("Login Tests")
	class LoginTests {

		@Test
		@DisplayName("browser login sets HttpOnly cookies and nulls the body credential")
		void browserLogin_SetsCookiesAndHidesCredential() {
			var param = new PasswordLoginParam("admin", "welcome", "test-id", "ABCD", AuthType.PASSWORD);
			when(authService.login(any(PasswordLoginParam.class))).thenReturn(testAccountView);

			MockHttpServletResponse response = new MockHttpServletResponse();
			ResponseEntity<AccountView> result = authController.login(param, browserRequest(), response);

			assertEquals(200, result.getStatusCode().value());
			AccountView body = result.getBody();
			assertNotNull(body);
			assertNull(body.getCredential(), "browser mode must not expose tokens in the body");

			List<String> cookies = response.getHeaders("Set-Cookie");
			assertEquals(2, cookies.size());
			assertTrue(cookies.stream().anyMatch(c -> c.startsWith("secure_access=") && c.contains("access")));
			assertTrue(cookies.stream().anyMatch(c -> c.startsWith("secure_refresh=") && c.contains("refresh")));
			assertTrue(cookies.stream().allMatch(c -> c.contains("HttpOnly") && c.contains("SameSite=Lax")));
		}

		@Test
		@DisplayName("programmatic login keeps the credential contract and sets no cookies")
		void programmaticLogin_KeepsCredentialContract() {
			var param = new PasswordLoginParam("admin", "welcome", "test-id", "ABCD", AuthType.PASSWORD);
			when(authService.login(any(PasswordLoginParam.class))).thenReturn(testAccountView);

			MockHttpServletResponse response = new MockHttpServletResponse();
			ResponseEntity<AccountView> result = authController.login(param, programmaticRequest(), response);

			assertEquals(200, result.getStatusCode().value());
			assertNotNull(result.getBody().getCredential());
			assertEquals(0, response.getHeaders("Set-Cookie").size());
		}

		@Test
		@DisplayName("login credential contains access and refresh token")
		void login_ReturnsTokenPair() {
			var param = new PasswordLoginParam("admin", "welcome", "test-id", "ABCD", AuthType.PASSWORD);
			when(authService.login(any(PasswordLoginParam.class))).thenReturn(testAccountView);

			ResponseEntity<AccountView> response = authController.login(param, programmaticRequest(),
					new MockHttpServletResponse());

			String credential = response.getBody().getCredential();
			assertNotNull(credential);
			assertTrue(credential.contains(":"), "credential should contain ':' separator");
			String[] parts = credential.split(":");
			assertEquals(2, parts.length, "credential should have access:refresh format");
		}
	}

	@Nested
	@DisplayName("Register Tests")
	class RegisterTests {

		@Test
		@DisplayName("register by password success")
		void registerByPassword_Success() {
			PasswordRegisterParam param = new PasswordRegisterParam("newuser", "newuser", "password123", "captcha-id",
					"ABCD");
			RegisterResultView result = new RegisterResultView(2L, 4, "注册成功，等待审核");

			when(authService.registerByPassword(param)).thenReturn(result);

			ResponseEntity<RegisterResultView> response = authController.registerByPassword(param);

			assertEquals(200, response.getStatusCode().value());
			assertEquals(4, response.getBody().getStatus());
		}

		@Test
		@DisplayName("register by password failure propagates exception")
		void registerByPassword_Failure_PropagatesException() {
			PasswordRegisterParam param = new PasswordRegisterParam("duplicate", null, "pass", "captcha-id", "ABCD");

			doThrow(new RuntimeException("Username already exists")).when(authService)
					.registerByPassword(any(PasswordRegisterParam.class));

			assertThrows(RuntimeException.class, () -> authController.registerByPassword(param));
		}
	}

	@Nested
	@DisplayName("Token Refresh Tests")
	class TokenRefreshTests {

		@Test
		@DisplayName("programmatic refresh with body token keeps the contract")
		void refreshToken_Programmatic_FromBody() {
			String refreshToken = "eyJhbGciOiJIUzUxMiJ9.refresh";
			when(authService.refreshToken(refreshToken)).thenReturn(testAccountView);

			MockHttpServletResponse response = new MockHttpServletResponse();
			ResponseEntity<AccountView> result = authController.refreshToken(new RefreshTokenParam(refreshToken),
					programmaticRequest(), response);

			assertEquals(200, result.getStatusCode().value());
			assertNotNull(result.getBody().getCredential());
			assertEquals(0, response.getHeaders("Set-Cookie").size());
		}

		@Test
		@DisplayName("browser refresh reads the refresh cookie and rotates via Set-Cookie")
		void refreshToken_Browser_ReadsCookie() {
			String refreshToken = "cookie-refresh-jwt";
			when(authService.refreshToken(refreshToken)).thenReturn(testAccountView);

			MockHttpServletRequest request = browserRequest();
			request.setCookies(new Cookie("secure_refresh", refreshToken));
			MockHttpServletResponse response = new MockHttpServletResponse();

			ResponseEntity<AccountView> result = authController.refreshToken(null, request, response);

			assertEquals(200, result.getStatusCode().value());
			assertNull(result.getBody().getCredential(), "browser refresh must not expose tokens");
			assertEquals(2, response.getHeaders("Set-Cookie").size());
		}
	}

	@Nested
	@DisplayName("Me Tests")
	class MeTests {

		@Test
		@DisplayName("me returns the current account view")
		void me_ReturnsIdentity() {
			SysUser user = new SysUser();
			user.setId(1L);
			SysUserDetails details = new SysUserDetails(user, AuthType.PASSWORD, "admin", null, List.of());
			SecurityContext context = new SecurityContextImpl();
			context.setAuthentication(new UsernamePasswordAuthenticationToken(details, null, List.of()));
			SecurityContextHolder.setContext(context);

			ResponseEntity<AccountView> response = authController.me();

			assertEquals(200, response.getStatusCode().value());
			assertEquals(1L, response.getBody().getUserId());
			assertEquals("admin", response.getBody().getIdentity());
			assertEquals(AuthType.PASSWORD, response.getBody().getAuthType());
		}
	}

	@Nested
	@DisplayName("QR Code Tests")
	class QrCodeTests {

		@Test
		@DisplayName("generate qr code success")
		void generateQrCode_Success() {
			when(authService.generateQrCode(eq("web"), any())).thenReturn(testCaptcha);

			ResponseEntity<CaptchaView> response = authController.generateQrCode("web", null);

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
		}

		@Test
		@DisplayName("generate qr code for admin client")
		void generateQrCode_AdminClient() {
			when(authService.generateQrCode(eq("admin"), any())).thenReturn(testCaptcha);

			ResponseEntity<CaptchaView> response = authController.generateQrCode("admin", "http://192.168.1.5:5173");

			assertEquals(200, response.getStatusCode().value());
		}

		@Test
		@DisplayName("qr confirmed returns a single-use ticket instead of a token")
		void checkQrCode_Confirmed_ReturnsTicket() {
			String qrCodeId = "qr-123";
			when(authService.getQrToken(qrCodeId)).thenReturn("access:refresh");
			when(authService.verifyQrSseToken(qrCodeId, "sse-abc")).thenReturn(true);

			ResponseEntity<QrCodeStatusView> response = authController.checkQrCodeStatus(qrCodeId, "sse-abc");

			assertEquals(200, response.getStatusCode().value());
			assertEquals("CONFIRMED", response.getBody().status());
			assertNotNull(response.getBody().ticket());
			assertNotEquals("access:refresh", response.getBody().ticket(),
					"the credential itself must never be returned");
		}

		@Test
		@DisplayName("qr confirmed does not leak a ticket when sseToken is missing")
		void checkQrCode_Confirmed_WithoutSseToken_NoTicketLeak() {
			String qrCodeId = "qr-123";
			when(authService.getQrToken(qrCodeId)).thenReturn("access:refresh");
			when(authService.verifyQrSseToken(qrCodeId, null)).thenReturn(false);
			when(authService.isQrCodeValid(qrCodeId)).thenReturn(true);

			ResponseEntity<QrCodeStatusView> response = authController.checkQrCodeStatus(qrCodeId, null);

			assertNull(response.getBody().ticket());
		}

		@Test
		@DisplayName("qr confirmed does not leak a ticket when sseToken is wrong")
		void checkQrCode_Confirmed_WithWrongSseToken_NoTicketLeak() {
			String qrCodeId = "qr-123";
			when(authService.getQrToken(qrCodeId)).thenReturn("access:refresh");
			when(authService.verifyQrSseToken(qrCodeId, "wrong")).thenReturn(false);
			when(authService.isQrCodeValid(qrCodeId)).thenReturn(true);

			ResponseEntity<QrCodeStatusView> response = authController.checkQrCodeStatus(qrCodeId, "wrong");

			assertNull(response.getBody().ticket());
		}

		@Test
		@DisplayName("qr scanned but not confirmed")
		void checkQrCode_Scanned() {
			String qrCodeId = "qr-123";
			when(authService.getQrToken(qrCodeId)).thenReturn(null);
			when(authService.isQrCodeValid(qrCodeId)).thenReturn(true);
			when(authService.getScannedInfo(qrCodeId)).thenReturn(QrCodeStatusView.scanned("tester", "138****8000"));

			ResponseEntity<QrCodeStatusView> response = authController.checkQrCodeStatus(qrCodeId, "sse-abc");

			assertEquals("SCANNED", response.getBody().status());
			assertEquals("tester", response.getBody().nickname());
			assertEquals("138****8000", response.getBody().phone());
		}

		@Test
		@DisplayName("qr valid but waiting for scan")
		void checkQrCode_Pending() {
			String qrCodeId = "qr-pending";
			when(authService.getQrToken(qrCodeId)).thenReturn(null);
			when(authService.isQrCodeValid(qrCodeId)).thenReturn(true);

			ResponseEntity<QrCodeStatusView> response = authController.checkQrCodeStatus(qrCodeId, "sse-abc");

			assertEquals("PENDING", response.getBody().status());
		}

		@Test
		@DisplayName("qr expired")
		void checkQrCode_Expired() {
			String qrCodeId = "qr-expired";
			when(authService.getQrToken(qrCodeId)).thenReturn(null);
			when(authService.isQrCodeValid(qrCodeId)).thenReturn(false);

			ResponseEntity<QrCodeStatusView> response = authController.checkQrCodeStatus(qrCodeId, "sse-abc");

			assertEquals("EXPIRED", response.getBody().status());
		}

		@Test
		@DisplayName("qr/login exchanges a valid ticket for HttpOnly cookies with no credential")
		void qrLogin_ExchangeSetsCookies() {
			String ticket = qrTicketService.issue("qr-123", "access:refresh");
			MockHttpServletResponse response = new MockHttpServletResponse();

			ResponseEntity<QrLoginResultView> result = authController.qrLogin(new QrTicketParam(ticket), response);

			assertEquals(200, result.getStatusCode().value());
			assertTrue(result.getBody().success());
			List<String> cookies = response.getHeaders("Set-Cookie");
			assertEquals(2, cookies.size());
			assertTrue(cookies.stream().anyMatch(c -> c.startsWith("secure_access=") && c.contains("access")));
		}

		@Test
		@DisplayName("qr/login rejects a replayed ticket with 400")
		void qrLogin_ReplayRejected() {
			String ticket = qrTicketService.issue("qr-123", "access:refresh");
			authController.qrLogin(new QrTicketParam(ticket), new MockHttpServletResponse());
			MockHttpServletResponse response = new MockHttpServletResponse();

			assertThrows(BusinessException.class, () -> authController.qrLogin(new QrTicketParam(ticket), response));
			assertEquals(0, response.getHeaders("Set-Cookie").size());
		}

		@Test
		@DisplayName("qr/login rejects an unknown ticket with 400")
		void qrLogin_UnknownTicketRejected() {
			assertThrows(BusinessException.class,
					() -> authController.qrLogin(new QrTicketParam("no-such-ticket"), new MockHttpServletResponse()));
		}

		@Test
		@DisplayName("confirm qr login success")
		void confirmQrCode_Success() {
			ResponseEntity<Void> response = authController.confirmQrCode("qr-123");

			assertEquals(200, response.getStatusCode().value());
		}
	}

	@Nested
	@DisplayName("Logout Tests")
	class LogoutTests {

		@Test
		@DisplayName("browser logout clears the auth cookies")
		void logout_Browser_ClearsCookies() {
			MockHttpServletResponse response = new MockHttpServletResponse();

			ResponseEntity<Void> result = authController.logout(browserRequest(), response);

			assertEquals(200, result.getStatusCode().value());
			List<String> cookies = response.getHeaders("Set-Cookie");
			assertEquals(2, cookies.size());
			assertTrue(cookies.stream().allMatch(c -> c.contains("Max-Age=0")));
		}

		@Test
		@DisplayName("programmatic logout just revokes without touching cookies")
		void logout_Programmatic_NoCookies() {
			MockHttpServletResponse response = new MockHttpServletResponse();

			ResponseEntity<Void> result = authController.logout(programmaticRequest(), response);

			assertEquals(200, result.getStatusCode().value());
			assertEquals(0, response.getHeaders("Set-Cookie").size());
		}
	}

	@Nested
	@DisplayName("QR SSE Tests")
	class QrSseTests {

		@Test
		@DisplayName("qr sse returns the emitter from the service")
		void qrSse_ReturnsServiceEmitter() {
			SseEmitter emitter = new SseEmitter();
			when(authService.subscribeQrCode("qr-123", "sse-abc")).thenReturn(emitter);

			SseEmitter result = authController.qrSse("qr-123", "sse-abc");

			assertEquals(emitter, result);
		}

		@Test
		@DisplayName("confirm qr publishes the sse event after confirming")
		void confirmQrCode_PublishesAfterConfirm() {
			authController.confirmQrCode("qr-123");

			InOrder inOrder = inOrder(authService);
			inOrder.verify(authService).confirmQrCode("qr-123");
			inOrder.verify(authService).publishQrConfirmed("qr-123");
		}
	}

	@Nested
	@DisplayName("OAuth2 Callback Tests")
	class OAuth2CallbackTests {

		@Test
		@DisplayName("valid token sets cookies and redirects without leaking the token in the body")
		void validToken_SetsCookiesAndRedirects() throws Exception {
			when(tokenProvider.validateToken("access-jwt")).thenReturn(true);
			MockHttpServletResponse response = new MockHttpServletResponse();

			authController.oauth2Callback("access-jwt:refresh-jwt", response);

			assertEquals(302, response.getStatus());
			assertEquals("/oauth2-callback.html", response.getRedirectedUrl());
			List<String> cookies = response.getHeaders("Set-Cookie");
			assertEquals(2, cookies.size());
			assertTrue(cookies.stream().anyMatch(c -> c.startsWith("secure_access=") && c.contains("access-jwt")));
			String body = response.getContentAsString();
			assertTrue(body == null || !body.contains("access-jwt"), "response body must never contain a token");
		}

		@Test
		@DisplayName("invalid access token redirects to the error page without cookies")
		void invalidToken_ErrorPage_NoCookies() throws Exception {
			when(tokenProvider.validateToken("garbage")).thenReturn(false);
			MockHttpServletResponse response = new MockHttpServletResponse();

			authController.oauth2Callback("garbage:refresh", response);

			assertEquals(302, response.getStatus());
			assertEquals("/oauth2-error.html", response.getRedirectedUrl());
			assertEquals(0, response.getHeaders("Set-Cookie").size());
		}

		@Test
		@DisplayName("missing or malformed token redirects to the error page without cookies")
		void missingToken_ErrorPage_NoCookies() throws Exception {
			MockHttpServletResponse response = new MockHttpServletResponse();

			authController.oauth2Callback(null, response);
			assertEquals("/oauth2-error.html", response.getRedirectedUrl());
			assertEquals(0, response.getHeaders("Set-Cookie").size());

			MockHttpServletResponse response2 = new MockHttpServletResponse();
			authController.oauth2Callback("only-access", response2);
			assertEquals("/oauth2-error.html", response2.getRedirectedUrl());
			assertEquals(0, response2.getHeaders("Set-Cookie").size());
		}
	}
}