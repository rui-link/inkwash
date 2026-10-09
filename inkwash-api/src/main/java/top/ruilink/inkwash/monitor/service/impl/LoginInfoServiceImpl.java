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
package top.ruilink.inkwash.monitor.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageImpl;
import top.ruilink.inkwash.base.util.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.domain.PageResult;
import top.ruilink.inkwash.monitor.api.query.LoginInfoQuery;
import top.ruilink.inkwash.monitor.api.view.LoginInfoView;
import top.ruilink.inkwash.monitor.domain.LoginInfo;
import top.ruilink.inkwash.monitor.mapper.LoginInfoMapper;
import top.ruilink.inkwash.monitor.service.LoginInfoService;
import top.ruilink.inkwash.monitor.service.converter.LoginInfoConverter;

/**
 * Login log service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class LoginInfoServiceImpl implements LoginInfoService {

	private final LoginInfoMapper loginInfoMapper;

	public LoginInfoServiceImpl(LoginInfoMapper loginInfoMapper) {
		this.loginInfoMapper = loginInfoMapper;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long recordLoginInfo(LoginInfo loginInfo) {
		if (loginInfo.getLoginTime() == null) {
			loginInfo.setLoginTime(LocalDateTime.now());
		}
		return loginInfoMapper.create(loginInfo);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int recordLogout(Long userId) {
		if (userId == null) {
			return 0;
		}
		return loginInfoMapper.closeLatestOpenSession(userId, LocalDateTime.now());
	}

	@Override
	public LoginInfo getById(Long id) {
		return loginInfoMapper.selectById(id);
	}

	@Override
	public PageResult<LoginInfoView> listLoginInfos(LoginInfoQuery query) {
		var offset = (long) (query.getPage() - 1) * query.getSize();
		var total = loginInfoMapper.count(query.getIdentity(), query.getStatus(), query.getStartTime(),
				query.getEndTime());
		var list = loginInfoMapper.selectPage(query.getIdentity(), query.getStatus(), query.getStartTime(),
				query.getEndTime(), (int) offset, query.getSize());
		var views = list.stream().map(LoginInfoConverter::toView).toList();
		var pageable = PageUtil.of(query.getPage(), query.getSize());
		var page = new PageImpl<>(views, pageable, total);
		return PageResult.of(page);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteById(Long id) {
		loginInfoMapper.deleteById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByIds(List<Long> ids) {
		loginInfoMapper.deleteByIds(ids);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteAll() {
		loginInfoMapper.deleteAll();
	}

	@Override
	public List<LoginInfo> listRecent(int limit) {
		return loginInfoMapper.selectRecent(limit);
	}
}
