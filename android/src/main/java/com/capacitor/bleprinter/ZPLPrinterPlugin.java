package com.capacitor.bleprinter;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.PermissionState;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.zebra.sdk.comm.BluetoothConnectionInsecure;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.graphics.internal.ZebraImageAndroid;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Set;

@CapacitorPlugin(
        name = "ZPLPrinter",
        permissions = {
                @Permission(
                        alias = "bluetooth",
                        strings = {Manifest.permission.BLUETOOTH}
                ),
                @Permission(
                        alias = "bluetooth_admin",
                        strings = {Manifest.permission.BLUETOOTH_ADMIN}
                )
        }
)
public class ZPLPrinterPlugin extends Plugin implements DiscoveryHandler {
    private static final String LOG_TAG = "ZebraPrinter";
    private PluginCall call;
    private boolean printerFound;
    private Connection thePrinterConn;
    private PrinterStatus printerStatus;
    private ZebraPrinter printer;
    private final int MAX_PRINT_RETRIES = 1;
    private BluetoothAdapter bluetoothAdapter;

    public ZPLPrinterPlugin() { }

    @PluginMethod
    public void listenPrinters(PluginCall call) throws JSONException{
        discoverPrinters(call);
    }

    @PluginMethod
    public void getZPLFromImage(PluginCall call){
       try {
           String base64Image = call.getString("base64Image");
           Boolean addHeaderFooter = call.getBoolean("addHeaderFooter");    	//Want to add header/footer ZPL code or not
           Integer blacknessPercentage = call.getInt("blacknessPercentage");
           getZPLfromImage(call, base64Image, blacknessPercentage ,addHeaderFooter);
       }catch (Exception e){
           Log.e(LOG_TAG, e.getMessage());
           e.printStackTrace();
       }
    }

