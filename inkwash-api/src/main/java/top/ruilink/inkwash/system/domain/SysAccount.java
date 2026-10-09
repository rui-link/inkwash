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
package top.ruilink.inkwash.system.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import top.ruilink.inkwash.base.domain.BaseEntity;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.system.domain.credential.Credential;
import top.ruilink.inkwash.system.domain.credential.OAuth2Credential;
import top.ruilink.inkwash.system.domain.credential.PasswordCredential;
import top.ruilink.inkwash.system.domain.credential.QRCredential;

/**
 * Account entity.
 * 
 * The credential field stores every kind of login credential as JSON:
 * AuthType.PASSWORD uses PasswordCredential(passwordHash), AuthType.OAUTH2 uses
 * OAuth2Credential(provider, openId, accessToken, refreshToken, tokenExpiresAt)
 * and AuthType.QR_CODE uses QrCredential(qrCodeId, qrStatus).
 * 
 * The verifyCredential and updateCredential methods use pattern matching to
 * dispatch on the actual credential type to the matching Credential record.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class SysAccount extends BaseEntity {
	private static final long serialVersionUID = -9019206235889153161L;

	/** Account ID */
	private Long id;

	/** Associated user ID */
	private Long userId;

	/** Login identity, such as a username, phone number or OpenID */
	private String identity;

	/** Login type */
	private AuthType authType;

	/**
	 * Login credential, JSON encoded and serialised through CredentialTypeHandler.
	 * Its exact shape is determined by AuthType.
	 */
	private Credential credential;

	/**
	 * Account status, stored as a string that each auth type interprets in a typed
	 * way
	 */
	private String status;

	/**
	 * Validity period.
	 */
	private Integer expiration;

	/** Last login time */
	private LocalDateTime loginTime;

	/**
	 * Verifies a credential, dispatching on the actual credential type.
	 */
	public boolean verifyCredential(String input) {
		return switch (credential) {
		case PasswordCredential p -> p.verify(input);
		case OAuth2Credential o -> o.accessToken() != null && o.accessToken().equals(input);
		case QRCredential q -> q.verify(input);
		case null, default -> false;
		};
	}

	/**
	 * Updates a credential, dispatching on the actual credential type.
	 */
	public void updateCredential(String newCredential) {
		switch (credential) {
		case PasswordCredential _ -> setCredential(new PasswordCredential(newCredential));
		case OAuth2Credential o -> setCredential(o.withTokens(newCredential, o.refreshToken(), o.expireTime()));
		case QRCredential q -> setCredential(q.withStatus(newCredential));
		case null -> throw new IllegalStateException("Cannot update null credential");
		default -> throw new IllegalArgumentException("Unknown credential type: " + credential.getClass());
		}
	}

	/**
	 * Records the last login time.
	 */
	public void recordLastLogin() {
		this.loginTime = LocalDateTime.now();
	}
}
