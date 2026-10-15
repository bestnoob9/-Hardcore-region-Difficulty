# Hardcore Region Difficulty - Forge Architecture Documentation

## 1. Kiến trúc tổng thể của mod (Forge-only)

Mod sử dụng kiến trúc sạch với code chia làm 2 phần chính:

```
src/main/java/org/Bacontete/hardcore_region_difficulty/
├── common/              # Core logic - không phụ thuộc loader
│   ├── HardcoreRegionDifficulty.java       # Constants và logger
│   ├── config/                                 # Hệ thống config JSON
│   ├── difficulty/                            # Location matching & region tracking
│   ├── enchantment/                           # Advanced enchantments
│   ├── entity/                                # Entity scaling
│   ├── loot/                                  # Chest loot injection
│   ├── villager/                              # Villager trade injection
│   ├── network/                               # Network packets
│   └── client/                                # Client-side title display
└── forge/              # Implementation riêng cho Forge
    ├── HardcoreRegionDifficultyForge.java     # Main mod class
    ├── NetworkHandler.java                    # Forge networking
    ├── ForgeConfig.java                       # Forge config wrapper
    └── loot/                                   # Forge Global Loot Modifier
        └── AdvancedEnchantLootModifier.java
```

**Lợi ích của kiến trúc này:**
- Code logic chung được viết một lần, dễ test và maintain
- Chỉ có phần registration và event handling cần Forge-specific code
- Dễ bảo trì và update

---

## 2. Hệ thống Difficulty (Location Matching)

### 2.1 LocationMatcher.java
**File**: `common/difficulty/LocationMatcher.java`

**Chức năng**: Xác định difficulty region dựa trên vị trí của player/entity.

**Priority matching** (cao đến thấp):
1. **Structure** - Ưu tiên cao nhất
2. **Biome** 
3. **Biome Tag**
4. **Dimension** - Ưu tiên thấp nhất

**Key methods**:
```java
public static DifficultyRegion matchRegion(Level level, BlockPos pos)
public static int getDifficultyLevel(Level level, BlockPos pos)
public static String getRegionName(Level level, BlockPos pos)
public static String getDifficultyTitle(Level level, BlockPos pos)
```

**Logic structure matching**:
- Sử dụng `StructureManager` để check structure tại position
- Tối ưu performance bằng cách dùng structure start system thay vì scan chunk

### 2.2 DifficultyRegion.java
**File**: `common/config/DifficultyRegion.java`

**Đại diện**: Một region với difficulty settings.

**Fields**:
- `id`: Unique identifier
- `regionName`: Tên hiển thị (VD: "Ancient Desert Temple")
- `difficultyTitle`: Tên độ khó (VD: "Difficulty IV")
- `difficultyLevel`: Số nguyên ≥ 1
- `dimensions`: List dimension IDs
- `biomes`: List biome IDs
- `biomeTags`: List biome tag IDs
- `structures`: List structure IDs

### 2.3 PlayerRegionTracker.java
**File**: `common/difficulty/PlayerRegionTracker.java`

**Chức năng**: Track region hiện tại của mỗi player để detect khi thay đổi.

**Key methods**:
```java
public static boolean updateAndCheckChange(Player player)
public static RegionInfo getPlayerRegion(Player player)
```

**Logic**:
- Server-side tracking để đảm bảo consistency
- So sánh RegionInfo (difficultyLevel, regionName, difficultyTitle)
- Return true khi region thay đổi → trigger title display

---

## 3. Hệ thống hiển thị Title khi thay đổi vùng

### 3.1 TitleDisplayPacket.java
**File**: `common/network/TitleDisplayPacket.java`

**Chức năng**: Network packet gửi từ server → client để hiển thị title.

**Fields**:
- `regionName`: Tên vùng (dòng lớn)
- `difficultyTitle`: Tên độ khó (dòng nhỏ)

**Methods**:
- `encode(FriendlyByteBuf)`: Serialize
- `Constructor(FriendlyByteBuf)`: Deserialize

### 3.2 TitleDisplayHandler.java
**File**: `common/client/TitleDisplayHandler.java`

**Chức năng**: Client-side handler để hiển thị title.

**Key method**:
```java
public static void displayTitle(String regionName, String difficultyTitle)
```

**Implementation**:
- Sử dụng `Minecraft.gui.setTitle()` với fade in/out
- Title: Region name (lớn)
- Subtitle: Difficulty title (nhỏ)
- Fade timing: configurable từ config (default: 10 ticks in, 70 ticks stay, 20 ticks out)

### 3.3 Network Handler (Forge)

**File**: `forge/NetworkHandler.java`

