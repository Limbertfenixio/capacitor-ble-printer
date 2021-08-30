import { WebPlugin } from '@capacitor/core';

import type { ZPLConverterPlugin } from './definitions';

export class ZPLConverterWeb extends WebPlugin implements ZPLConverterPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
