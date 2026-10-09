package top.ruilink.inkwash.security.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * OAuth2 user attribute enrichment with tokens and verified email unit tests.
 *
 * @author Dyllon
 * @since 0.5.1
 */
class OAuth2UserServiceImplTest {

	private static class StubOAuth2UserServiceImpl extends OAuth2UserServiceImpl {
		private final String verifiedEmail;

		StubOAuth2UserServiceImpl(String verifiedEmail) {
			this.verifiedEmail = verifiedEmail;
		}

		@Override
		String fetchVerifiedEmail(String accessToken) {
			return verifiedEmail;
		}
	}

	private ClientRegistration registration(String registrationId, String nameAttributeKey) {
		return ClientRegistration.withRegistrationId(registrationId).clientId("client-id").clientSecret("client-secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("http://localhost:9090/login/oauth2/code/" + registrationId).scope("read:user")
				.authorizationUri("https://example.com/oauth/authorize").tokenUri("https://example.com/oauth/token")
				.userInfoUri("https://example.com/userinfo").userNameAttributeName(nameAttributeKey)
				.clientName(registrationId).build();
	}

	private OAuth2AccessToken accessToken() {
		return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token-123", Instant.now(),
				Instant.now().plusSeconds(3600));
	}

	private OAuth2User oauth2User(String nameAttributeKey, String nameValue) {
		return new DefaultOAuth2User(Set.of(), Map.of(nameAttributeKey, nameValue, "name", "Test User"),
				nameAttributeKey);
	}

	@Test
	@DisplayName("非github提供商补充token到attributes")
	void enrichAttributes_NonGithub_AddsTokens() {
		// Given
		ClientRegistration registration = registration("google", "sub");
		OAuth2UserRequest userRequest = new OAuth2UserRequest(registration, accessToken(),
				Map.of("refresh_token", "refresh-token-456"));
		StubOAuth2UserServiceImpl service = new StubOAuth2UserServiceImpl(null);

		// When
		OAuth2User result = service.enrichAttributes("google", oauth2User("sub", "google-sub-1"), userRequest);

		// Then
		assertNotNull(result);
		assertEquals("access-token-123", result.getAttributes().get("oauth_access_token"));
		assertEquals("refresh-token-456", result.getAttributes().get("oauth_refresh_token"));
		assertFalse(result.getAttributes().containsKey("verified_email"));
		assertEquals("google-sub-1", result.getName());
	}

	@Test
	@DisplayName("github提供商补充已验证邮箱")
	void enrichAttributes_Github_AddsVerifiedEmail() {
		// Given
		ClientRegistration registration = registration("github", "login");
		OAuth2UserRequest userRequest = new OAuth2UserRequest(registration, accessToken());
		StubOAuth2UserServiceImpl service = new StubOAuth2UserServiceImpl("github@example.com");

		// When
		OAuth2User result = service.enrichAttributes("github", oauth2User("login", "github-user-1"), userRequest);

		// Then
		assertNotNull(result);
		assertEquals("github@example.com", result.getAttributes().get("verified_email"));
		assertEquals("access-token-123", result.getAttributes().get("oauth_access_token"));
		assertFalse(result.getAttributes().containsKey("oauth_refresh_token"));
		assertEquals("github-user-1", result.getName());
	}

	@Test
	@DisplayName("github邮箱获取失败时不阻断登录")
	void enrichAttributes_Github_FetchFails_StillReturnsUser() {
		// Given
		ClientRegistration registration = registration("github", "login");
		OAuth2UserRequest userRequest = new OAuth2UserRequest(registration, accessToken());
		StubOAuth2UserServiceImpl service = new StubOAuth2UserServiceImpl(null);

		// When
		OAuth2User result = service.enrichAttributes("github", oauth2User("login", "github-user-2"), userRequest);

		// Then
		assertNotNull(result);
		assertNull(result.getAttributes().get("verified_email"));
		assertEquals("access-token-123", result.getAttributes().get("oauth_access_token"));
	}

	@Test
	@DisplayName("nameAttributeKey为空时使用sub兜底")
	void enrichAttributes_NoNameAttributeKey_FallsBackToSub() {
		// Given
		ClientRegistration registration = registration("apple", "");
		OAuth2UserRequest userRequest = new OAuth2UserRequest(registration, accessToken());
		StubOAuth2UserServiceImpl service = new StubOAuth2UserServiceImpl(null);

		// When
		OAuth2User result = service.enrichAttributes("apple", oauth2User("sub", "apple-sub-1"), userRequest);

		// Then
		assertNotNull(result);
		assertEquals("apple-sub-1", result.getName());
	}
}
