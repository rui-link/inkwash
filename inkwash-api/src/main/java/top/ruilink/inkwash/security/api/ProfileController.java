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
package top.ruilink.inkwash.security.api;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonView;
import top.ruilink.inkwash.base.domain.ResultView;
import top.ruilink.inkwash.security.api.param.ChangePasswordParam;
import top.ruilink.inkwash.security.api.param.ProfileParam;
import top.ruilink.inkwash.security.api.param.ResetPasswordParam;
import top.ruilink.inkwash.security.api.param.SendEmailCodeParam;
import top.ruilink.inkwash.security.api.param.SendPhoneCodeParam;
import top.ruilink.inkwash.security.api.param.SetPasswordParam;
import top.ruilink.inkwash.security.api.param.VerifyEmailParam;
import top.ruilink.inkwash.security.api.param.VerifyPhoneParam;
import top.ruilink.inkwash.security.api.view.LinkedAccountView;
import top.ruilink.inkwash.security.service.ProfileService;
import top.ruilink.inkwash.system.api.view.UserView;
import top.ruilink.inkwash.system.api.view.PermissionView;

/**
 * User profile controller.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<UserView> getProfile() {
		UserView profile = profileService.getProfile();
		return ResponseEntity.ok(profile);
	}

	@PutMapping
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> updateProfile(@Valid @RequestBody ProfileParam param) {
		profileService.updateProfile(param);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/permissions")
	@JsonView(ResultView.Detail.class)
	public ResponseEntity<Set<PermissionView>> getPermissions() {
		Set<PermissionView> permissions = profileService.getPermissions();
		return ResponseEntity.ok(permissions);
	}

	@PutMapping("/password")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> updatePassword(@Valid @RequestBody ChangePasswordParam passwordParam) {
		profileService.updatePassword(passwordParam);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/password/reset")
	@PreAuthorize("hasAuthority('system:account:update')")
	public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordParam passwordParam) {
		profileService.resetPassword(passwordParam);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/password/set")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> setPassword(@Valid @RequestBody SetPasswordParam param) {
		profileService.setPassword(param);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/accounts")
	public ResponseEntity<List<LinkedAccountView>> listAccounts() {
		return ResponseEntity.ok(profileService.listAccounts());
	}

	@DeleteMapping("/accounts/{id}")
	@PreAuthorize("hasAuthority('system:account:update')")
	public ResponseEntity<Void> unbindAccount(@PathVariable Long id, @RequestParam String kind) {
		profileService.unbindAccount(id, kind);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/phone/code")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> sendPhoneVerifyCode(@Valid @RequestBody SendPhoneCodeParam param) {
		profileService.sendPhoneVerifyCode(param.phone());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/phone/verify")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> verifyPhone(@Valid @RequestBody VerifyPhoneParam param) {
		profileService.verifyPhone(param);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/email/code")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> sendEmailVerifyCode(@Valid @RequestBody SendEmailCodeParam param) {
		profileService.sendEmailVerifyCode(param.email());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/email/verify")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailParam param) {
		profileService.verifyEmail(param);
		return ResponseEntity.ok().build();
	}
}
