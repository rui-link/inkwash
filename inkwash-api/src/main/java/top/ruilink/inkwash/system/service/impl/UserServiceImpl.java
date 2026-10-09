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
package top.ruilink.inkwash.system.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageImpl;
import org.springframework.util.StringUtils;
import top.ruilink.inkwash.base.util.PageUtil;
import top.ruilink.inkwash.base.util.SortValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.CacheConsts;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.BaseStatus;
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.util.CryptoUtil;
import top.ruilink.inkwash.security.util.SecurityUtil;
import top.ruilink.inkwash.security.service.EmailService;
import top.ruilink.inkwash.system.api.param.UserParam;
import top.ruilink.inkwash.system.api.query.UserQuery;
import top.ruilink.inkwash.system.api.view.ResetPasswordView;
import top.ruilink.inkwash.system.api.view.UserView;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysGroup;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.SysUser;
import top.ruilink.inkwash.system.domain.credential.PasswordCredential;
import top.ruilink.inkwash.system.domain.credential.PasswordStatus;
import top.ruilink.inkwash.system.enums.UserStatus;
import top.ruilink.inkwash.system.mapper.AccountMapper;
import top.ruilink.inkwash.system.mapper.GroupMapper;
import top.ruilink.inkwash.system.mapper.UserMapper;
import top.ruilink.inkwash.system.service.IdentityService;
import top.ruilink.inkwash.system.service.UserService;
import top.ruilink.inkwash.system.service.converter.GroupConverter;
import top.ruilink.inkwash.system.service.converter.UserConverter;

