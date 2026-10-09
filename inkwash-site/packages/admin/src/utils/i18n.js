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
import i18n from '@/locale/index';

import { MENU_TITLE_KEYS } from './menuKeys';

// 后端路由 meta.title 为菜单 menu.name（中文，来自 DB）。
// 优先经 MENU_TITLE_KEYS 桥接到英文 key；未收录的中文标题按原样透传。
// 保留 te('menu.'+title) 兜底：静态路由使用英文 key（如 'home'）时仍可翻译。
export function translateRouteTitle(title) {
  const key = MENU_TITLE_KEYS[title];
  if (key && i18n.global.te('menu.' + key)) {
    return i18n.global.t('menu.' + key);
  }
  const hasKey = i18n.global.te('menu.' + title);
  if (hasKey) {
    return i18n.global.t('menu.' + title);
  }
  return title;
}
