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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Set password payload for passwordless users such as OAuth2 or SMS
 * registrations.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public record SetPasswordParam(@NotBlank(message = "用户名不能为空") String username,

		@NotBlank(message = "邮箱验证码不能为空") String emailCode,

		@NotBlank(message = "新密码不能为空") @Size(min = 6, max = 100, message = "新密码长度为6-100位") String newPassword,

		@NotBlank(message = "确认密码不能为空") String confirmPassword) implements PasswordChange {
}
