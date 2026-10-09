package top.ruilink.inkwash.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.security.adapter.SysUserDetails;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysUser;

/**
 * JWT generation, claim parsing, validation, and request extraction unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

	@Mock
	private JwtTokenConfig jwtConfig;

	@InjectMocks
	private JwtTokenProvider tokenProvider;

	private static final String SECRET = "aGVsbG8gd29ybGQgdGhpcyBpcyBhIHRlc3Qgc2VjcmV0IGZvciBqd3QgdG9rZW4gZ2VuZXJhdGlvbiBwdXJwb3Nlcy4uLi4=";
	private static final String TOKEN_PREFIX = "Bearer";
	private static final String HEADER = "Authorization";
	private static final String ISSUER = "inkwash-test";
	private static final long ACCESS_EXPIRE_TIME = 3600000L;
	private static final long REFRESH_EXPIRE_TIME = 86400000L;

	@BeforeEach
	void setUp() {
		lenient().when(jwtConfig.getSecret()).thenReturn(SECRET);
		lenient().when(jwtConfig.getTokenPrefix()).thenReturn(TOKEN_PREFIX);
		lenient().when(jwtConfig.getHeader()).thenReturn(HEADER);
		lenient().when(jwtConfig.getIssuer()).thenReturn(ISSUER);
		lenient().when(jwtConfig.getAccessExpireTime()).thenReturn(ACCESS_EXPIRE_TIME);
		lenient().when(jwtConfig.getRefreshExpireTime()).thenReturn(REFRESH_EXPIRE_TIME);
	}

	private SysUserDetails createUserDetails(Long userId, String identity, List<String> authorities) {
		SysUser user = new SysUser();
		user.setId(userId);
		SysAccount account = new SysAccount();
		account.setAuthType(AuthType.PASSWORD);
		account.setIdentity(identity);
		return new SysUserDetails(user, account, authorities.stream().map(SimpleGrantedAuthority::new).toList());
	}

	private SecretKey getSecretKey() {
		byte[] keyBytes = SECRET.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	private String generateToken(String subject, long expireOffset) {
		long currentTime = System.currentTimeMillis();
		return Jwts.builder().signWith(getSecretKey()).subject(subject).issuer(ISSUER).issuedAt(new Date(currentTime))
				.expiration(new Date(currentTime + expireOffset)).compact();
	}

	@Nested
	@DisplayName("Token Generation Tests")
	class TokenGenerationTests {

		@Test
		@DisplayName("Generate access token returns valid JWT with correct claims")
		void generateAccessToken_ReturnsValidJwt() {
			SysUserDetails userDetails = createUserDetails(1L, "testuser", List.of("ROLE_USER"));

			String token = tokenProvider.generateAccessToken(userDetails);

			assertNotNull(token);
			Claims claims = tokenProvider.parseToken(token);
			assertEquals(1L, claims.get("userId", Long.class));
			assertEquals("1:testuser", claims.getSubject());
			assertEquals(List.of("ROLE_USER"), claims.get("authorities", List.class));
		}

		@Test
		@DisplayName("Generate refresh token contains jti and fam claims")
		void generateRefreshToken_ContainsJtiAndFam() {
			String token = tokenProvider.generateRefreshToken("testuser", "jti-123", "fam-456");

			Claims claims = tokenProvider.parseToken(token);
			assertEquals("jti-123", claims.get("jti"));
			assertEquals("fam-456", claims.get("fam"));
			assertEquals("testuser", claims.getSubject());
		}
	}

	@Nested
	@DisplayName("Token Parsing Tests")
	class TokenParsingTests {

		@Test
		@DisplayName("Parse valid token returns claims")
		void parseToken_ValidToken_ReturnsClaims() {
			String token = generateToken("testuser", ACCESS_EXPIRE_TIME);

			Claims claims = tokenProvider.parseToken(token);

			assertNotNull(claims);
			assertEquals("testuser", claims.getSubject());
			assertEquals(ISSUER, claims.getIssuer());
		}
	}

	@Nested
	@DisplayName("Token Validation Tests")
	class TokenValidationTests {

		@Test
		@DisplayName("Validate valid token returns true")
		void validateToken_Valid_ReturnsTrue() {
			String token = generateToken("testuser", ACCESS_EXPIRE_TIME);

			assertTrue(tokenProvider.validateToken(token));
		}

		@Test
		@DisplayName("Validate expired token returns false")
		void validateToken_Expired_ReturnsFalse() {
			String token = generateToken("testuser", -100000);

			assertFalse(tokenProvider.validateToken(token));
		}

		@Test
		@DisplayName("Validate malformed token returns false")
		void validateToken_Malformed_ReturnsFalse() {
			assertFalse(tokenProvider.validateToken("invalid.jwt.string"));
		}

		@Test
		@DisplayName("Validate null token returns false")
		void validateToken_Null_ReturnsFalse() {
			assertFalse(tokenProvider.validateToken(null));
		}
	}

	@Nested
	@DisplayName("Token Extraction Tests")
	class TokenExtractionTests {

		@Test
		@DisplayName("Extract username returns subject")
		void extractUsername_ReturnsSubject() {
			String token = generateToken("testuser", ACCESS_EXPIRE_TIME);

			assertEquals("testuser", tokenProvider.extractUsername(token));
		}

		@Test
		@DisplayName("Extract userId returns userId claim")
		void extractUserId_ReturnsUserIdClaim() {
			long currentTime = System.currentTimeMillis();
			String token = Jwts.builder().signWith(getSecretKey()).claim("userId", 42L).subject("testuser")
					.issuer(ISSUER).issuedAt(new Date(currentTime))
					.expiration(new Date(currentTime + ACCESS_EXPIRE_TIME)).compact();

			assertEquals(42L, tokenProvider.extractUserId(token));
		}
	}

	@Nested
	@DisplayName("Request Extraction Tests")
	class RequestExtractionTests {

		@Test
		@DisplayName("Extract token from valid header")
		void extractTokenFromRequest_ValidHeader_ExtractsToken() {
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getHeader(HEADER)).thenReturn("Bearer some.jwt.token");

			String result = tokenProvider.extractTokenFromRequest(request);

			assertEquals("some.jwt.token", result);
		}

		@Test
		@DisplayName("Extract token with wrong prefix returns null")
		void extractTokenFromRequest_WrongPrefix_ReturnsNull() {
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getHeader(HEADER)).thenReturn("Basic some.token");

			assertNull(tokenProvider.extractTokenFromRequest(request));
		}

		@Test
		@DisplayName("Extract token with no header returns null")
		void extractTokenFromRequest_NoHeader_ReturnsNull() {
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getHeader(HEADER)).thenReturn(null);

			assertNull(tokenProvider.extractTokenFromRequest(request));
		}

		@Test
		@DisplayName("Extract token when exception thrown returns null")
		void extractTokenFromRequest_Exception_ReturnsNull() {
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getHeader(HEADER)).thenThrow(new RuntimeException("Header error"));

			assertNull(tokenProvider.extractTokenFromRequest(request));
		}
	}

	@Nested
	@DisplayName("Configuration Accessor Tests")
	class ConfigurationAccessorTests {

		@Test
		@DisplayName("Get token prefix returns config value")
		void getTokenPrefix_ReturnsConfigValue() {
			assertEquals(TOKEN_PREFIX, tokenProvider.getTokenPrefix());
		}

		@Test
		@DisplayName("Get access expire time returns config value")
		void getAccessExpireTime_ReturnsConfigValue() {
			assertEquals(ACCESS_EXPIRE_TIME, tokenProvider.getAccessExpireTime());
		}
	}
}
