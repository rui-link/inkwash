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
export * from './article.js';
export * from './auth.js';
export * from './category.js';
export * from './term.js';
export * from './user.js';
export * from './role.js';
export * from './permission.js';
export * from './group.js';
export * from './notice.js';
export * from './monitor.js';
export * from './sensitive.js';
export * from './file.js';
export * from './system.js';
export * from './preference.js';
export * from './menu.js';

export { getCategoryTree as getCategoriesTree, getCategoryDetail as getCategory } from './category.js';

export {
  getTermList as getTags,
  getTermDetail as getTag,
  createTerm as createTag,
  updateTerm as updateTag,
  deleteTerm as deleteTag,
} from './term.js';

export {
  captcha as getCaptcha,
  checkQrCode as checkQrStatus,
  getAccounts as listAccounts,
  registerByPassword as register,
} from './auth.js';

export { getMyAgrees as getMyAgreements, getMyComments as getMyCommented } from './article.js';

export { getCategoryList as getPublicCategories } from './category.js';

export { getTermList as getPublicTerms } from './term.js';
