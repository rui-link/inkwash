import { describe, expect, it } from 'vitest';

import { renderMarkdown } from '../../src/utils/markdown.js';

describe('renderMarkdown', () => {
  it('renders authored audio HTML as an audio element', () => {
    const html = renderMarkdown('<audio controls src="/uploads/a.mp3"></audio>');
    expect(html).toContain('<audio');
    expect(html).toContain('controls');
    expect(html).toContain('src="/uploads/a.mp3"');
  });

  it('renders authored video HTML as a video element', () => {
    const html = renderMarkdown('<video controls src="/uploads/v.mp4"></video>');
    expect(html).toContain('<video');
    expect(html).toContain('controls');
    expect(html).toContain('src="/uploads/v.mp4"');
  });

  it('strips script tags via DOMPurify', () => {
    const html = renderMarkdown('<script>alert(1)</script>');
    expect(html).not.toContain('<script');
    expect(html).not.toContain('alert(1)');
  });

  it('still renders markdown syntax', () => {
    const html = renderMarkdown('# Title\n\n**bold**');
    expect(html).toContain('<h1>Title</h1>');
    expect(html).toContain('<strong>bold</strong>');
  });
});
