# Hardcore Region Difficulty

Mod Minecraft **1.20.1 (Forge)**: độ khó cố định theo vị trí (dimension, biome, tag biome, structure) cùng 10 enchant "Advanced" kiểu RLCraft. Độ khó **không bao giờ** phụ thuộc vào tiến trình người chơi.

- Chỉ Forge 47.4.20+ (không có Fabric).
- Hệ thống scale mob, title đổi vùng, loot/villager enchant, lệnh reload.

## Cài đặt
1. Cài Forge 47.4.20+ cho Minecraft 1.20.1
2. Bỏ file jar vào thư mục `mods`
3. Chạy game. Config sinh ra tại `config/hardcore_region_difficulty.json`

## Lệnh
| Lệnh | Quyền | Tác dụng |
|---|---|---|
| `/hrd reload` | OP (2) | Đọc lại file config (file lỗi thì giữ cấu hình cũ) |
| `/hrd region` | Mọi người | Xem vùng/độ khó tại vị trí đang đứng |
| `/hrd scan` | OP (2) | Quét lại thủ công (mặc định đã tự quét mỗi khi server khởi động, xem bên dưới) |

## Region

```json
{
  "id": "desert_temple",
  "region_name": "Ancient Desert Temple",
  "difficulty_title": "Difficulty III",
  "difficulty_level": 3,
  "dimensions": [],
  "biomes": ["minecraft:desert"],
  "biome_tags": [],
  "structures": ["minecraft:desert_pyramid"]
}
```

**Một region khớp khi TẤT CẢ điều kiện không rỗng đều khớp** (AND). Trong mỗi danh sách là OR.
Ví dụ trên chỉ khớp khi đang đứng trong `desert_pyramid` **và** biome là `desert`.

**Ưu tiên** (theo điều kiện cụ thể nhất của region): structure > biome > biome tag > dimension.
Cùng cấp thì lấy `difficulty_level` cao hơn, bằng nhau thì region đứng trước trong file.

Thứ tự xử lý: region có structure/biome/tag → smart-auto (nếu bật) → region chỉ có dimension.

### Pattern (giống DungeonDifficulty)
Dùng được trong `dimensions`, `biomes`, `structures`:

| Cú pháp | Ý nghĩa |
|---|---|
| `minecraft:desert` | khớp chính xác |
| `#minecraft:is_forest` | tag |
| `~deep.*ocean` | regex (tìm trong id đầy đủ) |
| `*` | mọi thứ |
| `!minecraft:river` | phủ định |

`biome_tags` vẫn dùng được (có hoặc không có dấu `#`).

## Scale mob

```json
"scaling_settings": {
  "health_percent_per_level": 25.0,
  "damage_percent_per_level": 20.0,
  "armor_percent_per_level": 20.0,
  "speed_percent_per_level": 2.0,
  "armor_flat_per_level": 1.0,
  "exclude_entities": [],
  "boss_health_threshold": 150
}
```

Mỗi cấp độ khó mạnh hơn cấp liền trước `X%` (cộng dồn, cấp 1 là mức vanilla). Với `n = difficulty_level - 1`, mức tăng tổng = `(1 + X/100)^n - 1`, áp dụng bằng MULTIPLY_BASE.

| Thuộc tính | Mặc định / cấp | Cấp 2 | Cấp 3 | Cấp 4 | Cấp 5 |
|---|---|---|---|---|---|
| Máu | 25% | +25.0% | +56.2% | +95.3% | +144.1% |
| Sát thương | 20% | +20.0% | +44.0% | +72.8% | +107.4% |
| Giáp | 20% (+ `armor_flat_per_level` = +1 giáp mỗi cấp) | +20.0% | +44.0% | +72.8% | +107.4% |
| Tốc độ | 2% | +2.0% | +4.0% | +6.1% | +8.2% |

Đổi `*_percent_per_level` để chỉnh (ví dụ `20.0` = mỗi cấp mạnh hơn cấp trước 20%; tốc độ nên để thấp, 20% mỗi cấp làm quái nhanh gấp đôi ở cấp 5). Các khóa cũ `*_multiplier` không còn được đọc (ý nghĩa đã đổi), file config cũ sẽ tự dùng mặc định mới.

