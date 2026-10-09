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
const HEX_MAP = {
  0: 0,
  1: 1,
  2: 2,
  3: 3,
  4: 4,
  5: 5,
  6: 6,
  7: 7,
  8: 8,
  9: 9,
  A: 10,
  B: 11,
  C: 12,
  D: 13,
  E: 14,
  F: 15,
};
const rgbWhite = { r: 255, g: 255, b: 255 };
const rgbBlack = { r: 0, g: 0, b: 0 };

function hslToRgb(hsl) {
  const { h, s, l } = hsl;
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
  const p = 2 * l - q;
  const hUnit = h / 360;
  function fillCircleVal(val) {
    return val < 0 ? val + 1 : val > 1 ? val - 1 : val;
  }
  function computedRgb(val) {
    let colorVal;
    if (val < 1 / 6) colorVal = p + (q - p) * 6 * val;
    else if (val < 1 / 2) colorVal = q;
    else if (val < 2 / 3) colorVal = p + (q - p) * 6 * (2 / 3 - val);
    else colorVal = p;
    return colorVal * 255;
  }
  return {
    r: Number(computedRgb(fillCircleVal(hUnit + 1 / 3)).toFixed(0)),
    g: Number(computedRgb(fillCircleVal(hUnit)).toFixed(0)),
    b: Number(computedRgb(fillCircleVal(hUnit - 1 / 3)).toFixed(0)),
  };
}

function hexToRGB(hex) {
  hex = hex.toUpperCase();
  const hexRegExp = /^#([0-9A-F]{6})$/;
  if (!hexRegExp.test(hex)) throw new Error('Invalid hex color');
  const hexValArr = hexRegExp.exec(hex)?.[1] || '000000';
  const arr = hexValArr.split('');
  return {
    r: HEX_MAP[arr[0]] * 16 + HEX_MAP[arr[1]],
    g: HEX_MAP[arr[2]] * 16 + HEX_MAP[arr[3]],
    b: HEX_MAP[arr[4]] * 16 + HEX_MAP[arr[5]],
  };
}

const HEX_MAP_REVERSE = Object.keys(HEX_MAP).reduce((acc, k) => {
  acc[HEX_MAP[k]] = k;
  return acc;
}, {});

function rgbToHex(rgb) {
  function getHex(val) {
    val = Math.round(val);
    return `${HEX_MAP_REVERSE[Math.floor(val / 16)]}${HEX_MAP_REVERSE[val % 16]}`;
  }
  return `#${getHex(rgb.r)}${getHex(rgb.g)}${getHex(rgb.b)}`;
}

function mix(color, mixColor, weight) {
  return {
    r: color.r * (1 - weight) + mixColor.r * weight,
    g: color.g * (1 - weight) + mixColor.g * weight,
    b: color.b * (1 - weight) + mixColor.b * weight,
  };
}

export function genMixColor(base) {
  if (typeof base === 'string') base = hexToRGB(base);
  else if ('h' in base) base = hslToRgb(base);
  return {
    DEFAULT: rgbToHex(base),
    dark: {
      1: rgbToHex(mix(base, rgbBlack, 0.1)),
      2: rgbToHex(mix(base, rgbBlack, 0.2)),
      3: rgbToHex(mix(base, rgbBlack, 0.3)),
      4: rgbToHex(mix(base, rgbBlack, 0.4)),
      5: rgbToHex(mix(base, rgbBlack, 0.5)),
      6: rgbToHex(mix(base, rgbBlack, 0.6)),
      7: rgbToHex(mix(base, rgbBlack, 0.7)),
      8: rgbToHex(mix(base, rgbBlack, 0.78)),
      9: rgbToHex(mix(base, rgbBlack, 0.85)),
    },
    light: {
      1: rgbToHex(mix(base, rgbWhite, 0.1)),
      2: rgbToHex(mix(base, rgbWhite, 0.2)),
      3: rgbToHex(mix(base, rgbWhite, 0.3)),
      4: rgbToHex(mix(base, rgbWhite, 0.4)),
      5: rgbToHex(mix(base, rgbWhite, 0.5)),
      6: rgbToHex(mix(base, rgbWhite, 0.6)),
      7: rgbToHex(mix(base, rgbWhite, 0.7)),
      8: rgbToHex(mix(base, rgbWhite, 0.78)),
      9: rgbToHex(mix(base, rgbWhite, 0.85)),
    },
  };
}
