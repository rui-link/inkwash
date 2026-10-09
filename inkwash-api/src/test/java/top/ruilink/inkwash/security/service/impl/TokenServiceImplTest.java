package top.ruilink.inkwash.security.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jsonwebtoken.Claims;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.base.service.CacheService;
import top.ruilink.inkwash.security.adapter.SysUserDetails;
import top.ruilink.inkwash.security.adapter.SysUserDetailsService;
import top.ruilink.inkwash.security.api.view.AccountView;
import top.ruilink.inkwash.security.jwt.JwtTokenConfig;
import top.ruilink.inkwash.security.jwt.JwtTokenProvider;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysUser;

/**
 * Token pair issuance, refresh rotation, reuse detection, and blacklist unit
 * tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private JwtTokenConfig jwtConfig;

	@Mock
	private SysUserDetailsService userDetailsService;

	@Mock
	private CacheService cacheService;

	@InjectMocks
	private TokenServiceImpl tokenServiceImpl;

	private Map<String, Object> cacheStore;

	private void setupCacheSimulation() {
		cacheStore = new HashMap<>();
		lenient().doAnswer(inv -> cacheStore.containsKey(inv.getArgument(0, String.class))).when(cacheService)
				.hasKey(anyString());
		lenient().doAnswer(inv -> {
			cacheStore.put(inv.getArgument(0, String.class), inv.getArgument(1));
			return null;
		}).when(cacheService).put(anyString(), any(), any(Duration.class));
		lenient().doAnswer(inv -> {
			cacheStore.remove(inv.getArgument(0, String.class));
			return null;
		}).when(cacheService).evict(anyString());
		lenient().when(jwtConfig.getAccessExpireTime()).thenReturn(86400000L);
	}

	private SysUserDetails createUserDetails(Long userId, String identity) {
		SysUser user = new SysUser();
		user.setId(userId);
		SysAccount account = new SysAccount();
		account.setAuthType(AuthType.PASSWORD);
		account.setIdentity(identity);
		return new SysUserDetails(user, account, List.of());
	}

	private Claims mockClaims(String subject, String jti, String fam) {
		Claims claims = mock(Claims.class);
		lenient().when(claims.getSubject()).thenReturn(subject);
		when(claims.get("jti", String.class)).thenReturn(jti);
		when(claims.get("fam", String.class)).thenReturn(fam);
		return claims;
	}

	@Nested
	@DisplayName("Generate Tokens Tests")
	class GenerateTokensTests {

		@Test
		@DisplayName("Generate tokens returns AccountView with correct fields")
		void generateTokens_ReturnsAccountView() {
			SysUserDetails userDetails = createUserDetails(1L, "testuser");
			when(userDetailsService.loadUserByUsername("1:testuser")).thenReturn(userDetails);
			when(jwtTokenProvider.generateAccessToken(userDetails)).thenReturn("access-token");
			when(jwtTokenProvider.generateRefreshToken(eq("1:testuser"), anyString(), anyString()))
					.thenReturn("refresh-token-123");
			lenient().when(jwtConfig.getAccessExpireTime()).thenReturn(86400000L);

			AccountView result = tokenServiceImpl.generateTokens(1L, "testuser", 1);

			assertEquals(1L, result.getUserId());
			assertEquals("access-token:refresh-token-123", result.getCredential());
			assertEquals("1:testuser", result.getIdentity());
			assertNotNull(result.getExpireTime());
		}
	}

	@Nested
	@DisplayName("Refresh Tokens Tests")
	class RefreshTokensTests {

		@Test
		@DisplayName("Refresh tokens succeeds with valid token")
		void refreshTokens_Success() {
			setupCacheSimulation();
			String refreshToken = "test-refresh-token";
			Claims claims = mockClaims("1:testuser", "jti-1", "fam-1");
			SysUserDetails userDetails = createUserDetails(1L, "testuser");

			when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
			when(jwtTokenProvider.parseToken(refreshToken)).thenReturn(claims);
			when(userDetailsService.loadUserByUsername("1:testuser")).thenReturn(userDetails);
			when(jwtTokenProvider.generateAccessToken(userDetails)).thenReturn("new-access");
			when(jwtTokenProvider.generateRefreshToken(eq("1:testuser"), anyString(), eq("fam-1")))
					.thenReturn("new-refresh");

			AccountView result = tokenServiceImpl.refreshTokens(refreshToken);

			assertEquals("new-access:new-refresh", result.getCredential());
		}

		@Test
		@DisplayName("Refresh with invalid token throws exception")
		void refreshTokens_InvalidToken_Throws() {
			String refreshToken = "invalid-token";
			when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(false);

			BusinessException ex = assertThrows(BusinessException.class,
					() -> tokenServiceImpl.refreshTokens(refreshToken));
			assertEquals("error.auth.refresh_invalid", ex.getMessageKey());
		}

		@Test
		@DisplayName("Refresh with missing JTI or FAM throws exception")
		void refreshTokens_MissingJtiOrFam_Throws() {
			String refreshToken = "test-refresh-token";
			Claims claims = mockClaims("1:testuser", null, "fam-1");

			when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
			when(jwtTokenProvider.parseToken(refreshToken)).thenReturn(claims);

			BusinessException ex = assertThrows(BusinessException.class,
					() -> tokenServiceImpl.refreshTokens(refreshToken));
			assertEquals("error.auth.refresh_malformed", ex.getMessageKey());
		}

		@Test
		@DisplayName("Refresh with revoked family throws exception")
		void refreshTokens_RevokedFamily_Throws() {
			setupCacheSimulation();
			String refreshToken = "test-refresh-token";
			Claims claims1 = mockClaims("1:testuser", "jti-1", "fam-1");
			Claims claims2 = mockClaims("1:testuser", "jti-1", "fam-1");
			Claims claims3 = mockClaims("1:testuser", "jti-2", "fam-1");
			SysUserDetails userDetails = createUserDetails(1L, "testuser");

			when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
			when(jwtTokenProvider.parseToken(refreshToken)).thenReturn(claims1, claims2, claims3);
			when(userDetailsService.loadUserByUsername("1:testuser")).thenReturn(userDetails);
			when(jwtTokenProvider.generateAccessToken(userDetails)).thenReturn("new-access");
			when(jwtTokenProvider.generateRefreshToken(eq("1:testuser"), anyString(), eq("fam-1")))
					.thenReturn("new-refresh");

			tokenServiceImpl.refreshTokens(refreshToken);

			BusinessException reuseEx = assertThrows(BusinessException.class,
					() -> tokenServiceImpl.refreshTokens(refreshToken));
			assertEquals("error.auth.token_reuse_detected", reuseEx.getMessageKey());

			BusinessException ex = assertThrows(BusinessException.class,
					() -> tokenServiceImpl.refreshTokens(refreshToken));
			assertEquals("error.auth.token_expired", ex.getMessageKey());
		}

		@Test
		@DisplayName("Refresh with reused JTI throws and revokes family")
		void refreshTokens_ReusedJti_Throws_And_RevokesFamily() {
			setupCacheSimulation();
			String refreshToken = "test-refresh-token";
			Claims claims1 = mockClaims("1:testuser", "jti-1", "fam-1");
			Claims claims2 = mockClaims("1:testuser", "jti-1", "fam-1");
			Claims claims3 = mockClaims("1:testuser", "jti-2", "fam-1");
			SysUserDetails userDetails = createUserDetails(1L, "testuser");

			when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
			when(jwtTokenProvider.parseToken(refreshToken)).thenReturn(claims1, claims2, claims3);
			when(userDetailsService.loadUserByUsername("1:testuser")).thenReturn(userDetails);
			when(jwtTokenProvider.generateAccessToken(userDetails)).thenReturn("new-access");
			when(jwtTokenProvider.generateRefreshToken(eq("1:testuser"), anyString(), eq("fam-1")))
					.thenReturn("new-refresh");

			AccountView result = tokenServiceImpl.refreshTokens(refreshToken);
			assertEquals("new-access:new-refresh", result.getCredential());

			BusinessException ex2 = assertThrows(BusinessException.class,
					() -> tokenServiceImpl.refreshTokens(refreshToken));
			assertEquals("error.auth.token_reuse_detected", ex2.getMessageKey());

			BusinessException ex3 = assertThrows(BusinessException.class,
					() -> tokenServiceImpl.refreshTokens(refreshToken));
			assertEquals("error.auth.token_expired", ex3.getMessageKey());
		}
	}

	@Nested
	@DisplayName("Revoke Token Tests")
	class RevokeTokenTests {

		@Test
		@DisplayName("Revoke null token does nothing")
		void revokeToken_Null_DoesNothing() {
			tokenServiceImpl.revokeToken(null);
		}

		@Test
		@DisplayName("Revoke valid token adds to blacklist")
		void revokeToken_Valid_AddsToBlacklist() {
			setupCacheSimulation();
			String token = "test-token";
			Claims claims = mock(Claims.class);
			when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 100000));

			when(jwtTokenProvider.extractUserId(token)).thenReturn(1L);
			when(jwtTokenProvider.parseToken(token)).thenReturn(claims);

			tokenServiceImpl.revokeToken(token);

			assertTrue(tokenServiceImpl.isTokenRevoked(token));
		}
	}

	@Nested
	@DisplayName("Is Token Revoked Tests")
	class IsTokenRevokedTests {

		@Test
		@DisplayName("Token not in blacklist returns false")
		void isTokenRevoked_NotInBlacklist_ReturnsFalse() {
			setupCacheSimulation();
			assertFalse(tokenServiceImpl.isTokenRevoked("unknown-token"));
		}

		@Test
		@DisplayName("Expired blacklisted token is auto-removed and returns false")
		void isTokenRevoked_Expired_RemovesAndReturnsFalse() {
			setupCacheSimulation();
			String token = "test-token";
			Claims claims = mock(Claims.class);
			when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 100000));

			when(jwtTokenProvider.extractUserId(token)).thenReturn(1L);
			when(jwtTokenProvider.parseToken(token)).thenReturn(claims);

			tokenServiceImpl.revokeToken(token);

			assertFalse(tokenServiceImpl.isTokenRevoked(token));
		}
	}

	@Nested
	@DisplayName("Extract UserId Tests")
	class ExtractUserIdTests {

		@Test
		@DisplayName("Extract userId delegates to provider")
		void extractUserId_DelegatesToProvider() {
			String token = "test-token";
			when(jwtTokenProvider.extractUserId(token)).thenReturn(42L);

			assertEquals(42L, tokenServiceImpl.extractUserId(token));
		}
	}
}
