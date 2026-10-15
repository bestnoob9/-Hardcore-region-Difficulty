# Integration Notes - Loot Table and Villager Trade Injection

## Overview

The core systems for the Hardcore Region Difficulty mod are implemented, but loot table injection and villager trade injection require additional integration work that depends on each loader's specific API. This document outlines the remaining work needed for production-ready implementation.

---

## 1. Loot Table Injection

### Current State
- `LootTableInjector.java` provides the logic for enchanting items based on difficulty
- The method `tryEnchantItem()` is ready to use
- However, integrating this into the actual loot table generation requires loader-specific approaches

### Challenge
The main challenge is that loot tables are loaded and applied at different points in the loading process, and we need access to the chest's position to determine the difficulty level. Standard loot table events don't provide position information.

### Recommended Approaches

#### Forge Approach: Global Loot Modifiers (GLM)

Forge provides Global Loot Modifiers which allow modifying loot tables based on context.

**Steps:**
1. Create a Global Loot Modifier class
2. Use Forge's `@GlobalLootModifier` annotation
3. Implement the `apply()` method to modify loot
4. Access the chest position through the loot context

**Example Structure:**
```java
@GlobalLootModifier
public class AdvancedEnchantLootModifier implements LootModifier {
    private final ILootCondition[] conditions;
    
    public AdvancedEnchantLootModifier(ILootCondition[] conditions) {
        this.conditions = conditions;
    }
    
    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // Get chest position from context
        BlockPos pos = context.getParamOrNull(LootContextParams.ORIGIN);
        Level level = context.getLevel();
        
        // Apply enchantment logic
        for (ItemStack stack : generatedLoot) {
            ItemStack enchanted = LootTableInjector.tryEnchantItem(stack, pos, level);
            if (enchanted != stack) {
                generatedLoot.set(generatedLoot.indexOf(stack), enchanted);
            }
        }
        
        return generatedLoot;
    }
}
```

**JSON Configuration (forge/modid/loot_modifiers):**
```json
{
  "type": "hardcore_region_difficulty:advanced_enchant_modifier",
  "conditions": [
    {
      "condition": "minecraft:location_check",
      "predicate": {
        "block": {
          "tag": "minecraft:chests"
        }
      }
    }
  ]
}
```

#### Fabric Approach: Loot Table API

Fabric provides `LootTableEvents` for modifying loot tables.

**Steps:**
1. Use `LootTableEvents.REPLACE` to modify loot tables
2. Access position through the loot context
3. Apply enchantment logic

**Example:**
```java
LootTableEvents.REPLACE.register((resourceManager, lootManager, id, table, setter) -> {
    if (id.getPath().contains("chests")) {
        // Modify the loot table
        // Note: Position access is limited here
        // May need to use mixin approach instead
    }
});
```

**Alternative Fabric Approach: Mixin**

Since Fabric's loot table events don't provide position, a mixin approach may be necessary:

```java
@Mixin(ChestBlockEntity.class)
public class ChestBlockEntityMixin {
    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void onUnpackLoot(CallbackInfo ci) {
        // Access position via (ChestBlockEntity)(this)
        BlockPos pos = ((ChestBlockEntity)(this)).getBlockPos();
        Level level = ((ChestBlockEntity)(this)).getLevel();
        
        // Modify loot items based on position
    }
}
```

---

## 2. Villager Trade Injection

### Current State
- `VillagerTradeInjector.java` provides the logic for upgrading villager trades
- The method `tryUpgradeTrade()` is ready to use
- Integration with villager trade generation needs loader-specific event handling

### Challenge
Villager trades are generated when a villager's offers are refreshed. We need to intercept this process and modify the offers based on the villager's position.

### Recommended Approaches

#### Forge Approach: VillagerTradesEvent

Forge provides the `VillagerTradesEvent` which allows modifying trade listings.

**Steps:**
1. Listen to `VillagerTradesEvent`
2. Wrap existing `ItemListing` implementations
3. Modify the generated offers

**Example:**
```java
@SubscribeEvent
public void onVillagerTrades(VillagerTradesEvent event) {
    if (event.getType() == VillagerProfession.LIBRARIAN) {
        // Modify librarian trades (enchanted books)
        event.getTrades().get(1).replaceAll(trade -> {
            return new ItemListing() {
                @Override
                public MerchantOffer getOffer(Entity entity, RandomSource random) {
                    MerchantOffer original = trade.getOffer(entity, random);
                    return VillagerTradeInjector.tryUpgradeTrade((Villager)entity, original);
                }
            };
        });
    }
}
```

**Note:** This approach requires wrapping each trade listing, which can be complex. A simpler approach is to use a mixin on the villager trade generation code.

#### Fabric Approach: Mixin

Fabric doesn't have a direct equivalent to VillagerTradesEvent, so a mixin approach is recommended.

**Example:**
```java
@Mixin(Villager.class)
public class VillagerMixin {
    @Inject(method = "updateSpecialPrices", at = @At("HEAD"))
    private void onUpdateSpecialPrices(ServerPlayer player, CallbackInfo ci) {
        // Access villager position
        BlockPos pos = ((Villager)(this)).blockPosition();
        Level level = ((Villager)(this)).level();
        
        // Modify trades based on position
    }
}
```

Alternatively, mixin into the `EnchantedItemForEmeralds` class:

