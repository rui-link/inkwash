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

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.api.param.LoginParam;
import top.ruilink.inkwash.security.api.param.PasswordLoginParam;
import top.ruilink.inkwash.security.config.CredentialConfig;
import top.ruilink.inkwash.security.service.CaptchaService;
import top.ruilink.inkwash.security.service.LoginAttemptService;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.credential.PasswordCredential;
import top.ruilink.inkwash.system.service.AccountService;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.UserService;

/**
 * Password login strategy.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class PasswordLoginStrategy extends AbstractLoginStrategy {

	private final CaptchaService captchaService;
	private final LoginAttemptService loginAttemptService;
	private final CredentialConfig credentialConfig;

	public PasswordLoginStrategy(CaptchaService captchaService, LoginAttemptService loginAttemptService,
			AccountService accountService, UserService userService, IdentityService identityService,
			CredentialConfig credentialConfig) {
		super(userService, accountService, identityService);
		this.captchaService = captchaService;
		this.loginAttemptService = loginAttemptService;
		this.credentialConfig = credentialConfig;
	}

	@Override
	public boolean supports(LoginParam loginParam) {
		return loginParam instanceof PasswordLoginParam;
	}

	@Override
	public LoginResult authenticate(LoginParam loginParam) {
		PasswordLoginParam param = (PasswordLoginParam) loginParam;
		String username = param.identity();

		if (loginAttemptService.isLocked(username)) {
			throw new BusinessException("error.auth.account_locked");
		}

		int remainingAttempts = loginAttemptService.getRemainingAttempts(username);
		if (remainingAttempts <= credentialConfig.getPassword().getCaptchaTriggerTimes()) {
			if (!StringUtils.hasText(param.captchaId()) || !StringUtils.hasText(param.inputCaptcha())) {
				throw new BusinessException("error.auth.captcha_required");
			}
			boolean captchaVerified = captchaService.verifyCaptcha(param.captchaId(), param.inputCaptcha());
			if (!captchaVerified) {
				throw new BusinessException("error.auth.captcha_error");
			}
		}

		SysAccount account = accountService.findByIdentityAndType(username, AuthType.PASSWORD);
		if (account == null) {
			throw new BusinessException("error.account.not_found");
		}

		PasswordCredential cred = (PasswordCredential) account.getCredential();
		if (cred == null) {
			throw new BusinessException("error.account.no_password");
		}
		boolean verified = cred.verify(param.plainPassword());

		if (!verified) {
			loginAttemptService.recordLoginFailure(username);
			int remaining = loginAttemptService.getRemainingAttempts(username);
			if (remaining > 0) {
				throw new BusinessException("error.auth.password_error", remaining);
			} else {
				throw new BusinessException("error.auth.password_exhausted");
			}
		}

		return accountSuccess(account);
	}
}
