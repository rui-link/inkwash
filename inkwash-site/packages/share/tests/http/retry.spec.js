import { afterEach, describe, expect, it } from 'vitest';

import http from '../../src/http/index.js';

const originalAdapter = http.defaults.adapter;

afterEach(() => {
  http.defaults.adapter = originalAdapter;
});

function mkResponse(status, data, config) {
  return {
    status,
    statusText: String(status),
    headers: {},
    data,
    config,
    request: {},
  };
}

function networkError(config) {
  const error = new Error('Network Error');
  error.config = config;
  error.request = {};
  error.code = 'ERR_NETWORK';
  error.isAxiosError = true;
  return error;
}

function statusError(status, config) {
  const error = new Error(`Request failed with status code ${status}`);
  error.response = mkResponse(status, { status }, config);
  error.config = config;
  error.isAxiosError = true;
  return error;
}

// The client deliberately performs no automatic replay for any method or status.
// See the comment block in packages/share/src/http/index.js for the rationale.
describe('request replay policy', () => {
  it('does not replay a POST after a network error', async () => {
    const calls = [];
    http.defaults.adapter = async (config) => {
      calls.push(config);
      throw networkError(config);
    };

    await expect(http.post('/cms/articles/1/commit')).rejects.toThrow('Network Error');

    expect(calls).toHaveLength(1);
  });

  it('does not replay a POST after a 5xx', async () => {
    const calls = [];
    http.defaults.adapter = async (config) => {
      calls.push(config);
      throw statusError(500, config);
    };

    await expect(http.post('/cms/articles/1/commit')).rejects.toThrow('Request failed with status code 500');

    expect(calls).toHaveLength(1);
  });

  it('does not replay a GET after a network error', async () => {
    const calls = [];
    http.defaults.adapter = async (config) => {
      calls.push(config);
      throw networkError(config);
    };

    await expect(http.get('/cms/articles/listAll')).rejects.toThrow('Network Error');

    expect(calls).toHaveLength(1);
  });

  it('does not replay a GET after a 5xx', async () => {
    const calls = [];
    http.defaults.adapter = async (config) => {
      calls.push(config);
      throw statusError(503, config);
    };

    await expect(http.get('/cms/articles/listAll')).rejects.toThrow('Request failed with status code 503');

    expect(calls).toHaveLength(1);
  });
});
