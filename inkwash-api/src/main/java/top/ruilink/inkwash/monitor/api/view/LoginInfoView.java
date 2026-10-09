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
package top.ruilink.inkwash.monitor.api.view;

import java.time.LocalDateTime;

import lombok.Data;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.monitor.enums.LoginStatus;

/**
 * Login log response view.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class LoginInfoView {
	private Long id;
	private Long userId;
	private String identity;
	private AuthType loginType;
	private String address;
	private String device;
	private String browser;
	private String ostype;
	private LoginStatus status;
	private String message;
	private LocalDateTime loginTime;

	private LocalDateTime logoutTime;
	private LocalDateTime createTime;
	private Long creator;
	private Long updater;
}
