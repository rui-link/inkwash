import { describe, it, expect } from 'vitest';

import { resolveIcon } from '@/utils/icons';

describe('resolveIcon', () => {
  const seededMenuIcons = [
    'Setting',
    'User',
    'Avatar',
    'Management',
    'Menu',
    'Lock',
    'Document',
    'Tickets',
    'Message',
    'Warning',
    'FolderOpened',
    'PriceTag',
    'Monitor',
    'Right',
    'Notebook',
    'DataLine',
    'QuestionFilled',
    'InfoFilled',
    'HomeFilled',
  ];

  it('resolves every icon name seeded in the sys_menu backend data', () => {
    for (const name of seededMenuIcons) {
      expect(resolveIcon(name)).toBeTruthy();
    }
  });

  it('resolves the license dialog lock icons', () => {
    expect(resolveIcon('Lock')).toBeTruthy();
    expect(resolveIcon('Unlock')).toBeTruthy();
  });

  it('returns null for empty and unknown icon names', () => {
    expect(resolveIcon('')).toBeNull();
    expect(resolveIcon('DoesNotExist')).toBeNull();
    expect(resolveIcon('home')).toBeNull();
  });
});
