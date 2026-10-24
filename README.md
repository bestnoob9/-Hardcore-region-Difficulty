---
### ⚔️ Hardcore Region Difficulty

- Where you stand decides how dangerous the world is.
- Hardcore Region Difficulty is a location-based difficulty mod for **Minecraft 1.20.1 (Forge)**. Monster strength depends on the dimension, biome and structure you are in, never on your progression. Go deeper into dangerous places and you will face tougher mobs, but you will also find better gear and rarer enchantments.
- Inspired by Dungeon Difficulty and Quality Equipment.

---

## 🔥 Features

### ☠️ Regional Difficulty Scaling
- Every region has a difficulty level, based on its dimension, biome and structures. Nether Fortresses, Ancient Cities and End Cities push it higher.
- Five levels come built in, and you can add as many of your own as you like. Each one can have its own name, color, icon and stats.
- Smart Auto mode rates dangerous areas for you by looking at nearby spawners and groups of hostile mobs.
- Hostile mobs get more health, damage, armor and speed as the level rises. Growth slows down at high levels so things never spiral out of control.
- Raids always run at Difficulty III by default, for every raider, vanilla or modded. You can change that level or turn it off.
- On-screen titles tell you how dangerous an area is when you walk into it, and you get a warning when a boss is close.
- Everything is flexible: you can match mobs, biomes and structures by exact name, by tag, by pattern, with wildcards, or exclude them.

### 👑 Bosses and Mini Bosses
- Bosses and mini bosses have their own difficulty class, their own skull icons and their own stat bonuses.
- The Ender Dragon, the Wither and the Warden can have their health and damage adjusted.

### 💀 Elite Mobs
- In dangerous regions, regular mobs can spawn as Elites with a special name and particles around them.
- Elites carry random powers: **Fire, Lightning, Poison, Regeneration, Armored, Swift, Tough and Vampiric**.
- Higher levels mean a better chance of an Elite, and more powers at once.
- There is a cap on how many Elites can be alive per level, so a single area never gets flooded.

### 🎁 Better Loot in Dangerous Places
- Chests and mob drops get more generous the higher the region level: extra rolls, bigger stacks, and a chance for gear to come pre-enchanted.
- The quality of the gear you find also improves with the level, and Luck helps.
- Every bonus can be tuned or switched off.

### 🗡️ Item Quality System
- Weapons, tools, armor, bows and shields can roll a quality, from bad ones (Rusted, Damaged) all the way up to Flawless.
- The Quality Forge is a block where you can reroll and upgrade your gear using materials.
- Tooltips show exactly what each quality changes.
- **Fully customizable now:** you can edit, remove or add your own qualities for each type of gear, including their colors, chances and stat effects. There is an in-game editor for it.

### ✨ Advanced Enchantments
Ten enchantments that go well past vanilla limits: Advanced Protection, Sharpness, Power, Fire Aspect, Thorns, Feather Falling, Looting, Efficiency, Unbreaking and Knockback. On top of those you get True Damage, Armor Piercing and Swift Strike.
- Found in chests of dangerous structures, and as librarian trades (expensive and limited).
- Never from the enchanting table, fishing or mob drops.
- The higher the region level, the higher the enchantment levels you can find.
- You can turn them off if you feel they are too strong for your world.

### 🌌 Optional Compat: Deeper and Darker × Cataclysm
Turn on the compat option in the config to link two of the hardest mods:
- Cataclysm's Ignitium and Cursium armor are crafted from Deeper and Darker Warden gear instead of Netherite.
- Needs both Deeper and Darker and L_Ender's Cataclysm when the option is on.

## 🛠️ Commands

| Command | Who can use it | What it does |
|---|---|---|
| `/hrd region` | Everyone | Shows the region and difficulty where you are standing |
| `/hrd regions` | Everyone | Lists all regions |
| `/hrd levels` | Everyone | Lists all difficulty levels and their bonuses |
| `/hrd stats` | Everyone | Shows stats for the mobs around you |
| `/hrd preview <level> [mob]` | Everyone | Previews how strong a mob would be at a given level |
| `/hrd quality show` | Everyone | Shows the qualities of a gear type |
| `/hrd reload` | OP | Reloads the config (a broken file keeps the old config) |
| `/hrd scan` | OP | Rescans structures manually |
| `/hrd export` | OP | Saves your whole setup as one shareable file |
| `/hrd import file` | OP | Loads a shared setup file |
| `/hrd quality reload` | OP | Reloads the quality list |
| `/hrd quality set` | OP | Sets a quality on the item you are holding |

Main config file: `config/hardcore_region_difficulty.json`
Quality file: `config/hardcore_region_difficulty-qualities.json`

---

## 📋 Requirements & Dependencies

* **Minecraft 1.20.1**
* **Forge 47.4.20+** (Fabric and NeoForge are not supported)
* *Optional:* Deeper and Darker + Cataclysm for the compat feature
* **[Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config)** *(required for the in-game settings screen)*

---

## 🙏 Credits & Acknowledgments

This mod was inspired by the core mechanics and gameplay concepts of:

* **[Dungeon Difficulty](https://www.curseforge.com/minecraft/mc-mods/dungeon-difficulty)** by *daedelus_dev*
* **[Quality Equipment](https://www.curseforge.com/minecraft/mc-mods/quality-equipment)** by *PooPmeep*

---
