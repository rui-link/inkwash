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
package top.ruilink.inkwash.monitor.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import top.ruilink.inkwash.monitor.enums.LoginStatus;
import top.ruilink.inkwash.base.enums.AuthType;

/**
 * Login log entity.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class LoginInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	/** Log ID */
	private Long id;

	/** User ID */
	private Long userId;

	/** User identifier */
	private String identity;

	/** Login type */
	private AuthType loginType;

	/** IP address */
	private String address;

	/** Login device, recorded when the login location cannot be resolved */
	private String device;

	/** Browser */
	private String browser;

	/** Operating system */
	private String ostype;

	/** Login outcome */
	private LoginStatus status;

	/** Failure reason */
	private String message;

	/** Login time */
	private LocalDateTime loginTime;
	private LocalDateTime logoutTime;

	/** Creation time */
	private LocalDateTime createTime;

	/** Created by */
	private Long creator;

	/** Updated by */
	private Long updater;
}
