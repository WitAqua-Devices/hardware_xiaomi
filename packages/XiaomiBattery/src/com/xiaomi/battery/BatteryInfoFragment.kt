/*
 * SPDX-FileCopyrightText: 2026 WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.battery

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class BatteryInfoFragment : PreferenceFragmentCompat() {
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
