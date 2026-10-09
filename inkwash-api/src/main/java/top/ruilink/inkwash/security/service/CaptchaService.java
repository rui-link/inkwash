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

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import top.ruilink.inkwash.security.api.view.CaptchaView;

/**
 * Captcha domain service handling generation and validation of image captchas,
 * SMS codes and QR codes.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface CaptchaService {

	/**
	 * Generates an image captcha.
	 * 
	 * @return an object holding the captcha ID and a Base64 image
	 */
	CaptchaView generateCaptcha();

	/**
	 * Validates an image captcha.
	 * 
	 * @param captchaId the captcha ID
	 * @param inputCode the code entered by the user
	 * @return whether validation passed
	 */
	boolean verifyCaptcha(String captchaId, String inputCode);

	/**
	 * Generates an SMS verification code.
	 * 
	 * @param phone the phone number
	 * @return the generated code
	 */
	String generateSmsCode(String phone);

	/**
	 * Validates an SMS verification code.
	 * 
	 * @param phone     the phone number
	 * @param inputCode the code entered by the user
	 * @return whether validation passed
	 */
	boolean verifySmsCode(String phone, String inputCode);

	/**
	 * Clears the cached SMS verification code.
	 * 
	 * @param phone the phone number
	 */
	void clearSmsCode(String phone);

	/**
	 * Generates a QR code login credential.
	 * 
	 * @param client the client identifier, web or admin; the admin QR code always
	 *               points at the configured web-url
	 * @param origin the origin from which the scan confirmation page is opened,
	 *               such as http://192.168.1.5:3001; for the web client an http(s)
	 *               origin builds a phone reachable confirmation URL, otherwise the
	 *               configured qr-login.web-url is used
	 * @return an object holding the QR code ID and image
	 */
	CaptchaView generateQrCode(String client, String origin);

	/**
	 * Checks the QR code login status.
	 * 
	 * @param qrCodeId the QR code ID
	 * @return whether it has been confirmed
	 */
	boolean checkQrCodeStatus(String qrCodeId);

	/**
	 * Checks whether the QR code is still valid, meaning present and unexpired.
	 * 
	 * @param qrCodeId the QR code ID
	 * @return whether it is valid
	 */
	boolean isQrCodeValid(String qrCodeId);

	/**
	 * Confirms the QR code login from the mobile client.
	 * 
	 * @param qrCodeId the QR code ID
	 * @param userId   the confirming user ID
	 */
	void confirmQrCode(String qrCodeId, Long userId);

	/**
	 * Records that the QR code has been scanned, called when the mobile client
	 * opens the confirmation page to identify the scanning user.
	 * 
	 * @param qrCodeId the QR code ID
	 * @param userId   the scanning user ID
	 */
	void scanQrCode(String qrCodeId, Long userId);

	/**
	 * Gets the ID of the user who scanned the code.
	 * 
	 * @param qrCodeId the QR code ID
	 * @return the scanning user ID, or null when unscanned or expired
	 */
	Long getScannedUser(String qrCodeId);

	/**
	 * Gets the confirmed QR code login token.
	 */
	String getQrToken(String qrCodeId);

	/**
	 * Sets the QR code login token.
	 */
	void setQrToken(String qrCodeId, String token);

	/**
	 * Gets the user ID confirmed by the QR code.
	 */
	Long getQrConfirmedUser(String qrCodeId);

	/**
	 * Checks whether the caller supplied SSE subscription token matches the QR code
	 * bound token, comparing in constant time.
	 *
	 * @param qrCodeId the QR code ID
	 * @param sseToken the subscription token issued when the QR code was generated
	 * @return true when the token matches and the QR code is valid
	 */
	boolean checkSseToken(String qrCodeId, String sseToken);

	/**
	 * Subscribes to QR code login status pushes over SSE.
	 * <p>
	 * Only subscribers presenting the matching bound token may connect. An already
	 * confirmed code returns a CONFIRMED stream that ends immediately and an
	 * invalidated code an EXPIRED stream, otherwise the connection stays open until
	 * the QR code validity
	 *
	 * @param qrCodeId the QR code ID
	 * @param sseToken the subscription token issued when the QR code was generated
	 * @return the SSE emitter
	 */
	SseEmitter subscribeQrCode(String qrCodeId, String sseToken);

	/**
	 * Pushes the QR code confirmed event over SSE.
	 * <p>
	 * Takes effect only when the QR code is confirmed and a subscription exists,
	 * pushing CONFIRMED with the token to the subscriber and ending the connection.
	 *
	 * @param qrCodeId the QR code ID
	 */
	void publishQrConfirmed(String qrCodeId);
}
