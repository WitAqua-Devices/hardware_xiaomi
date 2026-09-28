/*
 * SPDX-FileCopyrightText: 2026 WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.battery;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Validates health HAL dates without changing the battery's stored values. */
final class BatteryDate {
    private BatteryDate() {}

    static LocalDate parse(long value, LocalDate today) {
        if (value <= 0) return null;
        try {
            final LocalDate date;
            // Some Xiaomi HALs pass the sysfs YYYYMMDD value through unchanged,
            // although BatteryManager specifies Unix seconds. Do not interpret
            // a malformed packed date as a timestamp (which would show 1970).
            if (value >= 20000101 && value <= 29991231) {
                date = LocalDate.of((int) (value / 10000),
                        (int) (value / 100 % 100), (int) (value % 100));
            } else {
                date = Instant.ofEpochSecond(value).atOffset(ZoneOffset.UTC).toLocalDate();
            }
            return date.getYear() >= 2000 && !date.isAfter(today) ? date : null;
        } catch (DateTimeException e) {
            return null;
        }
    }
}
