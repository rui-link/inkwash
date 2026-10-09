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

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.system.domain.SysIdentity;
import top.ruilink.inkwash.system.mapper.IdentityMapper;
import top.ruilink.inkwash.system.service.IdentityService;

/**
 * Identity claim service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class IdentityServiceImpl implements IdentityService {

	private final IdentityMapper identityMapper;

	public IdentityServiceImpl(IdentityMapper identityMapper) {
		this.identityMapper = identityMapper;
	}

	@Override
	public SysIdentity findByTypeValue(IdentityType identityType, String identityValue) {
		return identityMapper.selectByTypeValue(identityType, identityValue);
	}

	@Override
	public SysIdentity findByTypeProviderValue(IdentityType identityType, String provider, String identityValue) {
		return identityMapper.selectByTypeProviderValue(identityType, provider, identityValue);
	}

	@Override
	public List<SysIdentity> listByUserId(Long userId) {
		return identityMapper.selectByUserId(userId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public SysIdentity create(SysIdentity identity) {
		LocalDateTime now = LocalDateTime.now();
		identity.setCreateTime(now);
		identity.setUpdateTime(now);
		try {
			identityMapper.create(identity);
			return identity;
		} catch (DuplicateKeyException e) {
			// Concurrent creation: on a unique key conflict, re-read and return the
			// existing row and let the caller decide ownership
			SysIdentity existing = findByTypeProviderValue(identity.getIdentityType(), identity.getProvider(),
					identity.getIdentityValue());
			if (existing == null) {
				existing = findByTypeValue(identity.getIdentityType(), identity.getIdentityValue());
			}
			if (existing == null) {
				throw e;
			}
			return existing;
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysIdentity identity) {
		identity.setUpdateTime(LocalDateTime.now());
		identityMapper.update(identity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteById(Long id) {
		identityMapper.deleteById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public SysIdentity findOrCreateVerified(IdentityType type, String value, String provider, Long userId,
			IdentityVerifier by) {
		SysIdentity existing = (provider == null) ? findByTypeValue(type, value)
				: findByTypeProviderValue(type, provider, value);
		if (existing != null) {
			if (existing.getUserId().equals(userId)) {
				if (!existing.identityVerified()) {
					existing.markVerified(by);
					update(existing);
				}
				return existing;
			}
			throw new BusinessException(occupiedKey(type));
		}
		SysIdentity identity = new SysIdentity();
		identity.setUserId(userId);
		identity.setIdentityType(type);
		identity.setIdentityValue(value);
		identity.setProvider(provider);
		identity.markVerified(by);
		return create(identity);
	}

	private String occupiedKey(IdentityType type) {
		return switch (type) {
		case PHONE -> "error.identity.phone_occupied";
		case EMAIL -> "error.identity.email_occupied";
		case OIDC_SUB -> "error.identity.occupied";
		};
	}
}