- Chỉ mob thù địch (`Enemy`). Không scale: Ender Dragon, Wither, Warden, mob có máu gốc 150-249 (>= `boss_health_threshold`, dưới ngưỡng mini boss) (mặc định 150, `0` = tắt kiểm tra), và mob khớp `exclude_entities` (id, `#tag` hoặc `~regex`, ví dụ `["~^somemod:.*_boss$"]`).
- Mob chỉ được scale **một lần** (modifier lưu cùng thực thể), load lại chunk không cộng dồn.
- Nhiều mob có giáp gốc bằng 0 nên `armor_percent_per_level` không có tác dụng với chúng. Muốn mob có thêm giáp thì đặt `armor_flat_per_level` > 0 (ví dụ `1.0`).

## Enchant Advanced

| Enchant | Loại | Cấp tối đa | Hiệu ứng |
|---|---|---|---|
| Advanced Protection | Giáp | 5 | +2 điểm bảo vệ mỗi cấp (vanilla 1) |
| Advanced Sharpness | Kiếm và rìu | 5 | +2.5 sát thương mỗi cấp |
| Advanced Power | Cung | 5 | +1.0 sát thương mũi tên mỗi cấp |
| Advanced Fire Aspect | Vũ khí | 3 | 6 giây cháy mỗi cấp |
| Advanced Thorns | Giáp | 3 | phản 2 sát thương mỗi cấp, tỉ lệ 30% + 20%/cấp |
| Advanced Feather Falling | Giày | 4 | +5 điểm bảo vệ té ngã mỗi cấp |
| Advanced Looting | Vũ khí | 3 | +2 cấp looting mỗi cấp |
| Advanced Efficiency | Công cụ | 5 | tốc độ đào cộng thêm `2*cấp² + 2` |
| Advanced Unbreaking | Mọi đồ bền | 4 | bỏ qua phần lớn hao mòn (giữ lại 1/(1+2*cấp) mỗi điểm) |
| Advanced Knockback | Vũ khí | 3 | +0.5 knockback mỗi cấp |

Chỉ xuất hiện từ **rương** (loot table `chests/...`) và **sách enchant của thủ thư**. Không có từ bàn enchant, câu cá, mob drop, hay `enchant_randomly`.

Nguồn xuất hiện (cấu hình trong `enchantment_settings`):

| Nguồn | Cách hoạt động | Cấu hình | Mặc định |
|---|---|---|---|
| Thủ thư | Mỗi lần thủ thư tạo 1 lượt bán sách enchant, có tỉ lệ đổi thành sách Advanced. **Chỉ bán cấp I** (không phụ thuộc độ khó vùng), giá tối thiểu 40 emerald, tối đa 8 lượt mua mỗi offer | `librarian_trade_chance`, `librarian_only_level_1`, `librarian_min_emerald_price`, `librarian_max_uses` | 8%, `true`, 40, 8 |
| Rương dungeon | Mỗi rương có tỉ lệ nhận thêm **1 sách enchant Advanced** | `dungeon_chest_chance`, `dungeon_loot_tables` | 8%, `minecraft:chests/simple_dungeon` |
| Rương trong công trình (tự nhận) | Rương `chests/...` **nằm trong một công trình** có cấp từ 3 trở lên (kể cả công trình của mod khác, không cần liệt kê loot table) có tỉ lệ nhận thêm 1 sách Advanced. Cấp 3: 4%, sách I-II; cấp 4: 6%, sách I-III; cấp 5: 8%, sách I-V. Sách cấp thấp luôn dễ ra hơn | `dungeon_book_auto`, `dungeon_book_by_level`, `dungeon_book_level_decay` | bật; 3 = 4% / 2, 4 = 6% / 3, 5 = 8% / 5; 0.5 |
| Mọi rương khác | Mỗi món đồ enchant được có tỉ lệ nhận 1 enchant Advanced theo độ khó vùng | `base_chance_per_level` | 1% - 8% |

