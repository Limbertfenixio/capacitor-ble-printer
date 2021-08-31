export interface ZPLConverterPlugin {
  /**
   * You can send data in ZPL Zebra Programing Language
   * @param options 
   * @returns returns a promise
   */
  print(options: PrinterOptions): Promise<any>
  /**
   * Discover bonded devices
   * @returns returns a promise
   */
  listenPrinters(): Promise<{devices: BluetoothDevices[]}>
  /**
   * Show the Bluetooth settings on the device
   * @returns returns a promise
   */
  openBluetoothSettings(): Promise<any>
  /**
   * Enable Bluetooth on the device
   * @returns returns a promise
   */
  enableBluetooth(): Promise<any>
  /**
   * You can get a status response from a connected Zebra printer using
   * @param options 
   * @returns returns a promise
   */
  getStatusPrinter(options: StatusPrinterOptions): Promise<{status: string }>
  /**
   * Get ZPL equivalent code from the base64 Image string
   * @param options 
   * @returns returns a promise
   */
  getZPLFromImage(options: ZPLConverterOptions): Promise< {zplCode: string} >
}

export interface PrinterOptions {
  /**
   * Identifier of the remote device
   */
  macAddress: string,
  /**
   * text to print
   */
  printText: string
}

export interface BluetoothDevices {
  /**
   * Name of the remote device
   */
  name: string,
  /**
   * Identifier of the remote device
   */
  macAddress: string,
  /**
   * 
   */
  id: string,
  /**
   * 
   */
  class?: string
}

export interface StatusPrinterOptions {
  /**
   * Identifier of the remote device
   */
  macAddress: string
}

export interface ZPLConverterOptions {
  /**
   * base64 Image string
   */
  base64Image: string,
  /**
   * Want to add header/footer ZPL code or not
   */
  blacknessPercentage: number,
  /**
   * Want to add header/footer ZPL code or not
   */
  addHeaderFooter: boolean,
}

export declare enum StatusPrinter {
  IS_READY_TO_PRINT = "Printer is ready for use",
  IS_PAUSED = "Printer is currently paused",
  IS_PAPER_OUT = "Printer is out of paper",
  IS_HEAD_OPEN = "Printer head is open",
  UNKNOWN_ERROR = "Cannot print, unknown error"
}