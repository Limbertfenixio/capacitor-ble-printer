
import { WebPlugin } from '@capacitor/core';

import type { PrinterOptions, ZPLConverterPlugin } from './definitions';

export class ZPLPrinterWeb extends WebPlugin implements ZPLConverterPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  print(options: PrinterOptions): Promise<any>{
    console.log(options);
    return new Promise((resolve, reject) => {
      if(options) {
        resolve(true);
      }else {
        reject(false);
      }
    });
  }

  listenPrinters(): any{}

  openBluetoothSettings(): any{}

  enableBluetooth(): any {}

  disconnect(): any{}

  getStatusPrinter(): any{}

  getZPLFromImage(): any{}
}