- Thủ thư: `librarian_only_level_1` (`true`) ép sách luôn cấp I; đặt `false` để cấp sách theo `max_level_per_difficulty` như cũ. `librarian_min_emerald_price` (40, tối đa 64) là giá sàn: giá = max(giá sàn, 20 + 10 x cấp). `librarian_max_uses` (8) là số lần mua tối đa của offer trước khi hết hàng/chờ restock. Thiếu khóa nào thì dùng mặc định.
- `dungeon_loot_tables` là danh sách id loot table, cuối id có thể thêm `*` để khớp theo tiền tố (ví dụ `"somemod:chests/dungeon_*"`).
- Sách tự nhận (rương trong công trình): mỗi rương tối đa 1 sách thưởng; rương thuộc `dungeon_loot_tables` dùng cách cũ (`dungeon_chest_chance`), không nhân đôi. Chỉ tính khi vị trí rương **nằm trong công trình** (cấp chỉ do biome hoặc spawner ngoài công trình thì không tính). `dungeon_book_by_level` là bảng *cấp độ khó -> `chance` và `max_level`*; có khóa này thì **thay hẳn** mặc định, nên xóa dòng `"3"` là chỉ bắt đầu từ cấp 4. `dungeon_book_level_decay` (0.5) là tỉ lệ giữa hai cấp sách liền nhau: với `max_level` 5 thì I 52%, II 26%, III 13%, IV 6%, V 3%; đặt `1.0` để mọi cấp bằng nhau. `dungeon_book_auto: false` tắt hẳn phần tự nhận. Cấp sách vẫn bị giới hạn thêm bởi `max_level_per_difficulty` và cấp tối đa của enchant.
- Loot Advanced khó hơn (đồ enchant Advanced trong rương, sách dungeon, sách rương công trình, sách Armor Piercing trong rương): `advanced_min_level` (3) là cấp vùng tối thiểu, dưới cấp này **không ra** loot Advanced nào; `advanced_chance_multiplier` (0.7) nhân vào mọi tỉ lệ ở trên; `advanced_loot_level_decay` (0.6) làm cấp enchant cao khó ra hơn cho đồ enchant và sách `dungeon_loot_tables` (max cấp 3: I 51%, II 31%, III 18%). Thủ thư không bị ảnh hưởng. Thiếu khóa thì dùng mặc định, file config cũ không cần sửa.
- Cấp enchant bị giới hạn bởi `max_level_per_difficulty` của vùng. Ở Overworld thường (độ khó 1) tối đa là cấp I; muốn sách ra cấp cao hơn thì tăng giá trị của `"1"`.
- Dùng `/hrd reload` sau khi sửa config.

Giới hạn: Advanced Unbreaking hoàn lại hao mòn sau mỗi tick, nên điểm bền cuối cùng làm hỏng đồ thì không hoàn lại được.

## Title đổi vùng
Kiểm tra mỗi giây khi người chơi di chuyển. Mặc định hiện dạng **gọn**: chữ nhỏ ở phía trên màn hình, dòng 1 = tên vùng, dòng 2 = độ khó (+ tên structure), mờ dần vào/ra. Cấu hình trong `title_display_settings`:

| Khóa | Mặc định | Ý nghĩa |
|---|---|---|
| `enabled` | `true` | Tắt/bật |
| `style` | `"compact"` | `"compact"` = chữ nhỏ phía trên (dòng 1: tên biome / công trình, dòng 2: **đầu lâu theo cấp** + độ khó), `"vanilla"` = title to giữa màn hình (đầu lâu là ký tự ☠ đổi màu theo cấp) |
| `scale` | `1.8` | Cỡ chữ của kiểu compact (`1.0` nhỏ như bản cũ, `2.5` rất to; tối đa `3.0`) |
| `y_offset` | `30` | Cách mép trên màn hình bao nhiêu pixel (nhỏ = cao hơn; thanh máu boss nằm ở khoảng 12-26) |
| `fade_in_ticks` / `stay_ticks` / `fade_out_ticks` | `10` / `40` / `20` | Thời gian hiện |
| `repeat_cooldown_seconds` | `60` | Không báo lại cùng một vùng trong khoảng này (hết cảnh title nhấp nháy khi đi dọc biên vùng); `0` = luôn báo. **Ngoại lệ:** ra khỏi công trình về biome thì luôn báo, không bị cooldown chặn |

