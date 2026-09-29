/*
 * SPDX-FileCopyrightText: 2026 WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.battery;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.RemoteException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Reads platform health data and optional vendor details. Never writes battery nodes. */
final class BatteryInfoReader {
    private static final Uri URI = Uri.parse("content://com.xiaomi.battery.info");
    private final Context mContext;

    BatteryInfoReader(Context context) {
        mContext = context;
    }

    Bundle read() {
        Bundle result = new Bundle();
        readFrameworkInfo(result);
        // Standard rows remain usable even when the vendor provider is absent or denied.
        try (ContentProviderClient client = mContext.getContentResolver()
                .acquireUnstableContentProviderClient(URI)) {
            Bundle vendor = client == null ? null : client.call("get_info", null, null);
            if (vendor != null) {
                putNumber(result, vendor, "soh", R.string.xiaomi_battery_percent);
                putNumber(result, vendor, "adapter_watts", R.string.xiaomi_battery_watts);
                putNumber(result, vendor, "input_mv", R.string.xiaomi_battery_mv);
                putNumber(result, vendor, "input_ma", R.string.xiaomi_battery_ma);
                putString(result, "model", vendor.getString("model"));
                putString(result, "serial", vendor.getString("serial"));
            }
        } catch (RemoteException | IllegalArgumentException | SecurityException e) {
            // Missing values are displayed as Unavailable, not as fabricated zeros.
        }
        return result;
    }

    private void readFrameworkInfo(Bundle result) {
        Intent battery = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) {
            putString(result, "technology", battery.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY));
            result.putString("health", mContext.getString(healthLabel(battery.getIntExtra(
                    BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN))));
            if (battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) {
                // A real zero/negative temperature is valid; missing is not zero.
                int tenths = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
                if (tenths != Integer.MIN_VALUE) {
                    putMeasure(result, "temperature", tenths / 10f, MeasureUnit.CELSIUS);
                }
            }
            int millivolts = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            if (millivolts > 0) putMeasure(result, "voltage", millivolts / 1000f, MeasureUnit.VOLT);
            int design = battery.getIntExtra(BatteryManager.EXTRA_DESIGN_CAPACITY, -1);
            int full = battery.getIntExtra(BatteryManager.EXTRA_MAXIMUM_CAPACITY, -1);
            if (design >= 1000) {
                result.putString("design_capacity", mContext.getString(
                        R.string.xiaomi_battery_mah, design / 1000));
            }
            if (full >= 1000) {
                // Match LOS: full charge capacity and its percentage of design capacity.
                result.putString("full_capacity", design >= 1000
                        ? mContext.getString(R.string.battery_maximum_capacity_summary,
                                full / 1000, (full / 1000L) * 100 / (design / 1000L))
                        : mContext.getString(R.string.xiaomi_battery_mah, full / 1000));
            }
            int cycles = battery.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1);
            if (cycles >= 0) result.putString("cycle_count", Integer.toString(cycles));
        }
        BatteryManager manager = mContext.getSystemService(BatteryManager.class);
        if (manager == null) return;
        putDate(result, manager, "manufacturing_date", BatteryManager.BATTERY_PROPERTY_MANUFACTURING_DATE);
        putDate(result, manager, "first_usage_date", BatteryManager.BATTERY_PROPERTY_FIRST_USAGE_DATE);
        try {
            putString(result, "serial", manager.getStringProperty(BatteryManager.BATTERY_PROPERTY_SERIAL_NUMBER));
        } catch (SecurityException | UnsupportedOperationException e) {
            // Keep the row, and allow the vendor provider to supply it if accessible.
        }
        try {
            long soh = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH);
            if (soh > 0 && soh <= 100) {
                result.putString("soh", mContext.getString(R.string.xiaomi_battery_percent, soh));
            }
        } catch (SecurityException | UnsupportedOperationException e) {
            // The Xiaomi fuel gauge may still provide SOH independently.
        }
    }

    private void putDate(Bundle result, BatteryManager manager, String key, int property) {
        try {
            LocalDate date = BatteryDate.parse(manager.getLongProperty(property),
                    LocalDate.now(ZoneOffset.UTC));
            if (date != null) {
                DateTimeFormatter format = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                        .withLocale(locale()).withDecimalStyle(DecimalStyle.of(locale()));
                result.putString(key, date.format(format));
            }
        } catch (SecurityException | UnsupportedOperationException e) {
            // No extra privileges or raw sysfs fallback for restricted dates.
        }
    }

    private Locale locale() {
        return mContext.getResources().getConfiguration().getLocales().get(0);
    }

    private void putMeasure(Bundle result, String key, float value, MeasureUnit unit) {
        result.putString(key, MeasureFormat.getInstance(locale(), MeasureFormat.FormatWidth.SHORT)
                .format(new Measure(value, unit)));
    }

    private void putNumber(Bundle result, Bundle vendor, String key, int format) {
        if (vendor.containsKey(key)) {
            result.putString(key, mContext.getString(format, vendor.getLong(key)));
        }
    }

    private static void putString(Bundle result, String key, String value) {
        if (value != null && !value.trim().isEmpty()) result.putString(key, value.trim());
    }

    private static int healthLabel(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return R.string.battery_health_good;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return R.string.battery_health_overheat;
            case BatteryManager.BATTERY_HEALTH_DEAD: return R.string.battery_health_dead;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return R.string.battery_health_over_voltage;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return R.string.battery_health_unspecified_failure;
            case BatteryManager.BATTERY_HEALTH_COLD: return R.string.battery_health_cold;
            default: return R.string.battery_health_unknown;
        }
    }
}
