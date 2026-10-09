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

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.system.domain.SysAccount;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.domain.credential.OAuth2Credential;
import top.ruilink.inkwash.system.mapper.AccountMapper;
import top.ruilink.inkwash.system.service.AccountService;
import top.ruilink.inkwash.system.service.IdentityService;

/**
 * Login account service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Service
public class AccountServiceImpl implements AccountService {

	private final AccountMapper accountMapper;
	private final IdentityService identityService;

	public AccountServiceImpl(AccountMapper accountMapper, IdentityService identityService) {
		this.accountMapper = accountMapper;
		this.identityService = identityService;
	}

	@Override
	public SysAccount findByIdentityAndType(String identity, AuthType authType) {
		return accountMapper.selectByIdentityAndType(identity, authType);
	}

	@Override
	public SysAccount findByProviderAndOpenId(String provider, String openId) {
		if (provider == null || openId == null) {
			return null;
		}
		SysIdentity claim = identityService.findByTypeProviderValue(IdentityType.OIDC_SUB, provider.toLowerCase(),
				openId);
		if (claim == null) {
			return null;
		}
		List<SysAccount> accounts = accountMapper.selectByUserId(claim.getUserId());
		if (accounts == null) {
			return null;
		}
		for (SysAccount account : accounts) {
			if (account.getCredential() instanceof OAuth2Credential oauth2Credential
					&& oauth2Credential.openId() != null && oauth2Credential.openId().equals(openId)
					&& oauth2Credential.provider() != null
					&& oauth2Credential.provider().name().equalsIgnoreCase(provider)) {
				return account;
			}
		}
		return null;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long create(SysAccount account) {
		return accountMapper.create(account);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysAccount account) {
		accountMapper.update(account);
	}

	@Override
	public SysAccount getById(Long id) {
		return accountMapper.selectById(id);
	}

	@Override
	public List<SysAccount> listByUserId(Long userId) {
		return accountMapper.selectByUserId(userId);
	}

	@Override
	public int countByUserId(Long userId) {
		return accountMapper.countByUserId(userId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteById(Long id) {
		accountMapper.deleteById(id);
	}
}
