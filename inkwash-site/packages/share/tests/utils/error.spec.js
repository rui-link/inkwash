import { describe, it, expect } from 'vitest';

import { getErrorMessage, isLicenseError } from '../../src/utils/error.js';

describe('getErrorMessage', () => {
  it('returns ProblemDetail.detail first', () => {
    const err = { response: { data: { detail: '用户名已存在' } } };
    expect(getErrorMessage(err, 'fb')).toBe('用户名已存在');
  });

  it('falls back through message, msg, warning', () => {
    expect(getErrorMessage({ response: { data: { message: 'm' } } })).toBe('m');
    expect(getErrorMessage({ response: { data: { msg: 'g' } } })).toBe('g');
    expect(getErrorMessage({ response: { data: { warning: 'w' } } })).toBe('w');
  });

  it('uses err.message when no response data', () => {
    expect(getErrorMessage({ message: 'Request failed' })).toBe('Request failed');
  });

  it('returns fallback when no message is found', () => {
    expect(getErrorMessage(null, 'fb')).toBe('fb');
    expect(getErrorMessage({ response: {} })).toBe('');
  });

  it('handles string response data', () => {
    expect(getErrorMessage({ response: { data: 'plain' } })).toBe('plain');
  });
});

describe('isLicenseError', () => {
  it('detects code 40003', () => {
    expect(isLicenseError({ code: 40003 })).toBe(true);
  });
  it('detects requireLicense flag', () => {
    expect(isLicenseError({ requireLicense: true })).toBe(true);
  });
  it('returns false for non-license errors', () => {
    expect(isLicenseError({ code: 403 })).toBe(false);
    expect(isLicenseError(null)).toBe(false);
    expect(isLicenseError(undefined)).toBe(false);
  });
});
