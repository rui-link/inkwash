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
import { ref, onUnmounted } from 'vue';

export function useCountdown(seconds = 60) {
  const countdown = ref(0);
  let timer = null;

  const clear = () => {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  };

  const start = () => {
    clear();
    countdown.value = seconds;
    timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clear();
      }
    }, 1000);
  };

  onUnmounted(clear);

  return { countdown, start, clear };
}
