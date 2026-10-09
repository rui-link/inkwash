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
export function findOutermostParent(tree, findName) {
  const parentMap = {};
  function buildParentMap(node, parent) {
    parentMap[node.name] = parent;
    if (node.children) {
      for (let i = 0; i < node.children.length; i++) {
        buildParentMap(node.children[i], node);
      }
    }
  }
  for (let i = 0; i < tree.length; i++) {
    buildParentMap(tree[i], null);
  }
  let currentNode = parentMap[findName];
  while (currentNode) {
    if (!parentMap[currentNode.name]) return currentNode;
    currentNode = parentMap[currentNode.name];
  }
  return null;
}
