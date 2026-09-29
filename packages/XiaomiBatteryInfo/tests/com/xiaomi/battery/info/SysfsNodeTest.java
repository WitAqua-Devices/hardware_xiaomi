/*
 * SPDX-FileCopyrightText: 2026 WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.battery.info;

public final class SysfsNodeTest {
    private static int sCases;

    private static void check(String value, String expected) {
        String actual = SysfsNode.resolve(value);
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
        sCases++;
    }

    public static void main(String[] args) {
        check("battery/batt_sn", "/sys/class/power_supply/battery/batt_sn");
        check(" usb/apdo_max\n", "/sys/class/power_supply/usb/apdo_max");
        check("/sys/class/qcom-battery/fg1_soh", "/sys/class/qcom-battery/fg1_soh");
        check("/data/local/tmp/soh", null);
        check("/sysfoo/soh", null);
        check("/sys/class/../../data/soh", null);
        check("../../../data/soh", null);
        check("", null);
        check("  ", null);
        check(null, null);
        System.out.println("SysfsNodeTest: PASS (" + sCases + " cases)");
    }
}