**Implementation**:
- Sử dụng `SimpleChannel` của Forge
- Register packet với protocol version
- Send to player via `ServerPlayNetworking`

---

## 4. Hệ thống Entity Scaling

### 4.1 EntityScaler.java
**File**: `common/entity/EntityScaler.java`

**Chức năng**: Scale attributes của mob dựa trên difficulty level.

**Key method**:
```java
public static void applyScaling(Mob mob)
```

**Attributes được scale**:
1. **MAX_HEALTH**: Scaled theo `healthMultiplier`
2. **ARMOR**: Scaled theo `armorMultiplier`
3. **ATTACK_DAMAGE**: Scaled theo `damageMultiplier`
4. **MOVEMENT_SPEED**: Scaled theo `speedMultiplier`

**Formula**:
```
scaledValue = baseValue * (1 + (difficultyLevel - 1) * multiplier)
```

**Exclusions**:
- Không scale Players
- Không scale Bosses (Ender Dragon, Wither, Warden)
- Chỉ scale hostile/neutral mobs

### 4.2 ScalingSettings.java
**File**: `common/config/ScalingSettings.java`

**Chức năng**: Config cho scaling multipliers.

**Methods**:
```java
public double scaleHealth(double baseHealth, int difficultyLevel)
public double scaleArmor(double baseArmor, int difficultyLevel)
public double scaleDamage(double baseDamage, int difficultyLevel)
public double scaleSpeed(double baseSpeed, int difficultyLevel)
```

### 4.3 Event Registration (Forge)

**File**: `forge/HardcoreRegionDifficultyForge.java`

```java
@SubscribeEvent
public void onEntityJoin(EntityJoinLevelEvent event)
```

---

## 5. Hệ thống Advanced Enchantments

### 5.1 AdvancedEnchantments.java
**File**: `common/enchantment/AdvancedEnchantments.java`

**Chức năng**: Registry và initialization của tất cả advanced enchants.

**Enchantments được đăng ký**:
- `ADVANCED_PROTECTION` - Armor
- `ADVANCED_SHARPNESS` - Weapon
- `ADVANCED_POWER` - Bow
- `ADVANCED_FIRE_ASPECT` - Weapon
- `ADVANCED_THORNS` - Armor
- `ADVANCED_FEATHER_FALLING` - Boots
- `ADVANCED_LOOTING` - Weapon
- `ADVANCED_EFFICIENCY` - Tool
- `ADVANCED_UNBREAKING` - Any
- `ADVANCED_KNOCKBACK` - Weapon

### 5.2 Individual Enchantment Classes

Mỗi enchantment extend `Enchantment` class:

**Example**: `AdvancedProtectionEnchantment.java`
```java
public class AdvancedProtectionEnchantment extends Enchantment {
    public AdvancedProtectionEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] slots) {
        super(rarity, category, slots);
    }
    
    @Override
    public int getMinLevel() { return 1; }
    
    @Override
    public int getMaxLevel() { return 5; }
    
    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 8;
    }
    
    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 10;
    }
}
```

**Special implementations**:
- `AdvancedSharpnessEnchantment`: Override `getDamageBonus()` → +2.5 damage/level
- `AdvancedPowerEnchantment`: Override `getDamageBonus()` → +1.0 damage/level
- `AdvancedProtectionEnchantment`: Incompatible với regular Protection

### 5.3 Registration (Forge)

**File**: `forge/HardcoreRegionDifficultyForge.java`

```java
private static final DeferredRegister<Enchantment> ENCHANTMENTS = 
    DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MODID);

ENCHANTMENTS.register("advanced_protection", () -> AdvancedEnchantments.ADVANCED_PROTECTION);
// ... similar for other enchants
```

---

## 6. Inject Advanced Enchant vào Chest Loot và Villager Trading

### 6.1 LootTableInjector.java
**File**: `common/loot/LootTableInjector.java`

**Chức năng**: Inject advanced enchants vào chest loot.

**Key method**:
```java
public static ItemStack tryEnchantItem(ItemStack item, BlockPos chestPos, Level level)
```

**Logic**:
1. Check nếu item có thể enchant (books, enchantable items)
2. Get difficulty level từ position
3. Calculate chance từ config (`baseChancePerLevel`)
4. Random check theo chance
5. Select random advanced enchant
6. Determine level từ config (`maxLevelPerDifficulty`)
7. Apply enchantment

**Chance rates** (default):
- Difficulty 1: 1%
- Difficulty 2: 2%
- Difficulty 3: 3%
- Difficulty 4: 5%
- Difficulty 5: 8%

### 6.2 Forge Global Loot Modifier

**File**: `forge/loot/AdvancedEnchantLootModifier.java`

**Chức năng**: Forge Global Loot Modifier để inject advanced enchants vào chest loot.