/**
 * User service implementation.
 * 
 * Responsibilities: business logic, transaction control and business
 * validation.
 * 
 * Conversion is delegated to UserConverter.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

	private final UserMapper userMapper;
	private final AccountMapper accountMapper;
	private final IdentityService identityService;
	private final SortValidator sortValidator;
	private final GroupMapper groupMapper;
	private final EmailService emailService;

	@Value("${auth.register.default-group-name:Users}")
	private String defaultGroupName;

	public UserServiceImpl(UserMapper userMapper, AccountMapper accountMapper, IdentityService identityService,
			SortValidator sortValidator, GroupMapper groupMapper, EmailService emailService) {
		this.userMapper = userMapper;
		this.accountMapper = accountMapper;
		this.identityService = identityService;
		this.sortValidator = sortValidator;
		this.groupMapper = groupMapper;
		this.emailService = emailService;
	}

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	// ========== Query operations ==========

	@Override
	public SysUser findByIdentityAndType(String identity, AuthType authType) {
		return userMapper.selectByIdentityAndType(identity, authType);
	}

	@Override
	public SysUser getById(Long userId) {
		return userMapper.selectById(userId);
	}

	@Override
	public SysUser getByPhone(String phone) {
		if (!StringUtils.hasText(phone)) {
			return null;
		}
		SysIdentity claim = identityService.findByTypeValue(IdentityType.PHONE, phone);
		if (claim == null) {
			return null;
		}
		return userMapper.selectById(claim.getUserId());
	}

	@Override
	public SysUser getByEmail(String email) {
		if (!StringUtils.hasText(email)) {
			return null;
		}
		SysIdentity claim = identityService.findByTypeValue(IdentityType.EMAIL, email);
		if (claim == null) {
			return null;
		}
		return userMapper.selectById(claim.getUserId());
	}

	@Override
	public PageResult<UserView> pageSearch(UserQuery query) {
		var sortSql = sortValidator.resolve(query.getSortField(), query.getSortOrder());
		var offset = (long) (query.getPage() - 1) * query.getSize();
		var total = userMapper.countUser(query);
		var list = userMapper.selectUserList(query, offset, query.getSize(), sortSql);
		List<Long> userIds = list.stream().map(SysUser::getId).toList();
		var accountMap = accountMapper.selectByUserIds(userIds).stream()
				.collect(Collectors.groupingBy(SysAccount::getUserId));
		var views = list.stream().map(user -> {
			var view = UserConverter.toUserView(user);
			view.setAccounts(UserConverter.toAccountViews(accountMap.getOrDefault(user.getId(), List.of())));
			return view;
		}).toList();
		var pageable = PageUtil.of(query.getPage(), query.getSize());
		var page = new PageImpl<>(views, pageable, total);
		return PageResult.of(page);
	}

	@Override
	public long count() {
		return userMapper.count();
	}

	@Override
	public UserView getUserDetail(Long userId) {
		SysUser user = userMapper.selectById(userId);
		if (user == null || UserStatus.DELETED.equals(user.getStatus())) {
			throw new BusinessException("error.user.not_found");
		}
		UserView view = UserConverter.toUserDetailView(user);
		view.setAccounts(UserConverter.toAccountViews(accountMapper.selectByUserId(userId)));
		view.setGroups(
				groupMapper.selectByUserId(userId).stream().map(GroupConverter::toView).collect(Collectors.toSet()));
		return view;
	}

	// ========== Write operations ==========

	@Override
	@Transactional(rollbackFor = Exception.class)
	public UserView createUser(UserParam param) {
		// Business validation: username is unique
		if (accountMapper.selectByIdentityAndType(param.getUsername(), AuthType.PASSWORD) != null) {
			throw new BusinessException("error.user.username_exists");
		}
		// Business validation: nickname is unique
		if (userMapper.selectByNickname(param.getNickname()) != null) {
			throw new BusinessException("error.user.nickname_exists");
		}
		// Business validation: the phone and email identity claims are free
		if (StringUtils.hasText(param.getPhone())) {
			assertClaimFree(IdentityType.PHONE, param.getPhone());
		}
		if (StringUtils.hasText(param.getEmail())) {
			assertClaimFree(IdentityType.EMAIL, param.getEmail());
		}

		// Create the user entity through the converter
		SysUser user = UserConverter.toUserEntity(param);
		user.setCreator(SecurityUtil.getCurrentUserId());
		user.setUpdater(SecurityUtil.getCurrentUserId());

		// Persist the user, with MyBatis writing the generated key back into user.id
		userMapper.create(user);

		// Create the password authentication account
		SysAccount account = new SysAccount();
		account.setUserId(user.getId());
		account.setIdentity(param.getUsername());
		account.setAuthType(AuthType.PASSWORD);
		account.setCredential(new PasswordCredential(CryptoUtil.bcryptEncrypt(param.getPassword())));
		account.setStatus(PasswordStatus.ENABLE.getCode());
		accountMapper.create(account);

		// Synchronise the phone and email identity claims
		if (StringUtils.hasText(user.getPhone())) {
			identityService.findOrCreateVerified(IdentityType.PHONE, user.getPhone(), null, user.getId(),
					IdentityVerifier.ADMIN);
		}
		if (StringUtils.hasText(user.getEmail())) {
			identityService.findOrCreateVerified(IdentityType.EMAIL, user.getEmail(), null, user.getId(),
					IdentityVerifier.ADMIN);
		}

		log.info("创建用户成功, userId={}, username={}", user.getId(), param.getUsername());
		return UserConverter.toUserDetailView(user);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public UserView updateUser(Long userId, UserParam param) {
		// Load the existing user
		SysUser user = userMapper.selectById(userId);
		if (user == null) {
			throw new BusinessException("error.user.not_found");
		}
		if (UserStatus.DELETED.equals(user.getStatus())) {
			throw new BusinessException("error.user.deleted");
		}

		// Business validation: check whether the nickname is taken
		if (param.getNickname() != null && !param.getNickname().equals(user.getNickname())) {
			SysUser existing = userMapper.selectByNickname(param.getNickname());
			if (existing != null && !existing.getId().equals(userId)) {
				throw new BusinessException("error.user.username_taken");
			}
		}

		String oldPhone = user.getPhone();
		String oldEmail = user.getEmail();

		// Update the entity through the converter
		UserConverter.updateUserEntity(user, param);
		user.setUpdater(SecurityUtil.getCurrentUserId());

		// Synchronise the phone and email identity claims, where null means leave
		// unchanged
		syncClaim(user, param.getPhone(), oldPhone, IdentityType.PHONE);
		syncClaim(user, param.getEmail(), oldEmail, IdentityType.EMAIL);

		// Persist
		userMapper.update(user);

		log.info("更新用户成功, userId={}", userId);
		return UserConverter.toUserDetailView(user);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteUser(Long userId) {
		SysUser user = userMapper.selectById(userId);
		if (user == null || UserStatus.DELETED.equals(user.getStatus())) {
			throw new BusinessException("error.user.not_found");
		}
		userMapper.updateStatus(userId, UserStatus.DELETED.getCode(), LocalDateTime.now());
		log.info("删除用户成功(软删除), userId={}", userId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CacheConsts.USER_PERMISSIONS_CACHE, key = "#userId")
	public void joinDefaultGroup(Long userId) {
		SysUser check = userMapper.selectById(userId);
		if (check == null || UserStatus.DELETED.equals(check.getStatus())) {
			throw new BusinessException("error.user.deleted");
		}
		if (!StringUtils.hasText(defaultGroupName)) {
			log.info("未配置默认用户组，跳过自动分组, userId={}", userId);
			return;
		}
		try {
			SysGroup group = groupMapper.selectByName(defaultGroupName);
			if (group == null) {
				log.warn("默认用户组不存在, groupName={}, 跳过自动分组, userId={}", defaultGroupName, userId);
				return;
			}
			boolean alreadyMember = userMapper.selectGroupsByUserId(userId).stream()
					.anyMatch(g -> g.getId().equals(group.getId()));
			if (alreadyMember) {
				log.info("用户已在默认用户组中, userId={}, groupName={}", userId, defaultGroupName);
				return;
			}
			userMapper.assignGroup(userId, group.getId());
			log.info("新用户加入默认用户组, userId={}, groupName={}, groupId={}", userId, defaultGroupName, group.getId());
		} catch (Exception e) {
			log.warn("加入默认用户组失败, userId={}, err={}", userId, e.getMessage());
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CacheConsts.USER_PERMISSIONS_CACHE, key = "#userId")
	public void assignGroups(Long userId, List<Long> groupIds) {
		// Business validation
		SysUser user = userMapper.selectById(userId);
		if (user == null) {
			throw new BusinessException("error.user.not_found");
		}
		if (UserStatus.DELETED.equals(user.getStatus())) {
			throw new BusinessException("error.user.deleted");
		}

		// Assign the user groups
		userMapper.removeGroups(userId);
		if (groupIds != null && !groupIds.isEmpty()) {
			userMapper.assignGroupsBatch(userId, groupIds);
		}

		log.info("分配用户组成功, userId={}, groupIds={}", userId, groupIds);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ResetPasswordView resetPassword(Long userId) {
		SysUser user = userMapper.selectById(userId);
		if (user == null) {
			throw new BusinessException("error.user.not_found");
		}
		if (UserStatus.DELETED.equals(user.getStatus())) {
			throw new BusinessException("error.user.deleted");
		}

		String newPassword = generateRandomPassword();
		String encryptedPassword = CryptoUtil.bcryptEncrypt(newPassword);

		SysAccount passwordAccount = accountMapper.selectByUserId(userId).stream()
				.filter(a -> AuthType.PASSWORD.equals(a.getAuthType())).findFirst().orElse(null);
		if (passwordAccount == null) {
			// Created when the account has no password: the phone claim is preferred,
			// falling back to the user ID
			SysIdentity phoneClaim = StringUtils.hasText(user.getPhone())
					? identityService.findByTypeValue(IdentityType.PHONE, user.getPhone())
					: null;
			String identity = phoneClaim != null ? phoneClaim.getIdentityValue() : "user_" + userId;
			passwordAccount = new SysAccount();
			passwordAccount.setUserId(user.getId());
			passwordAccount.setIdentity(identity);
			passwordAccount.setAuthType(AuthType.PASSWORD);
			passwordAccount.setCredential(new PasswordCredential(encryptedPassword));
			passwordAccount.setStatus(PasswordStatus.ENABLE.getCode());
			accountMapper.create(passwordAccount);
		} else {
			passwordAccount.setCredential(new PasswordCredential(encryptedPassword));
			passwordAccount.setStatus(PasswordStatus.ENABLE.getCode());
			accountMapper.update(passwordAccount);
		}

		boolean emailSent = false;
		if (StringUtils.hasText(user.getEmail())) {
			// The mail body carries the one-time password, but the sending implementation
			// only records recipient and subject, never logging or storing it
			emailSent = emailService.sendMail(user.getEmail(), "密码重置通知",
					"您的登录密码已被管理员重置，本次新密码为：" + newPassword + "。请登录后立即修改密码。");
		}

		log.info("重置密码成功, userId={}, emailSent={}", userId, emailSent);

		ResetPasswordView view = new ResetPasswordView();
		view.setUserId(userId);
		view.setEmailSent(emailSent);
		view.setPassword(newPassword);
		return view;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public SysUser createSimpleUser(String nickname, String email) {
		SysUser user = new SysUser();
		user.setNickname(nickname);
		user.setEmail(email);
		user.setStatus(UserStatus.ENABLE);
		Long currentUserId = SecurityUtil.getCurrentUserIdOrNull();
		user.setCreator(currentUserId);
		user.setUpdater(currentUserId);
		user.setCreateTime(LocalDateTime.now());
		userMapper.create(user);
		if (StringUtils.hasText(email)) {
			identityService.findOrCreateVerified(IdentityType.EMAIL, email, null, user.getId(),
					IdentityVerifier.OAUTH2);
		}
		log.info("创建简单用户成功, userId={}, nickname={}", user.getId(), nickname);
		return user;
	}

	/**
	 * Validates that an identity claim is not taken by another user.
	 */
	private void assertClaimFree(IdentityType type, String value) {
		SysIdentity existing = identityService.findByTypeValue(type, value);
		if (existing != null) {
			throw new BusinessException(occupiedKey(type));
		}
	}

	/**
	 * Synchronises the phone and email identity claims. A null newValue means no
	 * change, an empty string removes the claim and clears the field, and the taken
	 * check excludes the user itself when it changes.
	 */
	private void syncClaim(SysUser user, String newValue, String oldValue, IdentityType type) {
		if (newValue == null || newValue.equals(oldValue)) {
			return;
		}
		if (StringUtils.hasText(newValue)) {
			SysIdentity existing = identityService.findByTypeValue(type, newValue);
			if (existing != null && !existing.getUserId().equals(user.getId())) {
				throw new BusinessException(occupiedKey(type));
			}
			identityService.findOrCreateVerified(type, newValue, null, user.getId(), IdentityVerifier.ADMIN);
		} else {
			if (IdentityType.PHONE.equals(type)) {
				user.setPhone(null);
			} else {
				user.setEmail(null);
			}
		}
		if (StringUtils.hasText(oldValue)) {
			SysIdentity oldClaim = identityService.findByTypeValue(type, oldValue);
			if (oldClaim != null && oldClaim.getUserId().equals(user.getId())) {
				identityService.deleteById(oldClaim.getId());
			}
		}
	}

	private String occupiedKey(IdentityType type) {
		return IdentityType.PHONE.equals(type) ? "error.identity.phone_occupied" : "error.identity.email_occupied";
	}

	private String generateRandomPassword() {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
		StringBuilder sb = new StringBuilder(12);
		for (int i = 0; i < 12; i++) {
			sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
		}
		return sb.toString();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public SysUser createRawUser(SysUser user) {
		userMapper.create(user);
		return user;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateRawUser(SysUser user) {
		SysUser check = userMapper.selectById(user.getId());
		if (check == null || UserStatus.DELETED.equals(check.getStatus())) {
			throw new BusinessException("error.user.not_found");
		}
		userMapper.update(user);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateStatus(Long userId, BaseStatus status) {
		SysUser user = userMapper.selectById(userId);
		if (user == null || UserStatus.DELETED.equals(user.getStatus())) {
			throw new BusinessException("error.user.not_found");
		}

		if (status.equals(BaseStatus.ENABLE)) {
			user.enable();
		} else {
			user.disable();
		}

		userMapper.updateStatus(userId, user.getStatus().getCode(), user.getUpdateTime());

		log.info("更新用户状态成功, userId={}, enabled={}", userId, status.getName());
	}
}
