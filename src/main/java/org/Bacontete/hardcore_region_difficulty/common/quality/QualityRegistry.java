package org.Bacontete.hardcore_region_difficulty.common.quality;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.TextColor;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * All qualities, loaded from config/hardcore_region_difficulty-qualities.json.
 * <p>
 * The server (or single player) always uses the "local" data read from the file. A client connected to a remote
 * server uses the "remote" data the server sent at login, so colors, names and effects always match the server.
 */
public final class QualityRegistry {

    public static final String FILE_NAME = "hardcore_region_difficulty-qualities.json";
    private static final String DEFAULT_RESOURCE = "/defaults/hardcore_region_difficulty-qualities.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Z0-9_]{1,40}");

    // effect slots
    private static final int DAMAGE = 0;
    private static final int SPEED = 1;
    private static final int DURABILITY = 2;
    private static final int MINING = 3;
    private static final int REACH = 4;
    private static final int ARMOR_BONUS = 5;
    private static final int MOVE_SPEED = 6;
    private static final int HOLD_MINING = 7;
    private static final int SLOTS = 8;

    private static final class CategoryData {
        final List<QualityDefinition> list;
        final Map<String, QualityDefinition> byId;

        CategoryData(List<QualityDefinition> list) {
            this.list = Collections.unmodifiableList(list);
            Map<String, QualityDefinition> map = new HashMap<>();
            for (QualityDefinition d : list) {
                map.put(d.name(), d);
            }
            this.byId = map;
        }
    }

    private static final class Snapshot {
        final EnumMap<QualityCategory, CategoryData> data = new EnumMap<>(QualityCategory.class);

        Snapshot() {
            for (QualityCategory c : QualityCategory.values()) {
                data.put(c, new CategoryData(new ArrayList<>()));
            }
        }
    }

    private static volatile Snapshot local = new Snapshot();
    private static volatile Snapshot remote = null;
    private static volatile File file = null;
    private static volatile long loadedModified = -1L;

    /** Allowed range of the "tier" of a quality (tier_min / tier_max of the file). */
    public static final int DEFAULT_TIER_MIN = -5;
    public static final int DEFAULT_TIER_MAX = 5;
    private static final int TIER_LIMIT = 50;
    private static volatile int tierMin = DEFAULT_TIER_MIN;
    private static volatile int tierMax = DEFAULT_TIER_MAX;
    private static volatile boolean strictLimits = false;

    private QualityRegistry() {}

    // ------------------------------------------------------------------
    // Lookup
    // ------------------------------------------------------------------

    private static Snapshot active() {
        Snapshot r = remote;
        return r != null ? r : local;
    }

    /** Qualities of the category, in file order. Never null. */
    public static List<QualityDefinition> all(QualityCategory category) {
        return active().data.get(category).list;
    }

    /** Exact id lookup (ids are stored upper case on items). */
    public static QualityDefinition findExact(QualityCategory category, String id) {
        if (category == null || id == null) {
            return null;
        }
        return active().data.get(category).byId.get(id);
    }

    /** Case-insensitive id lookup (commands). */
    public static QualityDefinition find(QualityCategory category, String id) {
        if (id == null) {
            return null;
        }
        return findExact(category, id.trim().toUpperCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------
    // Local file
    // ------------------------------------------------------------------

    public static File getFile() {
        return file;
    }

    /** Lowest tier a quality may have (0 or below). */
    public static int getTierMin() { return tierMin; }

    /** Highest tier a quality may have (0 or above). */
    public static int getTierMax() { return tierMax; }

    /** True if the percent / flat effects are clamped to the original balance limits. */
    public static boolean isStrictLimits() { return strictLimits; }

    /** Call once at startup with the config directory. */
    public static synchronized void init(File configDir) {
        file = new File(configDir, FILE_NAME);
        reload();
    }

    public static boolean hasFileChanged() {
        File f = file;
        return f != null && f.exists() && f.lastModified() != loadedModified;
    }

    /**
     * Reads the file again (writing the default one first if it does not exist).
     * @return true if the file was read; false if it was broken and the previous data was kept.
     */
    public static synchronized boolean reload() {
        File f = file;
        if (f == null) {
            return false;
        }
        if (!f.exists()) {
            writeDefaultFile(f);
        }
        loadedModified = f.lastModified();

        JsonObject root = null;
        try (Reader reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
            if (parsed != null && parsed.isJsonObject()) {
                root = parsed.getAsJsonObject();
            } else {
                HardcoreRegionDifficulty.LOGGER.error("Quality file root must be a JSON object: {}", f);
            }
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Failed to read quality file {}", f, e);
        }

        boolean ok = root != null;
        if (root == null) {
            if (hasAnyLocal()) {
                HardcoreRegionDifficulty.LOGGER.error("Keeping the previous quality definitions.");
                return false;
            }
            HardcoreRegionDifficulty.LOGGER.error("Using the built-in default quality definitions.");
            root = readBundledDefault();
        }
        local = parse(root);
        int total = 0;
        for (QualityCategory c : QualityCategory.values()) {
            total += local.data.get(c).list.size();
        }
        HardcoreRegionDifficulty.LOGGER.info("Loaded {} quality definitions from {}", total, f.getName());
        return ok;
    }

    private static boolean hasAnyLocal() {
        for (QualityCategory c : QualityCategory.values()) {
            if (!local.data.get(c).list.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void writeDefaultFile(File f) {
        try (InputStream in = QualityRegistry.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                HardcoreRegionDifficulty.LOGGER.error("Bundled default quality file is missing: {}", DEFAULT_RESOURCE);
                return;
            }
            if (f.getParentFile() != null) {
                Files.createDirectories(f.getParentFile().toPath());
            }
            Files.copy(in, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not write the default quality file {}", f, e);
        }
    }

    private static JsonObject readBundledDefault() {
        try (InputStream in = QualityRegistry.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in != null) {
                JsonElement e = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
                if (e != null && e.isJsonObject()) {
                    return e.getAsJsonObject();
                }
            }
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not read the bundled default quality file", e);
        }
        return new JsonObject();
    }

    // ------------------------------------------------------------------
    // Remote data (client connected to a server)
    // ------------------------------------------------------------------

    /** All local definitions, flattened, for the sync packet. */
    public static List<QualityDefinition> allLocal() {
        List<QualityDefinition> out = new ArrayList<>();
        for (QualityCategory c : QualityCategory.values()) {
            out.addAll(local.data.get(c).list);
        }
        return out;
    }

    /** Replaces what a remote client uses with the definitions the server sent. */
    public static void setRemote(List<QualityDefinition> definitions) {
        Map<QualityCategory, List<QualityDefinition>> grouped = new EnumMap<>(QualityCategory.class);
        for (QualityCategory c : QualityCategory.values()) {
            grouped.put(c, new ArrayList<>());
        }
        for (QualityDefinition d : definitions) {
            grouped.get(d.getCategory()).add(d);
        }
        Snapshot snapshot = new Snapshot();
        for (QualityCategory c : QualityCategory.values()) {
            snapshot.data.put(c, new CategoryData(grouped.get(c)));
        }
        remote = snapshot;
    }

    public static void clearRemote() {
        remote = null;
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    private static Snapshot parse(JsonObject root) {
        boolean strict = root.has("strict_limits") && root.get("strict_limits").isJsonPrimitive()
                && root.get("strict_limits").getAsBoolean();
        int min = readLimit(root, "tier_min", DEFAULT_TIER_MIN);
        int max = readLimit(root, "tier_max", DEFAULT_TIER_MAX);
        if (min > 0) {
            HardcoreRegionDifficulty.LOGGER.warn("tier_min must be 0 or below, using 0");
            min = 0;
        }
        if (max < 0) {
            HardcoreRegionDifficulty.LOGGER.warn("tier_max must be 0 or above, using 0");
            max = 0;
        }
        tierMin = min;
        tierMax = max;
        strictLimits = strict;
        JsonObject categories = root.has("categories") && root.get("categories").isJsonObject()
                ? root.getAsJsonObject("categories") : new JsonObject();
        JsonObject bundled = null;

        Snapshot snapshot = new Snapshot();
        for (QualityCategory category : QualityCategory.values()) {
            JsonArray array;
            if (categories.has(category.key()) && categories.get(category.key()).isJsonArray()) {
                array = categories.getAsJsonArray(category.key());
            } else {
                // category missing (older file): use the built-in defaults for it
                if (bundled == null) {
                    bundled = readBundledDefault();
                }
                JsonObject bundledCategories = bundled.has("categories") && bundled.get("categories").isJsonObject()
                        ? bundled.getAsJsonObject("categories") : new JsonObject();
                array = bundledCategories.has(category.key()) && bundledCategories.get(category.key()).isJsonArray()
                        ? bundledCategories.getAsJsonArray(category.key()) : new JsonArray();
                HardcoreRegionDifficulty.LOGGER.warn("Quality file has no '{}' list, using the defaults for it", category.key());
            }
            List<QualityDefinition> list = new ArrayList<>();
            Map<String, Boolean> seen = new HashMap<>();
            for (JsonElement element : array) {
                try {
                    QualityDefinition d = parseEntry(category, element, strict, min, max);
                    if (d == null) {
                        continue;
                    }
                    if (seen.put(d.name(), Boolean.TRUE) != null) {
                        HardcoreRegionDifficulty.LOGGER.warn("Duplicate quality id '{}' in '{}' ignored", d.name(), category.key());
                        continue;
                    }
                    list.add(d);
                } catch (Exception e) {
                    HardcoreRegionDifficulty.LOGGER.warn("Skipping invalid quality entry in '{}': {}", category.key(), e.getMessage());
                }
            }
            // always show / list the qualities from the weakest to the strongest
            list.sort(QualityRegistry::compareStrength);
            snapshot.data.put(category, new CategoryData(list));
        }
        return snapshot;
    }

    private static QualityDefinition parseEntry(QualityCategory category, JsonElement element, boolean strict,
                                                int tierMin, int tierMax) {
        if (!element.isJsonObject()) {
            HardcoreRegionDifficulty.LOGGER.warn("Quality entry in '{}' is not an object, skipped", category.key());
            return null;
        }
        JsonObject o = element.getAsJsonObject();
        if (!o.has("id")) {
            HardcoreRegionDifficulty.LOGGER.warn("Quality entry in '{}' has no id, skipped", category.key());
            return null;
        }
        String id = o.get("id").getAsString().trim().toUpperCase(Locale.ROOT);
        if (!ID_PATTERN.matcher(id).matches()) {
            HardcoreRegionDifficulty.LOGGER.warn("Quality id '{}' in '{}' is invalid (use A-Z, 0-9 and _ only), skipped", id, category.key());
            return null;
        }

        String name = o.has("name") && o.get("name").isJsonPrimitive() ? o.get("name").getAsString() : null;

        TextColor color = TextColor.fromRgb(0xFFFFFF);
        if (o.has("color") && o.get("color").isJsonPrimitive()) {
            TextColor parsed = parseColor(o.get("color").getAsString());
            if (parsed != null) {
                color = parsed;
            } else {
                HardcoreRegionDifficulty.LOGGER.warn("Quality '{}' ({}) has an unknown color '{}', using white",
                        id, category.key(), o.get("color").getAsString());
            }
        }

        double[] eff = new double[SLOTS];
        if (o.has("effects") && o.get("effects").isJsonObject()) {
            readEffects(category, id, o.getAsJsonObject("effects"), eff, strict);
        }

        int rawTier = o.has("tier") && o.get("tier").isJsonPrimitive() ? o.get("tier").getAsInt() : deriveTier(eff);
        int tier = clampInt(rawTier, tierMin, tierMax);
        if (tier != rawTier) {
            HardcoreRegionDifficulty.LOGGER.warn("Quality '{}' ({}): tier {} is outside tier_min/tier_max ({}..{}), using {}",
                    id, category.key(), rawTier, tierMin, tierMax, tier);
        }
        int weight = o.has("weight") && o.get("weight").isJsonPrimitive()
                ? clampInt(o.get("weight").getAsInt(), 0, 10000) : 0;
        int forgeWeight = o.has("forge_weight") && o.get("forge_weight").isJsonPrimitive()
                ? clampInt(o.get("forge_weight").getAsInt(), -1, 10000) : -1;
        QualityDefinition.WeightGroup group = QualityDefinition.WeightGroup.parse(
                o.has("weight_group") && o.get("weight_group").isJsonPrimitive() ? o.get("weight_group").getAsString() : null,
                QualityDefinition.WeightGroup.fromTier(tier));
        int unlock = o.has("unlock_level") && o.get("unlock_level").isJsonPrimitive()
                ? clampInt(o.get("unlock_level").getAsInt(), 0, 100) : 0;

        return new QualityDefinition(category, id, name, color, tier, weight, forgeWeight, group, unlock,
                eff[DAMAGE], eff[SPEED], eff[DURABILITY], eff[MINING], eff[REACH],
                eff[ARMOR_BONUS], eff[MOVE_SPEED], eff[HOLD_MINING]);
    }

    /** Vanilla color name or #RRGGBB. Null if unknown. */
    private static TextColor parseColor(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("#")) {
            return TextColor.parseColor(s);
        }
        return TextColor.parseColor(s.toLowerCase(Locale.ROOT));
    }

    /** Effect keys each category understands; anything else is ignored. */
    private static Map<String, Integer> keysFor(QualityCategory category) {
        Map<String, Integer> keys = new LinkedHashMap<>();
        switch (category) {
            case WEAPON -> {
                keys.put("damage", DAMAGE);
                keys.put("attack_speed", SPEED);
                keys.put("durability", DURABILITY);
                keys.put("reach", REACH);
            }
            case TOOL -> {
                keys.put("damage", DAMAGE);
                keys.put("attack_speed", SPEED);
                keys.put("durability", DURABILITY);
                keys.put("mining_speed", MINING);
                keys.put("reach", REACH);
            }
            case ARMOR -> {
                keys.put("armor", DAMAGE);
                keys.put("durability", DURABILITY);
                keys.put("armor_bonus", ARMOR_BONUS);
            }
            case RANGED -> {
                keys.put("arrow_damage", DAMAGE);
                keys.put("draw_speed", SPEED);
                keys.put("durability", DURABILITY);
            }
            case SHIELD -> {
                keys.put("durability", DURABILITY);
                keys.put("armor_bonus", ARMOR_BONUS);
                keys.put("move_speed", MOVE_SPEED);
                keys.put("hold_mining", HOLD_MINING);
            }
        }
        return keys;
    }

    private static void readEffects(QualityCategory category, String id, JsonObject effects, double[] out, boolean strict) {
        Map<String, Integer> keys = keysFor(category);
        for (Map.Entry<String, JsonElement> entry : effects.entrySet()) {
            String key = entry.getKey().trim().toLowerCase(Locale.ROOT);
            Integer slot = keys.get(key);
            if (slot == null) {
                HardcoreRegionDifficulty.LOGGER.warn("Quality '{}' ({}): effect '{}' does not apply to this category and is ignored. Allowed: {}",
                        id, category.key(), entry.getKey(), keys.keySet());
                continue;
            }
            double value;
            try {
                value = entry.getValue().getAsDouble();
            } catch (Exception e) {
                HardcoreRegionDifficulty.LOGGER.warn("Quality '{}' ({}): effect '{}' is not a number, ignored", id, category.key(), key);
                continue;
            }
            out[slot] = clampEffect(slot, value, strict);
        }
    }

    private static double clampEffect(int slot, double value, boolean strict) {
        boolean flat = slot == REACH || slot == ARMOR_BONUS;
        double min;
        double max;
        if (strict) {
            if (slot == REACH) {
                // reach is capped at +/-0.5 blocks (a higher reach breaks combat / mining balance)
                min = -0.5D;
                max = 0.5D;
            } else {
                min = flat ? -0.5D : -0.20D;
                max = flat ? 1.5D : 0.20D;
            }
        } else {
            // safety range only: a percent below -90% would break the formulas
            min = flat ? -10.0D : -0.9D;
            max = flat ? 10.0D : 5.0D;
        }
        if (Double.isNaN(value)) {
            return 0.0D;
        }
        return Math.max(min, Math.min(max, value));
    }

    /** Overall strength of a quality (percent effects + flat effects at a lower weight). */
    private static double strength(QualityDefinition d) {
        return d.getDamagePercent() + d.getSpeedPercent() + d.getDurabilityPercent() + d.getMiningPercent()
                + d.getMoveSpeedPercent() + d.getHoldMiningPercent()
                + d.getReach() * 0.1D + d.getArmorBonus() * 0.1D;
    }

    /** Low to high: by tier first, then by overall strength, then by id (stable, deterministic). */
    private static int compareStrength(QualityDefinition a, QualityDefinition b) {
        int c = Integer.compare(a.getTier(), b.getTier());
        if (c != 0) {
            return c;
        }
        c = Double.compare(strength(a), strength(b));
        return c != 0 ? c : a.name().compareTo(b.name());
    }

    private static int deriveTier(double[] eff) {
        double sum = eff[DAMAGE] + eff[SPEED] + eff[DURABILITY] + eff[MINING] + eff[MOVE_SPEED] + eff[HOLD_MINING]
                + eff[REACH] * 0.1D + eff[ARMOR_BONUS] * 0.1D;
        if (Math.abs(sum) < 1.0E-9D) {
            return 0;
        }
        if (sum < 0) {
            return -1;
        }
        return sum >= 0.25D ? 2 : 1;
    }

    private static int readLimit(JsonObject root, String key, int def) {
        if (!root.has(key) || !root.get(key).isJsonPrimitive()) {
            return def;
        }
        try {
            return clampInt(root.get(key).getAsInt(), -TIER_LIMIT, TIER_LIMIT);
        } catch (Exception e) {
            return def;
        }
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
