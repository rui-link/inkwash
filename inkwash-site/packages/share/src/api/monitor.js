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

export function getLoginLogs(params) {
  return http.get('/monitor/logins', { params });
}

export function getLoginLogDetail(id) {
  return http.get(`/monitor/logins/${id}`);
}

export function deleteLoginLog(id) {
  return http.delete(`/monitor/logins/${id}`);
}

export function batchDeleteLoginLogs(data) {
  return http.post('/monitor/logins/delete/batch', data);
}

export function clearLoginLogs() {
  return http.post('/monitor/logins/clear');
}

export function getJournals(params) {
  return http.get('/monitor/journals', { params });
}

export function getHealth() {
  return http.get('/monitor/metrics/health');
}

export function getMetrics() {
  return http.get('/monitor/metrics/metrics');
}

export function getSystemInfo() {
  return http.get('/monitor/metrics/info');
}

export function getAuditDashboard() {
  return http.get('/monitor/audit');
}

export const monitorApi = {
  getLoginLogs,
  getLoginLogDetail,
  deleteLoginLog,
  batchDeleteLoginLogs,
  clearLoginLogs,
  getJournals,
  getHealth,
  getMetrics,
  getSystemInfo,
  getAuditDashboard,
};
