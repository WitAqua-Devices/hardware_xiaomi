/*
 * SPDX-FileCopyrightText: 2026 WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.battery.info;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Read-only, fixed-schema access to Xiaomi gauge and charger information. */
public class BatteryInfoProvider extends ContentProvider {
    private static final String PERMISSION = "com.xiaomi.battery.info.READ_INFO";

    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        getContext().enforceCallingOrSelfPermission(PERMISSION, "Battery information");
        if (!"get_info".equals(method)) return null;
        Bundle result = new Bundle();
        String model = readText(R.array.config_battery_model_nodes);
        if (model != null) result.putString("model", model);
        String serial = readSerial(R.array.config_battery_serial_nodes);
        if (serial != null) result.putString("serial", serial);
        Long soh = read(R.array.config_battery_soh_nodes);
        if (soh != null && soh >= 0 && soh <= 100) result.putLong("soh", soh);
        Long apdo = read(R.array.config_adapter_watts_nodes);
        if (apdo != null && apdo > 0) result.putLong("adapter_watts", apdo);
        int divisor = getContext().getResources().getInteger(R.integer.config_usb_input_divisor);
        Long voltage = read(R.array.config_usb_voltage_nodes);
        Long current = read(R.array.config_usb_current_nodes);
        if (divisor > 0 && voltage != null && voltage > 0) {
            result.putLong("input_mv", voltage / divisor);
            if (current != null && current >= 0) result.putLong("input_ma", current / divisor);
        }
        return result;
    }

    private String[] nodes(int array) {
        return getContext().getResources().getStringArray(array);
    }

    private String readText(int array) {
        for (String node : nodes(array)) {
            String value = readText(node);
            if (value != null) return value;
        }
        return null;
    }

    // A node can exist and still hold nothing usable, so keep looking past it.
    private String readSerial(int array) {
        for (String node : nodes(array)) {
            String serial = decodeSerial(readText(node));
            if (serial != null) return serial;
        }
        return null;
    }

    private Long read(int array) {
        for (String node : nodes(array)) {
            try {
                String value = readText(node);
                if (value != null) return Long.parseLong(value);
            } catch (NumberFormatException e) {
                // Try the next node.
            }
        }
        return null;
    }

    private static String readText(String node) {
        String path = SysfsNode.resolve(node);
        if (path == null) return null;
        try {
            String value = new String(Files.readAllBytes(Paths.get(path))).trim();
            return value.isEmpty() ? null : value;
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    // Xiaomi kernels expose either a plain serial or whitespace-separated ASCII bytes.
    static String decodeSerial(String value) {
        return BatterySerial.parse(value);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] args, String order) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read only");
    }
    @Override public int delete(Uri uri, String selection, String[] args) {
        throw new UnsupportedOperationException("Read only");
    }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException("Read only");
    }
}
