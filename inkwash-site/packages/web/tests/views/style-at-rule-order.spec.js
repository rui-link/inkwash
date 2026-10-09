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
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it } from 'vitest';

/**
 * Sass requires every `@use` to precede all other rules in a stylesheet. It is a hard parse
 * error, not a warning, so a single misplaced `@use` inside an SFC `<style lang="scss">` block
 * fails the whole Vite dev server and build — with an error that names the stylesheet but not
 * the rule that happens to sit above it.
 *
 * `Login.vue` and `Register.vue` both shipped `@use '@/styles/auth-form';` *after* a
 * `.sms-notice { ... }` rule, which broke the web portal's login and register pages. Nothing
 * caught it because no unit test mounts those two components, so this guard walks the
 * stylesheet source instead.
 */
const PACKAGE_ROOT = join(import.meta.dirname, '..', '..', 'src');

/** Source of the `<style>` blocks that declare `lang="scss"`. */
function scssStyleBlocks(file) {
  const src = readFileSync(file, 'utf8');
  const blocks = [];
  const openTag = /<style\b[^>]*\blang=["']scss["'][^>]*>/g;

  let match = openTag.exec(src);
  while (match !== null) {
    const bodyStart = match.index + match[0].length;
    const bodyEnd = src.indexOf('</style>', bodyStart);
    if (bodyEnd === -1) break;
    blocks.push({
      file,
      line: src.slice(0, match.index).split('\n').length,
      body: src.slice(bodyStart, bodyEnd),
    });
    openTag.lastIndex = bodyEnd;
    match = openTag.exec(src);
  }

  return blocks;
}

function vueFilesUnder(dir) {
  const found = [];
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      found.push(...vueFilesUnder(full));
    } else if (entry.endsWith('.vue')) {
      found.push(full);
    }
  }
  return found;
}

const offenders = [];

describe('scss @use ordering', () => {
  it('places every @use before other rules in SFC style blocks', () => {
    offenders.length = 0;
    for (const file of vueFilesUnder(PACKAGE_ROOT)) {
      for (const block of scssStyleBlocks(file)) {
        const lines = block.body.split('\n');
        const firstUse = lines.findIndex((l) => /^\s*@(use|forward)\b/.test(l));
        if (firstUse === -1) continue;

        const firstOther = lines.findIndex(
          (l) => !/^\s*$/.test(l) && !/^\s*(\/\/|\/\*|\*)/.test(l) && !/^\s*@(use|forward)\b/.test(l),
        );

        if (firstOther !== -1 && firstOther < firstUse) {
          offenders.push(`${file}:${block.line + firstOther} (rule before @use at line ${block.line + firstUse})`);
        }
      }
    }

    // Sass: "@use rules must be written before any other rules." — a parse error that takes
    // down `pnpm dev:web` and `pnpm build:web`, not a lint warning.
    expect(
      offenders,
      `这些 <style lang="scss"> 块里 @use 前面还有别的规则，Sass 会直接解析失败:\n${offenders.join('\n')}`,
    ).toEqual([]);
  });

  it('actually exercises the auth pages that broke', () => {
    // Pin the regression directly: these two files carried the misplaced @use, so a future
    // edit that moves a rule back above it must fail here rather than at runtime.
    const login = scssStyleBlocks(join(PACKAGE_ROOT, 'views', 'Login.vue'));
    const register = scssStyleBlocks(join(PACKAGE_ROOT, 'views', 'Register.vue'));
    expect(login.length, 'Login.vue 应保留一个 scss 样式块').toBeGreaterThan(0);
    expect(register.length, 'Register.vue 应保留一个 scss 样式块').toBeGreaterThan(0);
    for (const block of [...login, ...register]) {
      expect(block.body.trimStart().startsWith('@use'), `${block.file} 的样式块应以 @use 开头`).toBe(true);
    }
  });
});
