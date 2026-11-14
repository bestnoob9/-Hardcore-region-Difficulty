package org.Bacontete.hardcore_region_difficulty.forge.client;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyPreset;

public final class PresetBinding {

    private final AbstractConfigListEntry<?>[] fields = new AbstractConfigListEntry<?>[DifficultyPreset.KEYS.length];

    /** Registers the field that edits {@code DifficultyPreset.KEYS[index]}; returns it so it can be added in one go. */
    public <E extends AbstractConfigListEntry<?>> E bind(int index, E entry) {
        fields[index] = entry;
        return entry;
    }

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

    private static boolean set(AbstractConfigListEntry<?> entry, Object value) {
        Class<?> c = entry.getClass();
        for (java.lang.reflect.Method m : c.getMethods()) {
            if (!m.getName().equals("setValue") || m.getParameterCount() != 1) continue;
            Class<?> pt = m.getParameterTypes()[0];
            Object arg = convert(value, pt);
            if (arg == null) continue;
            try {
                m.invoke(entry, arg);
                return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // try the next candidate
            }
        }
        String text = text(value);
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            try {
                java.lang.reflect.Field f = k.getDeclaredField("textFieldWidget");
                f.setAccessible(true);
                Object box = f.get(entry);
                if (box instanceof net.minecraft.client.gui.components.EditBox eb) {
                    eb.setValue(text);
                    return true;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // look in the superclass
            }
        }
        return false;
    }

    private static Object convert(Object v, Class<?> type) {
        if (type == Object.class || type.isInstance(v)) return v;
        if (v instanceof Number n) {
            if (type == int.class || type == Integer.class) return (int) Math.round(n.doubleValue());
            if (type == double.class || type == Double.class) return n.doubleValue();
            if (type == long.class || type == Long.class) return Math.round(n.doubleValue());
        }
        if (type == String.class) return text(v);
        return null;
    }
    
    private static String text(Object v) {
        return v instanceof Integer || v instanceof Long ? v.toString() : trim(((Number) v).doubleValue());
    }

    private static String trim(double d) {
        return Double.toString(d);
    }
}