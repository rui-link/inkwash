import { describe, it, expect } from 'vitest';

import { formatDate, formatFileSize, formatDateCell } from '../../src/utils/format.js';

describe('formatDate', () => {
  const ts = new Date(2026, 7, 25, 9, 5, 3).getTime();

  it('formats default pattern YYYY-MM-DD HH:mm:ss', () => {
    expect(formatDate(ts)).toBe('2026-08-25 09:05:03');
  });

  it('supports custom patterns', () => {
    expect(formatDate(ts, 'YYYY/MM/DD')).toBe('2026/08/25');
    expect(formatDate(ts, 'HH:mm')).toBe('09:05');
    expect(formatDate(ts, 'DD日MM月')).toBe('25日08月');
  });

  it('returns empty string for falsy input', () => {
    expect(formatDate(null)).toBe('');
    expect(formatDate(undefined)).toBe('');
    expect(formatDate('')).toBe('');
    expect(formatDate(0)).toBe('');
  });

  it('returns empty string for invalid date values instead of NaN soup', () => {
    expect(formatDate('not-a-date')).toBe('');
  });

  it('accepts Date objects and ISO strings', () => {
    expect(formatDate(new Date(2026, 0, 2), 'YYYY-MM-DD')).toBe('2026-01-02');
    expect(formatDate('2026-01-02T00:00:00', 'YYYY-MM-DD')).toBe('2026-01-02');
  });
});

describe('formatDateCell', () => {
  it('renders table cell timestamps', () => {
    const ts = new Date(2026, 11, 31, 23, 59, 59).getTime();
    expect(formatDateCell({}, {}, ts)).toBe('2026-12-31 23:59:59');
  });

  it('returns empty for falsy or invalid cell values', () => {
    expect(formatDateCell({}, {}, null)).toBe('');
    expect(formatDateCell({}, {}, '')).toBe('');
    expect(formatDateCell({}, {}, 'garbage')).toBe('');
  });
});

describe('formatFileSize', () => {
  it('formats bytes without decimals', () => {
    expect(formatFileSize(0)).toBe('0 B');
    expect(formatFileSize(512)).toBe('512 B');
    expect(formatFileSize(1023)).toBe('1023 B');
  });

  it('scales up through units', () => {
    expect(formatFileSize(1024)).toBe('1.0 KB');
    expect(formatFileSize(1536)).toBe('1.5 KB');
    expect(formatFileSize(1048576)).toBe('1.0 MB');
    expect(formatFileSize(1073741824)).toBe('1.0 GB');
  });

  it('caps at the largest unit', () => {
    const tb = 1024 ** 4 * 2;
    expect(formatFileSize(tb)).toBe('2.0 TB');
    const overPb = 1024 ** 5;
    expect(formatFileSize(overPb)).toBe('1024.0 TB');
  });

  it('treats falsy input as zero bytes', () => {
    expect(formatFileSize(null)).toBe('0 B');
    expect(formatFileSize(undefined)).toBe('0 B');
  });
});
