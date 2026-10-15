package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EnchantmentSettings {
    private final Map<Integer, Double> baseChancePerLevel;
    private final Map<Integer, Integer> maxLevelPerDifficulty;

    private double librarianTradeChance = 0.08;

    private int librarianMaxUses = 8;

    private int librarianMinEmeraldPrice = 40;

    private boolean librarianOnlyLevel1 = true;

    private double dungeonChestChance = 0.08;

    private List<String> dungeonLootTables = new ArrayList<>(List.of("minecraft:chests/simple_dungeon"));

    public record DungeonBookTier(double chance, int maxLevel) {}

    private final TreeMap<Integer, DungeonBookTier> dungeonBookTiers = new TreeMap<>();

    private boolean dungeonBookAuto = true;

    /** Loot Advanced (enchant nâng cao trên đồ trong rương + sách) chỉ ra ở vùng có cấp từ mức này trở lên. */
    private int advancedMinLevel = 3;

    /** Nhân vào mọi tỉ lệ ra loot Advanced (đồ enchant, sách dungeon, sách công trình, sách Armor Piercing). */
    private double advancedChanceMultiplier = 0.7;

    /** Tỉ lệ giữa hai cấp enchant liền nhau của loot Advanced (đồ + sách dungeon): nhỏ hơn = cấp cao khó ra hơn. */
    private double advancedLootLevelDecay = 0.6;

    private double dungeonBookLevelDecay = 0.5;

    public EnchantmentSettings() {
        dungeonBookTiers.put(3, new DungeonBookTier(0.04, 2));
        dungeonBookTiers.put(4, new DungeonBookTier(0.06, 3));
        dungeonBookTiers.put(5, new DungeonBookTier(0.08, 5));

        this.baseChancePerLevel = new HashMap<>();
        this.maxLevelPerDifficulty = new HashMap<>();

        baseChancePerLevel.put(1, 0.01);
        baseChancePerLevel.put(2, 0.02);
        baseChancePerLevel.put(3, 0.03);
        baseChancePerLevel.put(4, 0.05);
        baseChancePerLevel.put(5, 0.08);

        maxLevelPerDifficulty.put(1, 1);
        maxLevelPerDifficulty.put(2, 2);
        maxLevelPerDifficulty.put(3, 3);
        maxLevelPerDifficulty.put(4, 4);
        maxLevelPerDifficulty.put(5, 5);
    }

    public static EnchantmentSettings fromJson(JsonObject obj) {
        EnchantmentSettings settings = new EnchantmentSettings();

        if (obj.has("base_chance_per_level")) {
            var chanceObj = obj.getAsJsonObject("base_chance_per_level");
            chanceObj.entrySet().forEach(entry -> {
                try {
                    settings.baseChancePerLevel.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsDouble());
                } catch (RuntimeException ignored) {

                }
            });
        }

        if (obj.has("max_level_per_difficulty")) {
            var maxObj = obj.getAsJsonObject("max_level_per_difficulty");
            maxObj.entrySet().forEach(entry -> {
                try {
                    settings.maxLevelPerDifficulty.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsInt());
                } catch (RuntimeException ignored) {

                }
            });
        }

        if (obj.has("librarian_trade_chance")) {
            try {
                settings.librarianTradeChance = clamp01(obj.get("librarian_trade_chance").getAsDouble());
            } catch (RuntimeException ignored) {

            }
        }

        if (obj.has("librarian_max_uses")) {
            try {
                settings.librarianMaxUses = Math.max(1, obj.get("librarian_max_uses").getAsInt());
            } catch (RuntimeException ignored) {

            }
        }
        if (obj.has("librarian_min_emerald_price")) {
            try {

                settings.librarianMinEmeraldPrice = Math.max(1, Math.min(64, obj.get("librarian_min_emerald_price").getAsInt()));
            } catch (RuntimeException ignored) {

            }
        }
        if (obj.has("librarian_only_level_1")) {
            try {
                settings.librarianOnlyLevel1 = obj.get("librarian_only_level_1").getAsBoolean();
            } catch (RuntimeException ignored) {

            }
        }
        if (obj.has("dungeon_chest_chance")) {
            try {
                settings.dungeonChestChance = clamp01(obj.get("dungeon_chest_chance").getAsDouble());
            } catch (RuntimeException ignored) {

            }
        }
        if (obj.has("dungeon_book_auto")) {
            try {
                settings.dungeonBookAuto = obj.get("dungeon_book_auto").getAsBoolean();
            } catch (RuntimeException ignored) {

            }
        }
        if (obj.has("advanced_min_level")) {
            try {
                settings.advancedMinLevel = Math.max(1, Math.min(7, obj.get("advanced_min_level").getAsInt()));
            } catch (RuntimeException ignored) {

            }
        }
        if (obj.has("advanced_chance_multiplier")) {
            try {
                settings.advancedChanceMultiplier = Math.max(0.0, Math.min(1.0, obj.get("advanced_chance_multiplier").getAsDouble()));
            } catch (RuntimeException ignored) {

            }
        }
        if (obj.has("advanced_loot_level_decay")) {
            try {
                settings.advancedLootLevelDecay = Math.max(0.1, Math.min(1.0, obj.get("advanced_loot_level_decay").getAsDouble()));
            } catch (RuntimeException ignored) {

            }
        }
        if (obj.has("dungeon_book_level_decay")) {
            try {
                settings.dungeonBookLevelDecay = Math.max(0.1, Math.min(1.0, obj.get("dungeon_book_level_decay").getAsDouble()));
            } catch (RuntimeException ignored) {

            }
        }
        if (obj.has("dungeon_book_by_level") && obj.get("dungeon_book_by_level").isJsonObject()) {

            TreeMap<Integer, DungeonBookTier> tiers = new TreeMap<>();
            obj.getAsJsonObject("dungeon_book_by_level").entrySet().forEach(entry -> {
                try {
                    JsonObject t = entry.getValue().getAsJsonObject();
                    double chance = clamp01(t.get("chance").getAsDouble());
                    int max = Math.max(1, t.get("max_level").getAsInt());
                    tiers.put(Integer.parseInt(entry.getKey()), new DungeonBookTier(chance, max));
                } catch (RuntimeException ignored) {

                }
            });
            settings.dungeonBookTiers.clear();
            settings.dungeonBookTiers.putAll(tiers);
        }
        if (obj.has("dungeon_loot_tables") && obj.get("dungeon_loot_tables").isJsonArray()) {
            List<String> tables = new ArrayList<>();
            for (JsonElement e : obj.getAsJsonArray("dungeon_loot_tables")) {
                try {
                    String id = e.getAsString().trim();
                    if (!id.isEmpty()) tables.add(id);
                } catch (RuntimeException ignored) {

                }
            }
            settings.dungeonLootTables = tables;
        }

        return settings;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    public boolean isDungeonLootTable(String tableId) {
        for (String entry : dungeonLootTables) {
            if (entry.endsWith("*")) {
                if (tableId.startsWith(entry.substring(0, entry.length() - 1))) return true;
            } else if (entry.equals(tableId)) {
                return true;
            }
        }
        return false;
    }

    public int getAdvancedMinLevel() { return advancedMinLevel; }
    public double getAdvancedChanceMultiplier() { return advancedChanceMultiplier; }
    public double getAdvancedLootLevelDecay() { return advancedLootLevelDecay; }

    /** Vùng cấp này đã đủ để loot ra đồ / sách Advanced chưa. */
    public boolean allowsAdvancedLoot(int difficultyLevel) { return difficultyLevel >= advancedMinLevel; }

    public boolean isDungeonBookAuto() { return dungeonBookAuto; }
    public double getDungeonBookLevelDecay() { return dungeonBookLevelDecay; }

    public DungeonBookTier getDungeonBookTier(int difficultyLevel) {
        Map.Entry<Integer, DungeonBookTier> e = dungeonBookTiers.floorEntry(difficultyLevel);
        return e == null ? null : e.getValue();
    }

    public double getLibrarianTradeChance() { return librarianTradeChance; }
    public int getLibrarianMaxUses() { return librarianMaxUses; }
    public int getLibrarianMinEmeraldPrice() { return librarianMinEmeraldPrice; }
    public boolean isLibrarianOnlyLevel1() { return librarianOnlyLevel1; }
    public double getDungeonChestChance() { return dungeonChestChance; }
    public List<String> getDungeonLootTables() { return dungeonLootTables; }

    public double getBaseChance(int difficultyLevel) {
        if (difficultyLevel <= 0) return 0.0;
        Double exact = baseChancePerLevel.get(difficultyLevel);
        if (exact != null) return exact;
        return baseChancePerLevel.entrySet().stream()
            .filter(e -> e.getKey() < difficultyLevel)
            .max(java.util.Map.Entry.comparingByKey())
            .map(java.util.Map.Entry::getValue)
            .orElse(0.01);
    }

    public int getMaxLevel(int difficultyLevel) {
        Integer exact = maxLevelPerDifficulty.get(difficultyLevel);
        if (exact != null) return Math.max(1, exact);
        return maxLevelPerDifficulty.entrySet().stream()
            .filter(e -> e.getKey() < difficultyLevel)
            .max(java.util.Map.Entry.comparingByKey())
            .map(e -> Math.max(1, e.getValue()))
            .orElse(1);
    }

    /** Tỉ lệ cơ sở cao nhất trong bảng (dùng để quy về 0..1 khi tính trần tỉ lệ loot). */
    public double getMaxBaseChance() {
        return baseChancePerLevel.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    public Map<Integer, Double> getBaseChancePerLevel() { return baseChancePerLevel; }
    public Map<Integer, Integer> getMaxLevelPerDifficulty() { return maxLevelPerDifficulty; }
}
