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

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.util.ClientInfoUtil;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.monitor.domain.LoginContext;
import top.ruilink.inkwash.monitor.domain.LoginEvent;
import top.ruilink.inkwash.monitor.domain.LogoutEvent;
import top.ruilink.inkwash.security.api.param.LoginParam;
import top.ruilink.inkwash.security.api.param.OAuth2LoginParam;
import top.ruilink.inkwash.security.api.param.PasswordLoginParam;
import top.ruilink.inkwash.security.api.param.PasswordRegisterParam;
import top.ruilink.inkwash.security.api.param.QrLoginParam;
import top.ruilink.inkwash.security.api.param.SmsCodeParam;
import top.ruilink.inkwash.security.api.param.SmsLoginParam;
import top.ruilink.inkwash.security.api.param.SmsRegisterParam;
import top.ruilink.inkwash.security.api.view.AccountView;
import top.ruilink.inkwash.security.api.view.CaptchaView;
import top.ruilink.inkwash.security.api.view.QrCodeStatusView;
import top.ruilink.inkwash.security.api.view.RegisterResultView;
import top.ruilink.inkwash.security.service.AuthService;
import top.ruilink.inkwash.security.service.CaptchaService;
import top.ruilink.inkwash.security.service.LoginAttemptService;
import top.ruilink.inkwash.security.service.TokenService;
import top.ruilink.inkwash.security.service.login.LoginResult;
import top.ruilink.inkwash.security.util.CryptoUtil;
import top.ruilink.inkwash.security.util.SecurityUtil;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.domain.credential.PasswordCredential;
import top.ruilink.inkwash.system.enums.UserStatus;
import top.ruilink.inkwash.system.service.AccountService;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.UserService;

