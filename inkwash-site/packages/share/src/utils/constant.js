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
export const ArticleStatus = {
  DRAFT: 1,
  PENDING: 2,
  APPROVED: 3,
  REJECTED: 4,
  PUBLISHED: 5,
  RETRACTED: 6,
};
export const ArticleStatusLabelKey = {
  1: 'statusDraft',
  2: 'statusPending',
  3: 'statusApproved',
  4: 'statusRejected',
  5: 'statusPublished',
  6: 'statusRetracted',
};
export const ArticleStatusMap = {
  1: { label: '草稿', type: 'info' },
  2: { label: '待审核', type: 'warning' },
  3: { label: '通过', type: 'success' },
  4: { label: '驳回', type: 'danger' },
  5: { label: '已发布', type: 'info' },
  6: { label: '已撤回', type: 'info' },
};
export const UserStatus = {
  DISABLE: 0,
  ENABLE: 1,
  LOCKED: 2,
  EXPIRED: 3,
  PENDING: 4,
};
export const UserStatusMap = {
  0: { label: '禁用', type: 'danger' },
  1: { label: '启用', type: 'success' },
  2: { label: '锁定', type: 'warning' },
  3: { label: '过期', type: 'info' },
  4: { label: '待审核', type: 'info' },
};
export const BaseStatus = { DISABLE: 0, ENABLE: 1 };
export const BaseStatusMap = {
  0: { label: '禁用', type: 'danger' },
  1: { label: '启用', type: 'success' },
};
export const PermType = { MENU: 1, BUTTON: 2, API: 3, LINK: 4 };
export const PermTypeMap = {
  1: { label: '菜单', type: 'info' },
  2: { label: '按钮', type: 'success' },
  3: { label: 'API', type: 'warning' },
  4: { label: '链接', type: 'info' },
};
export const NoticeType = { NOTIFICATION: 1, ANNOUNCEMENT: 2, WARNING: 3 };
export const NoticeTypeMap = {
  1: { label: '通知', type: 'info' },
  2: { label: '公告', type: 'warning' },
  3: { label: '警报', type: 'danger' },
};
export const Gender = { UNKNOWN: 0, MALE: 1, FEMALE: 2 };
export const GenderMap = {
  0: { label: '未知' },
  1: { label: '男' },
  2: { label: '女' },
};
export const UserCategory = { STAFF: 1, READER: 2, WRITER: 3 };
export const UserCategoryMap = {
  1: { label: '员工' },
  2: { label: '读者' },
  3: { label: '创作者' },
};
export const UserEducation = {
  UNKNOWN: 0,
  COMMONER: 1,
  PRIMARY: 2,
  JUNIOR: 3,
  SENIOR: 4,
  COLLEGE: 5,
  BACHELOR: 6,
  MASTER: 7,
  DOCTOR: 8,
  EXPERT: 9,
};
export const UserEducationMap = {
  0: { label: '未知' },
  1: { label: '未入学' },
  2: { label: '小学' },
  3: { label: '初中' },
  4: { label: '高中' },
  5: { label: '专科' },
  6: { label: '本科' },
  7: { label: '硕士' },
  8: { label: '博士' },
  9: { label: '博士后' },
};
export const LoginStatus = { FAILED: 0, SUCCESS: 1, LOCKED: 3, OTHER: 4 };
export const LoginStatusMap = {
  0: { label: '失败', type: 'danger' },
  1: { label: '成功', type: 'success' },
  3: { label: '锁定', type: 'warning' },
  4: { label: '其他', type: 'info' },
};
export const AuthType = { PASSWORD: 1, SMS_CODE: 2, OAUTH2: 3, QR_CODE: 4 };
export const AuthTypeMap = {
  1: { label: '密码', type: 'info' },
  2: { label: '短信验证码', type: 'warning' },
  3: { label: 'OAuth2', type: 'primary' },
  4: { label: '二维码', type: 'success' },
};
