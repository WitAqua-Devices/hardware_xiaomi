/*
 * SPDX-FileCopyrightText: 2026 WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.battery

import android.os.Bundle
import com.android.settingslib.widget.SettingsBasePreferenceFragment

class BatteryInfoFragment : SettingsBasePreferenceFragment() {
    private lateinit var section: XiaomiBatteryInfoSection

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        section = XiaomiBatteryInfoSection(requireContext(), preferenceScreen)
    }

    override fun onResume() {
        super.onResume()
        section.onResume()
    }

    override fun onPause() {
        section.onPause()
        super.onPause()
    }
}
