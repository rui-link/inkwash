<!--
This file is part of Inkwash.
Copyright (C) 2026 ruilink team.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
-->
<template>
  <component :is="type" v-bind="linkProps(to)">
    <slot></slot>
  </component>
</template>

<script setup>
defineOptions({ name: 'AppLink', inheritAttrs: false });

import { computed } from 'vue';

import { isExternal } from '@/utils/index';

const props = defineProps({
  to: { type: String, required: true },
});

const isExternalLink = computed(() => isExternal(props.to));
const type = computed(() => (isExternalLink.value ? 'a' : 'router-link'));
const linkProps = (to) => (isExternalLink.value ? { href: to, target: '_blank', rel: 'noopener noreferrer' } : { to });
</script>
