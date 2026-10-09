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
// 菜单标题(中文，来自后端 DB 的 menu.name) → 英文 key 桥接表。
// locale 字典中 menu.* 的 KEY 已统一为英文标识，
// 路由 meta.title = 后端 menu.name(中文)，Breadcrumb/MenuBar/Tabview 经
// translateRouteTitle 翻译显示，这里将中文标题映射回对应的英文 key。
// 注意：字典 VALUES 仍保持本地化（zh-CN 为中文、en 为英文），本表不涉及显示文案。
export const MENU_TITLE_KEYS = {
  首页: 'home',
  内容管理: 'contentManagement',
  文章管理: 'articleManagement',
  分类管理: 'categoryManagement',
  标签管理: 'termManagement',
  系统管理: 'systemManagement',
  用户管理: 'userManagement',
  用户组管理: 'userGroupManagement',
  角色管理: 'roleManagement',
  权限管理: 'permissionManagement',
  菜单管理: 'menuManagement',
  系统监控: 'monitor',
  登录日志: 'loginLog',
  指标信息: 'metrics',
  操作日志: 'journal',
  资源统计: 'resourceStats',
  关于系统: 'about',
  通知公告: 'notices',
  个人中心: 'profile',
  评论管理: 'comments',
  敏感词管理: 'sensitiveWords',
  帮助中心: 'helpCenter',
  接口文档: 'apiDocs',
  API文档: 'openApiDocs',
  编辑文章: 'editArticle',
  审核文章: 'reviewArticle',
};
