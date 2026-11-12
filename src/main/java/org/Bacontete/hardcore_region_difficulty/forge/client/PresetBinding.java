package org.Bacontete.hardcore_region_difficulty.forge.client;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyPreset;

/**
 * Links the preset panel to the input fields of the settings screen (the per-level growth, the boss bonuses, the
 * Ender Dragon / Wither / Warden and the raid level, which live in different groups and even different tabs).
 * <p>
 * With it the panel can
 * <ul>
 *   <li>read what is typed in the fields <b>right now</b> (also what is not saved yet), to create a preset from it,</li>
 *   <li>write the values of a preset into the fields, so the player sees them change at once.</li>
 * </ul>
 * Only load this class when cloth_config is present.
 */
public final class PresetBinding {

    private final AbstractConfigListEntry<?>[] fields = new AbstractConfigListEntry<?>[DifficultyPreset.KEYS.length];

    /** Registers the field that edits {@code DifficultyPreset.KEYS[index]}; returns it so it can be added in one go. */
    public <E extends AbstractConfigListEntry<?>> E bind(int index, E entry) {
        fields[index] = entry;
        return entry;
    }

    /**
     * The values of all the fields now, in the order of {@link DifficultyPreset#KEYS}.
     * @param fallback used for a field that is not built (or that holds no number)
     */
    public double[] read(double[] fallback) {
        double[] out = fallback.clone();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null) continue;
            try {
                Object value = fields[i].getValue();
                if (value instanceof Number n) out[i] = n.doubleValue();
            } catch (RuntimeException ignored) {
                // an unreadable field keeps the fallback
            }
        }
        return out;
    }

    /**
     * Types the values of the preset into the fields.
     * @return false if some field could not be set (the screen then still works, the player edits it by hand)
     */
    public boolean apply(DifficultyPreset preset) {
        boolean all = true;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null) {
                all = false;
                continue;
            }
            Object value = i == DifficultyPreset.RAID_LEVEL
                    ? (Object) Integer.valueOf(preset.raid())
                    : (Object) Double.valueOf(preset.get(i));
            if (!set(fields[i], value)) all = false;
        }
        return all;
    }

    /**
     * Calls {@code setValue(Object)} of the Cloth text field entries (generic erasure of {@code setValue(T)}).
     * Reflection keeps this class free of the exact entry types of the Cloth version in use.
     */
    private static boolean set(AbstractConfigListEntry<?> entry, Object value) {
        try {
            entry.getClass().getMethod("setValue", Object.class).invoke(entry, value);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
