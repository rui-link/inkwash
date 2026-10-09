import { beforeEach, describe, expect, it } from 'vitest';

import { abortAllRequests, createAbortSignal } from '../../src/http/index.js';

describe('abortAllRequests', () => {
  beforeEach(() => {
    abortAllRequests();
  });

  it('aborts every in-flight request, not only the most recent one', () => {
    const first = createAbortSignal();
    const second = createAbortSignal();

    abortAllRequests();

    expect(first.aborted).toBe(true);
    expect(second.aborted).toBe(true);
  });

  it('does not abort signals created after abortAllRequests', () => {
    abortAllRequests();

    const later = createAbortSignal();

    expect(later.aborted).toBe(false);
  });
});
