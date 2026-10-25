package org.Bacontete.hardcore_region_difficulty.common.elite;

/**
 * Elite spawn switch of one region or structure: follow the global elite_enabled, or force it on / off here.
 * Stored as the optional "elite_enabled" boolean of a region (missing = FOLLOW).
 */
public enum EliteMode {
    FOLLOW, ON, OFF;

    public static EliteMode of(Boolean value) {
        return value == null ? FOLLOW : value ? ON : OFF;
    }

    /** null = follow the global setting. */
    public Boolean toBoolean() {
        return this == FOLLOW ? null : this == ON;
    }
}
