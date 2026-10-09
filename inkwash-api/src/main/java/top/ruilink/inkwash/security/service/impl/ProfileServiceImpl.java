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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.api.param.ChangePasswordParam;
import top.ruilink.inkwash.security.api.param.ProfileParam;
import top.ruilink.inkwash.security.api.param.ResetPasswordParam;
import top.ruilink.inkwash.security.api.param.SetPasswordParam;
import top.ruilink.inkwash.security.api.param.VerifyEmailParam;
import top.ruilink.inkwash.security.api.param.VerifyPhoneParam;
import top.ruilink.inkwash.security.api.view.LinkedAccountView;
import top.ruilink.inkwash.security.service.CaptchaService;
import top.ruilink.inkwash.security.service.EmailService;
import top.ruilink.inkwash.security.service.LoginAttemptService;
import top.ruilink.inkwash.security.service.PasswordPolicyService;
import top.ruilink.inkwash.security.service.ProfileService;
import top.ruilink.inkwash.security.util.CryptoUtil;
import top.ruilink.inkwash.security.util.SecurityUtil;
import top.ruilink.inkwash.system.api.view.PermissionView;
import top.ruilink.inkwash.system.api.view.UserView;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.SysPermission;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.domain.credential.OAuth2Credential;
import top.ruilink.inkwash.system.domain.credential.PasswordCredential;
import top.ruilink.inkwash.system.domain.credential.PasswordStatus;
import top.ruilink.inkwash.system.service.AccountService;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.PermissionService;
import top.ruilink.inkwash.system.service.UserService;

