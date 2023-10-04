# capacitor-ble-printer

Capacitor plugin for Zebra printers with Bluetooth.

## Install

```bash
npm install capacitor-ble-printer
npx cap sync
```

## Android
Add the following to your AndroidManifest.xml
```
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
```

## Usage
You can send data in ZPL Zebra

```ts
import { ZPLPrinter } from 'capacitor-ble-printer'

//Bounded Devices
ZPLPrinter.listenPrinters().then(value => {
    console.log(value.devices);
});

//Print Text
var printText = "^XA"
	+ "^FO20,20^A0N,25,25^FDThis is a ZPL test.^FS"
	+ "^XZ";

var printerOpts: PrinterOptions = {
    macAddress: macAddressPrinter,
    printText: printText
}

ZPLPrinter.print(printerOpts).then(_ => {
    console.log("Printer Ok!")
}
```

## API

<docgen-index>

* [`print(...)`](#print)
* [`listenPrinters()`](#listenprinters)
* [`openBluetoothSettings()`](#openbluetoothsettings)
* [`enableBluetooth()`](#enablebluetooth)
* [`getStatusPrinter(...)`](#getstatusprinter)
* [`getZPLFromImage(...)`](#getzplfromimage)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### print(...)

```typescript
print(options: PrinterOptions) => Promise<any>
```

You can send data in ZPL Zebra Programing Language

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#printeroptions">PrinterOptions</a></code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### listenPrinters()

```typescript
listenPrinters() => Promise<{ devices: BluetoothDevices[]; }>
```

Discover bonded devices

**Returns:** <code>Promise&lt;{ devices: BluetoothDevices[]; }&gt;</code>

--------------------


### openBluetoothSettings()

```typescript
openBluetoothSettings() => Promise<any>
```

Show the Bluetooth settings on the device

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### enableBluetooth()

```typescript
enableBluetooth() => Promise<any>
```

Enable Bluetooth on the device

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### getStatusPrinter(...)

```typescript
getStatusPrinter(options: StatusPrinterOptions) => Promise<{ status: string; }>
```

You can get a status response from a connected Zebra printer using

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#statusprinteroptions">StatusPrinterOptions</a></code> |

**Returns:** <code>Promise&lt;{ status: string; }&gt;</code>

--------------------


### getZPLFromImage(...)

```typescript
getZPLFromImage(options: ZPLConverterOptions) => Promise<{ zplCode: string; }>
```

Get ZPL equivalent code from the base64 Image string

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code><a href="#zplconverteroptions">ZPLConverterOptions</a></code> |

**Returns:** <code>Promise&lt;{ zplCode: string; }&gt;</code>

--------------------


### Interfaces


#### PrinterOptions

| Prop             | Type                | Description                     |
| ---------------- | ------------------- | ------------------------------- |
| **`macAddress`** | <code>string</code> | Identifier of the remote device |
| **`printText`**  | <code>string</code> | text to print                   |


#### BluetoothDevices

| Prop             | Type                | Description                     |
| ---------------- | ------------------- | ------------------------------- |
| **`name`**       | <code>string</code> | Name of the remote device       |
| **`macAddress`** | <code>string</code> | Identifier of the remote device |
| **`id`**         | <code>string</code> |                                 |
| **`class`**      | <code>string</code> |                                 |


#### StatusPrinterOptions

| Prop             | Type                | Description                     |
| ---------------- | ------------------- | ------------------------------- |
| **`macAddress`** | <code>string</code> | Identifier of the remote device |


#### ZPLConverterOptions

| Prop                      | Type                 | Description                               |
| ------------------------- | -------------------- | ----------------------------------------- |
| **`base64Image`**         | <code>string</code>  | base64 Image string                       |
| **`blacknessPercentage`** | <code>number</code>  | Want to add header/footer ZPL code or not |
| **`addHeaderFooter`**     | <code>boolean</code> | Want to add header/footer ZPL code or not |

</docgen-api>
