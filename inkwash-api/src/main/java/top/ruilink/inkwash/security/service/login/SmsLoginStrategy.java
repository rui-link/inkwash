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
package top.ruilink.inkwash.security.service.login;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.api.param.LoginParam;
import top.ruilink.inkwash.security.api.param.SmsLoginParam;
import top.ruilink.inkwash.security.service.CaptchaService;
import top.ruilink.inkwash.security.service.LoginAttemptService;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.enums.UserStatus;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.UserService;

/**
 * SMS verification code login strategy.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class SmsLoginStrategy extends AbstractLoginStrategy {

	private final CaptchaService captchaService;
	private final LoginAttemptService loginAttemptService;
	private final boolean registerAudit;

	public SmsLoginStrategy(CaptchaService captchaService, LoginAttemptService loginAttemptService,
			UserService userService, IdentityService identityService,
			@Value("${auth.register.audit:true}") boolean registerAudit) {
		super(userService, identityService);
		this.captchaService = captchaService;
		this.loginAttemptService = loginAttemptService;
		this.registerAudit = registerAudit;
	}

	@Override
	public boolean supports(LoginParam loginParam) {
		return loginParam instanceof SmsLoginParam;
	}

	@Override
	public LoginResult authenticate(LoginParam loginParam) {
		SmsLoginParam param = (SmsLoginParam) loginParam;
		String phone = param.identity();

		if (loginAttemptService.isLocked(phone)) {
			throw new BusinessException("error.auth.account_locked");
		}

		if (!captchaService.verifySmsCode(phone, param.inputSmsCode())) {
			loginAttemptService.recordLoginFailure(phone);
			throw new BusinessException("error.auth.sms_code_error");
		}

		SysIdentity claim = identityService.findByTypeValue(IdentityType.PHONE, phone);
		Long userId;
		if (claim == null) {
			// First SMS login: create the user and a PHONE claim, with findOrCreateVerified
			// keeping it concurrency safe
			SysUser user = new SysUser();
			user.setNickname("用户_" + UUID.randomUUID().toString().substring(0, 8));
			user.setPhone(phone);
			user.setStatus(registerAudit ? UserStatus.PENDING : UserStatus.ENABLE);
			userService.createRawUser(user);
			userService.joinDefaultGroup(user.getId());
			claim = identityService.findOrCreateVerified(IdentityType.PHONE, phone, null, user.getId(),
					IdentityVerifier.SMS_CODE);
			userId = user.getId();
			if (!userId.equals(claim.getUserId())) {
				throw new BusinessException("error.identity.phone_occupied");
			}
		} else {
			userId = claim.getUserId();
			if (!claim.identityVerified()) {
				claim.markVerified(IdentityVerifier.SMS_CODE);
				identityService.update(claim);
			}
		}
		return claimSuccess(claim, AuthType.SMS_CODE);
	}
}