/**
 * User profile service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

	private final UserService userService;
	private final PermissionService permissionService;
	private final AccountService accountService;
	private final IdentityService identityService;
	private final CaptchaService captchaService;
	private final LoginAttemptService loginAttemptService;
	private final EmailService emailService;
	private final PasswordPolicyService passwordPolicyService;

	public ProfileServiceImpl(UserService userService, PermissionService permissionService,
			AccountService accountService, IdentityService identityService, CaptchaService captchaService,
			LoginAttemptService loginAttemptService, EmailService emailService,
			PasswordPolicyService passwordPolicyService) {
		this.userService = userService;
		this.permissionService = permissionService;
		this.accountService = accountService;
		this.identityService = identityService;
		this.captchaService = captchaService;
		this.loginAttemptService = loginAttemptService;
		this.emailService = emailService;
		this.passwordPolicyService = passwordPolicyService;
	}

	@Override
	public UserView getProfile() {
		Long userId = SecurityUtil.getCurrentUserId();
		SysUser user = userService.getById(userId);
		if (user == null) {
			throw new BusinessException("error.user.not_found");
		}
		UserView userView = new UserView();
		userView.setId(user.getId());
		userView.setNickname(user.getNickname());
		userView.setRealname(user.getRealname());
		userView.setGender(user.getGender());
		userView.setPhone(user.getPhone());
		userView.setEmail(user.getEmail());
		userView.setAvatar(user.getAvatar());
		userView.setStatus(user.getStatus());
		userView.setIdtype(user.getIdtype());
		userView.setIdcode(user.getIdcode());
		userView.setMotto(user.getMotto());
		userView.setBirthDate(user.getBirthDate());
		userView.setEducation(user.getEducation());
		userView.setLocation(user.getLocation());
		userView.setBiography(user.getBiography());
		userView.setCreateTime(user.getCreateTime());

		Set<SysPermission> permissions = permissionService.findByUserId(userId);
		Set<String> permCodes = permissions.stream().map(SysPermission::getAuthority).filter(Objects::nonNull)
				.collect(Collectors.toSet());
		userView.setPermissions(permCodes);

		List<String> roleCodes = permissionService.findRoleCodesByUserId(userId);
		userView.setRoles(roleCodes);

		return userView;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateProfile(ProfileParam param) {
		Long userId = SecurityUtil.getCurrentUserId();
		SysUser user = userService.getById(userId);
		if (user == null) {
			throw new BusinessException("error.user.not_found");
		}
		if (param.getNickname() != null) {
			user.setNickname(param.getNickname());
		}
		if (param.getAvatar() != null) {
			user.setAvatar(param.getAvatar());
		}
		if (param.getIdtype() != null) {
			user.setIdtype(param.getIdtype());
		}
		if (param.getIdcode() != null) {
			user.setIdcode(param.getIdcode());
		}
		if (param.getRealname() != null) {
			user.setRealname(param.getRealname());
		}
		if (param.getGender() != null) {
			user.setGender(param.getGender());
		}
		if (param.getMotto() != null) {
			user.setMotto(param.getMotto());
		}
		if (param.getBirthDate() != null) {
			user.setBirthDate(param.getBirthDate());
		}
		if (param.getEducation() != null) {
			user.setEducation(param.getEducation());
		}
		if (param.getLocation() != null) {
			user.setLocation(param.getLocation());
		}
		if (param.getBiography() != null) {
			user.setBiography(param.getBiography());
		}
		user.setUpdateTime(LocalDateTime.now());
		userService.updateRawUser(user);
	}

	@Override
	public Set<PermissionView> getPermissions() {
		Long userId = SecurityUtil.getCurrentUserId();
		Set<SysPermission> permissions = permissionService.findByUserId(userId);

		Set<PermissionView> permissionViews = new HashSet<>();
		for (SysPermission permission : permissions) {
			PermissionView view = new PermissionView();
			view.setAuthority(permission.getAuthority());
			view.setAction(permission.getAction());
			view.setResource(permission.getResource());
			permissionViews.add(view);
		}
		return permissionViews;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updatePassword(ChangePasswordParam param) {
		if (!param.newPassword().equals(param.confirmPassword())) {
			throw new BusinessException("error.auth.password_mismatch");
		}

		Long userId = SecurityUtil.getCurrentUserId();
		SysAccount account = accountService.listByUserId(userId).stream()
				.filter(a -> a.getAuthType() == AuthType.PASSWORD).findFirst()
				.orElseThrow(() -> new BusinessException("error.account.no_password"));

		PasswordCredential oldCred = account.getCredential() instanceof PasswordCredential cred ? cred : null;
		if (oldCred == null) {
			throw new BusinessException("error.credential.not_found");
		}
		if (!oldCred.verify(param.oldPassword())) {
			throw new BusinessException("error.auth.old_password_wrong");
		}

		passwordPolicyService.validateStrength(param.newPassword());
		passwordPolicyService.validateHistory(oldCred.passwordHash(), param.newPassword());

		String passwordHash = CryptoUtil.bcryptEncrypt(param.newPassword());
		account.setCredential(new PasswordCredential(passwordHash));
		accountService.update(account);

		log.info("用户修改密码成功, userId={}", userId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void resetPassword(ResetPasswordParam param) {
		if (!param.newPassword().equals(param.confirmPassword())) {
			throw new BusinessException("error.auth.password_mismatch");
		}
		passwordPolicyService.validateStrength(param.newPassword());

		String phone = param.phone();
		if (!captchaService.verifySmsCode(phone, param.smsCode())) {
			throw new BusinessException("error.auth.sms_code_error");
		}

		SysUser user = userService.getByPhone(phone);
		if (user == null) {
			throw new BusinessException("error.user.not_found");
		}

		SysAccount account = accountService.findByIdentityAndType(phone, AuthType.PASSWORD);
		if (account == null) {
			var pwAccount = new SysAccount();
			pwAccount.setUserId(user.getId());
			pwAccount.setIdentity(phone);
			pwAccount.setAuthType(AuthType.PASSWORD);
			pwAccount.setCredential(new PasswordCredential(CryptoUtil.bcryptEncrypt(param.newPassword())));
			pwAccount.setStatus(PasswordStatus.ENABLE.getCode());
			accountService.create(pwAccount);
			account = pwAccount;
		} else {
			if (account.getCredential() instanceof PasswordCredential oldCred) {
				passwordPolicyService.validateHistory(oldCred.passwordHash(), param.newPassword());
			}
			account.setCredential(new PasswordCredential(CryptoUtil.bcryptEncrypt(param.newPassword())));
		}

		captchaService.clearSmsCode(phone);
		loginAttemptService.unlock(phone);

		log.info("用户重置密码成功, phone={}", phone);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void setPassword(SetPasswordParam param) {
		if (!param.newPassword().equals(param.confirmPassword())) {
			throw new BusinessException("error.auth.password_mismatch");
		}

		Long userId = SecurityUtil.getCurrentUserId();

		SysIdentity emailClaim = identityService.listByUserId(userId).stream()
				.filter(c -> c.getIdentityType() == IdentityType.EMAIL && c.identityVerified()).findFirst()
				.orElseThrow(() -> new BusinessException("error.email.not_verified"));

		if (!emailService.verifyCode(emailClaim.getIdentityValue(), param.emailCode())) {
			throw new BusinessException("error.email.code_error");
		}

		if (accountService.findByIdentityAndType(param.username(), AuthType.PASSWORD) != null) {
			throw new BusinessException("error.user.username_exists");
		}

		boolean hasPassword = accountService.listByUserId(userId).stream()
				.anyMatch(a -> a.getAuthType() == AuthType.PASSWORD);
		if (hasPassword) {
			throw new BusinessException("error.account.password_exists");
		}

		passwordPolicyService.validateStrength(param.newPassword());

		SysAccount account = new SysAccount();
		account.setUserId(userId);
		account.setIdentity(param.username());
		account.setAuthType(AuthType.PASSWORD);
		account.setCredential(new PasswordCredential(CryptoUtil.bcryptEncrypt(param.newPassword())));
		account.setStatus(BaseStatus.ENABLE.toString());
		account.setCreateTime(LocalDateTime.now());
		accountService.create(account);

		log.info("用户设置密码成功, userId={}, username={}", userId, param.username());
	}

	@Override
	public void sendPhoneVerifyCode(String phone) {
		Long userId = SecurityUtil.getCurrentUserId();
		boolean mine = identityService.listByUserId(userId).stream()
				.anyMatch(c -> c.getIdentityType() == IdentityType.PHONE && phone.equals(c.getIdentityValue()));
		if (!mine && identityService.findByTypeValue(IdentityType.PHONE, phone) != null) {
			throw new BusinessException("error.identity.phone_occupied");
		}
		captchaService.generateSmsCode(phone);
		log.info("发送手机验证码, phone={}, userId={}", phone, userId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void verifyPhone(VerifyPhoneParam param) {
		Long userId = SecurityUtil.getCurrentUserId();
		if (!captchaService.verifySmsCode(param.phone(), param.smsCode())) {
			throw new BusinessException("error.auth.sms_code_error");
		}

		SysIdentity claim = identityService.findOrCreateVerified(IdentityType.PHONE, param.phone(), null, userId,
				IdentityVerifier.SMS_CODE);
		if (!claim.getUserId().equals(userId)) {
			throw new BusinessException("error.identity.phone_occupied");
		}

		SysUser user = userService.getById(userId);
		if (user != null && !param.phone().equals(user.getPhone())) {
			user.setPhone(param.phone());
			userService.updateRawUser(user);
		}
		log.info("用户验证手机成功, userId={}, phone={}", userId, param.phone());
	}

	@Override
	public void sendEmailVerifyCode(String email) {
		Long userId = SecurityUtil.getCurrentUserId();
		boolean mine = identityService.listByUserId(userId).stream()
				.anyMatch(c -> c.getIdentityType() == IdentityType.EMAIL && email.equals(c.getIdentityValue()));
		if (!mine && identityService.findByTypeValue(IdentityType.EMAIL, email) != null) {
			throw new BusinessException("error.identity.email_occupied");
		}
		emailService.sendCode(email);
		log.info("发送邮箱验证码, email={}, userId={}", email, userId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void verifyEmail(VerifyEmailParam param) {
		Long userId = SecurityUtil.getCurrentUserId();
		if (!emailService.verifyCode(param.email(), param.emailCode())) {
			throw new BusinessException("error.email.code_error");
		}

		SysIdentity claim = identityService.findOrCreateVerified(IdentityType.EMAIL, param.email(), null, userId,
				IdentityVerifier.USER);
		if (!claim.getUserId().equals(userId)) {
			throw new BusinessException("error.identity.email_occupied");
		}

		SysUser user = userService.getById(userId);
		if (user != null && !param.email().equals(user.getEmail())) {
			user.setEmail(param.email());
			userService.updateRawUser(user);
		}
		log.info("用户验证邮箱成功, userId={}, email={}", userId, param.email());
	}

	@Override
	public List<LinkedAccountView> listAccounts() {
		Long userId = SecurityUtil.getCurrentUserId();
		List<LinkedAccountView> views = new ArrayList<>();

		for (SysIdentity claim : identityService.listByUserId(userId)) {
			if (claim.getIdentityType() != IdentityType.PHONE && claim.getIdentityType() != IdentityType.EMAIL) {
				continue;
			}
			if (!claim.identityVerified()) {
				continue;
			}
			views.add(new LinkedAccountView(claim.getId(), claim.getIdentityType().name(), claim.getIdentityValue(),
					null, claim.getVerified(), claim.getVerifier() != null ? claim.getVerifier().name() : null));
		}

		for (SysAccount account : accountService.listByUserId(userId)) {
			String provider = null;
			if (account.getCredential() instanceof OAuth2Credential oauth2 && oauth2.provider() != null) {
				provider = oauth2.provider().name();
			}
			views.add(new LinkedAccountView(account.getId(), account.getAuthType().name(), account.getIdentity(),
					provider, 1, null));
		}
		return views;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void unbindAccount(Long id, String kind) {
		Long userId = SecurityUtil.getCurrentUserId();

		long credentials = accountService.listByUserId(userId).stream().filter(a -> a.getAuthType() == AuthType.PASSWORD
				|| a.getAuthType() == AuthType.OAUTH2 || a.getAuthType() == AuthType.QR_CODE).count();
		long verifiedClaims = identityService.listByUserId(userId).stream()
				.filter(c -> (c.getIdentityType() == IdentityType.PHONE || c.getIdentityType() == IdentityType.EMAIL)
						&& c.identityVerified())
				.count();
		if (credentials + verifiedClaims <= 1) {
			throw new BusinessException("error.account.at_least_one");
		}

		if ("ACCOUNT".equalsIgnoreCase(kind)) {
			SysAccount account = accountService.getById(id);
			if (account == null || !account.getUserId().equals(userId)) {
				throw new BusinessException("error.account.not_found");
			}
			accountService.deleteById(id);
			log.info("用户解绑凭证成功, userId={}, accountId={}", userId, id);
			return;
		}

		if ("IDENTITY".equalsIgnoreCase(kind)) {
			SysIdentity claim = identityService.listByUserId(userId).stream().filter(c -> c.getId().equals(id))
					.findFirst().orElseThrow(() -> new BusinessException("error.account.not_found"));
			identityService.deleteById(id);

			if (claim.getIdentityType() == IdentityType.PHONE || claim.getIdentityType() == IdentityType.EMAIL) {
				SysUser user = userService.getById(userId);
				if (user != null) {
					if (claim.getIdentityType() == IdentityType.PHONE
							&& claim.getIdentityValue().equals(user.getPhone())) {
						user.setPhone(null);
						userService.updateRawUser(user);
					}
					if (claim.getIdentityType() == IdentityType.EMAIL
							&& claim.getIdentityValue().equals(user.getEmail())) {
						user.setEmail(null);
						userService.updateRawUser(user);
					}
				}
			}
			log.info("用户解绑身份声明成功, userId={}, identityId={}", userId, id);
			return;
		}

		throw new BusinessException("error.auth.unsupported_type");
	}

	@Override
	public Long getCurrentUserId() {
		return SecurityUtil.getCurrentUserId();
	}

	@Override
	public String getCurrentUsername() {
		return SecurityUtil.getCurrentUsername();
	}
}
