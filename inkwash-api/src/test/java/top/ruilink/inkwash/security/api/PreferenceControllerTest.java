package top.ruilink.inkwash.security.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import top.ruilink.inkwash.security.api.param.PreferenceParam;
import top.ruilink.inkwash.security.api.view.PreferenceView;
import top.ruilink.inkwash.system.service.PreferenceService;

/**
 * PreferenceController unit tests
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class PreferenceControllerTest {

	@Mock
	private PreferenceService preferenceService;

	@InjectMocks
	private PreferenceController preferenceController;

	@Test
	@DisplayName("get preference success")
	void getPreference_Success() {
		PreferenceView view = new PreferenceView();
		view.setTheme("sky-blue");
		view.setMenuStyle("left");
		when(preferenceService.getPreference()).thenReturn(view);

		ResponseEntity<PreferenceView> response = preferenceController.getPreference();

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("sky-blue", response.getBody().getTheme());
	}

	@Test
	@DisplayName("save preference delegates to service")
	void savePreference_Delegates() {
		PreferenceParam param = new PreferenceParam();
		param.setTheme("ink-dark");
		param.setMenuStyle("top");

		ResponseEntity<Void> response = preferenceController.savePreference(param);

		assertEquals(200, response.getStatusCode().value());
		verify(preferenceService).savePreference(param);
	}
}
