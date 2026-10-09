package top.ruilink.inkwash.system.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import top.ruilink.inkwash.system.api.view.ResetPasswordView;
import top.ruilink.inkwash.system.service.UserService;

/**
 * User endpoint unit tests for enum options, counts, and password reset.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	@Mock
	private UserService userService;

	@InjectMocks
	private UserController userController;

	@Test
	@DisplayName("GET /gender - 返回性别枚举选项")
	void listGenderOptions_ReturnsAllGenders() {
		ResponseEntity<Map<Object, Object>> response = userController.listGenderOptions();

		assertEquals(200, response.getStatusCode().value());
		Map<Object, Object> body = response.getBody();
		assertNotNull(body);
		assertEquals(3, body.size());
		assertEquals("男", body.get(1));
		assertEquals("女", body.get(2));
		assertEquals("未知", body.get(0));
	}

	@Test
	@DisplayName("GET /education - 返回学历枚举选项")
	void listEducationOptions_ReturnsAllEducationLevels() {
		ResponseEntity<Map<Object, Object>> response = userController.listEducationOptions();

		assertEquals(200, response.getStatusCode().value());
		Map<Object, Object> body = response.getBody();
		assertNotNull(body);
		assertEquals("本科", body.get(6));
		assertEquals("博士", body.get(8));
		assertEquals("博士后", body.get(9));
		assertEquals(10, body.size());
	}

	@Test
	@DisplayName("GET /count - 返回用户总数")
	void getUserCount_ReturnsUserCount() {
		when(userService.count()).thenReturn(42L);

		ResponseEntity<Map<String, Long>> response = userController.getUserCount();

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals(Map.of("count", 42L), response.getBody());
	}

	@Test
	@DisplayName("POST /{id}/reset-password - 200且响应体包含一次性密码与emailSent")
	void resetPassword_ReturnsOneTimePasswordView() throws Exception {
		ResetPasswordView view = new ResetPasswordView();
		view.setUserId(1L);
		view.setEmailSent(true);
		view.setPassword("Abcd1234!wxyz");
		when(userService.resetPassword(1L)).thenReturn(view);

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

		mockMvc.perform(post("/api/system/users/1/reset-password")).andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(1)).andExpect(jsonPath("$.emailSent").value(true))
				.andExpect(jsonPath("$.password").value("Abcd1234!wxyz"));
	}
}
