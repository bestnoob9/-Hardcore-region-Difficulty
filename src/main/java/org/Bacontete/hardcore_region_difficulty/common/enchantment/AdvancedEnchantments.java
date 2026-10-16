package org.Bacontete.hardcore_region_difficulty.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.util.List;
import java.util.stream.Stream;

public class AdvancedEnchantments {

    // Rarity quyết định giá XP trên đe (đồ / sách mỗi cấp): COMMON 1/1, UNCOMMON 2/1, RARE 4/2, VERY_RARE 8/4.
    // Nâng cao = UNCOMMON, Đánh Nhanh + Xuyên Giáp = COMMON, Sát Thương Chuẩn = RARE.
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, HardcoreRegionDifficulty.MODID);

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final EquipmentSlot[] MAINHAND = { EquipmentSlot.MAINHAND };

    public static final RegistryObject<Enchantment> ADVANCED_PROTECTION = ENCHANTMENTS.register("advanced_protection",
            () -> new AdvancedProtectionEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.ARMOR, ARMOR_SLOTS));
    public static final RegistryObject<Enchantment> ADVANCED_THORNS = ENCHANTMENTS.register("advanced_thorns",
            () -> new AdvancedThornsEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.ARMOR, ARMOR_SLOTS));
    public static final RegistryObject<Enchantment> ADVANCED_FEATHER_FALLING = ENCHANTMENTS.register("advanced_feather_falling",
            () -> new AdvancedFeatherFallingEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.ARMOR_FEET,
                    new EquipmentSlot[] { EquipmentSlot.FEET }));

    public static final RegistryObject<Enchantment> ADVANCED_SHARPNESS = ENCHANTMENTS.register("advanced_sharpness",
            () -> new AdvancedSharpnessEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.WEAPON, MAINHAND));
    public static final RegistryObject<Enchantment> ADVANCED_FIRE_ASPECT = ENCHANTMENTS.register("advanced_fire_aspect",
            () -> new AdvancedFireAspectEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.WEAPON, MAINHAND));
    public static final RegistryObject<Enchantment> ADVANCED_KNOCKBACK = ENCHANTMENTS.register("advanced_knockback",
            () -> new AdvancedKnockbackEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.WEAPON, MAINHAND));
    public static final RegistryObject<Enchantment> ADVANCED_LOOTING = ENCHANTMENTS.register("advanced_looting",
            () -> new AdvancedLootingEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.WEAPON, MAINHAND));

    public static final RegistryObject<Enchantment> SWIFT_STRIKE = ENCHANTMENTS.register("swift_strike",
            () -> new SwiftStrikeEnchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.WEAPON, MAINHAND));

    public static final RegistryObject<Enchantment> ARMOR_PIERCING = ENCHANTMENTS.register("armor_piercing",
            () -> new ArmorPiercingEnchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.WEAPON, MAINHAND));

    public static final RegistryObject<Enchantment> TRUE_DAMAGE = ENCHANTMENTS.register("true_damage",
            () -> new TrueDamageEnchantment(Enchantment.Rarity.RARE, EnchantmentCategory.WEAPON, MAINHAND));

    public static final RegistryObject<Enchantment> ADVANCED_POWER = ENCHANTMENTS.register("advanced_power",
            () -> new AdvancedPowerEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.BOW, MAINHAND));
    public static final RegistryObject<Enchantment> ADVANCED_EFFICIENCY = ENCHANTMENTS.register("advanced_efficiency",
            () -> new AdvancedEfficiencyEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.DIGGER, MAINHAND));
    public static final RegistryObject<Enchantment> ADVANCED_UNBREAKING = ENCHANTMENTS.register("advanced_unbreaking",
            () -> new AdvancedUnbreakingEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.BREAKABLE,
                    new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }));

    public static List<Enchantment> all() {
        return Stream.of(ADVANCED_PROTECTION, ADVANCED_THORNS, ADVANCED_FEATHER_FALLING,
                        ADVANCED_SHARPNESS, ADVANCED_FIRE_ASPECT, ADVANCED_KNOCKBACK, ADVANCED_LOOTING,
                        ADVANCED_POWER, ADVANCED_EFFICIENCY, ADVANCED_UNBREAKING, SWIFT_STRIKE, TRUE_DAMAGE)
                .filter(RegistryObject::isPresent)
                .map(RegistryObject::get)
                .toList();
    }

    /** Pool mặc định (thủ thư, sách ngoài loot): enchant Advanced + Đánh Nhanh, KHÔNG có Sát Thương Chuẩn (loot riêng). */
    public static List<Enchantment> lootPool() {
        return present(ADVANCED_PROTECTION, ADVANCED_THORNS, ADVANCED_FEATHER_FALLING,
                ADVANCED_SHARPNESS, ADVANCED_FIRE_ASPECT, ADVANCED_KNOCKBACK, ADVANCED_LOOTING,
                ADVANCED_POWER, ADVANCED_EFFICIENCY, ADVANCED_UNBREAKING, SWIFT_STRIKE);
    }

    /** Enchant nâng cao (VERY_RARE) loot trên đồ trong rương và sách dungeon. */
    public static List<Enchantment> advancedPool() {
        return present(ADVANCED_PROTECTION, ADVANCED_THORNS, ADVANCED_FEATHER_FALLING,
                ADVANCED_SHARPNESS, ADVANCED_FIRE_ASPECT, ADVANCED_KNOCKBACK, ADVANCED_LOOTING,
                ADVANCED_POWER, ADVANCED_EFFICIENCY, ADVANCED_UNBREAKING);
    }

    /** Enchant "thường" (UNCOMMON) loot trên đồ trong rương. */
    public static List<Enchantment> normalPool() {
        return present(SWIFT_STRIKE);
    }

    @SafeVarargs
    private static List<Enchantment> present(RegistryObject<Enchantment>... objs) {
        return Stream.of(objs).filter(RegistryObject::isPresent).map(RegistryObject::get).toList();
    }

    public static int levelOf(ItemStack stack, RegistryObject<Enchantment> enchantment) {
        if (stack.isEmpty() || !enchantment.isPresent()) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), stack);
    }
}
