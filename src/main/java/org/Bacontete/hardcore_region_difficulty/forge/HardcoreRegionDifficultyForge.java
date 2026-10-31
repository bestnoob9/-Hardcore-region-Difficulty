package org.Bacontete.hardcore_region_difficulty.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyIcons;
import org.Bacontete.hardcore_region_difficulty.common.danger.DangerAnalyzer;
import org.Bacontete.hardcore_region_difficulty.common.danger.DangerCache;
import org.Bacontete.hardcore_region_difficulty.common.danger.DangerScanner;
import org.Bacontete.hardcore_region_difficulty.common.danger.SpawnerScanner;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.PlayerRegionTracker;
import org.Bacontete.hardcore_region_difficulty.common.enchantment.AdvancedEnchantments;
import org.Bacontete.hardcore_region_difficulty.common.entity.BossProximityWarning;
import org.Bacontete.hardcore_region_difficulty.common.entity.EntityScaler;
import org.Bacontete.hardcore_region_difficulty.common.network.TitleDisplayPacket;
import org.Bacontete.hardcore_region_difficulty.common.villager.VillagerTradeInjector;
import org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityCategory;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityConfig;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityDefinition;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityRegistry;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityType;
import org.Bacontete.hardcore_region_difficulty.forge.client.ClientConfigHooks;
import org.Bacontete.hardcore_region_difficulty.forge.compat.CompatCondition;
import org.Bacontete.hardcore_region_difficulty.forge.compat.DeeperDarkerCataclysmCompat;
import org.Bacontete.hardcore_region_difficulty.forge.loot.AdvancedEnchantLootModifier;
import org.Bacontete.hardcore_region_difficulty.forge.quality.QualityEvents;
import org.Bacontete.hardcore_region_difficulty.forge.boss.VanillaBossEvents;
import org.Bacontete.hardcore_region_difficulty.forge.quality.QualityForgeRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Mod(HardcoreRegionDifficulty.MODID)
public class HardcoreRegionDifficultyForge {

    private static final int REGION_CHECK_INTERVAL = 20;
    /** Cheap biome-only check (no structure/danger scan): a biome change is sent within this many ticks. */
    private static final int BIOME_CHECK_INTERVAL = 1;

