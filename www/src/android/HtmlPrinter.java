package com.karamsawalha.fjo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class HtmlPrinter extends CordovaPlugin {
    private static final String PRINT_HTML_ACTION = "printHTML";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (PRINT_HTML_ACTION.equals(action)) {
            String htmlContent = args.getString(0);
            this.printHTML(htmlContent, callbackContext);
            return true;
        }
        return false;
    }

    private void printHTML(String htmlContent, CallbackContext callbackContext) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            callbackContext.error("Bluetooth is not enabled.");
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        BluetoothDevice printerDevice = null;
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().startsWith("YourPrinterName")) { // Replace with your printer's name or identifying characteristic
                printerDevice = device;
                break;
            }
        }

        if (printerDevice == null) {
            callbackContext.error("Printer not found.");
            return;
        }

        try {
            BluetoothSocket socket = printerDevice.createRfcommSocketToServiceRecord(UUID.randomUUID());
            socket.connect();
            OutputStream outputStream = socket.getOutputStream();
            byte[] htmlBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            outputStream.write(htmlBytes);
            outputStream.flush();
            outputStream.close();
            socket.close();
            callbackContext.success("Printed successfully.");
        } catch (IOException e) {
            callbackContext.error("Failed to print: " + e.getMessage());
        }
    }
}
