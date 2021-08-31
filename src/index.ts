import { registerPlugin } from '@capacitor/core';

import type { ZPLConverterPlugin } from './definitions';

const ZPLPrinter = registerPlugin<ZPLConverterPlugin>('ZPLPrinter', {
  web: () => import('./web').then(m => new m.ZPLPrinterWeb()),
});

export * from './definitions';
export { ZPLPrinter };
