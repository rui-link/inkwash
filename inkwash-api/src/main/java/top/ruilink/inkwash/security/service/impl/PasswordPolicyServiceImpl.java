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

import java.util.regex.Pattern;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.exception.BusinessException;
import top.ruilink.inkwash.security.service.PasswordPolicyService;

/**
 * Password policy service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

	private static final int MIN_LENGTH = 8;

	private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
	private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
	private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
	private static final Pattern SPECIAL = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

	@Override
	public void validateStrength(String password) {
		if (password == null || password.length() < MIN_LENGTH) {
			throw new BusinessException("error.password.strength.length");
		}
		if (!UPPER.matcher(password).matches()) {
			throw new BusinessException("error.password.strength.uppercase");
		}
		if (!LOWER.matcher(password).matches()) {
			throw new BusinessException("error.password.strength.lowercase");
		}
		if (!DIGIT.matcher(password).matches()) {
			throw new BusinessException("error.password.strength.digit");
		}
		if (!SPECIAL.matcher(password).matches()) {
			throw new BusinessException("error.password.strength.special");
		}
	}

	@Override
	public void validateHistory(String oldPasswordHash, String newPassword) {
		if (oldPasswordHash != null && BCrypt.checkpw(newPassword, oldPasswordHash)) {
			throw new BusinessException("error.password.history");
		}
	}
}