**Implementation**:
```java
@GlobalLootModifier
public class AdvancedEnchantLootModifier implements IGlobalLootModifier {
    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        BlockEntity blockEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (blockEntity != null) {
            Level level = context.getLevel();
            BlockPos pos = blockEntity.getBlockPos();
            
            for (int i = 0; i < generatedLoot.size(); i++) {
                ItemStack original = generatedLoot.get(i);
                ItemStack enchanted = LootTableInjector.tryEnchantItem(original, pos, level);
                if (enchanted != original) {
                    generatedLoot.set(i, enchanted);
                }
            }
        }
        return generatedLoot;
    }
}
```

**JSON Configuration**: `data/hardcore_region_difficulty/loot_modifiers/advanced_enchant_chests.json`
```json
{
  "type": "hardcore_region_difficulty:advanced_enchant_loot"
}
```

### 6.3 VillagerTradeInjector.java
**File**: `common/villager/VillagerTradeInjector.java`

**Chức năng**: Inject advanced enchants vào villager enchanted book trades.

**Key method**:
```java
public static MerchantOffer tryUpgradeTrade(Villager villager, MerchantOffer offer)
```

**Logic**:
1. Check nếu offer là enchanted book
2. Get difficulty level từ villager position
3. Calculate chance (same as chests)
4. Random check
5. Select random advanced enchant
6. Create new offer với advanced enchanted book

### 6.4 Forge VillagerTradesEvent

**File**: `forge/HardcoreRegionDifficultyForge.java`

```java
@SubscribeEvent
public void onVillagerTrades(VillagerTradesEvent event) {
    if (event.getEntity() instanceof Villager villager) {
        if (event.getType().getRegistryName() != null && 
            event.getType().getRegistryName().toString().equals("minecraft:librarian")) {
            
            var trades = event.getTrades();
            trades.forEach((level, tradeList) -> {
                for (int i = 0; i < tradeList.size(); i++) {
                    var trade = tradeList.get(i);
                    var wrappedTrade = new VillagerTradesEvent.ItemListing() {
                        @Override
                        public MerchantOffer getOffer(Entity entity, RandomSource random) {
                            var originalOffer = trade.getOffer(entity, random);
                            return VillagerTradeInjector.tryUpgradeTrade(villager, originalOffer);
                        }
                    };
                    tradeList.set(i, wrappedTrade);
                }
            });
        }
    }
}
```

---

## 7. File Config JSON mẫu

**File**: `hardcore_region_difficulty.json` (tự động tạo ở config folder)

**Structure**:
```json
{
  "difficulty_regions": [
    {
      "id": "overworld_default",
      "region_name": "Overworld",
      "difficulty_title": "Difficulty I",
      "difficulty_level": 1,
      "dimensions": ["minecraft:overworld"],
      "biomes": [],
      "biome_tags": [],
      "structures": []
    },
    {
      "id": "nether_fortress",
      "region_name": "Nether Fortress",
      "difficulty_title": "Difficulty IV",
      "difficulty_level": 4,
      "dimensions": [],
      "biomes": [],
      "biome_tags": [],
      "structures": ["minecraft:fortress"]
    }
  ],
  "scaling_settings": {
    "health_multiplier": 0.2,
    "armor_multiplier": 0.15,
    "damage_multiplier": 0.15,
    "speed_multiplier": 0.05
  },
  "enchantment_settings": {
    "base_chance_per_level": {
      "1": 0.01,
      "2": 0.02,
      "3": 0.03,
      "4": 0.05,
      "5": 0.08
    },
    "max_level_per_difficulty": {
      "1": 1,
      "2": 2,
      "3": 3,
      "4": 4,
      "5": 5
    }
  },
  "title_display_settings": {
    "enabled": true,
    "fade_in_ticks": 10,
    "stay_ticks": 70,
    "fade_out_ticks": 20
  }
}
```

**Config loading**: `DifficultyConfig.java`
- Load từ JSON file khi mod khởi động
- Auto-create default config nếu file không tồn tại
- Support reload khi config thay đổi

---

## 8. File Ngôn ngữ

### 8.1 en_us.json
**File**: `src/main/resources/assets/hardcore_region_difficulty/lang/en_us.json`

