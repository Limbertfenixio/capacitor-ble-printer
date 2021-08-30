import { registerPlugin } from '@capacitor/core';

import type { ZPLConverterPlugin } from './definitions';

const ZPLConverter = registerPlugin<ZPLConverterPlugin>('ZPLConverter', {
  web: () => import('./web').then(m => new m.ZPLConverterWeb()),
});

export * from './definitions';
export { ZPLConverter };
