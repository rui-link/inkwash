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
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;

/**
 * Identity claim entity backing sys_identity.
 *
 * Records an identity verified through a trusted channel, such as a phone
 * number, email address or OIDC subject. It is kept separate from sys_account
 * login credentials, so one user may hold several verified identity claims
 * alongside several login credentials such as password, OAuth2 or QR code, all
 * pointing at the same user.
 * 
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class SysIdentity extends BaseEntity {
	private static final long serialVersionUID = 7291832405968712541L;

	/** Claim ID */
	private Long id;

	/** Associated user ID */
	private Long userId;

	/** Claim type */
	private IdentityType identityType;

	/** Claim value, such as a phone number, email address or OIDC subject */
	private String identityValue;

	/**
	 * Third party provider, used only by OIDC_SUB claims, such as github or google
	 */
	private String provider;

	/** Whether it is verified: 0 means unverified and 1 means verified */
	private Integer verified;

	/** Verification time */
	private LocalDateTime verifyTime;

	/** Verification source */
	private IdentityVerifier verifier;

	/** Status: 0 means disabled and 1 means enabled */
	private Integer status;

	/** Time of the most recent login through this claim */
	private LocalDateTime loginTime;

	/**
	 * Whether it has been verified through a trusted channel.
	 */
	public boolean identityVerified() {
		return verified != null && verified == 1;
	}

	/**
	 * Marks it verified and records the verification source.
	 */
	public void markVerified(IdentityVerifier verifier) {
		this.verified = 1;
		this.verifyTime = LocalDateTime.now();
		this.verifier = verifier;
		this.status = 1;
	}
}
