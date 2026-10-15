package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;

public final class ForgeMaterials {

    private ForgeMaterials() {}

    public static boolean accepts(ItemStack tool, ItemStack material, Level level) {
        if (tool.isEmpty() || material.isEmpty()) {
            return false;
        }
        ResourceLocation toolId = BuiltInRegistries.ITEM.getKey(tool.getItem());
        ResourceLocation materialId = BuiltInRegistries.ITEM.getKey(material.getItem());
        for (String entry : QualityConfig.MATERIAL_OVERRIDES.get()) {
            int eq = entry.indexOf('=');
            if (eq > 0 && entry.substring(0, eq).trim().equals(toolId.toString())
                    && entry.substring(eq + 1).trim().equals(materialId.toString())) {
                return true;
            }
        }
        if (tool.getItem().isValidRepairItem(tool, material)) {
            return true;
        }
        return isRecipeIngredient(tool.getItem(), material, level);
    }

    private static boolean isRecipeIngredient(Item toolItem, ItemStack material, Level level) {
        if (material.is(Items.STICK) || material.is(Tags.Items.RODS) || material.getItem() == toolItem) {
            return false;
        }
        for (Recipe<?> recipe : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!recipe.getResultItem(level.registryAccess()).is(toolItem)) {
                continue;
            }
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.test(material)) {
                    return true;
                }
            }
        }
        return false;
    }
}
