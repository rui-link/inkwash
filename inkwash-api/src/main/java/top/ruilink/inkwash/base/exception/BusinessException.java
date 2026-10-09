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
package top.ruilink.inkwash.base.exception;

import org.springframework.http.HttpStatus;

import top.ruilink.inkwash.base.util.LocaleUtil;

/**
 * Base class for business exceptions.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class BusinessException extends RuntimeException {
	private static final long serialVersionUID = -2866810700875898359L;
	private final Integer code;
	private final HttpStatus httpStatus;
	private final String messageKey;

	public BusinessException() {
		this(HttpStatus.BAD_REQUEST, (Integer) null, (String) null, (Throwable) null);
	}

	public BusinessException(String message) {
		this(HttpStatus.BAD_REQUEST, (Integer) null, message, (Throwable) null);
	}

	public BusinessException(String messageKey, Object... args) {
		this(HttpStatus.BAD_REQUEST, null, messageKey, null, args);
	}

	public BusinessException(HttpStatus httpStatus, String messageKey, Object... args) {
		this(httpStatus, null, messageKey, null, args);
	}

	public BusinessException(HttpStatus httpStatus, Integer errorCode, String messageKey, Object... args) {
		this(httpStatus, errorCode, messageKey, null, args);
	}

	public BusinessException(HttpStatus httpStatus, Integer errorCode, String messageKey, Throwable cause,
			Object... args) {
		super(messageKey != null ? LocaleUtil.getValue(messageKey, args) : null, cause);
		this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.BAD_REQUEST;
		this.code = errorCode;
		this.messageKey = messageKey;
	}

	public Integer getCode() {
		return code;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String getMessageKey() {
		return messageKey;
	}
}
