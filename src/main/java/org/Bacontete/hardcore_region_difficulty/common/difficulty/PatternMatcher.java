package org.Bacontete.hardcore_region_difficulty.common.difficulty;

import net.minecraft.resources.ResourceLocation;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class PatternMatcher {
    private static final Pattern NEVER = Pattern.compile("(?!)");
    private static final Map<String, Pattern> REGEX_CACHE = new ConcurrentHashMap<>();

    private PatternMatcher() {}

    public static boolean matches(List<String> patterns, ResourceLocation id, Predicate<ResourceLocation> tagTest) {
        if (patterns == null || patterns.isEmpty() || id == null) {
            return false;
        }
        boolean anyPattern = false;
        boolean anyPositive = false;
        boolean positiveHit = false;

        for (String raw : patterns) {
            if (raw == null) continue;
            String p = raw.trim();
            if (p.isEmpty()) continue;
            anyPattern = true;

            boolean negated = p.startsWith("!");
            if (negated) p = p.substring(1).trim();

            boolean hit = single(p, id, tagTest);
            if (negated) {
                if (hit) return false;
            } else {
                anyPositive = true;
                if (hit) positiveHit = true;
            }
        }
        if (!anyPattern) return false;
        return !anyPositive || positiveHit;
    }

    private static boolean single(String p, ResourceLocation id, Predicate<ResourceLocation> tagTest) {
        if (p.equals("*")) return true;
        if (p.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(p.substring(1));
            return tag != null && tagTest != null && tagTest.test(tag);
        }
        if (p.startsWith("~")) {
            return regex(p.substring(1)).matcher(id.toString()).find();
        }
        return p.equals(id.toString());
    }

    private static Pattern regex(String regex) {
        return REGEX_CACHE.computeIfAbsent(regex, r -> {
            try {
                return Pattern.compile(r);
            } catch (PatternSyntaxException e) {
                HardcoreRegionDifficulty.LOGGER.warn("Invalid regex '{}' in config: {}", r, e.getDescription());
                return NEVER;
            }
        });
    }
}
