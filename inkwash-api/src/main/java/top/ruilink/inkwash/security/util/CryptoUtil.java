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
package top.ruilink.inkwash.security.util;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Password hashing and verification helper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Component
public class CryptoUtil {
	private static PasswordEncoder passwordEncoder;

	public CryptoUtil(PasswordEncoder passwordEncoder) {
		CryptoUtil.passwordEncoder = passwordEncoder;
	}

	// ========== BCrypt hashing and matching, irreversible and used for passwords
	// and verification codes ==========
	public static String bcryptEncrypt(String content) {
		if (content == null || content.isBlank()) {
			return "";
		}
		return passwordEncoder.encode(content);
	}

	public static boolean bcryptMatches(String content, String otherContent) {
		if (content == null || otherContent == null) {
			return false;
		}
		return passwordEncoder.matches(content, otherContent);
	}
}