    private static final int CONFIG_CHECK_INTERVAL = 100;
    private int configCheckTicks = 0;

    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, HardcoreRegionDifficulty.MODID);

    public HardcoreRegionDifficultyForge() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        NetworkHandler.register();

        AdvancedEnchantments.ENCHANTMENTS.register(modEventBus);
        QualityForgeRegistry.BLOCKS.register(modEventBus);
        QualityForgeRegistry.ITEMS.register(modEventBus);
        QualityForgeRegistry.MENUS.register(modEventBus);
        modEventBus.addListener(QualityForgeRegistry::addToCreativeTab);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, QualityConfig.SPEC, "hardcore_region_difficulty-quality.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeConfig.SPEC, "hardcore_region_difficulty-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "hardcore_region_difficulty-client.toml");
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientConfigHooks::register);
        LOOT_MODIFIERS.register("advanced_enchant_loot", AdvancedEnchantLootModifier.CODEC);
        LOOT_MODIFIERS.register(modEventBus);

        CraftingHelper.register(CompatCondition.SERIALIZER);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new AdvancedEnchantEvents());
        MinecraftForge.EVENT_BUS.register(new QualityEvents());
        MinecraftForge.EVENT_BUS.register(new VanillaBossEvents());
        MinecraftForge.EVENT_BUS.register(new org.Bacontete.hardcore_region_difficulty.forge.elite.EliteEvents());
        MinecraftForge.EVENT_BUS.register(new org.Bacontete.hardcore_region_difficulty.forge.loot.RegionLootEvents());
        MinecraftForge.EVENT_BUS.register(new DeeperDarkerCataclysmCompat());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        HardcoreRegionDifficulty.LOGGER.info("Hardcore Region Difficulty Forge initializing");

        new DifficultyConfig(FMLPaths.CONFIGDIR.get().toFile());
        QualityRegistry.init(FMLPaths.CONFIGDIR.get().toFile());
        DeeperDarkerCataclysmCompat.verifyDependencies();
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Mob mob && !EntityScaler.isScaled(mob) && EntityScaler.shouldScale(mob)) {
            EntityScaler.applyScaling(mob);
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.tickCount <= 220 && !mob.level().isClientSide) {
            EntityScaler.fixInitialHealth(mob);
        }
        if (event.getEntity() instanceof Mob raidMob && !raidMob.level().isClientSide && raidMob.tickCount % 20 == 0) {
            EntityScaler.checkRaidJoin(raidMob);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        boolean fullCheck = player.tickCount % REGION_CHECK_INTERVAL == 0;
        if (!fullCheck && (player.tickCount % BIOME_CHECK_INTERVAL != 0 || !PlayerRegionTracker.biomeChanged(player))) {
            return;
        }
        if (PlayerRegionTracker.updateAndCheckChange(player)) {
            PlayerRegionTracker.RegionInfo info = PlayerRegionTracker.getPlayerRegion(player);
            if (info != null && !info.regionName.isEmpty()) {
                NetworkHandler.sendToPlayer(player,
                        new TitleDisplayPacket(info.regionName, info.difficultyTitle, info.structureName, info.difficultyLevel,
                                info.icon, DifficultyIcons.parseColorRgb(info.titleColor),
                                PlayerRegionTracker.wasLegacyAnnounce(player)));
            }
        }

        if (fullCheck) {
            BossProximityWarning.checkForNearbyBosses(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerRegionTracker.removePlayer(event.getEntity().getUUID());
        BossProximityWarning.clearPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config != null) {
            config.loadConfig();
            DangerCache.clear();
            org.Bacontete.hardcore_region_difficulty.common.config.ConfigShare.ensureFile(config);
        }
        runAutoScan(event.getServer());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.sendQualitiesTo(player);
        }
    }

    /** Reads the quality file again and sends the result to every connected player. */
    static boolean reloadQualities() {
        boolean ok = QualityRegistry.reload();
        if (ok && ServerLifecycleHooks.getCurrentServer() != null) {
            NetworkHandler.sendQualitiesToAll();
        }
        return ok;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++configCheckTicks < CONFIG_CHECK_INTERVAL) {
            return;
        }
        configCheckTicks = 0;
        if (QualityRegistry.hasFileChanged()) {
            HardcoreRegionDifficulty.LOGGER.info("Quality file changed, reloading");
            reloadQualities();
        }
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null || !config.hasFileChanged()) {
            return;
        }
        HardcoreRegionDifficulty.LOGGER.info("Config file changed, reloading");
        if (config.loadConfig()) {
            DangerCache.clear();
            runAutoScan(ServerLifecycleHooks.getCurrentServer());
        }
    }

    private static void runAutoScan(MinecraftServer server) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null || server == null) {
            return;
        }
        try {
            DangerScanner.Summary sm = DangerScanner.run(server, config.getConfigDirectory());
            HardcoreRegionDifficulty.LOGGER.info(
                    "Auto scan: {} structures ({} dangerous, {} safe/ignored, {} unknown), {} biomes ({} dangerous), {} dimensions ({} rated). Report: {}",
                    sm.structures(), sm.dangerous(), sm.ignored(), sm.unknown(),
                    sm.biomes(), sm.dangerousBiomes(), sm.dimensions(), sm.dangerousDimensions(), sm.file().getPath());
            if (!sm.unknownByNamespace().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int shown = 0;
                for (var e : sm.unknownByNamespace()) {
                    if (shown++ >= 8) break;
                    sb.append(' ').append(e.getKey()).append('=').append(e.getValue());
                }
                HardcoreRegionDifficulty.LOGGER.info("Auto scan - unknown structures by mod:{}", sb);
            }
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Auto scan failed (the game keeps working, scan report not updated)", e);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getState().is(Blocks.SPAWNER)) {
            SpawnerScanner.invalidate(level.dimension().location(), new ChunkPos(event.getPos()));
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getPlacedBlock().is(Blocks.SPAWNER)) {
            SpawnerScanner.invalidate(level.dimension().location(), new ChunkPos(event.getPos()));
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SpawnerScanner.invalidate(level.dimension().location(), event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        DangerCache.clear();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        PlayerRegionTracker.clearAll();
        BossProximityWarning.clearAll();
        DangerCache.clear();
    }

    /** Enforces the minimum price of Advanced books after the cure / Hero of the Village discounts. */
    @SubscribeEvent
    public void onTradeScreenOpen(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof net.minecraft.world.inventory.MerchantMenu menu
                && !event.getEntity().level().isClientSide()) {
            VillagerTradeInjector.applyPriceFloor(menu.getOffers());
        }
    }

    @SubscribeEvent
    public void onVillagerTrades(VillagerTradesEvent event) {
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        for (List<VillagerTrades.ItemListing> listings : trades.values()) {
            listings.replaceAll(original -> (trader, random) -> {
                var offer = original.getOffer(trader, random);
                if (offer != null && trader instanceof Villager villager) {
                    return VillagerTradeInjector.tryUpgradeTrade(villager, offer, random);
                }
                return offer;
            });
        }
    }

    private static void appendSpawnerInfo(StringBuilder sb, CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            sb.append("\nSpawners: run /hrd scan as a player to count the spawners around you.");
            return;
        }
        DangerAnalyzer.SpawnerReport r = DangerAnalyzer.reportAt(player.level(), player.blockPosition());
        if (!r.enabled()) {
            sb.append("\nSpawners: analysis is off (analyze_spawners: false).");
            return;
        }
        sb.append("\nSpawners within ").append(r.radius()).append(" blocks: ").append(r.count());
        if (r.spawnerLevel() > 0) {
            sb.append(" -> spawner level ").append(r.spawnerLevel())
              .append(r.decisive() ? " (decides the level here)" : " (not the highest: " + r.overall().regionName
                      + ", level " + r.overall().difficultyLevel + ", wins)");
        } else {
            sb.append(" -> below the thresholds, no spawner level");
        }
        if (!r.enemiesEnabled()) {
            sb.append("\nEnemies: analysis is off (analyze_enemies: false).");
            return;
        }
        // the number of hostile mobs around moves all the time: this line explains a level that changes by itself
        sb.append("\nHostile mobs within ").append(r.enemyRadius()).append(" blocks: ").append(r.enemyCount());
        if (r.enemyLevel() > 0) {
            sb.append(" -> enemy level ").append(r.enemyLevel())
              .append(r.enemyDecisive() ? " (decides the level here)" : " (not the highest, " + r.overall().regionName
                      + " level " + r.overall().difficultyLevel + " wins)");
        } else {
            sb.append(" -> below the thresholds, no enemy level");
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(HrdCommands.build());
        dispatcher.register(Commands.literal("hrd")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            DifficultyConfig config = DifficultyConfig.getInstance();
                            boolean configOk = config != null && config.loadConfig();
                            boolean qualitiesOk = reloadQualities();
                            boolean ok = configOk && qualitiesOk;
                            PlayerRegionTracker.clearAll();
                            DangerCache.clear();
                            if (configOk) runAutoScan(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal(ok
                                    ? "Hardcore Region Difficulty config reloaded."
                                    : "Could not read the config file - previous settings kept. See the log."), true);
                            return ok ? 1 : 0;
                        }))
                .then(Commands.literal("scan")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            DifficultyConfig config = DifficultyConfig.getInstance();
                            if (config == null) return 0;
                            try {
                                DangerScanner.Summary sm = DangerScanner.run(ctx.getSource().getServer(), config.getConfigDirectory());
                                StringBuilder sb = new StringBuilder();
                                sb.append("Scan: ").append(sm.structures()).append(" structures (")
                                  .append(sm.dangerous()).append(" dangerous, ").append(sm.ignored()).append(" safe/ignored, ")
                                  .append(sm.unknown()).append(" unknown), ")
                                  .append(sm.biomes()).append(" biomes (").append(sm.dangerousBiomes()).append(" dangerous), ")
                                  .append(sm.dimensions()).append(" dimensions (")
                                  .append(sm.dangerousDimensions()).append(" rated).");
                                if (!sm.unknownByNamespace().isEmpty()) {
                                    sb.append("\nUnknown structures by mod:");
                                    int shown = 0;
                                    for (var e : sm.unknownByNamespace()) {
                                        if (shown++ >= 8) break;
                                        sb.append(' ').append(e.getKey()).append('=').append(e.getValue());
                                    }
                                }
                                appendSpawnerInfo(sb, ctx.getSource());
                                sb.append("\nFull report: ").append(sm.file().getPath());
                                ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), true);
                                return 1;
                            } catch (Exception e) {
                                HardcoreRegionDifficulty.LOGGER.error("/hrd scan failed", e);
                                ctx.getSource().sendFailure(Component.literal("Scan failed: " + e));
                                return 0;
                            }
                        }))
                .then(Commands.literal("region")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            var match = LocationMatcher.match(player.level(), player.blockPosition());
                            String text = match == null
                                    ? "No difficulty region here (level 1)."
                                    : match.region().getRegionName() + " - " + match.region().getDifficultyTitle()
                                      + " (level " + match.region().getDifficultyLevel() + ", region id: " + match.region().getId() + ")"
                                      + (match.structureName().isEmpty() ? "" : " in " + match.structureName());
                            var cfgNow = org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig.getInstance();
                            String capText = "";
                            if (cfgNow != null) {
                                int base = LocationMatcher.dimensionBaseLevel(player.level(), cfgNow);
                                int cap = cfgNow.getScalingSettings().levelCapFor(player.level().dimension().location(), base);
                                capText = "\nDimension level " + base + ", cap here: "
                                        + (cap == Integer.MAX_VALUE ? "none" : String.valueOf(cap))
                                        + " (bonus +" + cfgNow.getScalingSettings().getDimensionLevelBonus()
                                        + ", highest level of the mod " + cfgNow.getScalingSettings().maxRegionLevel() + ")";
                            }
                            StringBuilder full = new StringBuilder(text).append(capText);
                            appendSpawnerInfo(full, ctx.getSource());
                            final String shown = full.toString();
                            ctx.getSource().sendSuccess(() -> Component.literal(shown), false);
                            return 1;
                        }))
                .then(Commands.literal("regionshow")
                        .executes(ctx -> showRegionIds(ctx.getSource())))
                .then(buildQualityCommand()));
    }

    // ------------------------------------------------------------------
    // /hrd regionshow: ids of the dimension, biome (and its tags) and structures at the player's feet.
    // Every id is click-to-copy, ready to paste into the region editor / config.
    // ------------------------------------------------------------------

    private static MutableComponent copyable(String text) {
        return Component.literal(text).withStyle(style -> style
                .withColor(net.minecraft.ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                        net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                        Component.translatableWithFallback("hardcore_region_difficulty.regionshow.copy",
                                "Click to copy: %s", text))));
    }

    private static MutableComponent label(String key, String fallback) {
        return Component.translatableWithFallback(key, fallback).withStyle(net.minecraft.ChatFormatting.GOLD);
    }

    private static int showRegionIds(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        net.minecraft.core.BlockPos pos = player.blockPosition();
        String K = "hardcore_region_difficulty.regionshow.";

        MutableComponent out = Component.translatableWithFallback(K + "title", "Where you stand (click an id to copy it):")
                .withStyle(net.minecraft.ChatFormatting.YELLOW);

        out.append("\n").append(label(K + "dimension", "Dimension: "))
           .append(copyable(level.dimension().location().toString()));

        var biome = level.getBiome(pos);
        var biomeKey = biome.unwrapKey();
        out.append("\n").append(label(K + "biome", "Biome: "));
        if (biomeKey.isPresent()) {
            out.append(copyable(biomeKey.get().location().toString()));
            List<String> tags = new ArrayList<>();
            biome.tags().forEach(t -> tags.add("#" + t.location()));
            java.util.Collections.sort(tags);
            if (!tags.isEmpty()) {
                out.append("\n  ").append(label(K + "biome_tags", "Biome tags: "));
                for (int i = 0; i < tags.size(); i++) {
                    if (i > 0) out.append(Component.literal(", ").withStyle(net.minecraft.ChatFormatting.GRAY));
                    out.append(copyable(tags.get(i)));
                }
            }
        } else {
            out.append(Component.literal("?").withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        var hits = org.Bacontete.hardcore_region_difficulty.common.difficulty.StructureLookup.at(level, pos);
        out.append("\n").append(label(K + "structures", "Structures: "));
        if (hits.isEmpty()) {
            out.append(Component.translatableWithFallback(K + "none", "none here").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        for (var hit : hits) {
            out.append("\n  ").append(copyable(hit.id().toString()));
            List<String> tags = new ArrayList<>();
            hit.holder().tags().forEach(t -> tags.add("#" + t.location()));
            java.util.Collections.sort(tags);
            for (String tag : tags) {
                out.append(Component.literal("  ")).append(copyable(tag));
            }
        }

        source.sendSuccess(() -> out, false);
        return 1;
    }

    // ------------------------------------------------------------------
    // /hrd quality show [category]
    // /hrd quality set <category> <quality>
    // Categories: weapon, tool, armor, ranged, shield
    // ------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> buildQualityCommand() {
        return Commands.literal("quality")
                .then(Commands.literal("show")
                        .executes(ctx -> showQualities(ctx.getSource(), null))
                        .then(Commands.argument("category", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestNames(QualityCategory.COMMAND_NAMES, builder))
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "category");
                                    QualityCategory category = QualityCategory.parse(name);
                                    if (category == null) {
                                        ctx.getSource().sendFailure(Component.literal(unknownCategoryMessage(name)));
                                        return 0;
                                    }
                                    return showQualities(ctx.getSource(), category);
                                })))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            boolean ok = reloadQualities();
                            ctx.getSource().sendSuccess(() -> Component.literal(ok
                                    ? "Quality definitions reloaded from " + QualityRegistry.FILE_NAME + "."
                                    : "Could not read the quality file - previous definitions kept. See the log."), true);
                            return ok ? 1 : 0;
                        }))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("category", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestNames(QualityCategory.COMMAND_NAMES, builder))
                                .then(Commands.argument("quality", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestQualityArgument(ctx, builder))
                                        .executes(ctx -> setQuality(ctx,
                                                StringArgumentType.getString(ctx, "category"),
                                                StringArgumentType.getString(ctx, "quality"))))));
    }

    /** /hrd quality set <category> <quality>: only the qualities of the category that was chosen first. */
    private static CompletableFuture<Suggestions> suggestQualityArgument(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        QualityCategory category = QualityCategory.parse(StringArgumentType.getString(ctx, "category"));
        if (category == null) return builder.buildFuture();
        String typed = builder.getRemainingLowerCase();
        List<QualityType> list = new ArrayList<>(category.getAllowed());
        list.sort(Comparator.comparingInt(QualityType::getTier));
        for (QualityType type : list) {
            if (type.name().toLowerCase(Locale.ROOT).startsWith(typed)) {
                // the tooltip is translated by the client: Vietnamese names with vi_vn
                builder.suggest(type.name(), type.getDisplayName());
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestNames(List<String> names, SuggestionsBuilder builder) {
        String typed = builder.getRemainingLowerCase();
        for (String name : names) {
            if (name.toLowerCase(Locale.ROOT).startsWith(typed)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private static QualityType parseQuality(String qualityName, QualityCategory category) {
        return category.find(qualityName);
    }

    private static String unknownCategoryMessage(String name) {
        return "Unknown item category '" + name + "'. Use: weapon, tool, armor, ranged, shield.";
    }

    private static int showQualities(CommandSourceStack source, QualityCategory only) {
        MutableComponent result = Component.literal(only == null
                ? "Quality Types per item category (lowest to highest, from " + QualityRegistry.FILE_NAME + "):\n"
                : "Quality Types for one item category (lowest to highest):\n");
        for (QualityCategory category : QualityCategory.values()) {
            if (only != null && category != only) {
                continue;
            }
            result.append(Component.literal(category.name().toLowerCase(Locale.ROOT))
                    .withStyle(net.minecraft.ChatFormatting.BOLD))
                  .append(Component.literal(" (").withStyle(net.minecraft.ChatFormatting.GRAY))
                  .append(Component.translatable("hardcore_region_difficulty.quality.category." + category.name())
                          .withStyle(net.minecraft.ChatFormatting.GRAY))
                  .append(Component.literal("):\n").withStyle(net.minecraft.ChatFormatting.GRAY));
            List<QualityType> list = new ArrayList<>(category.getAllowed());
            list.sort(Comparator.comparingInt(QualityType::getTier));
            if (list.isEmpty()) {
                result.append(Component.literal("  (none - this category has no qualities)\n"));
            }
            double weightTotal = 0.0D;
            for (QualityType q : list) {
                if (q instanceof QualityDefinition d) weightTotal += d.getWeight();
            }
            for (QualityType type : list) {
                String weight = "";
                if (type instanceof QualityDefinition d) {
                    // loot / mob drops / villagers: the weight of the quality file (a 0-weight high quality unlocks at a level)
                    String loot;
                    if (d.getWeight() > 0) {
                        loot = String.format(Locale.ROOT, "%.2f", weightTotal > 0 ? d.getWeight() * 100.0D / weightTotal : 0.0D) + "%";
                    } else if (d.getUnlockLevel() > 0) {
                        loot = "from Lv " + d.getUnlockLevel();
                    } else {
                        loot = "0.00%";
                    }
                    // Quality Forge / newly crafted items: chance by tier only (ignores the file weights)
                    String crafted = String.format(Locale.ROOT, "%.2f", ItemQuality.forgeChancePercent(d)) + "%";
                    weight = ", loot " + loot + ", crafted " + crafted;
                }
                // id (what you type in commands) + the colored display name; the client translates it, so it is
                // Vietnamese with vi_vn and the prettified English name otherwise
                result.append(Component.literal("  ")
                        .append(Component.literal(type.name()).withStyle(type.getStyle()))
                        .append(Component.literal(" -> ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY))
                        .append(type.getDisplayName())
                        .append(Component.literal(" (tier " + type.getTier() + weight + ")\n")
                                .withStyle(net.minecraft.ChatFormatting.GRAY)));
            }
        }
        source.sendSuccess(() -> result, false);
        return 1;
    }

    private static int setQuality(CommandContext<CommandSourceStack> ctx, String categoryName, String qualityName)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        QualityCategory wanted = QualityCategory.parse(categoryName);
        if (wanted == null) {
            source.sendFailure(Component.literal(unknownCategoryMessage(categoryName)));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("You must hold an item in your main hand."));
            return 0;
        }

        QualityCategory category = QualityCategory.of(stack);
        if (category == null) {
            source.sendFailure(Component.literal("The held item cannot have a quality."));
            return 0;
        }
        if (category != wanted) {
            source.sendFailure(Component.literal("The held item is a " + category.name().toLowerCase(Locale.ROOT)
                    + ", not a " + wanted.name().toLowerCase(Locale.ROOT) + "."));
            return 0;
        }

        QualityType quality = parseQuality(qualityName, category);
        if (quality == null) {
            StringBuilder names = new StringBuilder();
            for (QualityType t : category.getAllowed()) {
                if (names.length() > 0) names.append(", ");
                names.append(t.name());
            }
            source.sendFailure(Component.literal("Invalid quality type '" + qualityName + "' for a "
                    + category.name().toLowerCase(Locale.ROOT) + ". Allowed: " + names));
            return 0;
        }

        ItemQuality.set(stack, quality);
        source.sendSuccess(() -> Component.literal("Set quality of held item to " + quality.name()), true);
        return 1;
    }
}