/**
 * Authentication service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

	private final CaptchaService captchaService;
	private final LoginAttemptService loginAttemptService;
	private final TokenService tokenService;
	private final AccountService accountService;
	private final ApplicationEventPublisher eventPublisher;
	private final UserService userService;
	private final IdentityService identityService;
	private final top.ruilink.inkwash.security.service.PasswordPolicyService passwordPolicyService;
	private final top.ruilink.inkwash.security.service.login.LoginStrategyFactory loginStrategyFactory;

	public AuthServiceImpl(CaptchaService captchaService, LoginAttemptService loginAttemptService,
			TokenService tokenService, AccountService accountService, ApplicationEventPublisher eventPublisher,
			UserService userService, IdentityService identityService,
			top.ruilink.inkwash.security.service.PasswordPolicyService passwordPolicyService,
			top.ruilink.inkwash.security.service.login.LoginStrategyFactory loginStrategyFactory) {
		this.captchaService = captchaService;
		this.loginAttemptService = loginAttemptService;
		this.tokenService = tokenService;
		this.accountService = accountService;
		this.eventPublisher = eventPublisher;
		this.userService = userService;
		this.identityService = identityService;
		this.passwordPolicyService = passwordPolicyService;
		this.loginStrategyFactory = loginStrategyFactory;
	}

	@Value("${auth.register.audit:true}")
	private boolean registerAudit;

	// ========== Image captcha ==========
	@Override
	public CaptchaView getCaptcha() {
		return captchaService.generateCaptcha();
	}

	// ========== SMS captcha ==========
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void sendSmsCode(SmsCodeParam smsCodeParam) {
		// 1. Validate the image captcha
		boolean captchaVerified = captchaService.verifyCaptcha(smsCodeParam.captchaId(), smsCodeParam.captchaCode());
		if (!captchaVerified) {
			throw new BusinessException("error.auth.captcha_error");
		}

		// 2. Generate the SMS verification code
		String smsCode = captchaService.generateSmsCode(smsCodeParam.phone());

		// TODO: send the SMS by calling the provider API. No SMS channel is wired up
		// yet, so messages are not
		// really delivered and cannot be tested end to end; codes are only stored
		// locally and logged for development integration. The whole SMS channel stays a
		// TODO for this iteration.
		log.info("模拟发送短信验证码{} to phone {}", smsCode.substring(0, Math.min(2, smsCode.length())) + "****",
				smsCodeParam.phone());
	}

	// ========== Common login ==========
	@Override
	public AccountView login(LoginParam loginParam) {
		try {
			AuthType authType = resolveAuthType(loginParam);
			log.info("login called with authType={}", authType);

			LoginResult result = loginStrategyFactory.getStrategy(loginParam).authenticate(loginParam);

			// Login succeeded, record it
			loginAttemptService.recordLoginSuccess(result.subject());

			// Load the user information
			SysUser user = userService.getById(result.userId());
			if (user == null || !UserStatus.ENABLE.equals(user.getStatus())) {
				throw new BusinessException("error.user.not_found");
			}

			// Generate the tokens
			AccountView accountView = tokenService.generateTokens(user.getId(), result.subject(),
					result.authType().getCode());

			log.info("用户登录成功, userId={}, username={}, authType={}", user.getId(), result.subject(), authType);

			// Record the login log
			recordLoginInfo(currentPrincipal(), new LoginContext(user.getId(), result.subject(), result.authType(),
					requestIp(), requestUserAgent()), true, null);

			return accountView;
		} catch (Exception e) {
			log.warn("登录失败: {}", e.getMessage());
			AuthType authType = resolveAuthType(loginParam);
			try {
				recordLoginInfo(currentPrincipal(),
						new LoginContext(null, loginParam.identity(), authType, requestIp(), requestUserAgent()), false,
						e.getMessage());
			} catch (Exception ignored) {
				log.warn("记录登录日志失败: {}", ignored.getMessage());
			}
			throw e;
		}
	}

	private static Object currentPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null ? authentication.getPrincipal() : null;
	}

	private static String requestIp() {
		HttpServletRequest request = currentRequest();
		return request != null ? ClientInfoUtil.getClientIp(request) : null;
	}

	private static String requestUserAgent() {
		HttpServletRequest request = currentRequest();
		return request != null ? request.getHeader("User-Agent") : null;
	}

	private static HttpServletRequest currentRequest() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		return attrs != null ? attrs.getRequest() : null;
	}

	private static AuthType resolveAuthType(LoginParam loginParam) {
		AuthType authType = loginParam.authType();
		if (authType != null) {
			return authType;
		}
		if (loginParam instanceof PasswordLoginParam) {
			return AuthType.PASSWORD;
		}
		if (loginParam instanceof SmsLoginParam) {
			return AuthType.SMS_CODE;
		}
		if (loginParam instanceof QrLoginParam) {
			return AuthType.QR_CODE;
		}
		if (loginParam instanceof OAuth2LoginParam) {
			return AuthType.OAUTH2;
		}
		return AuthType.PASSWORD;
	}

	// ========== Token refresh ==========
	@Override
	public AccountView refreshToken(String refreshToken) {
		return tokenService.refreshTokens(refreshToken);
	}

	// ========== Logout ==========
	@Override
	public void logout(HttpServletRequest request) {
		String token = extractToken(request);
		if (token != null) {
			tokenService.revokeToken(token);
		}
		recordLogoutInfo();
		SecurityContextHolder.clearContext();
		log.info("用户登出成功");
	}

	/**
	 * Publishes the logout so {@code mon_login_info} closes the session.
	 *
	 * <p>
	 * The user id is resolved before the security context is cleared, and
	 * defensively: logout is reachable with an expired or already-revoked token, so
	 * an unresolvable id must not turn a logout into an error. Publishing rather
	 * than calling the monitor service directly keeps the existing
	 * {@code security -> monitor} edge limited to event types, matching how
	 * {@link LoginEvent} is already handled.
	 */
	private void recordLogoutInfo() {
		try {
			Long userId = SecurityUtil.isAuthenticated() ? SecurityUtil.getCurrentUserId() : null;
			eventPublisher.publishEvent(new LogoutEvent(userId));
		} catch (Exception e) {
			log.warn("记录登出日志失败: {}", e.getMessage());
		}
	}

	// ========== QR code login ==========
	@Override
	public CaptchaView generateQrCode(String client, String origin) {
		return captchaService.generateQrCode(client, origin);
	}

	@Override
	public boolean checkQrCodeStatus(String qrCodeId) {
		return captchaService.checkQrCodeStatus(qrCodeId);
	}

	@Override
	public boolean isQrCodeValid(String qrCodeId) {
		return captchaService.isQrCodeValid(qrCodeId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void confirmQrCode(String qrCodeId) {
		Long userId = SecurityUtil.getCurrentUserId();

		// The scanning and confirming users must match, meaning the same phone scans
		// and confirms, to stop someone else confirming
		Long scannedUserId = captchaService.getScannedUser(qrCodeId);
		if (scannedUserId != null && !scannedUserId.equals(userId)) {
			throw new BusinessException("error.auth.qr_user_mismatch");
		}
		captchaService.scanQrCode(qrCodeId, userId);
		captchaService.confirmQrCode(qrCodeId, userId);

		SysUser user = userService.getById(userId);
		if (user == null) {
			throw new BusinessException("error.user.not_found");
		}

		SysAccount qrAccount = accountService.findByIdentityAndType(qrCodeId, AuthType.QR_CODE);
		if (qrAccount == null) {
			qrAccount = new SysAccount();
			qrAccount.setUserId(user.getId());
			qrAccount.setIdentity(qrCodeId);
			qrAccount.setAuthType(AuthType.QR_CODE);
			qrAccount.setStatus(BaseStatus.ENABLE.toString());
			qrAccount.setCreateTime(LocalDateTime.now());
			accountService.create(qrAccount);
		}

		AccountView accountView = tokenService.generateTokens(user.getId(), qrCodeId, AuthType.QR_CODE.getCode());
		captchaService.setQrToken(qrCodeId, accountView.getCredential());

		log.info("二维码登录确认成功, qrCodeId={}, userId={}", qrCodeId, userId);
	}

	@Override
	public SseEmitter subscribeQrCode(String qrCodeId, String sseToken) {
		return captchaService.subscribeQrCode(qrCodeId, sseToken);
	}

	@Override
	public boolean verifyQrSseToken(String qrCodeId, String sseToken) {
		return captchaService.checkSseToken(qrCodeId, sseToken);
	}

	@Override
	public void publishQrConfirmed(String qrCodeId) {
		captchaService.publishQrConfirmed(qrCodeId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public QrCodeStatusView scanQrCode(String qrCodeId) {
		Long userId = SecurityUtil.getCurrentUserId();
		if (!captchaService.isQrCodeValid(qrCodeId)) {
			throw new BusinessException("error.auth.qr_expired");
		}
		if (captchaService.getQrToken(qrCodeId) != null) {
			throw new BusinessException("error.auth.qr_already_confirmed");
		}
		captchaService.scanQrCode(qrCodeId, userId);

		SysUser user = userService.getById(userId);
		if (user == null) {
			throw new BusinessException("error.user.not_found");
		}
		log.info("二维码扫码上报, qrCodeId={}, userId={}", qrCodeId, userId);
		return QrCodeStatusView.scanned(user.getNickname(), maskPhone(user.getPhone()));
	}

	@Override
	public QrCodeStatusView getScannedInfo(String qrCodeId) {
		Long userId = captchaService.getScannedUser(qrCodeId);
		if (userId == null) {
			return null;
		}
		SysUser user = userService.getById(userId);
		if (user == null) {
			return null;
		}
		return QrCodeStatusView.scanned(user.getNickname(), maskPhone(user.getPhone()));
	}

	private String maskPhone(String phone) {
		if (phone == null || phone.length() < 7) {
			return phone;
		}
		return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
	}

	@Override
	public String getQrToken(String qrCodeId) {
		return captchaService.getQrToken(qrCodeId);
	}

	// ========== User registration ==========
	@Override
	@Transactional(rollbackFor = Exception.class)
	public RegisterResultView registerByPassword(PasswordRegisterParam param) {

		SysAccount existing = accountService.findByIdentityAndType(param.username(), AuthType.PASSWORD);
		if (existing != null) {
			throw new BusinessException("error.user.username_exists");
		}

		if (!captchaService.verifyCaptcha(param.captchaId(), param.captchaCode())) {
			throw new BusinessException("error.auth.captcha_error");
		}

		passwordPolicyService.validateStrength(param.password());

		UserStatus userStatus = registerAudit ? UserStatus.PENDING : UserStatus.ENABLE;

		SysUser user = new SysUser();
		user.setNickname(param.nickname() != null ? param.nickname() : param.username());
		user.setStatus(userStatus);
		user.setCreateTime(LocalDateTime.now());
		userService.createRawUser(user);
		userService.joinDefaultGroup(user.getId());

		String passwordHash = CryptoUtil.bcryptEncrypt(param.password());
		SysAccount account = new SysAccount();
		account.setUserId(user.getId());
		account.setIdentity(param.username());
		account.setAuthType(AuthType.PASSWORD);
		account.setCredential(new PasswordCredential(passwordHash));
		account.setStatus(BaseStatus.ENABLE.toString());
		account.setCreateTime(LocalDateTime.now());
		accountService.create(account);

		log.info("用户注册成功(密码), userId={}, username={}", user.getId(), param.username());

		String message = registerAudit ? "注册成功，请等待管理员审核" : "注册成功，请登录";
		return new RegisterResultView(user.getId(), userStatus.getCode(), message);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public RegisterResultView registerBySms(SmsRegisterParam param) {
		if (!captchaService.verifyCaptcha(param.captchaId(), param.captchaCode())) {
			throw new BusinessException("error.auth.captcha_error");
		}

		if (!captchaService.verifySmsCode(param.phone(), param.smsCode())) {
			throw new BusinessException("error.auth.sms_code_error");
		}

		UserStatus userStatus = registerAudit ? UserStatus.PENDING : UserStatus.ENABLE;

		String nickname = param.nickname();
		if (nickname == null) {
			String masked = param.phone().length() > 7
					? param.phone().substring(0, 3) + "****" + param.phone().substring(param.phone().length() - 4)
					: param.phone();
			nickname = "用户" + masked;
		}

		SysUser user = new SysUser();
		user.setNickname(nickname);
		user.setPhone(param.phone());
		user.setStatus(userStatus);
		user.setCreateTime(LocalDateTime.now());
		userService.createRawUser(user);
		userService.joinDefaultGroup(user.getId());

		SysIdentity claim = identityService.findOrCreateVerified(IdentityType.PHONE, param.phone(), null, user.getId(),
				IdentityVerifier.SMS_CODE);
		if (!claim.getUserId().equals(user.getId())) {
			throw new BusinessException("error.identity.phone_occupied");
		}

		log.info("用户注册成功(短信), userId={}, phone={}", user.getId(), param.phone());

		String message = registerAudit ? "注册成功，请等待管理员审核" : "注册成功，请登录";
		return new RegisterResultView(user.getId(), userStatus.getCode(), message);
	}

	// ========== Private methods ==========

	private String extractToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}

	private void recordLoginInfo(Object principal, LoginContext context, boolean success, String message) {
		try {
			eventPublisher.publishEvent(new LoginEvent(principal, context, success, message));
		} catch (Exception e) {
			log.warn("记录登录日志失败: {}", e.getMessage());
		}
	}
}
