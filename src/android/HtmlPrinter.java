package com.karamsawalha.fjo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class HtmlPrinter extends CordovaPlugin {
    private static final String PRINT_HTML_ACTION = "printHTML";
    private static final int REQUEST_CODE = 1;
    private static final String TAG = "HtmlPrinter";
    private BluetoothAdapter bluetoothAdapter;
    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (PRINT_HTML_ACTION.equals(action)) {
            String htmlContent = args.getString(0);

            // Check permissions before proceeding
            if (hasPermissions()) {
                findAndPrint(htmlContent);
            } else {
                requestPermissions();
            }

            return true;
        }
        return false;
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 and above, Bluetooth permissions are required
            return ContextCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        // For versions below Android 12, Bluetooth permissions are generally not requested
        return true;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                cordova.getActivity(),
                new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callbackContext.success("Permission granted");
            } else {
                callbackContext.error("Permission denied");
            }
        }
    }

    private void findAndPrint(String htmlContent) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            callbackContext.error("Bluetooth is not enabled.");
            return;
        }

        // Ensure Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            cordova.getActivity().startActivityForResult(enableBtIntent, REQUEST_CODE);
        }

        // Find paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        BluetoothDevice printerDevice = null;

        for (BluetoothDevice device : pairedDevices) {
            if (isPrinter(device)) {
                printerDevice = device;
                break;
            }
        }

        if (printerDevice == null) {
            callbackContext.error("No printer found.");
            return;
        }

        // Print using the found printer
        printUsingPrinter(printerDevice, htmlContent);
    }

    private boolean isPrinter(BluetoothDevice device) {
        // Customize this logic to identify printers. Check device name, type, or other identifiers.
        return device.getName() != null && device.getName().toLowerCase().contains("printer");
    }

    private void printUsingPrinter(BluetoothDevice printerDevice, String htmlContent) {
        BluetoothSocket socket = null;
        try {
            // Use a well-known SPP UUID
            UUID sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            socket = printerDevice.createRfcommSocketToServiceRecord(sppUuid);
            socket.connect();

            OutputStream outputStream = socket.getOutputStream();
            byte[] htmlBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            outputStream.write(htmlBytes);
            outputStream.flush();

            callbackContext.success("Printed successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Failed to print: ", e);
            callbackContext.error("Failed to print: " + e.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close socket: ", e);
                }
            }
        }
    }
}
