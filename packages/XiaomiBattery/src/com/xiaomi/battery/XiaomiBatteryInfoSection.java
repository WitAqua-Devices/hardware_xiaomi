/*
 * SPDX-FileCopyrightText: 2026 WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.battery;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.FooterPreference;
import java.util.LinkedHashMap;
import java.util.Map;

/** LOS battery information followed by read-only Xiaomi-specific details. */
final class XiaomiBatteryInfoSection {
    private final Context mContext;
    private final BatteryInfoReader mReader;
    private final Map<String, Preference> mRows = new LinkedHashMap<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mGeneration;
    private boolean mResumed;
    private final Runnable mRefresh = this::refresh;

    XiaomiBatteryInfoSection(Context context, PreferenceScreen screen) {
        mContext = context;
        mReader = new BatteryInfoReader(context);
        // Keep these nine rows in the same order as Settings/res/xml/battery_info.xml.
        addRow(screen, "technology", R.string.battery_technology);
        addRow(screen, "health", R.string.battery_health);
        addRow(screen, "temperature", R.string.battery_temperature);
        addRow(screen, "voltage", R.string.battery_voltage);
        addRow(screen, "design_capacity", R.string.xiaomi_battery_design_capacity);
        addRow(screen, "full_capacity", R.string.xiaomi_battery_full_capacity);
        addRow(screen, "manufacturing_date", R.string.xiaomi_battery_manufacturing_date);
        addRow(screen, "first_usage_date", R.string.xiaomi_battery_first_usage_date);
        addRow(screen, "cycle_count", R.string.xiaomi_battery_cycle_count);

        PreferenceCategory vendor = new PreferenceCategory(context);
        vendor.setKey("xiaomi_details");
        vendor.setTitle(R.string.xiaomi_battery_details);
        screen.addPreference(vendor);
        addRow(vendor, "soh", R.string.xiaomi_battery_soh);
        addRow(vendor, "model", R.string.xiaomi_battery_model);
        addRow(vendor, "serial", R.string.xiaomi_battery_serial);
        addRow(vendor, "adapter_watts", R.string.xiaomi_battery_adapter);
        addRow(vendor, "input_mv", R.string.xiaomi_battery_input_voltage);
        addRow(vendor, "input_ma", R.string.xiaomi_battery_input_current);

        FooterPreference footer = new FooterPreference(context);
        footer.setKey("battery_info_footer");
        footer.setTitle(R.string.battery_cycle_count_footer);
        screen.addPreference(footer);
    }

    private void addRow(PreferenceGroup parent, String key, int title) {
        Preference row = new Preference(mContext);
        row.setKey("xiaomi_battery_" + key);
        row.setTitle(title);
        row.setPersistent(false);
        row.setCopyingEnabled(true);
        row.setSummary(R.string.battery_info_unavailable);
        parent.addPreference(row);
        mRows.put(key, row);
    }

    void onResume() {
        mResumed = true;
        mGeneration++;
        refresh();
    }

    void onPause() {
        mResumed = false;
        mGeneration++;
        mHandler.removeCallbacks(mRefresh);
    }

    private void refresh() {
        final int generation = mGeneration;
        ThreadUtils.postOnBackgroundThread(() -> {
            final Bundle result = mReader.read();
            mHandler.post(() -> {
                if (!mResumed || generation != mGeneration) return;
                for (Map.Entry<String, Preference> entry : mRows.entrySet()) {
                    String summary = result.getString(entry.getKey());
                    entry.getValue().setSummary(summary == null
                            ? mContext.getString(R.string.battery_info_unavailable) : summary);
                }
                mHandler.postDelayed(mRefresh, 3000);
            });
        });
    }
}
