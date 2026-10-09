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
package top.ruilink.inkwash.security.api;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.CharConsts;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.api.param.LoginParam;
import top.ruilink.inkwash.security.api.param.PasswordRegisterParam;
import top.ruilink.inkwash.security.api.param.QrTicketParam;
import top.ruilink.inkwash.security.api.param.RefreshTokenParam;
import top.ruilink.inkwash.security.api.param.SmsCodeParam;
import top.ruilink.inkwash.security.api.param.SmsRegisterParam;
import top.ruilink.inkwash.security.api.view.AccountView;
import top.ruilink.inkwash.security.api.view.CaptchaView;
import top.ruilink.inkwash.security.api.view.QrCodeStatusView;
import top.ruilink.inkwash.security.api.view.QrLoginResultView;
import top.ruilink.inkwash.security.api.view.RegisterResultView;
import top.ruilink.inkwash.security.jwt.AuthCookieService;
import top.ruilink.inkwash.security.jwt.JwtTokenProvider;
import top.ruilink.inkwash.security.service.AuthService;
import top.ruilink.inkwash.security.service.impl.QrTicketService;
import top.ruilink.inkwash.security.util.SecurityUtil;

/**
 * Unified authentication controller covering password, SMS and QR code login,
 * GitHub OAuth2, token refresh and logout.
 *
 * <p>
 * H-FS-3: browser clients, detected by X-Requested-With or a secure_* cookie,
 * receive only Set-Cookie after login or refresh, with credential=null in the
 * body so tokens never reach JS.
 * </p>
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;
	private final AuthCookieService cookieService;
	private final QrTicketService qrTicketService;
	private final JwtTokenProvider tokenProvider;

	public AuthController(AuthService authService, AuthCookieService cookieService, QrTicketService qrTicketService,
			JwtTokenProvider tokenProvider) {
		this.authService = authService;
		this.cookieService = cookieService;
		this.qrTicketService = qrTicketService;
		this.tokenProvider = tokenProvider;
	}

	@GetMapping("/captcha")
	public ResponseEntity<CaptchaView> getCaptcha() {
		CaptchaView captchaVO = authService.getCaptcha();
		return ResponseEntity.ok(captchaVO);
	}

	@PostMapping("/sms/code")
	public ResponseEntity<Void> sendSmsCode(@Valid @RequestBody SmsCodeParam smsCodeParam) {
		authService.sendSmsCode(smsCodeParam);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/login")
	public ResponseEntity<AccountView> login(@Valid @RequestBody LoginParam loginParam, HttpServletRequest request,
			HttpServletResponse response) {
		AccountView accountView = authService.login(loginParam);
		if (cookieService.isBrowserRequest(request)) {
			writeTokensFromCredential(response, accountView.getCredential());
			accountView.setCredential(null);
		}
		return ResponseEntity.ok(accountView);
	}

	@PostMapping("/register/password")
	public ResponseEntity<RegisterResultView> registerByPassword(@Valid @RequestBody PasswordRegisterParam param) {
		RegisterResultView result = authService.registerByPassword(param);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/register/sms")
	public ResponseEntity<RegisterResultView> registerBySms(@Valid @RequestBody SmsRegisterParam param) {
		RegisterResultView result = authService.registerBySms(param);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		authService.logout(request);
		if (cookieService.isBrowserRequest(request)) {
			cookieService.clearTokens(response);
		}
		return ResponseEntity.ok().build();
	}

	@PostMapping("/token/refresh")
	public ResponseEntity<AccountView> refreshToken(@RequestBody(required = false) RefreshTokenParam param,
			HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = (param != null && StringUtils.hasText(param.refreshToken())) ? param.refreshToken()
				: cookieService.readRefreshToken(request);
		AccountView accountView = authService.refreshToken(refreshToken);
		if (cookieService.isBrowserRequest(request)) {
			writeTokensFromCredential(response, accountView.getCredential());
			accountView.setCredential(null);
		}
		return ResponseEntity.ok(accountView);
	}

	@GetMapping("/me")
	public ResponseEntity<AccountView> me() {
		AccountView view = new AccountView();
		view.setUserId(SecurityUtil.getCurrentUserIdOrNull());
		String username = SecurityUtil.getCurrentUsername();
		if (username != null) {
			String[] parts = username.split(CharConsts.COLON, 2);
			if (parts.length == 2) {
				try {
					view.setAuthType(AuthType.fromCode(Integer.parseInt(parts[0])));
				} catch (NumberFormatException ignored) {
					// A non-numeric prefix does not resolve an authType
				}
				view.setIdentity(parts[1]);
			} else {
				view.setIdentity(username);
			}
		}
		return ResponseEntity.ok(view);
	}

	@GetMapping("/qr/code")
	public ResponseEntity<CaptchaView> generateQrCode(@RequestParam(defaultValue = "web") String client,
			@RequestParam(required = false) String origin) {
		CaptchaView qrCodeView = authService.generateQrCode(client, origin);
		return ResponseEntity.ok(qrCodeView);
	}

	@GetMapping("/qr/check")
	public ResponseEntity<QrCodeStatusView> checkQrCodeStatus(@RequestParam String qrCodeId,
			@RequestParam(required = false) String sseToken) {
		String credential = authService.getQrToken(qrCodeId);
		if (credential != null && authService.verifyQrSseToken(qrCodeId, sseToken)) {
			return ResponseEntity.ok(QrCodeStatusView.confirmed(qrTicketService.issue(qrCodeId, credential)));
		}
		if (!authService.isQrCodeValid(qrCodeId)) {
			return ResponseEntity.ok(QrCodeStatusView.expired());
		}
		QrCodeStatusView scannedInfo = authService.getScannedInfo(qrCodeId);
		if (scannedInfo != null) {
			return ResponseEntity.ok(scannedInfo);
		}
		return ResponseEntity.ok(QrCodeStatusView.pending());
	}

	@PostMapping("/qr/login")
	public ResponseEntity<QrLoginResultView> qrLogin(@Valid @RequestBody QrTicketParam param,
			HttpServletResponse response) {
		QrTicketService.Ticket ticket = qrTicketService.exchange(param.ticket());
		if (ticket == null) {
			throw new BusinessException("error.auth.qr_ticket_invalid");
		}
		writeTokensFromCredential(response, ticket.credential());
		return ResponseEntity.ok(new QrLoginResultView(true));
	}

	@PostMapping("/qr/scan")
	public ResponseEntity<QrCodeStatusView> scanQrCode(@RequestParam String qrCodeId) {
		QrCodeStatusView scanned = authService.scanQrCode(qrCodeId);
		return ResponseEntity.ok(scanned);
	}

	@PostMapping("/qr/confirm")
	public ResponseEntity<Void> confirmQrCode(@RequestParam String qrCodeId) {
		authService.confirmQrCode(qrCodeId);
		authService.publishQrConfirmed(qrCodeId);
		return ResponseEntity.ok().build();
	}

	@GetMapping(value = "/qr/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter qrSse(@RequestParam String qrCodeId, @RequestParam(required = false) String sseToken) {
		return authService.subscribeQrCode(qrCodeId, sseToken);
	}

	@GetMapping("/oauth2/token")
	public void oauth2Callback(@RequestParam(required = false) String token, HttpServletResponse response)
			throws IOException {
		String[] parts = splitCredential(token);
		if (parts == null) {
			response.sendRedirect("/oauth2-error.html");
			return;
		}
		// Verify the access token signature, writing no cookie when it is invalid
		if (!tokenProvider.validateToken(parts[0])) {
			response.sendRedirect("/oauth2-error.html");
			return;
		}
		cookieService.writeTokens(response, parts[0], parts[1]);
		response.sendRedirect("/oauth2-callback.html");
	}

	/**
	 * Splits an "access:refresh" credential, returning null when it is malformed or
	 * incomplete
	 */
	private String[] splitCredential(String credential) {
		if (!StringUtils.hasText(credential)) {
			return null;
		}
		String[] parts = credential.split(CharConsts.COLON, -1);
		if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
			return null;
		}
		return parts;
	}

	private void writeTokensFromCredential(HttpServletResponse response, String credential) {
		String[] parts = splitCredential(credential);
		if (parts != null) {
			cookieService.writeTokens(response, parts[0], parts[1]);
		}
	}
}
