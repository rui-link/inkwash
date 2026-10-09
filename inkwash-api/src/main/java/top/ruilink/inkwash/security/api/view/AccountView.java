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
package top.ruilink.inkwash.security.api.view;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.base.enums.AuthType;

/**
 * Account response view.
 *
 * <p>
 * Nested inside {@code UserView.accounts}, so every field declares its own
 * {@link JsonView} — otherwise the whole object collapses to {@code {}} under
 * any activated view, leaving an administrator unable to see which login
 * methods a user has.
 *
 * <p>
 * {@code credential} is deliberately left un-annotated. It is overloaded across
 * two call paths: {@code TokenServiceImpl} puts the raw {@code access:refresh}
 * token pair there for the login / refresh responses, while
 * {@code UserConverter} never sets it at all. Jackson only filters un-annotated
 * properties when a view <em>is</em> active, so leaving it bare keeps the
 * non-browser token transport working while guaranteeing it can never appear in
 * a projected response such as {@code GET /api/system/users/{id}}.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class AccountView {
	/**
	 * Account ID.
	 */
	@JsonView(ResultView.Basic.class)
	private Long id;
	/**
	 * User ID.
	 */
	@JsonView(ResultView.Basic.class)
	private Long userId;
	/**
	 * Username.
	 */
	@JsonView(ResultView.Basic.class)
	private String identity;
	/**
	 * Login type.
	 */
	@JsonView(ResultView.Basic.class)
	private AuthType authType;
	/**
	 * Login credential: the {@code access:refresh} token pair on the login /
	 * refresh path, and never populated by {@code UserConverter}. Intentionally
	 * carries no {@link JsonView} so it is dropped whenever an endpoint activates a
	 * view — see the class comment for why.
	 */
	private String credential;
	/**
	 * Account status, a string whose meaning is interpreted by each account type's
	 * business layer.
	 */
	@JsonView(ResultView.Basic.class)
	private String status;
	/**
	 * Validity period.
	 */
	@JsonView(ResultView.Basic.class)
	private LocalDateTime expireTime;
	/**
	 * Last login time.
	 */
	@JsonView(ResultView.Basic.class)
	private LocalDateTime loginTime;
}
