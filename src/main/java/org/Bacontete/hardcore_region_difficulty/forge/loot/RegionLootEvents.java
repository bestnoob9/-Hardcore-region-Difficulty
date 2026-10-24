package org.Bacontete.hardcore_region_difficulty.forge.loot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;
import org.Bacontete.hardcore_region_difficulty.common.loot.RegionLootBonus;

/** Mob drops grow with the level of the mob (the one it was scaled with, else the region where it died). */
public class RegionLootEvents {

    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob.level().isClientSide() || mob instanceof Player) return;
        CompoundTag data = mob.getPersistentData();
        String levelKey = HardcoreRegionDifficulty.MODID + ":level";
        int level;
        if (data.contains(levelKey)) {
            level = data.getInt(levelKey);
        } else if (mob instanceof Enemy) {
            level = LocationMatcher.getDifficultyLevel(mob.level(), mob.blockPosition());
        } else {
            return;
        }
        // Luck of whoever gets the kill (also for arrows, pets...): a Potion of Luck improves the quality of the drops
        float luck = org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality.luckOf(
                event.getSource().getEntity() instanceof Player ? event.getSource().getEntity() : mob.getKillCredit());
        RegionLootBonus.applyToMobDrops(event.getDrops(), mob, level, luck);
    }
}
