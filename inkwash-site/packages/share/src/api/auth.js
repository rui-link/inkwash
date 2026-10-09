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
import http from '../http/index.js';

/**
 * 认证（Authentication）接口
 */

export function captcha() {
  return http.get('/auth/captcha');
}

export function verifyCaptcha(data) {
  return http.post('/auth/captcha', data);
}

export function sendSmsCode(data) {
  return http.post('/auth/sms/code', data);
}

export function login(data) {
  return http.post('/auth/login', data);
}

export function registerByPassword(data) {
  return http.post('/auth/register/password', data);
}

export function registerBySms(data) {
  return http.post('/auth/register/sms', data);
}

export function refreshToken(refreshToken, config = {}) {
  return http.post('/auth/token/refresh', refreshToken ? { refreshToken } : {}, config);
}

export function getMe(config = {}) {
  return http.get('/auth/me', config);
}

export function qrLogin(ticket) {
  return http.post('/auth/qr/login', { ticket });
}

export function logout() {
  return http.post('/auth/logout');
}

/**
 * 个人资料（Profile）接口
 */

export function getProfile() {
  return http.get('/profile');
}

export function updateProfile(data) {
  return http.put('/profile', data);
}

export function getPermissions() {
  return http.get('/profile/permissions');
}

export function changePassword(data) {
  return http.put('/profile/password', data);
}

export function resetPassword(data) {
  return http.post('/profile/password/reset', data);
}

export function getQrCode(client = 'web', origin = '') {
  return http.get('/auth/qr/code', { params: { client, origin } });
}

export function checkQrCode(params) {
  return http.get('/auth/qr/check', { params });
}

/**
 * 订阅二维码登录状态推送（SSE，绕过 axios）
 *
 * @param {string} qrCodeId 二维码ID
 * @param {string} sseToken 生成二维码接口下发的订阅令牌，仅桌面端持有，用于绑定 SSE 连接
 * @param {{ onConfirmed?: (data: {status: string, ticket?: string}) => void, onExpired?: (data: {status: string}) => void, onError?: () => void }} handlers
 * @returns {EventSource}
 */
export function createQrEventSource(qrCodeId, sseToken, handlers = {}) {
  const baseURL = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '');
  const url = `${baseURL}/auth/qr/sse?qrCodeId=${encodeURIComponent(qrCodeId)}&sseToken=${encodeURIComponent(sseToken || '')}`;
  const eventSource = new EventSource(url);

  eventSource.addEventListener('qr', (event) => {
    let data;
    try {
      data = JSON.parse(event.data);
    } catch {
      return;
    }
    if (data.status === 'CONFIRMED' && handlers.onConfirmed) {
      handlers.onConfirmed(data);
    } else if (data.status === 'EXPIRED' && handlers.onExpired) {
      handlers.onExpired(data);
    }
  });

  eventSource.onerror = () => {
    if (eventSource.readyState === EventSource.CLOSED && handlers.onError) {
      handlers.onError();
    }
  };

  return eventSource;
}

export function scanQrCode(qrCodeId) {
  return http.post('/auth/qr/scan', null, { params: { qrCodeId } });
}

export function confirmQrCode(qrCodeId) {
  return http.post('/auth/qr/confirm', null, { params: { qrCodeId } });
}

export function getAccounts() {
  return http.get('/profile/accounts');
}

export function setPassword(data) {
  return http.post('/profile/password/set', data);
}

export function sendPhoneVerifyCode(phone) {
  return http.post('/profile/phone/code', { phone });
}

export function verifyPhone(data) {
  return http.post('/profile/phone/verify', data);
}

export function sendEmailVerifyCode(email) {
  return http.post('/profile/email/code', { email });
}

export function verifyEmail(data) {
  return http.post('/profile/email/verify', data);
}

export function unbindAccount(id, kind) {
  return http.delete(`/profile/accounts/${id}`, { params: { kind } });
}

export function getOAuth2Token(params) {
  return http.get('/auth/oauth2/token', { params });
}

export function getOAuth2Status() {
  return http.get('/auth/oauth2/status');
}
