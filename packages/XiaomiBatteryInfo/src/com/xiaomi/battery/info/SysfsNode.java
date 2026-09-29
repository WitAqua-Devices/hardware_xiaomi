/*
 * SPDX-FileCopyrightText: 2026 WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.battery.info;

/** Resolves the sysfs node names a device lists for each value. */
final class SysfsNode {
    private static final String SYSFS = "/sys/";
    private static final String POWER_SUPPLY = "/sys/class/power_supply/";

    private SysfsNode() {}

    /**
     * Names relative to power_supply cover most kernels; an absolute path lets a
     * device point at a vendor class such as qcom-battery instead. Anything that
     * would leave sysfs is rejected so an overlay cannot turn this into a file reader.
     */
    static String resolve(String node) {
        if (node == null) return null;
        String name = node.trim();
        if (name.isEmpty() || name.contains("..")) return null;
        if (name.startsWith("/")) return name.startsWith(SYSFS) ? name : null;
        return POWER_SUPPLY + name;
    }
}
