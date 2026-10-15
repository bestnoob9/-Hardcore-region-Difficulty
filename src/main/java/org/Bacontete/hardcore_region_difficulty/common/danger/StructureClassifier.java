package org.Bacontete.hardcore_region_difficulty.common.danger;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

public final class StructureClassifier {
    private StructureClassifier() {}

    public record Rating(StructureType type, int level) {
        public static final Rating SAFE = new Rating(StructureType.SAFE, 0);
        public static final Rating UNKNOWN = new Rating(StructureType.UNKNOWN, 0);

        public boolean isDangerous() {
            return level > 0 && (type == StructureType.DUNGEON || type == StructureType.BOSS);
        }

        public static Rating ofLevel(int level) {
            if (level <= 0) return SAFE;
            return new Rating(level >= 5 ? StructureType.BOSS : StructureType.DUNGEON, level);
        }
    }

    private static final int BOSS = 5;
    private static final int MAJOR = 4;
    private static final int DUNGEON = 3;
    private static final int MINOR = 2;

    private static final Map<String, Rating> VANILLA = new HashMap<>();
    static {
        VANILLA.put("minecraft:ancient_city", new Rating(StructureType.BOSS, 5));
        VANILLA.put("minecraft:end_city", new Rating(StructureType.DUNGEON, 5));
        VANILLA.put("minecraft:bastion_remnant", new Rating(StructureType.DUNGEON, 5));
        VANILLA.put("minecraft:fortress", new Rating(StructureType.DUNGEON, 4));
        VANILLA.put("minecraft:stronghold", new Rating(StructureType.DUNGEON, 3));
        VANILLA.put("minecraft:monument", new Rating(StructureType.DUNGEON, 4));
        VANILLA.put("minecraft:mansion", new Rating(StructureType.DUNGEON, 4));
        VANILLA.put("minecraft:desert_pyramid", new Rating(StructureType.DUNGEON, 3));
        VANILLA.put("minecraft:jungle_pyramid", new Rating(StructureType.DUNGEON, 3));
        VANILLA.put("minecraft:pillager_outpost", new Rating(StructureType.DUNGEON, 3));
        VANILLA.put("minecraft:mineshaft", new Rating(StructureType.DUNGEON, 3));
        VANILLA.put("minecraft:mineshaft_mesa", new Rating(StructureType.DUNGEON, 3));

        for (String s : new String[]{
                "village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga",
                "ruined_portal", "ruined_portal_desert", "ruined_portal_jungle", "ruined_portal_swamp",
                "ruined_portal_mountain", "ruined_portal_ocean", "ruined_portal_nether",
                "shipwreck", "shipwreck_beached", "ocean_ruin_cold", "ocean_ruin_warm",
                "igloo", "swamp_hut", "buried_treasure", "nether_fossil", "trail_ruins"}) {
            VANILLA.put("minecraft:" + s, Rating.SAFE);
        }
    }

    private static final Map<String, Integer> DANGER_WORDS = new HashMap<>();
    private static final Set<String> BOSS_WORDS = new HashSet<>();
    private static final Set<String> SAFE_WORDS = new HashSet<>();
    static {
        for (String w : new String[]{"boss", "lair", "arena", "citadel", "palace"}) {
            DANGER_WORDS.put(w, BOSS);
            BOSS_WORDS.add(w);
        }
        for (String w : new String[]{"fortress", "castle", "stronghold", "mansion", "monument", "bastion"}) {
            DANGER_WORDS.put(w, MAJOR);
        }
        for (String w : new String[]{"dungeon", "crypt", "tomb", "catacomb", "labyrinth", "maze", "vault",
                "prison", "temple", "pyramid", "ziggurat", "mineshaft", "tower", "outpost", "ruin", "fort",
                "keep", "haunted", "nest", "hive", "spawner"}) {
            DANGER_WORDS.put(w, DUNGEON);
        }
        for (String w : new String[]{"skeleton", "zombie", "spider", "witch", "pillager", "illager",
                "necromancer", "bandit", "undead", "goblin", "orc", "ogre", "troll", "cultist"}) {
            DANGER_WORDS.put(w, MINOR);
        }

        for (String w : new String[]{"village", "house", "hut", "farm", "bridge", "road", "path", "well",
                "shack", "cottage", "barn", "stable", "library", "church", "market", "inn", "tavern",
                "windmill", "lighthouse", "fountain", "statue", "cabin", "portal", "fossil", "treasure",
                "shipwreck", "igloo"}) {
            SAFE_WORDS.add(w);
        }
    }

    public static Rating rate(ResourceLocation id) {
        if (id == null) return Rating.UNKNOWN;
        return rate(id.getNamespace(), id.getPath());
    }

    public static Rating rate(String namespace, String path) {
        if (path == null || path.isEmpty()) return Rating.UNKNOWN;
        String ns = namespace == null ? "minecraft" : namespace.toLowerCase(Locale.ROOT);

        Rating exact = VANILLA.get(ns + ":" + path.toLowerCase(Locale.ROOT));
        if (exact != null) return exact;

        List<String> words = words(path);
        if (words.isEmpty()) return Rating.UNKNOWN;

        int best = 0;
        boolean boss = false;

        if (containsSequence(words, "ancient", "city")) {
            best = BOSS;
            boss = true;
        }
        boolean safe = false;
        for (String w : words) {
            Integer level = lookup(DANGER_WORDS, w);
            if (level != null) {
                if (level > best) best = level;
                if (BOSS_WORDS.contains(w)) boss = true;
            } else if (hasWord(SAFE_WORDS, w)) {
                safe = true;
            }
        }
        if (best > 0) {
            return new Rating(boss ? StructureType.BOSS : StructureType.DUNGEON, best);
        }
        return safe ? Rating.SAFE : Rating.UNKNOWN;
    }

    public static StructureType classify(ResourceLocation structureId) {
        return rate(structureId).type();
    }

    public static StructureType classify(String structureIdStr) {
        if (structureIdStr == null || structureIdStr.isEmpty()) return StructureType.UNKNOWN;
        ResourceLocation loc = ResourceLocation.tryParse(structureIdStr);
        return loc == null ? StructureType.UNKNOWN : classify(loc);
    }

    static List<String> words(String path) {
        String spaced = path.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String w : spaced.split("[^a-z0-9]+")) {
            if (!w.isEmpty()) out.add(w);
        }
        return out;
    }

    private static boolean containsSequence(List<String> words, String a, String b) {
        for (int i = 0; i + 1 < words.size(); i++) {
            if (words.get(i).equals(a) && words.get(i + 1).equals(b)) return true;
        }
        return false;
    }

    private static Integer lookup(Map<String, Integer> map, String word) {
        Integer v = map.get(word);
        if (v == null && word.length() > 3 && word.endsWith("s")) {
            v = map.get(word.substring(0, word.length() - 1));
        }
        return v;
    }

    private static boolean hasWord(Set<String> set, String word) {
        return set.contains(word)
                || (word.length() > 3 && word.endsWith("s") && set.contains(word.substring(0, word.length() - 1)));
    }
}
