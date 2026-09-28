/* SPDX-License-Identifier: Apache-2.0 */
package com.xiaomi.battery;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import android.content.ContentProviderClient;
import android.os.RemoteException;
import com.android.settingslib.utils.ThreadUtils;

/** Read-only vendor information for the dedicated Xiaomi battery screen. */
final class XiaomiBatteryInfoSection {
    private static final Uri URI = Uri.parse("content://com.xiaomi.battery.info");
    private final Context mContext;
    private final Preference[] mRows = new Preference[6];
    private final String[] mKeys = {"soh", "adapter_watts", "input_mv", "input_ma", "model", "serial"};
    private final int[] mFormats = {R.string.xiaomi_battery_percent,
            R.string.xiaomi_battery_watts, R.string.xiaomi_battery_mv,
            R.string.xiaomi_battery_ma, 0, 0};
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mGeneration;
    private boolean mResumed;
    private final Runnable mRefresh = this::refresh;

    XiaomiBatteryInfoSection(Context context, PreferenceScreen screen) {
        mContext = context;
        int[] titles = {R.string.xiaomi_battery_soh, R.string.xiaomi_battery_adapter,
                R.string.xiaomi_battery_input_voltage, R.string.xiaomi_battery_input_current,
                R.string.xiaomi_battery_model, R.string.xiaomi_battery_serial};
        for (int i = 0; i < mRows.length; i++) {
            Preference row = new Preference(context);
            row.setKey("xiaomi_battery_" + mKeys[i]);
            row.setTitle(titles[i]);
            row.setSelectable(false);
            row.setPersistent(false);
            row.setVisible(false);
            screen.addPreference(row);
            mRows[i] = row;
        }
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
            Bundle info = null;
            try (ContentProviderClient client = mContext.getContentResolver()
                    .acquireUnstableContentProviderClient(URI)) {
                if (client != null) info = client.call("get_info", null, null);
            } catch (RemoteException | IllegalArgumentException | SecurityException e) {
                // No provider (other devices), or not available for this user.
            }
            final Bundle result = info;
            mHandler.post(() -> {
                if (!mResumed || generation != mGeneration) return;
                for (int i = 0; i < mRows.length; i++) {
                    boolean available = result != null && result.containsKey(mKeys[i]);
                    mRows[i].setVisible(available);
                    if (available) {
                        mRows[i].setSummary(mFormats[i] == 0 ? result.getString(mKeys[i])
                                : mContext.getString(mFormats[i], result.getLong(mKeys[i])));
                    }
                }
                mHandler.postDelayed(mRefresh, 3000);
            });
        });
    }
}
