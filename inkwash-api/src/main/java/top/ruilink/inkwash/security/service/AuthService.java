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
package top.ruilink.inkwash.security.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import top.ruilink.inkwash.security.api.param.LoginParam;
import top.ruilink.inkwash.security.api.param.PasswordRegisterParam;
import top.ruilink.inkwash.security.api.param.SmsRegisterParam;
import top.ruilink.inkwash.security.api.param.SmsCodeParam;
import top.ruilink.inkwash.security.api.view.AccountView;
import top.ruilink.inkwash.security.api.view.CaptchaView;
import top.ruilink.inkwash.security.api.view.QrCodeStatusView;
import top.ruilink.inkwash.security.api.view.RegisterResultView;

/**
 * Authentication service interface, an application layer port.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface AuthService {
	CaptchaView getCaptcha();

	void sendSmsCode(SmsCodeParam smsCodeParam);

	AccountView login(LoginParam loginParam);

	AccountView refreshToken(String refreshToken);

	void logout(HttpServletRequest request);

	CaptchaView generateQrCode(String client, String origin);

	boolean checkQrCodeStatus(String qrCodeId);

	boolean isQrCodeValid(String qrCodeId);

	void confirmQrCode(String qrCodeId);

	String getQrToken(String qrCodeId);

	/**
	 * Reports a scan action from the mobile QR confirmation page, records the
	 * scanning user and returns their identity information.
	 * 
	 * @param qrCodeId the QR code ID
	 * @return the SCANNED state with the scanning user's nickname and masked phone
	 *         number
	 */
	QrCodeStatusView scanQrCode(String qrCodeId);

	/**
	 * Validates the QR code SSE subscription token, which is bound to the token
	 * issued when the QR code was generated.
	 *
	 * @param qrCodeId the QR code ID
	 * @param sseToken the subscription token issued when the QR code was generated
	 * @return true when the token matches and the QR code is valid
	 */
	boolean verifyQrSseToken(String qrCodeId, String sseToken);

	/**
	 * Subscribes to QR code login status pushes over SSE.
	 *
	 * @param qrCodeId the QR code ID
	 * @param sseToken the subscription token issued when the QR code was generated
	 */
	SseEmitter subscribeQrCode(String qrCodeId, String sseToken);

	/**
	 * Pushes the QR code confirmed event over SSE.
	 */
	void publishQrConfirmed(String qrCodeId);

	/**
	 * Gets the identity information of the user who scanned the code.
	 * 
	 * @param qrCodeId the QR code ID
	 * @return the SCANNED state with the nickname and masked phone number, or null
	 *         when nobody has scanned
	 */
	QrCodeStatusView getScannedInfo(String qrCodeId);

	RegisterResultView registerByPassword(PasswordRegisterParam param);

	RegisterResultView registerBySms(SmsRegisterParam param);
}
