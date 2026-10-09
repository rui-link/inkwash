package top.ruilink.inkwash.security.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * QR login origin allowlist unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@DisplayName("二维码登录 origin 白名单单元测试")
class QrLoginConfigTest {

	private QrLoginConfig config;

	@BeforeEach
	void setUp() {
		config = new QrLoginConfig();
		config.setWebUrl("http://localhost:3001/qr-login");
		config.setAllowedOrigins("http://192.168.1.10:3001, https://qr.example.com");
		config.setCorsAllowedOrigins("https://admin.example.com");
	}

	@Test
	@DisplayName("webUrl 自身来源默认在白名单")
	void webUrlBaseOriginAllowed() {
		assertTrue(config.isOriginAllowed("http://localhost:3001"));
		assertTrue(config.isOriginAllowed("http://localhost:3001/"));
	}

	@Test
	@DisplayName("allowedOrigins 与 CORS 白名单来源被允许")
	void configuredOriginsAllowed() {
		assertTrue(config.isOriginAllowed("http://192.168.1.10:3001"));
		assertTrue(config.isOriginAllowed("https://qr.example.com"));
		assertTrue(config.isOriginAllowed("https://admin.example.com"));
	}

	@Test
	@DisplayName("非白名单来源被拒绝")
	void outOfAllowlistRejected() {
		assertFalse(config.isOriginAllowed("https://evil.example.com"));
		assertFalse(config.isOriginAllowed("http://localhost:9999"));
		assertFalse(config.isOriginAllowed("http://localhost:3001.evil.example.com"));
	}

	@Test
	@DisplayName("空/异常来源被拒绝")
	void blankOrMalformedRejected() {
		assertFalse(config.isOriginAllowed(null));
		assertFalse(config.isOriginAllowed(""));
		assertFalse(config.isOriginAllowed("   "));
		assertFalse(config.isOriginAllowed("javascript:alert(1)"));
	}

	@Test
	@DisplayName("通配符不参与匹配")
	void wildcardNeverAllowed() {
		config.setAllowedOrigins("https://*.example.com");
		assertFalse(config.isOriginAllowed("https://a.example.com"));
	}

	@Test
	@DisplayName("来源携带路径不被视为同源")
	void originWithPathRejected() {
		assertFalse(config.isOriginAllowed("http://localhost:3001/foo"));
	}
}
