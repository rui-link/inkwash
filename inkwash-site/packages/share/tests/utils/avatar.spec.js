import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../../src/http/index.js', () => ({
  default: { post: vi.fn() },
}));

import http from '../../src/http/index.js';
import { uploadAvatar } from '../../src/utils/avatar.js';

describe('uploadAvatar', () => {
  beforeEach(() => {
    http.post.mockReset();
  });

  it('posts the file with folderType=2 as multipart and returns FileView', async () => {
    http.post.mockResolvedValue({ id: 1, url: '/uploads/2/2026/08/me.png' });
    const file = new File(['x'], 'me.png', { type: 'image/png' });

    const result = await uploadAvatar(file);

    expect(http.post).toHaveBeenCalledTimes(1);
    const [url, formData, config] = http.post.mock.calls[0];
    expect(url).toBe('/support/file/upload');
    expect(formData.get('folderType')).toBe('2');
    expect(formData.get('file')).toBe(file);
    expect(config.headers['Content-Type']).toBe('multipart/form-data');
    expect(result.url).toBe('/uploads/2/2026/08/me.png');
  });
});