**Đầu lâu theo cấp** (kiểu compact): 5 icon đầu lâu riêng nằm trong `assets/hardcore_region_difficulty/textures/gui/skull_1.png` đến `skull_5.png` (12x12): cấp 1 xám, 2 vàng, 3 cam, 4 đỏ, 5 tím có sừng. Cấp từ 5 trở lên dùng icon thứ 5. Muốn đổi hình thì thay file cùng tên bằng resource pack (giữ ô vuông 12x12 hoặc cùng tỉ lệ). Dòng 2 hiện `[đầu lâu] Difficulty III`; tên công trình không còn lặp ở dòng này vì đã là dòng 1.

**Tên dimension chỉ hiện khi vào dimension**: vùng nền của cả dimension (region chỉ có `dimensions`, ví dụ "Overworld", hoặc cấp tự động của dimension mod) chỉ hiện làm title khi bạn **mới vào map** hoặc **đổi dimension** (kể cả quay lại Overworld). Sau đó, ở khu vực không thuộc công trình hay region riêng nào, title là **tên biome** đang đứng (lấy từ id biome, ví dụ `dark_forest` thành "Dark Forest") và đổi mỗi khi sang biome khác. Công trình, spawner và region biome vẫn hiện tên riêng của chúng. Áp dụng cho mọi dimension (cả The Nether, The End, dimension mod), không riêng Overworld.

**Ra khỏi công trình về biome luôn hiện title**: bất kể cấp thấp hay cao, cùng cấp hay khác cấp, bất kể vừa hiện biome đó chưa (không bị `repeat_cooldown_seconds` chặn). Đi từ biome này sang biome khác (không có công trình) vẫn chỉ hiện khi cấp/độ khó đổi.

## Nạp config
- Khi vào world hoặc bật server, mod luôn đọc lại `config/hardcore_region_difficulty.json`.
- Khi đang chơi, lưu file config thì mod tự đọc lại trong khoảng 5 giây (không cần `/hrd reload`; lệnh này vẫn dùng được để nạp ngay).
- File config đã có sẵn **không** tự nhận giá trị mặc định mới của bản mod mới. Muốn lấy mặc định mới thì xóa file để mod tạo lại.
- Mob đã spawn trước khi đổi config giữ chỉ số cũ, chỉ mob spawn sau mới theo config mới.

## Smart-auto
Smart-auto **luôn bật**: tự gán độ khó khi không có region thủ công khớp (khóa `smart_auto_enabled` cũ trong config được bỏ qua, có hay không đều như nhau). Tắt từng phần bằng `analyze_structures`, `analyze_biomes`, `analyze_spawners`, `analyze_dimensions`.

**Công trình** (cấp 0-5, khớp bảng region mặc định):

| Cấp | Công trình vanilla |
|---|---|
| 5 | Ancient City, Bastion Remnant, End City |
| 4 | Fortress, Stronghold, Ocean Monument, Woodland Mansion |
| 3 | Desert/Jungle Pyramid, Pillager Outpost, Mineshaft |
| bỏ qua | village, ruined portal, shipwreck, ocean ruin, igloo, swamp hut, buried treasure, nether fossil, trail ruins |

Công trình mod được đoán theo **từ nguyên** trong id (`betterdungeons:skeleton_dungeon` -> `dungeon`): boss/lair/arena = 5, castle/fortress/mansion... = 4, dungeon/crypt/tomb/temple/ruins/tower... = 3, tên quái (zombie, skeleton...) = 2. Từ nguy hiểm luôn thắng từ an toàn (`haunted_house` = 3, `village` = bỏ qua). Không còn khớp chuỗi con (`dwelling` không còn bị coi là `well`).

**Biome**: bảng chính xác cho 64 biome vanilla, từ nguyên cho biome mod, rồi tag `#minecraft:is_nether/is_end/is_ocean/...`. Biome không rõ = an toàn (trước đây mọi biome lạ đều bị gán cấp 1).

**Spawner (lồng spawn)**: đếm số Monster Spawner (`minecraft:spawner`) trong bán kính `spawner_scan_radius` (mặc định 48, tối đa 128) quanh vị trí; càng nhiều lồng, cấp càng cao. `spawner_level_thresholds` là bảng *cấp -> số spawner tối thiểu*, lấy cấp cao nhất đạt được:

