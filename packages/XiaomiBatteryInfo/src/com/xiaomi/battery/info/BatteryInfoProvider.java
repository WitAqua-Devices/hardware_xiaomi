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
    private static final String BASE = "/sys/class/power_supply/";

    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        getContext().enforceCallingOrSelfPermission(PERMISSION, "Battery information");
        if (!"get_info".equals(method)) return null;
        Bundle result = new Bundle();
        String model = readText("battery/model_name");
        if (model != null) result.putString("model", model);
        String serial = decodeSerial(readText("battery/soh_sn"));
        if (serial != null) result.putString("serial", serial);
        Long soh = read("bms/soh");
        if (soh != null && soh >= 0 && soh <= 100) result.putLong("soh", soh);
        Long apdo = read("usb/apdo_max");
        if (apdo != null && apdo > 0) result.putLong("adapter_watts", apdo);
        int divisor = getContext().getResources().getInteger(R.integer.config_usb_input_divisor);
        Long voltage = read("usb/voltage_now");
        Long current = read("usb/input_current_now");
        if (divisor > 0 && voltage != null && voltage > 0) {
            result.putLong("input_mv", voltage / divisor);
            if (current != null && current >= 0) result.putLong("input_ma", current / divisor);
        }
        return result;
    }

    private static String readText(String node) {
        try {
            String value = new String(Files.readAllBytes(Paths.get(BASE + node))).trim();
            return value.isEmpty() ? null : value;
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    private static Long read(String node) {
        try {
            String value = readText(node);
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Xiaomi's soh_sn exports decimal ASCII bytes rather than a text string.
    static String decodeSerial(String value) {
        if (value == null) return null;
        StringBuilder decoded = new StringBuilder();
        try {
            for (String token : value.split("\\s+")) {
                int ch = Integer.parseInt(token);
                if (ch == 0) break;
                if (ch < 32 || ch > 126) return null;
                decoded.append((char) ch);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        String result = decoded.toString().trim();
        return result.isEmpty() ? null : result;
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
