package top.ruilink.inkwash.security.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import top.ruilink.inkwash.security.api.param.ChangePasswordParam;
import top.ruilink.inkwash.security.api.param.ProfileParam;
import top.ruilink.inkwash.security.api.param.ResetPasswordParam;
import top.ruilink.inkwash.security.api.view.LinkedAccountView;
import top.ruilink.inkwash.security.service.ProfileService;
import top.ruilink.inkwash.system.api.view.PermissionView;
import top.ruilink.inkwash.system.api.view.UserView;

/**
 * ProfileController unit tests
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

	@Mock
	private ProfileService profileService;

	@InjectMocks
	private ProfileController profileController;

	private UserView testUserView;

	@BeforeEach
	void setUp() {
		testUserView = new UserView();
		testUserView.setId(1L);
		testUserView.setNickname("admin");
		testUserView.setEmail("admin@inkwash.com");
	}

	@Nested
	@DisplayName("Profile Tests")
	class ProfileTests {

		@Test
		@DisplayName("get profile success")
		void getProfile_Success() {
			when(profileService.getProfile()).thenReturn(testUserView);

			ResponseEntity<UserView> response = profileController.getProfile();

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
			assertEquals("admin", response.getBody().getNickname());
		}

		@Test
		@DisplayName("get permissions success")
		void getPermissions_Success() {
			Set<PermissionView> perms = new HashSet<>();
			PermissionView perm = new PermissionView();
			perm.setAuthority("ROLE_ADMIN");
			perm.setResource("system");
			perm.setAction("read");
			perms.add(perm);

			when(profileService.getPermissions()).thenReturn(perms);

			ResponseEntity<Set<PermissionView>> response = profileController.getPermissions();

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
			assertEquals(1, response.getBody().size());
		}

		@Test
		@DisplayName("update profile success")
		void updateProfile_Success() {
			ProfileParam param = new ProfileParam();
			param.setNickname("admin");
			param.setAvatar("/uploads/avatars/1.png");

			ResponseEntity<Void> response = profileController.updateProfile(param);

			assertEquals(200, response.getStatusCode().value());
			verify(profileService).updateProfile(param);
		}
	}

	@Nested
	@DisplayName("Password Tests")
	class PasswordTests {

		@Test
		@DisplayName("update password success")
		void updatePassword_Success() {
			ChangePasswordParam param = new ChangePasswordParam("old", "new", "new");

			ResponseEntity<Void> response = profileController.updatePassword(param);

			assertEquals(200, response.getStatusCode().value());
		}

		@Test
		@DisplayName("reset password success")
		void resetPassword_Success() {
			ResetPasswordParam param = new ResetPasswordParam("13800138000", "123456", "new", "new");

			ResponseEntity<Void> response = profileController.resetPassword(param);

			assertEquals(200, response.getStatusCode().value());
		}
	}

	@Nested
	@DisplayName("Account Tests")
	class AccountTests {

		@Test
		@DisplayName("list accounts success")
		void listAccounts_Success() {
			LinkedAccountView view = new LinkedAccountView(1L, "PHONE", "13800138000", null, 1, "SMS_CODE");

			when(profileService.listAccounts()).thenReturn(List.of(view));

			ResponseEntity<List<LinkedAccountView>> response = profileController.listAccounts();

			assertEquals(200, response.getStatusCode().value());
			assertNotNull(response.getBody());
			assertEquals(1, response.getBody().size());
			assertEquals("PHONE", response.getBody().getFirst().type());
		}

		@Test
		@DisplayName("unbind account delegates to service")
		void unbindAccount_Delegates() {
			ResponseEntity<Void> response = profileController.unbindAccount(3L, "IDENTITY");

			assertEquals(204, response.getStatusCode().value());
			verify(profileService).unbindAccount(3L, "IDENTITY");
		}
	}
}