| Số spawner | Cấp | Tên vùng |
|---|---|---|
| 1-2 | 2 | Spawner Nest II |
| 3-4 | 3 | Spawner Nest III |
| 5-7 | 4 | Spawner Nest IV |
| 8+ | 5 | Spawner Nest V |

Khi đang đứng trong một công trình (dungeon vanilla hoặc mod), tên vùng là **tên công trình** chứ không phải "Spawner Nest"; cấp vẫn tính theo số lồng. "Spawner Nest" chỉ hiện ở nơi không thuộc công trình nào (lồng đặt tay, công trình đã được xếp là an toàn như làng).

Tắt bằng `"analyze_spawners": false`. Chỉ đếm spawner trong chunk **đang được tải** (không bao giờ ép tải chunk); vị trí spawner được cache theo chunk, tự đếm lại sau 60 giây, khi đặt/phá spawner hoặc chunk bị gỡ.

**Kết hợp**: lấy cấp cao nhất giữa công trình, spawner và biome; bằng nhau thì ưu tiên công trình (tên cụ thể hơn), rồi spawner, rồi biome.

Tinh chỉnh (mọi khóa đều tùy chọn):

```json
"smart_auto_settings": {
  "analyze_structures": true,
  "analyze_biomes": true,
  "unknown_structure_level": 0,
  "structure_spawn_level": 3,
  "analyze_spawners": true,
  "spawner_scan_radius": 48,
  "spawner_level_thresholds": { "2": 1, "3": 3, "4": 5, "5": 8 },
  "structure_levels": { "minecraft:shipwreck": 2, "~^dungeons_arise:": 4, "#minecraft:village": 0 },
  "biome_levels":     { "minecraft:desert": 2, "~_ocean$": 1 },
  "analyze_dimensions": true,
  "unknown_dimension_level": 2,
  "dimension_levels": { "twilightforest:twilight_forest": 3, "~^mymod:": 0 }
}
```
Khóa dùng cùng cú pháp pattern như region (id, `#tag`, `~regex`), khóa khớp đầu tiên thắng, cấp `0` = bỏ qua. `unknown_structure_level` là cấp cho công trình không phân loại được (0 = bỏ qua).

**Tag datapack (sửa từng công trình mà không đụng config)**: mod đi kèm các tag công trình trong `data/hardcore_region_difficulty/tags/worldgen/structure/`, mặc định rỗng. Công trình nằm trong tag nào thì được xếp theo tag đó, **trước** bảng vanilla và việc đoán theo tên (chỉ `structure_levels` trong config thắng được tag):

| Tag | Tác dụng |
|---|---|
| `ignore` | Cấp 0, không tính là nguy hiểm. Dùng để sửa công trình bị đoán nhầm theo tên. Thắng các tag khác. |
| `level_1` ... `level_5` | Gán đúng cấp đó (nằm trong nhiều tag thì lấy cấp cao nhất). Cấp 5 là loại Boss. |
| `bosses` | Công trình boss; nếu không có tag `level_N` thì là cấp 5. |

Ví dụ một datapack (hoặc modpack) sửa hai công trình: tạo `data/hardcore_region_difficulty/tags/worldgen/structure/ignore.json` với nội dung `{ "replace": false, "values": [ "mymod:old_tower" ] }`, và `level_4.json` với `{ "replace": false, "values": [ "mymod:crypt_big", "#mymod:my_dungeons" ] }`. Thay đổi có hiệu lực sau `/reload` (cache được xóa khi tag đổi). Trong file scan, công trình này có `source: "tag"`.

**Công trình mod không nhận ra được theo tên**: nếu công trình tự khai báo danh sách quái thù địch (nhiều dungeon mod làm vậy) thì được gán cấp `structure_spawn_level` (mặc định 3, `0` = tắt). Công trình đăng ký trong tag `#minecraft:village` được coi là an toàn.

### Quét tự động và lệnh `/hrd scan` (OP)
**Tự động:** mỗi khi server khởi động (server dedicated bật lên, hoặc khi vào world singleplayer) và sau `/hrd reload`, mod tự quét, không cần gõ lệnh. Công trình/biome là datapack registry nên chỉ đọc được từ lúc server bắt đầu chạy, đó là thời điểm sớm nhất có thể. Kết quả ghi vào log (`Auto scan: ...`) và file báo cáo. Lệnh `/hrd scan` chỉ còn để quét lại bằng tay.

