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
package top.ruilink.inkwash.security.api.param;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import top.ruilink.inkwash.base.enums.AuthType;

/**
 * Login request payload dispatched to typed variants by auth type.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "authType", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({ @JsonSubTypes.Type(value = PasswordLoginParam.class, name = "1"),
		@JsonSubTypes.Type(value = SmsLoginParam.class, name = "2"),
		@JsonSubTypes.Type(value = OAuth2LoginParam.class, name = "3"),
		@JsonSubTypes.Type(value = QrLoginParam.class, name = "4") })
public sealed interface LoginParam permits PasswordLoginParam, SmsLoginParam, QrLoginParam, OAuth2LoginParam {

	/** Login identifier: username, phone number, OpenID, or QR code ID. */
	@NotBlank
	String identity();

	/** Login type. */
	AuthType authType();
}
