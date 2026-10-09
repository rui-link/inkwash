package top.ruilink.inkwash.base.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

/**
 * User agent and client IP parsing unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("客户端信息工具单元测试")
class ClientInfoUtilTest {

	private static final String UA_CHROME_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
			+ "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
	private static final String UA_EDGE_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
			+ "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";
	private static final String UA_FIREFOX_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
			+ "Gecko/20100101 Firefox/130.0";
	private static final String UA_SAFARI_IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) "
			+ "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1";
	private static final String UA_WECHAT_ANDROID = "Mozilla/5.0 (Linux; Android 13; Pixel 7) "
			+ "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 MicroMessenger/8.0.47";
	private static final String UA_SAFARI_IPAD = "Mozilla/5.0 (iPad; CPU OS 17_5 like Mac OS X) "
			+ "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1";

	@Nested
	@DisplayName("浏览器解析")
	class BrowserParseTests {

		@Test
		void chrome() {
			assertEquals("Chrome", ClientInfoUtil.parseBrowser(UA_CHROME_WINDOWS));
		}

		@Test
		void edge() {
			assertEquals("Edge", ClientInfoUtil.parseBrowser(UA_EDGE_WINDOWS));
		}

		@Test
		void firefox() {
			assertEquals("Firefox", ClientInfoUtil.parseBrowser(UA_FIREFOX_MAC));
		}

		@Test
		void safariIphone() {
			assertEquals("Safari", ClientInfoUtil.parseBrowser(UA_SAFARI_IPHONE));
		}

		@Test
		void wechat() {
			assertEquals("微信", ClientInfoUtil.parseBrowser(UA_WECHAT_ANDROID));
		}

		@Test
		void blankUserAgentReturnsEmpty() {
			assertEquals("", ClientInfoUtil.parseBrowser(null));
			assertEquals("", ClientInfoUtil.parseBrowser(" "));
		}
	}

	@Nested
	@DisplayName("操作系统解析")
	class OsParseTests {

		@Test
		void windows10() {
			assertEquals("Windows 10", ClientInfoUtil.parseOs(UA_CHROME_WINDOWS));
		}

		@Test
		void macOs() {
			assertEquals("macOS", ClientInfoUtil.parseOs(UA_FIREFOX_MAC));
		}

		@Test
		void android() {
			assertEquals("Android", ClientInfoUtil.parseOs(UA_WECHAT_ANDROID));
		}

		@Test
		void iosIphone() {
			assertEquals("iOS", ClientInfoUtil.parseOs(UA_SAFARI_IPHONE));
		}

		@Test
		void iosIpad() {
			assertEquals("iOS", ClientInfoUtil.parseOs(UA_SAFARI_IPAD));
		}

		@Test
		void blankUserAgentReturnsEmpty() {
			assertEquals("", ClientInfoUtil.parseOs(null));
			assertEquals("", ClientInfoUtil.parseOs(" "));
		}
	}

	@Nested
	@DisplayName("登录设备解析")
	class DeviceParseTests {

		@Test
		void windowsPc() {
			assertEquals("电脑端", ClientInfoUtil.parseDevice(UA_CHROME_WINDOWS));
		}

		@Test
		void macPc() {
			assertEquals("电脑端", ClientInfoUtil.parseDevice(UA_FIREFOX_MAC));
		}

		@Test
		void androidMobile() {
			assertEquals("安卓", ClientInfoUtil.parseDevice(UA_WECHAT_ANDROID));
		}

		@Test
		void iphone() {
			assertEquals("iphone", ClientInfoUtil.parseDevice(UA_SAFARI_IPHONE));
		}

		@Test
		void ipad() {
			assertEquals("平板", ClientInfoUtil.parseDevice(UA_SAFARI_IPAD));
		}

		@Test
		void blankUserAgentReturnsEmpty() {
			assertEquals("", ClientInfoUtil.parseDevice(null));
			assertEquals("", ClientInfoUtil.parseDevice(" "));
		}
	}

	@Nested
	@DisplayName("客户端IP解析")
	class IpParseTests {

		@AfterEach
		void resetTrustedProxies() {
			ClientInfoUtil.setTrustedProxies(Set.of());
		}

		@Test
		void noTrustedProxy_ignoresForwardedHeaders() {
			ClientInfoUtil.setTrustedProxies(Set.of());
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getRemoteAddr()).thenReturn("127.0.0.1");
			when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");
			assertEquals("127.0.0.1", ClientInfoUtil.getClientIp(request));
		}

		@Test
		void untrustedRemote_ignoresForwardedHeaders() {
			ClientInfoUtil.setTrustedProxies(Set.of("10.0.0.10"));
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getRemoteAddr()).thenReturn("203.0.113.9");
			when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.7");
			assertEquals("203.0.113.9", ClientInfoUtil.getClientIp(request));
		}

		@Test
		void trustedProxyExactHost_usesFirstXffIp() {
			ClientInfoUtil.setTrustedProxies(Set.of("10.0.0.10"));
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getRemoteAddr()).thenReturn("10.0.0.10");
			when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");
			assertEquals("203.0.113.5", ClientInfoUtil.getClientIp(request));
		}

		@Test
		void trustedProxyCidr_usesFirstXffIp() {
			ClientInfoUtil.setTrustedProxies(Set.of("10.0.0.0/8"));
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getRemoteAddr()).thenReturn("10.1.2.3");
			when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.7");
			assertEquals("198.51.100.7", ClientInfoUtil.getClientIp(request));
		}

		@Test
		void trustedProxy_fallsBackToProxyClientIp() {
			ClientInfoUtil.setTrustedProxies(Set.of("10.0.0.10"));
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getRemoteAddr()).thenReturn("10.0.0.10");
			when(request.getHeader("X-Forwarded-For")).thenReturn(null);
			when(request.getHeader("Proxy-Client-IP")).thenReturn("198.51.100.7");
			assertEquals("198.51.100.7", ClientInfoUtil.getClientIp(request));
		}

		@Test
		void trustedProxy_fallsBackToXRealIp() {
			ClientInfoUtil.setTrustedProxies(Set.of("10.0.0.10"));
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getRemoteAddr()).thenReturn("10.0.0.10");
			when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
			when(request.getHeader("Proxy-Client-IP")).thenReturn("unknown");
			when(request.getHeader("X-Real-IP")).thenReturn("192.0.2.9");
			assertEquals("192.0.2.9", ClientInfoUtil.getClientIp(request));
		}

		@Test
		void trustedProxy_allHeadersBlank_fallsBackToRemoteAddr() {
			ClientInfoUtil.setTrustedProxies(Set.of("10.0.0.10"));
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getRemoteAddr()).thenReturn("10.0.0.10");
			when(request.getHeader("X-Forwarded-For")).thenReturn(null);
			when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
			when(request.getHeader("X-Real-IP")).thenReturn(null);
			assertEquals("10.0.0.10", ClientInfoUtil.getClientIp(request));
		}

		@Test
		void nullRequestReturnsNull() {
			assertNull(ClientInfoUtil.getClientIp(null));
		}
	}
}
