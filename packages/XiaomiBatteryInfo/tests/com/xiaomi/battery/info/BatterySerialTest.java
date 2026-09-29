/*
 * SPDX-FileCopyrightText: 2026 WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.battery.info;

public final class BatterySerialTest {
    private static int sCases;

    private static void check(String value, String expected) {
        String actual = BatterySerial.parse(value);
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
        sCases++;
    }

    public static void main(String[] args) {
        check("  TEST1234567890\n", "TEST1234567890");
        check("00123456789", "00123456789");
        check("65", "65");
        check("84 69 83 84 49 50 51", "TEST123");
        check("84\t69\n83 84 0 0", "TEST");
        check(null, null);
        check("", null);
        check("   \n", null);
        check("0 0 0", null);
        check("84 0 69", null);
        check("84 256", null);
        check("84 -1", null);
        check("84 word", null);
        check("84 31", null);
        check("84 127", null);
        check("TEST\u0001SERIAL", null);
        check("X".repeat(1025), null);
        System.out.println("BatterySerialTest: PASS (" + sCases + " cases)");
    }
}
