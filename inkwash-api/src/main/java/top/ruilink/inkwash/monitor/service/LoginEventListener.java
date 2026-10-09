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
package top.ruilink.inkwash.monitor.service;

import java.time.LocalDateTime;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.util.ClientInfoUtil;
import top.ruilink.inkwash.monitor.domain.LoginEvent;
import top.ruilink.inkwash.monitor.domain.LogoutEvent;
import top.ruilink.inkwash.monitor.domain.LoginInfo;
import top.ruilink.inkwash.monitor.enums.LoginStatus;

/**
 * Login event listener recording outcomes with client details.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class LoginEventListener {

	private final LoginInfoService loginInfoService;

	public LoginEventListener(LoginInfoService loginInfoService) {
		this.loginInfoService = loginInfoService;
	}

	@EventListener
	public void handleLoginEvent(LoginEvent event) {
		try {
			LoginInfo loginInfo = new LoginInfo();
			loginInfo.setUserId(event.getContext().userId());
			loginInfo.setIdentity(event.getContext().subject());
			loginInfo.setLoginType(event.getContext().authType());
			loginInfo.setStatus(event.success() ? LoginStatus.SUCCESS : LoginStatus.FAILED);
			loginInfo.setMessage(event.message());
			loginInfo.setLoginTime(LocalDateTime.now());
			// Client details: IP, browser, OS; falls back to the login device when IP
			// geolocation is unavailable
			String userAgent = event.getContext().userAgent();
			loginInfo.setAddress(event.getContext().ip());
			loginInfo.setBrowser(ClientInfoUtil.parseBrowser(userAgent));
			loginInfo.setOstype(ClientInfoUtil.parseOs(userAgent));
			loginInfo.setDevice(ClientInfoUtil.parseDevice(userAgent));
			loginInfoService.recordLoginInfo(loginInfo);
		} catch (Exception e) {
			log.warn("记录登录日志失败: {}", e.getMessage());
		}
	}

	/**
	 * Closes the session in {@code mon_login_info} when a logout is published.
	 *
	 * <p>
	 * Kept in this listener rather than a new class so that both halves of the
	 * authentication trail — login and logout — are written from one place,
	 * matching the single-table responsibility defined in the design document
	 * (§6.2.1).
	 */
	@EventListener
	public void handleLogoutEvent(LogoutEvent event) {
		try {
			int closed = loginInfoService.recordLogout(event.userId());
			if (closed == 0) {
				log.debug("登出时未找到未关闭的登录记录: userId={}", event.userId());
			}
		} catch (Exception e) {
			log.warn("记录登出日志失败: {}", e.getMessage());
		}
	}
}