```json
{
  "enchantment.hardcore_region_difficulty.advanced_protection": "Advanced Protection",
  "enchantment.hardcore_region_difficulty.advanced_sharpness": "Advanced Sharpness",
  "enchantment.hardcore_region_difficulty.advanced_power": "Advanced Power",
  "enchantment.hardcore_region_difficulty.advanced_fire_aspect": "Advanced Fire Aspect",
  "enchantment.hardcore_region_difficulty.advanced_thorns": "Advanced Thorns",
  "enchantment.hardcore_region_difficulty.advanced_feather_falling": "Advanced Feather Falling",
  "enchantment.hardcore_region_difficulty.advanced_looting": "Advanced Looting",
  "enchantment.hardcore_region_difficulty.advanced_efficiency": "Advanced Efficiency",
  "enchantment.hardcore_region_difficulty.advanced_unbreaking": "Advanced Unbreaking",
  "enchantment.hardcore_region_difficulty.advanced_knockback": "Advanced Knockback"
}
```

### 8.2 vi_vn.json
**File**: `src/main/resources/assets/hardcore_region_difficulty/lang/vi_vn.json`

```json
{
  "enchantment.hardcore_region_difficulty.advanced_protection": "Bảo Vệ Nâng Cao",
  "enchantment.hardcore_region_difficulty.advanced_sharpness": "Sắc Bén Nâng Cao",
  "enchantment.hardcore_region_difficulty.advanced_power": "Sức Mạnh Nâng Cao",
  "enchantment.hardcore_region_difficulty.advanced_fire_aspect": "Lửa Nâng Cao",
  "enchantment.hardcore_region_difficulty.advanced_thorns": "Gai Nâng Cao",
  "enchantment.hardcore_region_difficulty.advanced_feather_falling": "Nhẹ Nhàng Nâng Cao",
  "enchantment.hardcore_region_difficulty.advanced_looting": "Thu Thập Nâng Cao",
  "enchantment.hardcore_region_difficulty.advanced_efficiency": "Hiệu Quả Nâng Cao",
  "enchantment.hardcore_region_difficulty.advanced_unbreaking": "Bền Bỉ Nâng Cao",
  "enchantment.hardcore_region_difficulty.advanced_knockback": "Đẩy Lùi Nâng Cao"
}
```

**Quan trọng**: Translation keys phải match format `enchantment.{modid}.{enchant_name}` để hiển thị đúng trên tooltip.

---

## 9. Class / Event quan trọng cho Forge

### 9.1 Main Class

**File**: `forge/HardcoreRegionDifficultyForge.java`
- Annotated với `@Mod(MODID)`
- Register DeferredRegister cho enchantments
- Register DeferredRegister cho Global Loot Modifiers
- Register network handler
- Register event listeners

**Key Events**:
```java
@SubscribeEvent
public void onEntityJoin(EntityJoinLevelEvent event) {
    // Entity scaling
}

@SubscribeEvent
public void onPlayerTick(TickEvent.PlayerTickEvent event) {
    // Region tracking & title display
}

@SubscribeEvent
public void onConfigReload(ModConfigEvent.Reloading event) {
    // Config reload
}

@SubscribeEvent
public void onVillagerTrades(VillagerTradesEvent event) {
    // Villager trade injection
}
```

**Network**: `forge/NetworkHandler.java`
- Sử dụng `SimpleChannel`
- Register packet với encoder/decoder
- Send to player via `ServerPlayNetworking`

### 9.2 Common Events (Both Loaders)

**Player Tick**:
- Check region change mỗi tick
- Send title packet nếu thay đổi

**Entity Join**:
- Apply scaling khi mob spawn
- Check if mob should be scaled

**Config Reload**:
- Reload JSON config khi file thay đổi
- Update cached values

---

## 10. Ràng buộc cứng (Hard Constraints)

### 10.1 Static Difficulty Only
✅ **Đã implement**: Difficulty chỉ dựa trên location
✅ **Không**: Không dựa trên gear, stats, thời gian, hoặc player progression

### 10.2 Advanced Enchant Sources Only
✅ **Đã implement**: Chỉ từ chest loot và villager trades
✅ **Không**: Không từ enchanting table, fishing, mob drops

### 10.3 Performance Optimization
✅ **Đã implement**: 
- Structure checking dùng structure start system
- Region tracking cached per player
- Config values cached after load

---

## Summary

Mod đã được implement hoàn chỉnh với:
- ✅ Forge-only architecture (common + forge)
- ✅ Static, location-based difficulty system
- ✅ 10 advanced enchantments với proper translations
- ✅ Entity attribute scaling
- ✅ Region change title display với configurable timing
- ✅ Data-driven JSON configuration
- ✅ Language support (en_us, vi_vn)
- ✅ Forge Global Loot Modifier cho chest injection
- ✅ Forge VillagerTradesEvent cho villager injection

Architecture tuân thủ tất cả requirements:
- Difficulty hoàn toàn static, chỉ dựa trên location
- Advanced enchants chỉ từ chest + villager trades
- Performance optimized
- Code sạch, có comments
- Dễ mở rộng và maintain
