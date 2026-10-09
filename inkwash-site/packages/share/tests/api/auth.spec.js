import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../../src/http/index.js', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}));

import { refreshToken, getMe, qrLogin } from '../../src/api/auth.js';
import http from '../../src/http/index.js';

describe('auth API cookie contract', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('refreshToken posts without a body token when omitted (cookie session)', () => {
    refreshToken();
    expect(http.post).toHaveBeenCalledWith('/auth/token/refresh', {}, {});
  });

  it('refreshToken forwards the refresh token when explicitly provided', () => {
    refreshToken('rt');
    expect(http.post).toHaveBeenCalledWith('/auth/token/refresh', { refreshToken: 'rt' }, {});
  });

  it('refreshToken merges an axios config for probe/silent calls', () => {
    refreshToken(undefined, { __skipAuthRefresh: true });
    expect(http.post).toHaveBeenCalledWith('/auth/token/refresh', {}, { __skipAuthRefresh: true });
  });

  it('getMe calls GET /auth/me', () => {
    getMe();
    expect(http.get).toHaveBeenCalledWith('/auth/me', {});
  });

  it('getMe forwards an axios config for the bootstrap probe', () => {
    getMe({ __skipAuthRefresh: true });
    expect(http.get).toHaveBeenCalledWith('/auth/me', {
      __skipAuthRefresh: true,
    });
  });

  it('qrLogin posts the one-time ticket', () => {
    qrLogin('ticket1');
    expect(http.post).toHaveBeenCalledWith('/auth/qr/login', {
      ticket: 'ticket1',
    });
  });
});
