package org.Bacontete.hardcore_region_difficulty.forge;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.config.ConfigShare;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyLevelDef;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyRegion;
import org.Bacontete.hardcore_region_difficulty.common.config.ScalingSettings;
import org.Bacontete.hardcore_region_difficulty.common.danger.DangerCache;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.PlayerRegionTracker;
import org.Bacontete.hardcore_region_difficulty.common.entity.BossLevel;
import org.Bacontete.hardcore_region_difficulty.common.entity.LevelPreview;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * Extra /hrd commands (merged into the "hrd" command of HardcoreRegionDifficultyForge):
 * regions, levels, stats, preview, export, import.
 */
final class HrdCommands {

    private static final String LEVEL_TAG = HardcoreRegionDifficulty.MODID + ":level";
    private static final String SCALED_TAG = HardcoreRegionDifficulty.MODID + ":scaled";
    private static final int MAX_LINES = 60;

    private HrdCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("hrd")
                .then(Commands.literal("regions").executes(ctx -> regions(ctx.getSource())))
                .then(Commands.literal("levels").executes(ctx -> levels(ctx.getSource())))
                .then(Commands.literal("stats").executes(ctx -> stats(ctx.getSource())))
                .then(Commands.literal("preview")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 98))
                                .suggests(HrdCommands::suggestLevels)
                                .executes(ctx -> preview(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "level"), null))
                                // ResourceLocationArgument: "minecraft:zombie" and mod ids ("mod:mob") can be typed
                                // (a plain word() stops at the ':'), and "zombie" still means minecraft:zombie
                                .then(Commands.argument("entity", ResourceLocationArgument.id())
                                        .suggests(HrdCommands::suggestMobs)
                                        .executes(ctx -> preview(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "level"),
                                                ResourceLocationArgument.getId(ctx, "entity").toString())))))
                .then(Commands.literal("export")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> export(ctx.getSource())))
                .then(Commands.literal("import")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("file")
                                .executes(ctx -> importFile(ctx.getSource(), ConfigShare.FILE_NAME))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(ctx -> importFile(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))));
    }

    private static DifficultyConfig config(CommandSourceStack source) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null) {
            source.sendFailure(Component.literal("The config is not loaded."));
        }
        return config;
    }

    /** Levels 1-5, plus the levels the file defines itself (same list as /hrd levels). */
    private static CompletableFuture<Suggestions> suggestLevels(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null) {
            return SharedSuggestionProvider.suggest(List.of("1", "2", "3", "4", "5"), builder);
        }
        int top = config.getScalingSettings().maxRegionLevel();
        List<String> levels = new ArrayList<>();
        for (int lvl = 1; lvl <= Math.max(5, top); lvl++) {
            if (lvl > 5 && !config.isUnlockedLevel(lvl)) continue;
            levels.add(String.valueOf(lvl));
        }
        return SharedSuggestionProvider.suggest(levels, builder);
    }

    /** Every mob that has attributes (vanilla and modded), the ones /hrd preview can compute. */
    private static CompletableFuture<Suggestions> suggestMobs(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (LevelPreview.typeOf(id.toString()) != null) ids.add(id);
        }
        return SharedSuggestionProvider.suggestResource(ids, builder);
    }

    private static MutableComponent head(String text) {
        return Component.literal(text).withStyle(ChatFormatting.YELLOW);
    }

    private static String roman(int level) {
        return org.Bacontete.hardcore_region_difficulty.common.danger.DangerAnalyzer.roman(level);
    }

    // ---------------------------------------------------------------- /hrd regions

    private static int regions(CommandSourceStack source) {
        DifficultyConfig config = config(source);
        if (config == null) return 0;
        List<DifficultyRegion> list = new ArrayList<>(config.getRegionList());
        list.sort(Comparator.comparingInt(DifficultyRegion::getDifficultyLevel).reversed()
                .thenComparing(DifficultyRegion::getId));
        MutableComponent out = head("Regions (" + list.size() + "), highest level first:");
        int shown = 0;
        for (DifficultyRegion r : list) {
            if (shown++ >= MAX_LINES) {
                out.append(Component.literal("\n... " + (list.size() - MAX_LINES) + " more, see the config file")
                        .withStyle(ChatFormatting.GRAY));
                break;
            }
            out.append("\n").append(Component.literal("Lv " + r.getDifficultyLevel() + " ").withStyle(ChatFormatting.GOLD))
               .append(Component.literal(r.getRegionName().isEmpty() ? r.getId() : r.getRegionName()))
               .append(Component.literal("  [" + kinds(r) + "]").withStyle(ChatFormatting.GRAY));
        }
        source.sendSuccess(() -> out, false);
        return list.size();
    }

    private static String kinds(DifficultyRegion r) {
        List<String> parts = new ArrayList<>();
        if (!r.getDimensions().isEmpty()) parts.add("dimension " + String.join(",", r.getDimensions()));
        if (!r.getStructures().isEmpty()) parts.add("structure " + String.join(",", r.getStructures()));
        if (!r.getBiomes().isEmpty() || !r.getBiomeTags().isEmpty()) {
            parts.add("biome " + (r.getBiomes().size() + r.getBiomeTags().size()));
        }
        return parts.isEmpty() ? r.getId() : String.join("; ", parts);
    }

    // ---------------------------------------------------------------- /hrd levels

    private static int levels(CommandSourceStack source) {
        DifficultyConfig config = config(source);
        if (config == null) return 0;
        ScalingSettings sc = config.getScalingSettings();
        int top = sc.maxRegionLevel();
        MutableComponent out = head("Levels 1-" + top + " (highest level of the mod: " + top + "), "
                + "health / damage / armor bonus:");
        for (int lvl = 1; lvl <= top; lvl++) {
            if (lvl > 5 && !config.isUnlockedLevel(lvl)) continue; // gaps between own levels
            DifficultyLevelDef def = config.getLevelDefs().get(lvl);
            out.append("\n").append(Component.literal("Lv " + lvl + " (" + roman(lvl) + ") ").withStyle(ChatFormatting.GOLD))
               .append(Component.literal(pct(sc.healthAmount(lvl)) + " / " + pct(sc.damageAmount(lvl)) + " / "
                       + pct(sc.armorMultiplyAmount(lvl))));
            if (def != null && def.getTitle() != null && !def.getTitle().isEmpty()) {
                out.append(Component.literal("  " + def.getTitle()).withStyle(ChatFormatting.GRAY));
            }
        }
        out.append("\n").append(head("Level cap of each dimension (cap = dimension level + "
                + sc.getDimensionLevelBonus() + ", never above " + top + "):"));
        for (ServerLevel level : source.getServer().getAllLevels()) {
            int base = LocationMatcher.dimensionBaseLevel(level, config);
            int cap = sc.levelCapFor(level.dimension().location(), base);
            out.append("\n").append(Component.literal(level.dimension().location().toString()).withStyle(ChatFormatting.AQUA))
               .append(Component.literal("  level " + base + ", cap " + (cap == Integer.MAX_VALUE ? "none" : String.valueOf(cap))));
        }
        source.sendSuccess(() -> out, false);
        return 1;
    }

    private static String pct(double fraction) {
        return "+" + Math.round(fraction * 1000.0) / 10.0 + "%";
    }

    // ---------------------------------------------------------------- /hrd stats

    private static int stats(CommandSourceStack source) {
        Map<Integer, Integer> byLevel = new TreeMap<>();
        Map<String, Integer> byType = new TreeMap<>();
        int scaled = 0;
        int bosses = 0;
        int plain = 0;
        double sumHealth = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (!(e instanceof LivingEntity living)) continue;
                CompoundTag data = living.getPersistentData();
                if (!data.getBoolean(SCALED_TAG)) {
                    if (living instanceof net.minecraft.world.entity.monster.Enemy) plain++;
                    continue;
                }
                scaled++;
                byLevel.merge(data.getInt(LEVEL_TAG), 1, Integer::sum);
                byType.merge(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString(), 1, Integer::sum);
                if (data.getBoolean("is_boss_entity")) bosses++;
                sumHealth += living.getMaxHealth();
            }
        }
        MutableComponent out = head("Scaled mobs in loaded chunks: " + scaled
                + (bosses > 0 ? " (" + bosses + " boss)" : "") + ", hostile mobs not scaled: " + plain);
        if (scaled > 0) {
            out.append("\n").append(Component.literal("Average max health: " + Math.round(sumHealth / scaled * 10) / 10.0));
            StringBuilder sb = new StringBuilder("By level:");
            byLevel.forEach((k, v) -> sb.append("  Lv ").append(k).append(" x").append(v));
            out.append("\n").append(Component.literal(sb.toString()));
            List<Map.Entry<String, Integer>> types = new ArrayList<>(byType.entrySet());
            types.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
            StringBuilder tb = new StringBuilder("Most common:");
            for (int i = 0; i < Math.min(6, types.size()); i++) {
                tb.append("  ").append(types.get(i).getKey()).append(" x").append(types.get(i).getValue());
            }
            out.append("\n").append(Component.literal(tb.toString()).withStyle(ChatFormatting.GRAY));
        }
        source.sendSuccess(() -> out, false);
        return scaled;
    }

    // ---------------------------------------------------------------- /hrd preview

    private static int preview(CommandSourceStack source, int level, String entity) {
        DifficultyConfig config = config(source);
        if (config == null) return 0;
        ScalingSettings sc = config.getScalingSettings();
        List<String> mobs = entity == null
                ? List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:creeper", "minecraft:enderman", "minecraft:wither_skeleton")
                : List.of(entity.contains(":") ? entity : "minecraft:" + entity);
        MutableComponent out = head("Mob stats at level " + level + " (base > now):");
        int shown = 0;
        for (String id : mobs) {
            LevelPreview.Stats s = LevelPreview.compute(id, level, null, sc);
            if (s == null) {
                out.append("\n").append(Component.literal(id + ": unknown mob").withStyle(ChatFormatting.RED));
                continue;
            }
            shown++;
            out.append("\n").append(Component.literal(LevelPreview.line(id, s)));
        }
        if (shown > 0 && entity != null) {
            LevelPreview.Stats mini = LevelPreview.compute(mobs.get(0), level, BossLevel.MINI_BOSS, sc);
            LevelPreview.Stats boss = LevelPreview.compute(mobs.get(0), level, BossLevel.BOSS, sc);
            if (mini != null) out.append("\n").append(Component.literal("As mini boss - " + LevelPreview.line(mobs.get(0), mini)).withStyle(ChatFormatting.DARK_PURPLE));
            if (boss != null) out.append("\n").append(Component.literal("As boss - " + LevelPreview.line(mobs.get(0), boss)).withStyle(ChatFormatting.DARK_RED));
        }
        source.sendSuccess(() -> out, false);
        return shown;
    }

    // ---------------------------------------------------------------- /hrd export, /hrd import

    private static int export(CommandSourceStack source) {
        DifficultyConfig config = config(source);
        if (config == null) return 0;
        JsonObject pack = ConfigShare.build(config, true);
        if (pack == null) {
            source.sendFailure(Component.literal("Could not read the config file."));
            return 0;
        }
        File file = new File(config.getConfigDirectory(), ConfigShare.FILE_NAME);
        boolean written = ConfigShare.writeFile(pack, file);
        MutableComponent out = head(written ? "Exported to " + file.getPath() : "Could not write " + file.getPath());
        out.append("\n").append(Component.literal("Copy this file into the config folder of another server or modpack and run "
                + "/hrd import file there.").withStyle(ChatFormatting.GRAY));
        source.sendSuccess(() -> out, true);
        return 1;
    }

    private static int importFile(CommandSourceStack source, String name) {
        DifficultyConfig config = config(source);
        if (config == null) return 0;
        // only a file in the config folder, never a path
        String clean = new File(name).getName();
        File file = new File(config.getConfigDirectory(), clean);
        JsonObject pack = ConfigShare.readFile(file);
        if (pack == null) {
            source.sendFailure(Component.literal("Cannot read " + file.getPath() + " (missing, or not a JSON object)."));
            return 0;
        }
        return applyPack(source, config, pack);
    }

    private static int applyPack(CommandSourceStack source, DifficultyConfig config, JsonObject pack) {
        String error = ConfigShare.apply(config, pack);
        if (error != null) {
            source.sendFailure(Component.literal("Import failed: " + error));
            return 0;
        }
        boolean ok = config.loadConfig();
        boolean qualities = HardcoreRegionDifficultyForge.reloadQualities();
        PlayerRegionTracker.clearAll();
        DangerCache.clear();
        source.sendSuccess(() -> Component.literal(ok && qualities
                ? "Setup imported and loaded. The old files were kept as *.bak-DATE in the config folder."
                : "Setup written, but it could not be loaded, see the log. The old files were kept as *.bak-DATE."), true);
        return ok ? 1 : 0;
    }
}
