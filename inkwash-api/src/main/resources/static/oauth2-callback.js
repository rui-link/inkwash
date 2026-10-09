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
(function () {
	'use strict';
	try {
		var opener = window.opener;
		if (opener) {
			opener.postMessage({ status: 'ok' }, '*');
		}
	} catch (e) {
		// 跨域/沙箱拒绝时忽略，页面仍可手动关闭
	}
	try {
		if (window.history && window.history.replaceState) {
			window.history.replaceState(null, '', window.location.pathname);
		}
	} catch (e) {
		// 忽略
	}
	window.setTimeout(function () {
		try { window.close(); } catch (e) {}
	}, 800);
})();
