/*
 * SPDX-FileCopyrightText: 2026 WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.battery;

import java.time.LocalDate;
import java.time.ZoneOffset;

/** Host-only regression test: compile with BatteryDate.java, then run this class. */
public final class BatteryDateTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    private static void check(long raw, LocalDate expected) {
        LocalDate actual = BatteryDate.parse(raw, TODAY);
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(raw + ": expected " + expected + ", got " + actual);
        }
    }

    public static void main(String[] args) {
        check(20260516L, LocalDate.of(2026, 5, 16));
        check(20260913L, LocalDate.of(2026, 9, 13));
        check(20240229L, LocalDate.of(2024, 2, 29));
        check(TODAY.atStartOfDay(ZoneOffset.UTC).toEpochSecond(), TODAY);
        check(LocalDate.of(2026, 5, 16).atStartOfDay(ZoneOffset.UTC).toEpochSecond(),
                LocalDate.of(2026, 5, 16));
        for (long raw : new long[] {0, -1, Long.MIN_VALUE, Long.MAX_VALUE,
                20260229, 20261301, 20260010, 20260500, 20260929, 19700101,
                TODAY.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond(),
                TODAY.atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1000}) {
            check(raw, null);
        }
        System.out.println("BatteryDateTest: PASS (17 cases)");
    }
}
