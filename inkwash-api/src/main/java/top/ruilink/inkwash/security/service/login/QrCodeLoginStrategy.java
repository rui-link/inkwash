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

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.api.param.LoginParam;
import top.ruilink.inkwash.security.api.param.QrLoginParam;
import top.ruilink.inkwash.security.service.CaptchaService;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.service.AccountService;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.UserService;

/**
 * QR code login strategy.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class QrCodeLoginStrategy extends AbstractLoginStrategy {

	private final CaptchaService captchaService;

	public QrCodeLoginStrategy(CaptchaService captchaService, AccountService accountService, UserService userService,
			IdentityService identityService) {
		super(userService, accountService, identityService);
		this.captchaService = captchaService;
	}

	@Override
	public boolean supports(LoginParam loginParam) {
		return loginParam instanceof QrLoginParam;
	}

	@Override
	public LoginResult authenticate(LoginParam loginParam) {
		QrLoginParam param = (QrLoginParam) loginParam;
		String qrCodeId = param.identity();

		String token = captchaService.getQrToken(qrCodeId);
		if (token != null) {
			Long userId = captchaService.getQrConfirmedUser(qrCodeId);
			if (userId != null) {
				requireEnabledUser(userId);
				SysAccount account = accountService.findByIdentityAndType(qrCodeId, AuthType.QR_CODE);
				if (account == null) {
					account = new SysAccount();
					account.setUserId(userId);
					account.setIdentity(qrCodeId);
					account.setAuthType(AuthType.QR_CODE);
					accountService.create(account);
				}
				return accountSuccess(account);
			}
		}
		throw new BusinessException("error.auth.qr_expired");
	}
}
