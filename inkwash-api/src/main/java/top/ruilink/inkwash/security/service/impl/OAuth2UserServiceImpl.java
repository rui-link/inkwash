/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.security.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import top.ruilink.inkwash.base.exception.BusinessException;

/**
 * OAuth2 user info loading and attribute enrichment service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Component
public class OAuth2UserServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private static final Logger log = LoggerFactory.getLogger(OAuth2UserServiceImpl.class);

	private static final String GITHUB_EMAILS_URL = "https://api.github.com/user/emails";

	private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		try {
			log.debug("OAuth2 user info request for provider: {}", registrationId);
			return enrichAttributes(registrationId, delegate.loadUser(userRequest), userRequest);
		} catch (OAuth2AuthenticationException e) {
			log.error("OAuth2用户信息获取失败, provider={}: {}", registrationId, e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("OAuth2用户信息获取异常, provider={}", registrationId, e);
			throw new BusinessException("error.oauth2.provider_error");
		}
	}

	/**
	 * Adds the verified email and access and refresh tokens to the attributes for
	 * the login handler to use. Any failing extra request is only logged and never
	 * blocks the login.
	 */
	OAuth2User enrichAttributes(String registrationId, OAuth2User user, OAuth2UserRequest userRequest) {
		Map<String, Object> attributes = new HashMap<>();
		if (user.getAttributes() != null) {
			attributes.putAll(user.getAttributes());
		}

		String accessToken = userRequest.getAccessToken().getTokenValue();
		attributes.put("oauth_access_token", accessToken);
		Object refreshToken = userRequest.getAdditionalParameters().get("refresh_token");
		if (refreshToken != null) {
			attributes.put("oauth_refresh_token", refreshToken.toString());
		}

		if ("github".equalsIgnoreCase(registrationId)) {
			String verifiedEmail = fetchVerifiedEmail(accessToken);
			if (StringUtils.hasText(verifiedEmail)) {
				attributes.put("verified_email", verifiedEmail);
			}
		}

		String nameAttributeKey = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
				.getUserNameAttributeName();
		return new DefaultOAuth2User(user.getAuthorities() == null ? Collections.emptyList() : user.getAuthorities(),
				attributes, StringUtils.hasText(nameAttributeKey) ? nameAttributeKey : "sub");
	}

	/**
	 * Gets the verified primary GitHub email, which is protected for testing
	 * overrides.
	 */
	String fetchVerifiedEmail(String accessToken) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setAccept(List.of(MediaType.APPLICATION_JSON));
			ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(GITHUB_EMAILS_URL,
					HttpMethod.GET, new HttpEntity<>(headers),
					new ParameterizedTypeReference<List<Map<String, Object>>>() {
					});
			List<Map<String, Object>> emails = response.getBody();
			if (emails == null) {
				return null;
			}
			return emails.stream()
					.filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
					.map(e -> (String) e.get("email")).filter(Objects::nonNull).findFirst().orElse(null);
		} catch (Exception e) {
			log.warn("获取 GitHub 已验证邮箱失败: {}", e.getMessage());
			return null;
		}
	}
}
