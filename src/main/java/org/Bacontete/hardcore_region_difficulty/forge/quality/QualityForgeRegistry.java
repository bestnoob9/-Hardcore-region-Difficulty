package org.Bacontete.hardcore_region_difficulty.forge.quality;

import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

public final class QualityForgeRegistry {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, HardcoreRegionDifficulty.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HardcoreRegionDifficulty.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, HardcoreRegionDifficulty.MODID);

    public static final RegistryObject<Block> QUALITY_FORGE = BLOCKS.register("quality_forge", QualityForgeBlock::new);
    public static final RegistryObject<Item> QUALITY_FORGE_ITEM = ITEMS.register("quality_forge",
            () -> new BlockItem(QUALITY_FORGE.get(), new Item.Properties()));
    public static final RegistryObject<MenuType<QualityForgeMenu>> QUALITY_FORGE_MENU = MENUS.register("quality_forge",
            () -> IForgeMenuType.create((id, inventory, buf) -> new QualityForgeMenu(id, inventory, ContainerLevelAccess.NULL)));

    private QualityForgeRegistry() {}

    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(QUALITY_FORGE_ITEM);
        }
    }
}
