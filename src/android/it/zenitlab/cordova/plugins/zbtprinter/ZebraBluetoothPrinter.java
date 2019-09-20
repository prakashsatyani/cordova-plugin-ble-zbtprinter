package it.zenitlab.cordova.plugins.zbtprinter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import it.zenitlab.cordova.plugins.zbtprinter.ZPLConverter;
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

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.Set;


public class ZebraBluetoothPrinter extends CordovaPlugin implements DiscoveryHandler {

    private static final String LOG_TAG = "ZebraBluetoothPrinter";
    private CallbackContext callbackContext;
    private boolean printerFound;
    private Connection thePrinterConn;
    private PrinterStatus printerStatus;
    private ZebraPrinter printer;
    private final int MAX_PRINT_RETRIES = 1;

    public ZebraBluetoothPrinter() {

    }


    //    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("printImage")) {
            try {
                JSONArray labels = args.getJSONArray(0);
                String MACAddress = args.getString(1);
                sendImage(labels, MACAddress);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else if (action.equals("print")) {
            try {
                String MACAddress = args.getString(0);
                String msg = args.getString(1);
                sendData(callbackContext, MACAddress, msg);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else if (action.equals("discoverPrinters")) {
            discoverPrinters();
            return true;
        } else if (action.equals("getPrinterName")) {
            String mac = args.getString(0);
            getPrinterName(mac);
            return true;
        } else if (action.equals("getStatus")) {
            try {
                String mac = args.getString(0);
                getPrinterStatus(callbackContext, mac);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else if(action.equals("getZPLfromImage")){
            try {
                String base64String = args.getString(0);
                boolean addHeaderFooter = args.getBoolean(1);
                int blacknessPercentage = args.getInt(2);
                getZPLfromImage(callbackContext, base64String, blacknessPercentage, addHeaderFooter);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
        }

        return false;
    }

    void getZPLfromImage(final CallbackContext callbackContext, final String base64Image, final int blacknessPercentage, final boolean addHeaderFooter) throws Exception {

        String zplCode = "";

        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        ZebraImageAndroid zebraimage = new ZebraImageAndroid(decodedByte);
        String base64Dithered = new String(zebraimage.getDitheredB64EncodedPng(), "UTF-8");

        byte[] ditheredB64Png = Base64.decode(base64Dithered, Base64.DEFAULT);
        Bitmap ditheredPng = BitmapFactory.decodeByteArray(ditheredB64Png, 0, ditheredB64Png.length);
        
        ZPLConverter zplConveter = new ZPLConverter();
        zplConveter.setCompressHex(false);
        zplConveter.setBlacknessLimitPercentage(blacknessPercentage);

        //Bitmap grayBitmap = toGrayScale(decodedByte);

        try {
            zplCode = zplConveter.convertFromImage(ditheredPng, addHeaderFooter);
            callbackContext.success(zplCode);
        } catch (Exception e){
            callbackContext.error(e.getMessage());
        }

    }

    void getPrinterStatus(final CallbackContext callbackContext, final String mac) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    Connection thePrinterConn = new BluetoothConnectionInsecure(mac);

                    Looper.prepare();

                    thePrinterConn.open();

                    ZebraPrinter zPrinter = ZebraPrinterFactory.getInstance(thePrinterConn);
                    PrinterStatus printerStatus = zPrinter.getCurrentStatus();

                    if (printerStatus.isReadyToPrint){
                        callbackContext.success("Printer is ready for use");
                    }

                    else if(printerStatus.isPaused){
                        callbackContext.error("Printer is currently paused");
                    }

                    else if(printerStatus.isPaperOut){
                        callbackContext.error("Printer is out of paper");
                    }

                    else if(printerStatus.isHeadOpen){
                        callbackContext.error("Printer head is open");
                    }
                    
                    else{
                        callbackContext.error("Cannot print, unknown error");
                    }

                    thePrinterConn.close();

                    Looper.myLooper().quit();
                } catch (Exception e){
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();

    }
    
    
    /*
     * This will send data to be printed by the bluetooth printer
     */
    void sendData(final CallbackContext callbackContext, final String mac, final String msg) throws IOException {
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
                    callbackContext.success("Done");

                    // } else {
                        // callbackContext.error("Printer is not ready");
                    // }
                } catch (Exception e) {
                    // Handle communications error here.
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();
    }

    
    private void sendImage(final JSONArray labels, final String MACAddress) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                printLabels(labels, MACAddress);
            }
        }).start();
    }