    @PluginMethod
    public void print(PluginCall call){
        try {
            String MACAddress = call.getString("macAddress");
            String printText = call.getString("printText");
            sendData(call, MACAddress, printText);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void printImage(PluginCall call)  {
        try {
            JSArray labels = call.getArray("base64StringArray");

            String MACAddress = call.getString("macAddress");
            Log.d("base64", labels.toString());
            sendImage(labels, MACAddress);
        } catch ( IOException e ){
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    @PluginMethod
    public void getStatusPrinter(PluginCall call){
        try {
            String macAddress = call.getString("macAddress");
            getPrinterStatus(call ,macAddress);
        } catch (IOException e){
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    @PluginMethod
    public void openBluetoothSettings(PluginCall call){
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        getActivity().startActivity(intent);
        call.resolve();
    }

    @PluginMethod
    public void enableBluetooth(PluginCall call){
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(call, intent, "enableBluetoothCallback");
    }

    /*
     * This will send data to be printed by the bluetooth printer
     */
    private void sendData(PluginCall call, final String mac, final String msg) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    // Instantiate insecure connection for given Bluetooth MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(mac);

                    // if (isPrinterReady(thePrinterConn)) {

                    // Initialize
                    Looper.prepare();

                    // Open the connection - physical connection is established here.
                    thePrinterConn.open();

                    SGD.SET("device.languages", "zpl", thePrinterConn);
                    thePrinterConn.write(msg.getBytes());

                    // Close the insecure connection to release resources.
                    thePrinterConn.close();

                    Looper.myLooper().quit();
                    call.resolve();

                    // } else {
                    // callbackContext.error("Printer is not ready");
                    // }
                } catch (Exception e) {
                    // Handle communications error here.
                    call.reject(e.getMessage());
                }
            }
        }).start();
    }

    private void sendImage(final JSArray labels, final String MACAddress) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                printLabels(labels, MACAddress);
            }
        }).start();
    }

    private void printLabels(JSArray labels, String MACAddress) {
        try {

            boolean isConnected = openBluetoothConnection(MACAddress);

            if (isConnected) {
                initializePrinter();

                boolean isPrinterReady = getPrinterStatus(0);

                if (isPrinterReady) {

                    printLabel(labels);

                    //Sufficient waiting for the label to print before we start a new printer operation.
                    Thread.sleep(15000);

                    thePrinterConn.close();

                    call.resolve();
                } else {
                    Log.e(LOG_TAG, "Printer not ready");
                    call.reject("Printer is not yet ready.");
                }

            }

        } catch (ConnectionException e) {
            Log.e(LOG_TAG, "Connection exception: " + e.getMessage());

            //The connection between the printer & the device has been lost.
            if (e.getMessage().toLowerCase().contains("broken pipe")) {
                call.reject("The connection between the device and the printer has been lost. Please try again.");

                //No printer found via Bluetooth, -1 return so that new printers are searched for.
            } else if (e.getMessage().toLowerCase().contains("socket might closed")) {
                int SEARCH_NEW_PRINTERS = -1;
                call.reject(String.valueOf(SEARCH_NEW_PRINTERS));
            } else {
                call.reject("Unknown printer error occurred. Restart the printer and try again please.");
            }

        } catch (ZebraPrinterLanguageUnknownException e) {
            Log.e(LOG_TAG, "ZebraPrinterLanguageUnknown exception: " + e.getMessage());
            call.reject("Unknown printer error occurred. Restart the printer and try again please.");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception: " + e.getMessage());
            call.reject(e.getMessage());
        }
    }

    private void initializePrinter() throws ConnectionException, ZebraPrinterLanguageUnknownException {
        Log.d(LOG_TAG, "Initializing printer...");
        printer = ZebraPrinterFactory.getInstance(thePrinterConn);
        String printerLanguage = SGD.GET("device.languages", thePrinterConn);
        if (!printerLanguage.contains("zpl")) {
            SGD.SET("device.languages", "hybrid_xml_zpl", thePrinterConn);
            Log.d(LOG_TAG, "printer language set...");
        }
    }

    public void getZPLfromImage(PluginCall call, final String base64Image, final int blacknessPercentage, final boolean addHeaderFooter) throws Exception {

        String zplCode = "";

        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        ZebraImageAndroid zebraimage = new ZebraImageAndroid(decodedByte);
        String base64Dithered = new String(zebraimage.getDitheredB64EncodedPng(), "UTF-8");

        byte[] ditheredB64Png = Base64.decode(base64Dithered, Base64.DEFAULT);
        Bitmap ditheredPng = BitmapFactory.decodeByteArray(ditheredB64Png, 0, ditheredB64Png.length);

        if(ditheredPng.getHeight() > ditheredPng.getWidth())
            ditheredPng = Bitmap.createScaledBitmap(ditheredPng, 300, 540, true);

        ZPLConverter zplConveter = new ZPLConverter();
        zplConveter.setCompressHex(false);
        zplConveter.setBlacknessLimitPercentage(blacknessPercentage);

        //Bitmap grayBitmap = toGrayScale(decodedByte);

        try {
            zplCode = zplConveter.convertFromImage(ditheredPng, addHeaderFooter);
            JSObject ret = new JSObject();
            ret.put("zplCode", zplCode);
            call.resolve(ret);
        } catch (Exception e){
            call.reject(e.getMessage());
        }

    }

    @SuppressLint("MissingPermission")
    private boolean openBluetoothConnection(String MACAddress) throws ConnectionException {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            Log.d(LOG_TAG, "Creating a bluetooth-connection for mac-address " + MACAddress);

            thePrinterConn = new BluetoothConnectionInsecure(MACAddress);

            Log.d(LOG_TAG, "Opening connection...");
            thePrinterConn.open();
            Log.d(LOG_TAG, "connection successfully opened...");

            return true;
        } else {
            Log.d(LOG_TAG, "Bluetooth is disabled...");
            call.reject("Bluetooth is not on.");
        }

        return false;
    }

    private static Bitmap toGrayScale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap grayScale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(grayScale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return grayScale;
    }

    private void printLabel(JSArray labels) throws Exception {
        ZebraPrinterLinkOs zebraPrinterLinkOs = ZebraPrinterFactory.createLinkOsPrinter(printer);

        for (int i = labels.length() - 1; i >= 0; i--) {
            String base64Image = labels.get(i).toString();
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ZebraImageAndroid zebraimage = new ZebraImageAndroid(decodedByte);

            //Lengte van het label eerst instellen om te kleine of te grote afdruk te voorkomen
            if (zebraPrinterLinkOs != null && i == labels.length() - 1) {
                setLabelLength(zebraimage);
            }

            if (zebraPrinterLinkOs != null) {
                printer.printImage(zebraimage, 150, 0, zebraimage.getWidth(), zebraimage.getHeight(), false);
            } else {
                Log.d(LOG_TAG, "Storing label on printer...");
                printer.storeImage("wgkimage.pcx", zebraimage, -1, -1);
                printImageTheOldWay(zebraimage);
            }
        }

    }

    private void printImageTheOldWay(ZebraImageAndroid zebraimage) throws Exception {

        Log.d(LOG_TAG, "Printing image...");

        String cpcl = "! 0 200 200 ";
        cpcl += zebraimage.getHeight();
        cpcl += " 1\r\n";
        cpcl += "PW 750\r\nTONE 0\r\nSPEED 6\r\nSETFF 203 5\r\nON - FEED FEED\r\nAUTO - PACE\r\nJOURNAL\r\n";
        cpcl += "PCX 150 0 !<wgkimage.pcx\r\n";
        cpcl += "FORM\r\n";
        cpcl += "PRINT\r\n";
        thePrinterConn.write(cpcl.getBytes());

    }

    private void getPrinterStatus(PluginCall call, final String mac) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    Connection thePrinterConn = new BluetoothConnectionInsecure(mac);

                    Looper.prepare();

                    thePrinterConn.open();

                    ZebraPrinter zPrinter = ZebraPrinterFactory.getInstance(thePrinterConn);
                    PrinterStatus printerStatus = zPrinter.getCurrentStatus();
                    JSObject res = new JSObject();

                    if (printerStatus.isReadyToPrint){
                        res.put("status", "Printer is ready for use");
                        call.resolve(res);
                    }

                    else if(printerStatus.isPaused){
                        res.put("status", "Printer is currently paused");
                        call.resolve(res);
                    }

                    else if(printerStatus.isPaperOut){
                        res.put("status", "Printer is out of paper");
                        call.resolve(res);
                    }

                    else if(printerStatus.isHeadOpen){
                        res.put("status", "Printer head is open");
                        call.resolve(res);
                    }

                    else{
                        res.put("status", "Cannot print, unknown error");
                        call.resolve(res);
                    }

                    thePrinterConn.close();

                    Looper.myLooper().quit();
                } catch (Exception e){
                    call.reject(e.getMessage());
                }
            }
        }).start();

    }

    private boolean getPrinterStatus(int retryAttempt) throws Exception {
        try {
            printerStatus = printer.getCurrentStatus();

            if (printerStatus.isReadyToPrint) {
                Log.d(LOG_TAG, "Printer is ready to print...");
                return true;
            } else {
                if (printerStatus.isPaused) {
                    throw new Exception("Printer is paused. Please activate it first.");
                } else if (printerStatus.isHeadOpen) {
                    throw new Exception("Printer is open. Please close it first.");
                } else if (printerStatus.isPaperOut) {
                    throw new Exception("Please complete the labels first.");
                } else {
                    throw new Exception("Could not get the printer status. Please try again. " +
                            "If this problem persists, restart the printer.");
                }
            }
        } catch (ConnectionException e) {
            if (retryAttempt < MAX_PRINT_RETRIES) {
                Thread.sleep(5000);
                return getPrinterStatus(++retryAttempt);
            } else {
                throw new Exception("Could not get the printer status. Please try again. " +
                        "If this problem persists, restart the printer.");
            }
        }

    }

    /**
     * Use the Zebra Android SDK to determine the length if the printer supports LINK-OS
     *
     * @param zebraimage
     * @throws Exception
     */
    private void setLabelLength(ZebraImageAndroid zebraimage) throws Exception {
        ZebraPrinterLinkOs zebraPrinterLinkOs = ZebraPrinterFactory.createLinkOsPrinter(printer);

        if (zebraPrinterLinkOs != null) {
            String currentLabelLength = zebraPrinterLinkOs.getSettingValue("zpl.label_length");
            if (!currentLabelLength.equals(String.valueOf(zebraimage.getHeight()))) {
                zebraPrinterLinkOs.setSetting("zpl.label_length", zebraimage.getHeight() + "");
            }
        }
    }

    @PermissionCallback
    private void bluetoothCallback(PluginCall call) throws JSONException {
        if(getPermissionState("bluetooth") != PermissionState.GRANTED){
            call.reject("Permission is required to bluetooth");
        }else{
            discoverPrinters(call);
        }
    }

    @ActivityCallback
    private void enableBluetoothCallback(PluginCall call, ActivityResult result){
        Log.d(LOG_TAG, result.toString());
        if(result.getResultCode() == Activity.RESULT_OK){
            Log.d(LOG_TAG, "User enabled Bluetooth");
            call.resolve();
        }else{
            Log.d(LOG_TAG, "User did *NOT* enable Bluetooth");
            call.reject("User did not enable Bluetooth");
        }
    }

    @SuppressLint("MissingPermission")
    private void discoverPrinters(PluginCall call) throws JSONException  {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        JSONArray deviceList = new JSONArray();
        JSObject res = new JSObject();
        Set< BluetoothDevice > bondedDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice device: bondedDevices){
            deviceList.put(deviceToJSON(device));
        }

        Log.d(LOG_TAG, deviceList.toString());
        res.put("devices", deviceList);
        call.resolve(res);
    }

    @SuppressLint("MissingPermission")
    private JSONObject deviceToJSON(BluetoothDevice device) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", device.getName());
        json.put("address", device.getAddress());
        json.put("id", device.getAddress());
        if (device.getBluetoothClass() != null) {
            json.put("class", device.getBluetoothClass().getDeviceClass());
        }
        return json;
    }

    @Override
    public void foundPrinter(DiscoveredPrinter discoveredPrinter) {
        Log.d(LOG_TAG, "Printer found: " + discoveredPrinter.address);
        if (!printerFound) {
            printerFound = true;
            JSObject res = new JSObject();
            res.put("value", discoveredPrinter.address);
            call.resolve(res);
        }
    }

    @Override
    public void discoveryFinished() {
        Log.d(LOG_TAG, "Finished searching for printers...");
        if (!printerFound) {
            call.reject("No printer found. If this problem persists, restart the printer.");
        }
    }

    @Override
    public void discoveryError(String s) {
        Log.e(LOG_TAG, "An error occurred while searching for printers. Message: " + s);
        call.reject(s);
    }
}

