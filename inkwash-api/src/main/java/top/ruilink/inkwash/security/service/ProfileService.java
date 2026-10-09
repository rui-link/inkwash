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

import java.util.List;
import java.util.Set;

import top.ruilink.inkwash.security.api.param.ChangePasswordParam;
import top.ruilink.inkwash.security.api.param.ProfileParam;
import top.ruilink.inkwash.security.api.param.ResetPasswordParam;
import top.ruilink.inkwash.security.api.param.SetPasswordParam;
import top.ruilink.inkwash.security.api.param.VerifyEmailParam;
import top.ruilink.inkwash.security.api.param.VerifyPhoneParam;
import top.ruilink.inkwash.security.api.view.LinkedAccountView;
import top.ruilink.inkwash.system.api.view.PermissionView;
import top.ruilink.inkwash.system.api.view.UserView;

/**
 * User profile service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface ProfileService {

	/**
	 * Gets the current user information.
	 */
	UserView getProfile();

	/**
	 * Updates the current user profile partially, writing only the supplied fields;
	 * phone and email go through the verification endpoints.
	 */
	void updateProfile(ProfileParam param);

	/**
	 * Gets the current user permissions.
	 */
	Set<PermissionView> getPermissions();

	/**
	 * Changes the password.
	 */
	void updatePassword(ChangePasswordParam param);

	/**
	 * Resets the password, setting a new one after SMS verification.
	 */
	void resetPassword(ResetPasswordParam param);

	/**
	 * Gets the current user ID.
	 */
	Long getCurrentUserId();

	/**
	 * Gets the current username.
	 */
	String getCurrentUsername();

	/**
	 * Lets a passwordless user set a login password with an email verification
	 * code.
	 */
	void setPassword(SetPasswordParam param);

	/**
	 * Sends a phone verification code, rejecting numbers already taken by another
	 * user.
	 */
	void sendPhoneVerifyCode(String phone);

	/**
	 * Verifies the phone number and creates or confirms the PHONE identity claim.
	 */
	void verifyPhone(VerifyPhoneParam param);

	/**
	 * Sends an email verification code, rejecting addresses already taken by
	 * another user.
	 */
	void sendEmailVerifyCode(String email);

	/**
	 * Verifies the email and creates or confirms the EMAIL identity claim.
	 */
	void verifyEmail(VerifyEmailParam param);

	/**
	 * The login methods currently bound to the user, covering verified PHONE and
	 * EMAIL claims plus all credential rows.
	 */
	List<LinkedAccountView> listAccounts();

	/**
	 * Unbinds a login method.
	 *
	 * @param id   the claim or credential row ID
	 * @param kind IDENTITY or ACCOUNT
	 */
	void unbindAccount(Long id, String kind);
}
