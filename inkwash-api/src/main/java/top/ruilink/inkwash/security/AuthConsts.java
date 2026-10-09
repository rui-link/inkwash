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
package top.ruilink.inkwash.security;

/**
 * Authentication related constants.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface AuthConsts {
	// Captcha code prefix
	String CAPTCHA_PREFIX = "auth:captcha:";
	// SMS verification code prefix
	String SMS_CODE_PREFIX = "sms:code:";
	// Login lock prefix
	String LOGIN_LOCK_PREFIX = "login:lock:";
	// Failed login counter prefix
	String LOGIN_ATTEMPT_PREFIX = "login:attempt:";
	// Login lock expiry prefix
	String LOGIN_LOCKTIME_PREFIX = "login:locktime:";
}