```java
@Mixin(EnchantedItemForEmeralds.class)
public class EnchantedItemForEmeraldsMixin {
    @Inject(method = "getOffer", at = @At("RETURN"), cancellable = true)
    private void onGetOffer(Entity entity, RandomSource random, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer original = cir.getReturnValue();
        if (entity instanceof Villager villager) {
            MerchantOffer upgraded = VillagerTradeInjector.tryUpgradeTrade(villager, original);
            cir.setReturnValue(upgraded);
        }
    }
}
```

---

## 3. Implementation Priority

### Phase 1: Core Functionality (Current State - ✅ Complete)
- ✅ Multi-loader architecture
- ✅ Static difficulty system
- ✅ Location matching
- ✅ Entity scaling
- ✅ Title display with configuration
- ✅ Advanced enchantments registered
- ✅ Language files
- ✅ JSON configuration

### Phase 2: Loot Table Injection (Needs Implementation)
- ⏳ Forge: Implement Global Loot Modifier
- ⏳ Fabric: Implement mixin for chest loot
- ⏳ Test with various chest types and locations

### Phase 3: Villager Trade Injection (Needs Implementation)
- ⏳ Forge: Implement VillagerTradesEvent handler
- ⏳ Fabric: Implement mixin for villager trades
- ⏳ Test with different villager professions

### Phase 4: Testing and Polish
- ⏳ Comprehensive testing across all difficulty levels
- ⏳ Performance profiling
- ⏳ Balance tuning for spawn rates
- ⏳ Documentation updates

---

## 4. Testing Strategy

### Location Matching Tests
- Test dimension matching (Overworld, Nether, End)
- Test biome matching (desert, deep dark, etc.)
- Test biome tag matching (#minecraft:is_forest, etc.)
- Test structure matching (fortress, monument, mansion, etc.)
- Verify priority order (structure > biome > biome tag > dimension)

### Entity Scaling Tests
- Spawn mobs in different difficulty regions
- Verify health, armor, damage, speed scaling
- Test with various mob types
- Verify exclusions (players, bosses)

### Title Display Tests
- Walk between regions and verify title display
- Test configuration options (enable/disable, timing)
- Verify fade in/out effects
- Test with rapid region changes

### Enchantment Tests
- Verify enchantments don't appear in enchanting table
- Verify enchantments don't appear from fishing
- Verify enchantments don't appear from mob drops
- Test chest loot injection (after implementation)
- Test villager trade injection (after implementation)
- Verify spawn rates scale with difficulty

---

## 5. Performance Considerations

### Location Matching
- Structure checking is expensive - use structure start system
- Cache region lookups per player (already implemented)
- Consider spatial partitioning for large servers

### Entity Scaling
- Only scale on entity spawn (already implemented)
- Avoid recalculating attributes every tick
- Use efficient attribute modification

### Loot Injection
- Minimize loot table modifications
- Cache difficulty calculations per chunk
- Avoid expensive operations during loot generation

---

## 6. Configuration Examples

### Adding Custom Regions

To add a custom region for a modded structure:

```json
{
  "id": "modded_dungeon",
  "region_name": "Modded Dungeon",
  "difficulty_title": "Difficulty V",
  "difficulty_level": 5,
  "dimensions": [],
  "biomes": [],
  "biome_tags": [],
  "structures": [
    "modid:dungeon_structure"
  ]
}
```

### Adjusting Spawn Rates

To make advanced enchantments more common:

```json
{
  "enchantment_settings": {
    "base_chance_per_level": {
      "1": 0.05,
      "2": 0.10,
      "3": 0.15,
      "4": 0.20,
      "5": 0.25
    }
  }
}
```

### Customizing Title Display

To disable title display:

```json
{
  "title_display_settings": {
    "enabled": false
  }
}
```

To make titles display longer:

```json
{
  "title_display_settings": {
    "enabled": true,
    "fade_in_ticks": 20,
    "stay_ticks": 140,
    "fade_out_ticks": 40
  }
}
```

---

## 7. Known Limitations

### Loot Table Injection
- Position access in loot table events is limited
- May require mixin approach for Fabric
- Global Loot Modifiers require Forge-specific JSON configuration

### Villager Trade Injection
- Trade wrapping in Forge can be complex
- Fabric requires mixin approach
- May need to handle different villager professions differently

### Structure Detection
- Structure detection depends on Minecraft's structure system
- Modded structures may not be properly detected
- May need to add compatibility for popular structure mods

---

## 8. Compatibility Notes

### Mod Compatibility
- The mod should work with most structure mods
- Custom structures need to be added to config
- Biome tags support makes it easy to add modded biomes

### Loader Compatibility
- Forge 47.4.20+ required
- Fabric Loader for 1.20.1 required
- Both loaders use official Mojang mappings

### Version Compatibility
- Designed for Minecraft 1.20.1
- May require updates for newer versions
- Architecture allows for relatively easy version updates

---

## Summary

The core systems of Hardcore Region Difficulty are fully implemented and functional. The remaining work is primarily integration of loot table and villager trade injection, which requires loader-specific approaches:

- **Forge**: Use Global Loot Modifiers for loot, VillagerTradesEvent for trades
- **Fabric**: Use mixins for both loot and trades due to API limitations

These integrations are well-documented and follow standard practices for each loader. Once implemented, the mod will be fully production-ready.
