package top.ruilink.inkwash.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import tools.jackson.databind.json.JsonMapper;

import top.ruilink.inkwash.security.api.param.PreferenceParam;
import top.ruilink.inkwash.security.api.view.PreferenceView;
import top.ruilink.inkwash.system.domain.SysPreference;
import top.ruilink.inkwash.system.mapper.PreferenceMapper;

/**
 * Preference service unit tests for loading, defaulting, and saving user
 * display options.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
class PreferenceServiceImplTest {

	@Mock
	private PreferenceMapper preferenceMapper;

	@Spy
	private JsonMapper objectMapper = JsonMapper.builder().build();

	@InjectMocks
	private PreferenceServiceImpl preferenceService;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1", null));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("无记录时返回默认偏好")
	void getPreference_noRecord_returnsDefaults() {
		when(preferenceMapper.selectByUserId(1L)).thenReturn(null);

		PreferenceView view = preferenceService.getPreference();

		assertEquals("sky-blue", view.getTheme());
		assertEquals("zh-CN", view.getLanguage());
		assertEquals("left", view.getMenuStyle());
		assertTrue(view.isShowTabs());
		assertTrue(view.isShowLogo());
		assertTrue(view.isShowFooter());
		assertTrue(view.isFixHeader());
	}

	@Test
	@DisplayName("有记录时返回存储值")
	void getPreference_withRecord_returnsStoredValues() {
		SysPreference pref = new SysPreference();
		pref.setUserId(1L);
		pref.setTheme("ink-dark");
		pref.setLanguage("en");
		pref.setMenuStyle("top");
		pref.setOptions("{\"showTabs\":false,\"showLogo\":true,\"showFooter\":true,\"fixHeader\":true}");
		when(preferenceMapper.selectByUserId(1L)).thenReturn(pref);

		PreferenceView view = preferenceService.getPreference();

		assertEquals("ink-dark", view.getTheme());
		assertEquals("en", view.getLanguage());
		assertEquals("top", view.getMenuStyle());
		assertFalse(view.isShowTabs());
		assertTrue(view.isShowLogo());
	}

	@Test
	@DisplayName("options 为空或非法时布尔用默认值")
	void getPreference_badOptions_usesDefaults() {
		SysPreference pref = new SysPreference();
		pref.setUserId(1L);
		pref.setTheme("sky-blue");
		pref.setLanguage("zh-CN");
		pref.setMenuStyle("left");
		pref.setOptions("not-json");
		when(preferenceMapper.selectByUserId(1L)).thenReturn(pref);

		PreferenceView view = preferenceService.getPreference();

		assertTrue(view.isShowTabs());
		assertTrue(view.isFixHeader());
	}

	@Test
	@DisplayName("savePreference 无记录时新增")
	void savePreference_noRecord_inserts() {
		when(preferenceMapper.selectByUserId(1L)).thenReturn(null);

		PreferenceParam param = new PreferenceParam();
		param.setTheme("sky-blue");
		param.setLanguage("zh-CN");
		param.setMenuStyle("left");
		param.setShowTabs(true);
		param.setShowLogo(true);
		param.setShowFooter(true);
		param.setFixHeader(true);

		preferenceService.savePreference(param);

		verify(preferenceMapper).insert(any(SysPreference.class));
		verify(preferenceMapper, never()).update(any(SysPreference.class));
	}

	@Test
	@DisplayName("savePreference 已有记录时更新")
	void savePreference_existing_updates() {
		SysPreference existing = new SysPreference();
		existing.setUserId(1L);
		when(preferenceMapper.selectByUserId(1L)).thenReturn(existing);

		PreferenceParam param = new PreferenceParam();
		param.setTheme("ink-dark");
		param.setLanguage("en");
		param.setMenuStyle("top");
		param.setShowTabs(false);

		preferenceService.savePreference(param);

		verify(preferenceMapper, never()).insert(any(SysPreference.class));
		verify(preferenceMapper).update(any(SysPreference.class));
	}

	@Test
	@DisplayName("savePreference 写入的 options 为合法 JSON")
	void savePreference_writesValidJsonOptions() {
		when(preferenceMapper.selectByUserId(1L)).thenReturn(null);

		PreferenceParam param = new PreferenceParam();
		param.setTheme("sky-blue");
		param.setLanguage("zh-CN");
		param.setMenuStyle("left");
		param.setShowTabs(false);

		preferenceService.savePreference(param);

		org.mockito.ArgumentCaptor<SysPreference> captor = org.mockito.ArgumentCaptor.forClass(SysPreference.class);
		verify(preferenceMapper).insert(captor.capture());
		String json = captor.getValue().getOptions();
		assertTrue(json.contains("\"showTabs\":false"));
		new JsonMapper().valueToTree(json);
	}
}
