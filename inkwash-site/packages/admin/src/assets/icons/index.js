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
const svgModules = import.meta.glob('./*.svg', {
  query: '?raw',
  import: 'default',
});

function svgSymbol(id, viewBox, content) {
  return `<symbol id="${id}" viewBox="${viewBox}">${content}</symbol>`;
}

async function registerIcons() {
  const entries = Object.entries(svgModules);
  const svgContents = await Promise.all(entries.map(([, loader]) => loader()));

  const symbols = entries
    .map(([path, _], i) => {
      const svg = svgContents[i];
      const match = svg.match(/<svg[^>]*viewBox="([^"]*)"[^>]*>([\s\S]*)<\/svg>/);
      if (!match) return null;
      const filename = path.split('/').pop().replace('.svg', '');
      return svgSymbol(`icon-${filename}`, match[1], match[2]);
    })
    .filter(Boolean)
    .join('');

  const svgSprite = `<svg xmlns="http://www.w3.org/2000/svg" style="display:none">${symbols}</svg>`;
  const div = document.createElement('div');
  div.innerHTML = svgSprite;
  document.body.insertBefore(div.firstElementChild, document.body.firstChild);
}

registerIcons();
