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
package top.ruilink.inkwash.system.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import top.ruilink.inkwash.security.api.param.PreferenceParam;
import top.ruilink.inkwash.security.api.view.PreferenceView;
import top.ruilink.inkwash.security.util.SecurityUtil;
import top.ruilink.inkwash.system.domain.SysPreference;
import top.ruilink.inkwash.system.mapper.PreferenceMapper;
import top.ruilink.inkwash.system.service.PreferenceService;

/**
 * User preference service implementation.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Service
public class PreferenceServiceImpl implements PreferenceService {

	public static final String DEFAULT_THEME = "sky-blue";
	public static final String DEFAULT_LANGUAGE = "zh-CN";
	public static final String DEFAULT_MENU_STYLE = "left";

	private final PreferenceMapper preferenceMapper;
	private final ObjectMapper objectMapper;

	public PreferenceServiceImpl(PreferenceMapper preferenceMapper, ObjectMapper objectMapper) {
		this.preferenceMapper = preferenceMapper;
		this.objectMapper = objectMapper;
	}

	@Override
	public PreferenceView getPreference() {
		Long userId = SecurityUtil.getCurrentUserId();
		SysPreference pref = preferenceMapper.selectByUserId(userId);
		PreferenceView view = new PreferenceView();
		view.setTheme(pref != null && pref.getTheme() != null ? pref.getTheme() : DEFAULT_THEME);
		view.setLanguage(pref != null && pref.getLanguage() != null ? pref.getLanguage() : DEFAULT_LANGUAGE);
		view.setMenuStyle(pref != null && pref.getMenuStyle() != null ? pref.getMenuStyle() : DEFAULT_MENU_STYLE);
		boolean[] defaults = new boolean[] { true, true, true, true };
		boolean[] booleans = defaults;
		if (pref != null && pref.getOptions() != null && !pref.getOptions().isBlank()) {
			try {
				JsonNode node = objectMapper.readTree(pref.getOptions());
				booleans = new boolean[] { node.path("showTabs").isBoolean() ? node.path("showTabs").asBoolean() : true,
						node.path("showLogo").isBoolean() ? node.path("showLogo").asBoolean() : true,
						node.path("showFooter").isBoolean() ? node.path("showFooter").asBoolean() : true,
						node.path("fixHeader").isBoolean() ? node.path("fixHeader").asBoolean() : true };
			} catch (Exception e) {
				booleans = defaults;
			}
		}
		view.setShowTabs(booleans[0]);
		view.setShowLogo(booleans[1]);
		view.setShowFooter(booleans[2]);
		view.setFixHeader(booleans[3]);
		return view;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void savePreference(PreferenceParam param) {
		Long userId = SecurityUtil.getCurrentUserId();
		SysPreference pref = preferenceMapper.selectByUserId(userId);
		boolean showTabs = param.getShowTabs() != null ? param.getShowTabs() : true;
		boolean showLogo = param.getShowLogo() != null ? param.getShowLogo() : true;
		boolean showFooter = param.getShowFooter() != null ? param.getShowFooter() : true;
		boolean fixHeader = param.getFixHeader() != null ? param.getFixHeader() : true;

		String optionsJson = null;
		if (!showTabs || !showLogo || !showFooter || !fixHeader) {
			try {
				JsonNode node = objectMapper.createObjectNode().put("showTabs", showTabs).put("showLogo", showLogo)
						.put("showFooter", showFooter).put("fixHeader", fixHeader);
				optionsJson = objectMapper.writeValueAsString(node);
			} catch (Exception e) {
				throw new RuntimeException("序列化偏好选项失败", e);
			}
		}

		if (pref == null) {
			SysPreference newPref = new SysPreference();
			newPref.setUserId(userId);
			newPref.setTheme(param.getTheme() != null ? param.getTheme() : DEFAULT_THEME);
			newPref.setLanguage(param.getLanguage() != null ? param.getLanguage() : DEFAULT_LANGUAGE);
			newPref.setMenuStyle(param.getMenuStyle() != null ? param.getMenuStyle() : DEFAULT_MENU_STYLE);
			newPref.setOptions(optionsJson);
			newPref.setCreateTime(LocalDateTime.now());
			newPref.setUpdateTime(LocalDateTime.now());
			preferenceMapper.insert(newPref);
		} else {
			pref.setTheme(param.getTheme() != null ? param.getTheme() : pref.getTheme());
			pref.setLanguage(param.getLanguage() != null ? param.getLanguage() : pref.getLanguage());
			pref.setMenuStyle(param.getMenuStyle() != null ? param.getMenuStyle() : pref.getMenuStyle());
			pref.setOptions(optionsJson);
			pref.setUpdateTime(LocalDateTime.now());
			preferenceMapper.update(pref);
		}
	}
}