    private void printLabels(JSONArray labels, String MACAddress) {
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

                    callbackContext.success();
                } else {
                    Log.e(LOG_TAG, "Printer not ready");
                    callbackContext.error("Printer is not yet ready.");
                }

            }

        } catch (ConnectionException e) {
            Log.e(LOG_TAG, "Connection exception: " + e.getMessage());

            //The connection between the printer & the device has been lost.
            if (e.getMessage().toLowerCase().contains("broken pipe")) {
                callbackContext.error("The connection between the device and the printer has been lost. Please try again.");

            //No printer found via Bluetooth, -1 return so that new printers are searched for.
            } else if (e.getMessage().toLowerCase().contains("socket might closed")) {
                int SEARCH_NEW_PRINTERS = -1;
                callbackContext.error(SEARCH_NEW_PRINTERS);
            } else {
                callbackContext.error("Unknown printer error occurred. Restart the printer and try again please.");
            }

        } catch (ZebraPrinterLanguageUnknownException e) {
            Log.e(LOG_TAG, "ZebraPrinterLanguageUnknown exception: " + e.getMessage());
            callbackContext.error("Unknown printer error occurred. Restart the printer and try again please.");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception: " + e.getMessage());
            callbackContext.error(e.getMessage());
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
            callbackContext.error("Bluetooth is not on.");
        }

        return false;
    }

    public static Bitmap toGrayScale(Bitmap bmpOriginal) {
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

    private void printLabel(JSONArray labels) throws Exception {
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

    private void discoverPrinters() {
        printerFound = false;

        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                try {

                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter.isEnabled()) {
                        Log.d(LOG_TAG, "Searching for printers...");
                        BluetoothDiscoverer.findPrinters(cordova.getActivity().getApplicationContext(), ZebraBluetoothPrinter.this);
                    } else {
                        Log.d(LOG_TAG, "Bluetooth is disabled...");
                        callbackContext.error("Bluetooth is not on.");
                    }

                } catch (ConnectionException e) {
                    Log.e(LOG_TAG, "Connection exception: " + e.getMessage());
                    callbackContext.error(e.getMessage());
                } finally {
                    Looper.myLooper().quit();
                }
            }
        }).start();
    }

    private void getPrinterName(final String macAddress) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String printerName = searchPrinterNameForMacAddress(macAddress);

                if (printerName != null) {
                    Log.d(LOG_TAG, "Successfully found connected printer with name " + printerName);
                    callbackContext.success(printerName);
                } else {
                    callbackContext.error("No printer found.");
                }
            }
        }).start();
    }

    private String searchPrinterNameForMacAddress(String macAddress) {
        Log.d(LOG_TAG, "Connecting with printer " + macAddress + " over bluetooth...");

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                Log.d(LOG_TAG, "Paired device found: " + device.getName());
                if (device.getAddress().equalsIgnoreCase(macAddress)) {
                    return device.getName();
                }
            }
        }

        return null;
    }

    @Override
    public void foundPrinter(DiscoveredPrinter discoveredPrinter) {
        Log.d(LOG_TAG, "Printer found: " + discoveredPrinter.address);
        if (!printerFound) {
            printerFound = true;
            callbackContext.success(discoveredPrinter.address);
        }
    }


    @Override
    public void discoveryFinished() {
        Log.d(LOG_TAG, "Finished searching for printers...");
        if (!printerFound) {
            callbackContext.error("No printer found. If this problem persists, restart the printer.");
        }
    }

    @Override
    public void discoveryError(String s) {
        Log.e(LOG_TAG, "An error occurred while searching for printers. Message: " + s);
        callbackContext.error(s);
    }

}