Quét **toàn bộ** công trình, biome, dimension đang có trong world (vanilla + mọi mod) và ghi `config/hardcore_region_difficulty_scan.json`: cấp từng cái, nguồn của cấp (`override` / `tag` / `table` / `words` / `spawns` / `unknown`...), danh sách công trình chưa nhận ra kèm đoạn JSON sẵn để dán vào `structure_levels`. Trong chat hiện số lượng và mod nào có nhiều công trình chưa nhận ra nhất; khi gõ bởi người chơi còn hiện **số spawner quanh bạn và cấp do spawner quyết định** (spawner có phải là yếu tố cao nhất tại chỗ đó hay không). Không cần biết trước pack có mod gì.

**Dimension của mod (tự chấm cấp)**: mọi dimension (vanilla + mod) được chấm cấp 0-5 tự động và dùng làm **mức tối thiểu** cho cả dimension (công trình, spawner, biome cao hơn thì vẫn thắng). Nguồn của cấp (ghi trong file báo cáo `hardcore_region_difficulty_scan.json`, mục `dimensions`; `/hrd scan` hiện số dimension đã chấm):
- `override`: khóa `dimension_levels` (id chính xác hoặc `~regex`, khớp đầu tiên thắng, `0` = không có cấp tự động);
- `table`: dimension vanilla (Overworld 0, Nether 2, End 3);
- `words`: đoán theo từ trong id (`abyss`, `hell`, `nether`, `void`, `dark`...);
- `biomes`: cấp trung bình của các biome dimension có thể sinh (cùng cách chấm biome của smart-auto);
- `type`: cờ của dimension type (`ultra_warm` = giống Nether, không có sky light = giống hang/void);
- `unknown_level`: `unknown_dimension_level` (mặc định 2, `0` = tắt) là mức sàn cho dimension mod mà không tín hiệu nào chấm được.

Lấy cấp cao nhất trong các tín hiệu `words` / `biomes` / `type`, rồi không thấp hơn `unknown_dimension_level`. Muốn chỉnh một dimension: thêm vào `dimension_levels`, hoặc viết một region chỉ có `dimensions` (ví dụ `["twilightforest:twilight_forest"]`, `difficulty_level: 3`): region thủ công này **thay thế hoàn toàn** cấp tự động của dimension đó. Tắt cả tính năng bằng `"analyze_dimensions": false`. Config cũ thiếu các khóa này vẫn chạy với mặc định.

## Liên kết Deeper and Darker + Cataclysm (tùy chọn)

```json
"mod_compat_settings": { "deeperdarker_cataclysm": false }
```

Mặc định tắt, mod hardcore không cần 2 mod kia. Đặt `true` thì **bắt buộc** cài cả `deeperdarker` và `cataclysm` (thiếu là game dừng khi khởi động, kèm tên mod còn thiếu).

Khi bật, đồ Cataclysm nâng cấp từ đồ Warden thay vì netherite (công thức netherite gốc bị gỡ):

| Món Cataclysm | Nguyên liệu nâng cấp |
|---|---|
| Ignitium mũ/giáp/quần/giày | Món Warden tương ứng + Ignitium Ingot + Ignitium Upgrade Template |
| Cursium mũ/giáp/quần/giày | Món Warden tương ứng + Cursium Ingot + Cursium Upgrade Template |
| Monstrous Helm | Mũ Warden + Monstrous Horn + Netherite Upgrade Template |
| The Incinerator | Kiếm Warden (thay kiếm netherite) + 3 Ignitium Ingot + 3 Blaze Rod, xếp như công thức cũ |

Lưu ý: bản thân đồ Warden vẫn được Deeper and Darker làm từ đồ netherite, nên chuỗi là netherite → Warden → Cataclysm. Đổi giá trị khi đang chơi cần `/reload`.

## Cấu trúc code
Xem [ARCHITECTURE.md](ARCHITECTURE.md). Thay đổi so với bản cũ: [CHANGELOG.md](CHANGELOG.md).

## Tương thích
Minecraft 1.20.1, Forge 47.4.20+, Official (Mojang) mappings.

## Giấy phép
All Rights Reserved. Tác giả: Bacontete
