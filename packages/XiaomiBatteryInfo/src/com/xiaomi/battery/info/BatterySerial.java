/*
 * SPDX-FileCopyrightText: 2026 WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.battery.info;

/** Parses the plain and decimal-ASCII forms used by Xiaomi battery drivers. */
final class BatterySerial {
    private BatterySerial() {}

    static String parse(String value) {
        if (value == null) return null;
        String serial = value.trim();
        if (serial.isEmpty() || serial.length() > 1024) return null;
        String[] tokens = serial.split("\\s+");
        if (tokens.length == 1) {
            // Plain numeric serials must not be mistaken for a single ASCII byte.
            for (int i = 0; i < serial.length(); i++) {
                char ch = serial.charAt(i);
                if (ch < 33 || ch > 126) return null;
            }
            return serial;
        }
        StringBuilder decoded = new StringBuilder();
        boolean terminated = false;
        try {
            for (String token : tokens) {
                if (!token.matches("[0-9]{1,3}")) return null;
                int ch = Integer.parseInt(token);
                if (ch == 0) {
                    terminated = true;
                } else {
                    if (terminated || ch < 32 || ch > 126) return null;
                    decoded.append((char) ch);
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        String result = decoded.toString().trim();
        return result.isEmpty() ? null : result;
    }
}
