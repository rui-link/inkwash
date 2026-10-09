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
import {
  Avatar,
  DataLine,
  Document,
  FolderOpened,
  HomeFilled,
  InfoFilled,
  Lock,
  Management,
  Menu,
  Message,
  Monitor,
  Notebook,
  PriceTag,
  QuestionFilled,
  Right,
  Setting,
  Tickets,
  Unlock,
  User,
  Warning,
} from '@element-plus/icons-vue';

const iconMap = {
  Avatar,
  DataLine,
  Document,
  FolderOpened,
  HomeFilled,
  InfoFilled,
  Lock,
  Management,
  Menu,
  Message,
  Monitor,
  Notebook,
  PriceTag,
  QuestionFilled,
  Right,
  Setting,
  Tickets,
  Unlock,
  User,
  Warning,
};

export function resolveIcon(name) {
  if (!name) return null;
  return iconMap[name] || null;
}

export default iconMap;
