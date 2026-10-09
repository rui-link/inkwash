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
package top.ruilink.inkwash.security.service;

/**
 * Email verification code service.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface EmailService {

	/**
	 * Sends a verification code to the given address and returns it, returned
	 * directly during development to ease integration testing.
	 */
	String sendCode(String email);

	/**
	 * Sends a general purpose email over the real SMTP channel.
	 *
	 * <p>
	 * Returns false and logs a warning when mail is disabled, meaning
	 * spring.mail.host is unset or mail.enabled is false, or when sending fails,
	 * never throwing to the caller.
	 * </p>
	 */
	boolean sendMail(String to, String subject, String text);

	boolean verifyCode(String email, String code);

	void clearCode(String email);
}
