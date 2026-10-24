package org.Bacontete.hardcore_region_difficulty.forge.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.Bacontete.hardcore_region_difficulty.common.loot.LootTableInjector;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class AdvancedEnchantLootModifier extends LootModifier {
    public static final Supplier<Codec<AdvancedEnchantLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, AdvancedEnchantLootModifier::new)));

    public AdvancedEnchantLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation table = context.getQueriedLootTableId();
        if (table == null || !table.getPath().startsWith("chests/")) {
            return generatedLoot;
        }
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) {
            return generatedLoot;
        }
        BlockPos pos = BlockPos.containing(origin);
        ServerLevel level = context.getLevel();

        int difficultyLevel = LocationMatcher.getDifficultyLevel(level, pos);

        // more loot in higher regions (extra rolls, bigger stacks, enchants), before the steps below
        org.Bacontete.hardcore_region_difficulty.common.loot.RegionLootBonus.applyToChest(generatedLoot, context, table, difficultyLevel);

        // Advanced loot (advanced enchants + books): only from region level 3 up (advanced_min_level), chance already multiplied by the "harder" factor
        if (LootTableInjector.advancedAllowed(difficultyLevel)) {
            for (int i = 0; i < generatedLoot.size(); i++) {
                generatedLoot.set(i, LootTableInjector.tryEnchantItem(generatedLoot.get(i), difficultyLevel, context.getRandom()));
            }

            ItemStack bonus = LootTableInjector.rollDungeonBook(table.toString(), pos, level, context.getRandom());
            if (!bonus.isEmpty()) {
                generatedLoot.add(bonus);
            }
        }

        if (org.Bacontete.hardcore_region_difficulty.common.quality.QualityConfig.isEnabled()
                && org.Bacontete.hardcore_region_difficulty.common.quality.QualityConfig.ROLL_IN_LOOT.get()) {
            for (ItemStack stack : generatedLoot) {
                if (org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality.isEligible(stack) && !org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality.has(stack)) {
                    org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality.set(stack, org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality.roll(context.getRandom(), difficultyLevel, stack, context.getLuck()));
                }
            }
        }

        ItemStack trueDamage = LootTableInjector.rollTrueDamageBook(difficultyLevel, context.getRandom());
        if (!trueDamage.isEmpty()) {
            generatedLoot.add(trueDamage);
        }

        ItemStack piercing = LootTableInjector.rollArmorPiercingBook(difficultyLevel, context.getRandom());
        if (!piercing.isEmpty()) {
            generatedLoot.add(piercing);
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
