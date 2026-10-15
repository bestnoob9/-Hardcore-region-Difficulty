# Ra khỏi công trình về biome luôn hiện title
- Trước đây ra khỏi công trình về biome chỉ hiện khi cấp/độ khó khác nhau, nên công trình cấp 1 về biome cấp 1 (hoặc cùng cấp) không hiện gì. Giờ **mọi lần ra khỏi công trình về biome đều hiện**, bất kể cấp thấp hay cao.
- `PlayerRegionTracker.shouldAnnounce`: thêm nhánh `oldInStructure && !newInStructure` -> luôn báo.
- `TitleDisplayHandler.displayTitle`: client nhớ công trình của packet trước (`lastPacketStructure`); khi packet mới không có công trình thì bỏ qua `repeat_cooldown_seconds`, tránh biome vừa hiện gần đây bị cooldown nuốt. Không đổi giao thức mạng.
- Biome -> biome vẫn chỉ hiện khi cấp/độ khó đổi; vào công trình vẫn luôn hiện như cũ.

# Công tắc Deeper and Darker + Cataclysm trong Cloth Config
- Mục **Tương thích mod** trong màn hình config có nút bật/tắt `mod_compat_settings.deeperdarker_cataclysm`. Nút ghi thẳng vào `hardcore_region_difficulty.json` (giữ nguyên phần còn lại của file) rồi load lại config.
- Không cho bật khi thiếu Deeper and Darker hoặc Cataclysm (tránh game dừng với thông báo thiếu mod ở lần mở sau).

# Công tắc tắt toàn bộ đánh giá độ khó
- Thêm `enabled` trong `hardcore_region_difficulty-common.toml` (mặc định bật), cũng có trong màn hình Cloth Config (mục Chung). Tắt: không tăng chỉ số mob, không title vùng/boss, không loot / sách enchant / thưởng phẩm chất theo độ khó (mọi nơi coi như cấp 1).
- Cảnh báo ảnh hưởng map cũ ở cả công tắc độ khó và công tắc phẩm chất (tooltip, dòng chữ đỏ trong màn hình config, comment trong file toml).

# Cloth Config + công tắc Quality / Title
- Thêm màn hình cài đặt trong game bằng **Cloth Config API** (Forge 1.20.1, bản 11.x): vào Mods -> Hardcore Region Difficulty -> Config. Cloth Config là TÙY CHỌN: không cài thì mod vẫn chạy bình thường, chỉ không có nút Config.
- Công tắc **bật/tắt phẩm chất trang bị** `general.enabled` (file `hardcore_region_difficulty-quality.toml`, mặc định bật). Tắt: không roll phẩm chất mới (rương, túi đồ, villager), phẩm chất cũ không còn tác dụng và không hiện tooltip, Lò Rèn Phẩm Chất báo đang bị tắt. Đồ vẫn giữ tag phẩm chất nên bật lại là có lại.
- Công tắc **hiện title độ khó** `title.show_region_title` và **title cảnh báo boss** `title.show_boss_title` (file mới `hardcore_region_difficulty-client.toml`, mặc định bật). Khóa `title_display_settings.enabled` trong JSON cũ vẫn có tác dụng: title chỉ hiện khi cả hai cùng bật.
- Màn hình Cloth Config còn chỉnh nhanh: roll trong rương / túi đồ, thưởng phẩm chất mỗi cấp độ khó, giá XP và nguyên liệu của Lò Rèn.
- build.gradle: thêm maven.shedaniel.me + `cloth_config_version` trong gradle.properties; mods.toml: dependency tùy chọn `cloth_config` (side CLIENT).

## Liên kết Deeper and Darker + Cataclysm (tùy chọn)
- Config mới `mod_compat_settings.deeperdarker_cataclysm` (mặc định `false`, config cũ không có khóa này vẫn chạy bình thường).
- Bật lên thì đồ Cataclysm nâng cấp từ **đồ Warden của Deeper and Darker** thay vì đồ netherite: Ignitium (4 món giáp), Cursium (4 món giáp), Monstrous Helm (từ mũ Warden), The Incinerator (kiếm Warden thay kiếm netherite).
- Công thức netherite gốc của Cataclysm cho các món trên bị gỡ khỏi RecipeManager khi bật; công thức mới nằm ở `data/hardcore_region_difficulty/recipes/compat/` và dùng điều kiện công thức `hardcore_region_difficulty:deeperdarker_cataclysm`.
- Chỉ khi bật mới cần cả 2 mod `deeperdarker` và `cataclysm`: thiếu một mod thì game dừng lúc khởi động kèm thông báo rõ ràng. Tắt thì mod hardcore chạy độc lập như cũ.
- Đổi giá trị trong lúc chơi cần `/reload` để công thức cập nhật; thiếu mod chỉ được kiểm tra lúc khởi động game.

## Sát Thương Chuẩn bỏ qua cả enchant bảo vệ
- Ngoài giáp và hiệu ứng Kháng, Sát Thương Chuẩn giờ bỏ qua cả enchant bảo vệ trên giáp (theo đúng tỉ lệ bỏ qua; cấp I = 100%).

## Cấp enchant thấp ra nhiều hơn cấp cao (thủ thư)
- Sách Advanced của thủ thư cũng roll cấp theo `advanced_loot_level_decay` (0.6), trước đây roll đều mọi cấp (chỉ có tác dụng khi `librarian_only_level_1 = false`).

## Loot enchant có trần tỉ lệ + Sát Thương Chuẩn còn 1 cấp
- Sát Thương Chuẩn (`true_damage`) chỉ còn **cấp I** (bỏ qua 100% giáp). Đồ cũ cấp II/III vẫn hoạt động như cấp I.
- Sát Thương Chuẩn chỉ ra dưới dạng sách trong rương ở vùng/công trình **cấp 5 trở lên**, tỉ lệ **1%**. Không còn random vào đồ enchant Advanced.
- Enchant nâng cao (đồ trong rương, sách dungeon, sách công trình): trần **5%**. Enchant thường (Xuyên Giáp, Đánh Nhanh): trần **6%**. Cấp vùng càng cao tỉ lệ càng gần trần.
- `advanced_chance_multiplier` giờ chỉ còn nhân vào sách dungeon (rồi bị cắt ở trần 5%).

## Bàn Rèn Phẩm Chất tốn XP (chưa cân bằng)
- Thêm điều kiện XP để rèn lại phẩm chất: mỗi lần rèn trừ một số CẤP kinh nghiệm của người chơi (giống đe). Config `forge_table.xp_cost` (mặc định 3, 0 = tắt, tối đa 100). Giá trị mặc định chưa được cân bằng.
- Không đủ cấp thì vẫn thấy kết quả nhưng không lấy được (cả click lẫn shift-click). Creative không tốn XP. Giá XP hiện cạnh ô kết quả (xanh = đủ, đỏ = thiếu).
- `QualityForgeMenu`: thêm `xpCostData` (DataSlot đồng bộ server -> client), `mayPickup` / trừ cấp trong `onTake`. Thêm lang `hardcore_region_difficulty.forge.xp_cost` (en_us / vi_vn).

## Sửa % giáp / chống chịu của phẩm chất chỉ tính cho riêng món giáp đó
- Trước đây % giáp và % chống chịu dùng `MULTIPLY_BASE` trên thuộc tính ARMOR / ARMOR_TOUGHNESS của người chơi, nên bị nhân lên tổng giáp của CẢ BỘ (mọi món đang mặc) thay vì chỉ món có phẩm chất. Giờ % được đổi thành số cộng thẳng (`ADDITION`) = giáp gốc của chính món đó x %, và chống chịu gốc của món đó x %. Món khác trong bộ không bị ảnh hưởng.
- Giáp cộng thêm (số cộng thẳng) giữ nguyên.

## Cân bằng lại QualityType + dọn lệnh /hrd quality
- Giới hạn chung cho MỌI loại (Weapon, Tool, Armor, Ranged, Shield): các chỉ số % (sát thương, tốc độ, độ bền, đào, tốc độ chạy, đào khi cầm khiên) trong khoảng -20% .. +20%; chỉ số cộng thẳng (reach, giáp cộng thêm) trong khoảng -0.5 .. +1.5. Mọi giá trị cũ vượt giới hạn đã được hạ xuống và xếp lại theo bậc (tên và tier của các phẩm chất không đổi nên đồ cũ vẫn dùng được).
- `/hrd quality show [category]`: chỉ còn tiếng Anh (tên loại + tên phẩm chất + tier), không còn dùng chuỗi dịch nên client tiếng Việt cũng thấy tiếng Anh.
- `/hrd quality set [category] <quality>`: bỏ phần gợi ý (tab-complete). Bỏ hết tên tiếng Việt (vukhi, congcu, giap, cung, khien...). Category chỉ nhận: weapon, tool, armor, ranged, shield.
- Sửa lỗi: `/hrd quality set <quality>` không gõ category trước đây luôn dò enum của Tool trước nên báo sai khi cầm vũ khí / giáp / cung / khiên; giờ dò theo loại của món đang cầm.

## Loot Advanced khó hơn (cấp 3 trở lên)
- Đồ enchant Advanced trong rương, sách dungeon (`dungeon_loot_tables`), sách rương công trình và sách Armor Piercing chỉ ra ở vùng cấp >= `advanced_min_level` (mặc định 3). Dưới cấp 3 không ra gì.
- `advanced_chance_multiplier` (0.7) nhân vào mọi tỉ lệ loot Advanced; `advanced_loot_level_decay` (0.6) làm cấp enchant cao khó ra hơn (trước đây đồ enchant và sách `dungeon_loot_tables` roll đều mọi cấp). Thủ thư giữ nguyên.
- Loot modifier tính cấp vùng một lần cho cả rương thay vì mỗi món.

## Lệnh /hrd quality nhận loại đồ
- `/hrd quality show [loại đồ]`: không có tham số thì liệt kê mọi loại, có thì chỉ liệt kê loại đó.
- `/hrd quality set [loại đồ] <phẩm chất>`: có loại đồ thì gợi ý chỉ phẩm chất của loại đó và báo lỗi nếu món đang cầm không thuộc loại đó.
- Loại đồ: weapon / vukhi, tool / congcu, armor / giap, ranged / cung (cung nỏ), shield / khien; gõ "vũ khí", "công cụ", "cung & nỏ" trong dấu nháy cũng được.

## Chia phẩm chất (QualityType) theo từng loại đồ
- Thêm `QualityCategory`: WEAPON (kiếm, đồ mod có sát thương), TOOL (cúp, xẻng, cuốc, rìu), ARMOR (giáp), RANGED (cung, nỏ), SHIELD (khiên). Mỗi loại chỉ roll / nhận các phẩm chất hợp với nó (vd giáp: Hư hại, Mài mòn, Rỉ sét...; công cụ: Sụp đổ, Hao mòn...; vũ khí: Sứt mẻ, Rỉ sét...; cung/nỏ: Hao mòn, Hư hại...).
- `ItemQuality.roll` roll theo pool của loại đồ (vẫn dùng chung các `weight_*` trong config). `ItemQuality.get` trả STANDARD nếu tag phẩm chất không hợp loại đồ (đồ cũ trong world có phẩm chất không còn hợp loại sẽ hiện Bình Thường).
- Thợ rèn / villager chỉ trade phẩm chất hợp loại đồ; `/hrd quality show` liệt kê theo loại, `/hrd quality set` chỉ gợi ý và chấp nhận phẩm chất hợp với món đang cầm.

## Sửa độ bền phẩm chất không áp dụng cho giáp đang mặc
- `QualityEvents` chỉ quét tay và túi đồ nên giáp ở ô giáp không được tăng / giảm độ bền theo phẩm chất dù tooltip có hiện. Giờ quét cả 4 ô giáp mỗi tick (chỉ giáp đã có phẩm chất; không roll phẩm chất mới cho giáp đang mặc).

## Sửa lỗi model Bàn Rèn không hiện (missing model)
- Blockstate `quality_forge.json` dùng biến thể `facing=north/east/south/west` nhưng class `QualityForgeBlock` thiếu thuộc tính `facing`, nên Minecraft báo thiếu variant và hiện model tím-đen. Đã thêm `FACING`, `getStateForPlacement`, `rotate`, `mirror` và `noOcclusion()` vào `QualityForgeBlock`.
- `QualityForgeScreen` vẽ chữ tiêu đề / "Túi đồ" màu sáng để đọc được trên GUI tối.

## Phẩm chất: độ bền hai chiều, tốc độ giương cung / lắp nỏ
- Độ bền theo phẩm chất giờ áp cho MỌI đồ có phẩm chất (kiếm, rìu, cúp, xẻng, cuốc, cung, nỏ, đồ mod): phẩm chất 4 / 5 tăng độ bền tối đa +15% / +30%, phẩm chất 2 / 1 giảm -15% / -30% (`durability_step`). Phẩm chất cao được hoàn lại một phần độ bền bị trừ; vẫn đúng sau khi sửa chữa.
- Cung: tốc độ giương -/+10% mỗi bậc (`draw_step`). Nỏ: tốc độ lắp tên -/+10% mỗi bậc. Cộng dồn với Quick Charge. Chạy cả client lẫn server nên animation khớp.
- Sát thương mũi tên của cung / nỏ: -/+10% mỗi bậc (giữ nguyên `damage_step`).
- Cúp / xẻng / cuốc: tốc độ đào + độ bền. Rìu: tốc độ đào, sát thương, tốc độ đánh + độ bền. Kiếm: sát thương, tốc độ đánh + độ bền.
- Tooltip hiện thêm từng dòng hiệu ứng (xanh = tốt hơn thường, đỏ = tệ hơn thường). Thêm lang en_us / vi_vn.

## Phẩm chất vũ khí / công cụ và Bàn Rèn Phẩm Chất
- Vũ khí và công cụ (đồ không xếp chồng) có phẩm chất 1-5: 1 Tồi, 2 Kém, 3 Thường, 4 Tốt, 5 Xuất sắc. Không có tag = 3. Hiện trong tooltip dạng sao.
- Cung và nỏ cũng có phẩm chất: sát thương mũi tên -/+10% mỗi bậc (cùng `damage_step`), giảm độ bền như các đồ khác.
- Vũ khí: sát thương -/+10% mỗi bậc và tốc độ đánh -/+8% mỗi bậc so với 3. Công cụ: tốc độ đào -/+10% mỗi bậc. Chỉnh trong `config/hardcore_region_difficulty-quality.toml`.
- Loot: vũ khí/công cụ trong rương nhận phẩm chất ngẫu nhiên (tỉ lệ mặc định 10/25/40/20/5). Mọi vũ khí/công cụ chưa có phẩm chất mà nằm trong túi đồ người chơi (chế tạo kể cả shift-click, lấy từ menu sáng tạo, nhặt...) được roll phẩm chất một lần rồi giữ tag (tắt bằng `roll_in_inventory`).
- Bàn Rèn Phẩm Chất (block mới, chế tạo từ iron ingot, anvil, smooth stone): đặt đồ + nguyên liệu chế tạo ra nó để rèn lại phẩm chất ngẫu nhiên, tốn 1 nguyên liệu mỗi lần (`material_cost`). Nguyên liệu hợp lệ: mục `material_overrides` trong config, vật liệu sửa chữa của đồ, hoặc bất kỳ nguyên liệu nào (trừ tay cầm, que) trong công thức chế tạo; đồ mod chế từ nhiều loại nhận từng loại. Giao diện dùng texture `textures/gui/quality_forge.png` (chỉnh từ GUI smithing table, 2 ô vào + 1 ô ra), chỉ có tên bàn và "Túi đồ". Phẩm chất mới được giấu: ô output chỉ hiện đồ không có sao, phẩm chất lộ ra khi bạn lấy đồ ra (kể cả shift-click). Kết quả cố định cho tới khi rèn xong (không reroll miễn phí bằng cách mở lại bàn).
- Phẩm chất 2 / 1 giảm độ bền tối đa 15% / 30% (`durability_step`): đồ hao mòn nhanh hơn tương ứng, vẫn đúng sau khi sửa chữa.

## Nhận boss chỉ cho mob thù địch
- Nhận boss/mini boss theo máu yêu cầu mob là thù địch (`Enemy`) và có sát thương gốc > 0. Mob hiền (heo, bò...) không bao giờ bị coi là boss dù mod gán sát thương hoặc nhiều máu.
- `exclude_entities` trong config giờ cũng loại mob khỏi nhận boss và title boss.
- Mob không phải `Enemy` nhưng là boss thật: gắn tag `hardcore_region_difficulty:boss` hoặc `mini_boss`.
- Log debug "Boss detected: ..." in id mob, máu gốc, sát thương gốc, có phải `Enemy` không và có tag boss không.

## Đánh giá boss thêm điều kiện sát thương
- Nhận boss/mini boss theo máu gốc giờ còn yêu cầu mob có sát thương tấn công gốc > 0 (thuộc tính `ATTACK_DAMAGE`). Mob nhiều máu nhưng không tấn công người không còn bị gọi là boss, không nhận bonus boss và không hiện title boss.
- Tag datapack `hardcore_region_difficulty:mini_boss` / `boss` và `forge:bosses` vẫn coi là boss bất kể sát thương (để ép boss tấn công bằng phép/đặc biệt mà không có thuộc tính này).

## Armor Piercing / True Damage trên cung và nỏ
- Cả hai enchant ghép được vào cung và nỏ (đe, bàn enchant với Armor Piercing). Mũi tên bắn ra mang cấp của cung và gây sát thương xuyên giáp / chuẩn như đòn cận chiến Dùng được cho cả nỏ.

## Enchant mới: True Damage (Sát Thương Chuẩn)
- 3 cấp: cận chiến bỏ qua 40% / 70% / 100% giáp, độ bền giáp VÀ hiệu ứng Đề kháng của mục tiêu. Enchant Protection vẫn tính. Loại trừ với Armor Piercing.
- VERY_RARE, treasure only, chỉ từ loot và thủ thư; rương mod chèn với tỉ lệ giữ lại 1/4 (khoảng 2%).
- Armor Piercing và True Damage dùng chung một handler tính lại sát thương (LivingHurtEvent/LivingDamageEvent).

## Enchant mới: Armor Piercing (Xuyên Giáp)
- 3 cấp: cận chiến bỏ qua 10% / 20% / 30% giáp và độ bền giáp của mục tiêu. Chỉ tính đòn đánh trực tiếp (không tính tên, trident ném).
- UNCOMMON: enchant được ở bàn enchant (chi phí cao 15 / 25 / 35), có trong loot vanilla, thủ thư, và mỗi rương sinh loot có 6% chứa thêm 1 sách Armor Piercing (cấp I 57%, II 29%, III 14%). Không còn nằm trong danh sách chèn chung của mod. Dùng cho kiếm và rìu. Loại trừ với True Damage.

## Enchant mới: Swift Strike (Đánh Nhanh)
- Tăng tốc độ đánh: +0.1 mỗi cấp (I 0.1, II 0.2, III 0.3, IV 0.4, V 0.5) khi cầm ở tay chính; hiện trong tooltip. Chỉ enchant được đồ có tốc độ đánh gốc <= 1.1 (rìu, xẻng, trident, vũ khí chậm của mod); kiếm (1.6) không enchant được. Đe cũng bị chặn (kể cả creative, nơi vanilla bỏ qua `canEnchant`): sách/đồ có Swift Strike không ghép được vào đồ quá nhanh.
- Là enchant thường (không phải treasure): có từ bàn enchant (độ hiếm UNCOMMON, cần năng lượng 5 tới 29 cho cấp I-V), loot vanilla (`enchant_randomly`, `enchant_with_levels`), thủ thư bán sách bình thường. Cũng nằm trong danh sách enchant nâng cao mà mod tự chèn vào rương/thủ thư.

## Cấp 6/7 chỉ cho boss
- Cấp vùng luôn nằm trong 1-5 (`LocationMatcher.MAX_REGION_LEVEL`), kể cả khi config đặt region cấp 6/7. Cấp 6/7 chỉ do `BossDetector` gán cho boss/mini boss.
- Config mặc định: stronghold hạ từ cấp 4 xuống 3 (khớp giới hạn Overworld). File config cũ trên đĩa vẫn bị kẹp xuống 3 trong Overworld.

## Giới hạn cấp theo dimension
- `LocationMatcher.match` kẹp mọi cấp vùng theo dimension: Overworld (cấp nền 1) tối đa 3, Nether (2) tối đa 4, End (3) tối đa 5; cấp nền 4-5 giữ nguyên. Áp cho mọi nơi dùng cấp vùng (title, scale mob, loot, villager). Boss/mini boss (cấp 6/7) không bị kẹp.

## Sửa lỗi title boss và boss thiếu máu
- Title boss: server nhớ boss nào đang trong tầm của từng người chơi và gửi title khi boss VÀO tầm (spawn gần hoặc người chơi đi tới). Ra khỏi tầm (bán kính + 8) rồi quay lại thì hiện lại. Bỏ cooldown cứng 30 giây (server) và cooldown 60 giây theo tên (client) của title boss. Nhiều boss cùng lúc: hiện boss cấp cao nhất, gần nhất.
- Boss thiếu máu (ví dụ 250/454): một số boss mod tự đặt máu về mức gốc ngay sau khi join. Trong 200 tick đầu, nếu máu đúng bằng máu gốc trước buff thì được nâng lên máu tối đa mới. Boss đã bị đánh không bị đụng tới.

## Sửa lỗi boss / mini boss
- Cấp 6 (mini boss, máu gốc >= 200 hoặc tag `mini_boss`) và 7 (boss, >= 400 hoặc tag `boss`) là cấp riêng của boss, không còn bị cap theo `getMaxDifficulty` (trước đây không bao giờ vượt 5 nên nhánh boss là code chết).
- Ngưỡng mini boss hạ xuống 200 máu GỐC. Chỉ đọc `getAttributeBaseValue(MAX_HEALTH)` (trước mọi modifier), nên mob thường được buff vùng lên 200+ máu không bị nhầm thành boss.
- Boss = scale theo cấp vùng + CỘNG bonus `mini_boss_*_percent` / `boss_*_percent`.
- Mob tag `forge:bosses` không còn bị loại: được scale, tối thiểu là mini boss. `exclude_entities` vẫn thắng. Ender Dragon / Wither / Warden vẫn không scale.
- `BossProximityWarning` không còn bỏ qua nhầm mob tag `hardcore_region_difficulty:boss`.

# Changelog

## Title có đầu lâu theo cấp

- Kiểu compact: dòng 1 là tên biome / công trình, dòng 2 là **đầu lâu của cấp** rồi đến độ khó (ví dụ `[đầu lâu] Difficulty III`). 5 icon đầu lâu riêng (`textures/gui/skull_1..5.png`, vẽ pixel): cấp 1 xám, 2 vàng, 3 cam, 4 đỏ, 5 tím có sừng; cấp > 5 dùng icon thứ 5. Icon mờ dần cùng chữ.
- Dòng 2 không còn nối ` • tên công trình` nữa (tên đã ở dòng 1).
- Kiểu vanilla: dòng phụ có ký tự ☠ đổi màu theo cấp (title vanilla không vẽ được ảnh).
- Gói tin title thêm `difficultyLevel`; `PROTOCOL_VERSION` của kênh mạng tăng 1 -> 2, nên **client và server phải cùng bản mới** (khác bản sẽ bị từ chối khi vào server thay vì lỗi giải mã).
- Chưa build thử với Forge: cần `gradlew build` và xem icon trong game (kích thước/vị trí icon so với chữ có thể cần chỉnh `SKULL_SIZE`, `SKULL_GAP` trong `RegionTitleOverlay`).

## "Spawner Nest" khi đi tới gần dungeon

- Nguyên nhân: spawner được đếm trong bán kính `spawner_scan_radius` (48 block), nhưng tên công trình chỉ lấy khi người chơi đã đứng trong khung bao của công trình. Đi tới gần thì cấp đã tăng nhưng chưa thuộc công trình nào nên hiện "Spawner Nest".
- Giờ khi spawner quyết định cấp mà vị trí chưa nằm trong công trình nào, tên lấy từ công trình chứa **spawner gần nhất**. Chỉ còn "Spawner Nest" khi các spawner không thuộc công trình nào.
- Sách tự nhận ở rương công trình vẫn đòi chính cái rương nằm trong công trình đó, nên rương ở gần dungeon không bị tính nhầm.
- Chưa build thử với Forge: cần `gradlew build` và đi tới gần một dungeon nhiều spawner.

## Sách Advanced tự nhận rương dungeon của mod khác

- Rương `chests/...` nằm trong một công trình có cấp 3+ (vanilla hoặc mod, không cần liệt kê loot table) có thể thêm 1 sách Advanced: cấp 3 = 4% (sách I-II), cấp 4 = 6% (I-III), cấp 5 = 8% (I-V). Cấu hình mới trong `enchantment_settings`: `dungeon_book_auto` (true), `dungeon_book_by_level` (bảng cấp -> `chance`, `max_level`; có khóa này thì thay hẳn mặc định), `dungeon_book_level_decay` (0.5).
- Cấp sách thấp dễ ra hơn: mỗi cấp cao hơn có xác suất bằng `decay` lần cấp liền dưới (cap 5: 52/26/13/6/3%). Chỉ áp dụng cho sách thưởng tự nhận; sách trong rương liệt kê và đồ enchant thường không đổi.
- Rương thuộc `dungeon_loot_tables` giữ nguyên cách cũ và không bị roll thêm lần nữa. Cấp chỉ do biome hoặc spawner ngoài công trình không tính.
- **Đổi hành vi**: config cũ thiếu các khóa này dùng mặc định, tức là rương dungeon mod bắt đầu có sách. Đặt `dungeon_book_auto: false` để giữ như cũ.
- Chưa build thử với Forge: cần `gradlew build` và mở thử rương trong một dungeon mod.

## Title: tên dimension chỉ hiện khi vào, sau đó là tên biome

- Vùng nền của dimension (region chỉ có `dimensions` như "Overworld", hoặc cấp tự động của dimension) chỉ hiện làm title khi mới vào map hoặc đổi dimension. Ở cùng dimension, khu vực không thuộc công trình/region riêng sẽ hiện tên biome (từ id biome) và đổi theo biome. Công trình, spawner nest, region biome không đổi.
- Áp dụng cho mọi dimension (không riêng Overworld). `LocationMatcher.Match` có thêm `dimensionWide`; `PlayerRegionTracker.RegionInfo` thêm `dimension`, `dimensionWide`, `biomeId` (so sánh theo biome thay vì tên hiển thị cho vùng nền).
- Chưa build thử với Forge: cần `gradlew build`.

## Tag datapack cho cấp công trình

- Mod gói sẵn các tag công trình rỗng `hardcore_region_difficulty:ignore`, `level_1` ... `level_5` và `bosses` (`data/hardcore_region_difficulty/tags/worldgen/structure/`). Công trình trong tag được xếp theo tag (`source: "tag"` trong file scan), đứng trước bảng vanilla và việc đoán theo tên. Chỉ `structure_levels` trong config thắng được tag.
- `ignore` (cấp 0) là cách sửa công trình bị đoán nhầm theo tên mà không cần sửa config hay code; thắng các tag khác. Nhiều tag `level_N`: lấy cấp cao nhất. `bosses` không kèm `level_N` là cấp 5.
- Cache cấp công trình được xóa khi tag đổi (`TagsUpdatedEvent`, ví dụ sau `/reload`).
- Không đổi hành vi nếu các tag để rỗng. Chưa build thử với Forge: cần `gradlew build` và thử thêm một công trình vào `ignore`.

## Dungeon nhiều spawner hiện đúng tên dungeon

- Khi cấp do số spawner quyết định mà vị trí đang nằm trong một công trình, tên vùng và tiêu đề dùng tên công trình (`Dungeon <cấp>`) thay vì "Spawner Nest". Cấp vẫn theo số lồng. Công trình rated SAFE (làng...) không đặt tên; công trình chưa được xếp loại vẫn đặt tên được. Ngoài công trình vẫn là "Spawner Nest".
- `/hrd scan`: cờ "spawner quyết định" so theo cấp thay vì theo tên.

## Tự chấm cấp cho dimension của mod

- Smart-auto giờ chấm cấp từng dimension (vanilla + mod) và dùng làm mức tối thiểu cho cả dimension. Nguồn cấp: `override` (`dimension_levels`), `table` (Overworld 0 / Nether 2 / End 3), `words` (từ trong id), `biomes` (cấp trung bình các biome dimension sinh ra), `type` (ultra_warm / không sky light), `unknown_level`.
- Khóa mới trong `smart_auto_settings`: `analyze_dimensions` (true), `unknown_dimension_level` (2, mức sàn cho dimension mod; 0 = tắt), `dimension_levels` (`{}`, id hoặc `~regex` -> cấp, 0 = bỏ qua). Config cũ thiếu khóa vẫn chạy với mặc định.
- **Đổi hành vi**: dimension mod trước đây không có region thủ công là cấp 1, nay tối thiểu cấp 2 (đặt `unknown_dimension_level: 0` hoặc `dimension_levels` để giữ như cũ). Region thủ công chỉ có `dimensions` vẫn thắng và thay thế cấp tự động.
- Tên vùng hiện là tên dimension, tiêu đề `Dimension II..V`.
- File báo cáo scan: mục `dimensions` đổi từ danh sách id thành object `id -> { level, source, manual_region? }`; `summary` thêm `dangerous_dimensions`. Log và chat của `/hrd scan` hiện số dimension đã chấm cấp.
- Chưa build thử với Forge trong môi trường làm việc (chỉ kiểm tra biên dịch phần class mới bằng stub): cần `gradlew build` và thử một dimension mod thật.

## Title to hơn

- `title_display_settings.scale` mặc định đổi 1.0 -> 1.8 (tên vùng vẽ ở 1.25x giá trị này, dòng phụ 0.9x). Config đã sinh sẵn trong `config/` giữ giá trị cũ: sửa `scale` tay hoặc xóa file để lấy mặc định mới.

## Smart-auto luôn bật

- `smart_auto_enabled` không còn được đọc: smart-auto luôn chạy (trước đây mặc định tắt). Config cũ có khóa này (kể cả `false`) vẫn nạp bình thường nhưng khóa bị bỏ qua. Muốn tắt từng phần dùng `analyze_structures` / `analyze_biomes` / `analyze_spawners`.

## Sửa crash khi khởi động (NullPointerException ở SmartAutoSettings)

- `SmartAutoSettings.DEFAULT` được khởi tạo trước `DEFAULT_SPAWNER_THRESHOLDS` nên `spawnerLevelThresholds` là `null` ở `common_setup` -> game văng. Đã đưa `DEFAULT` xuống sau các hằng số và thêm kiểm tra `null` trong constructor.

## Giới hạn sách Advanced của thủ thư

- Thủ thư chỉ bán sách Advanced **cấp I**, bất kể độ khó vùng dân làng đang đứng (`librarian_only_level_1`, mặc định `true`).
- Giá tối thiểu **40 emerald**: giá = max(`librarian_min_emerald_price`, 20 + 10 x cấp), tối đa 64 (một stack).
- Mỗi offer chỉ mua được tối đa **8 lần** (`librarian_max_uses`, mặc định 8); không còn dùng `maxUses` của offer gốc. `uses` bắt đầu từ 0; item thứ 2, xp, `priceMultiplier` giữ nguyên từ offer gốc.
- 3 khóa mới trong `enchantment_settings`; config cũ thiếu khóa vẫn chạy với mặc định. Rương và `max_level_per_difficulty` không bị ảnh hưởng (chỉ thủ thư bị giới hạn cấp).

## Độ khó theo số lồng spawn (Monster Spawner)

- Smart-auto đếm số `minecraft:spawner` trong bán kính `spawner_scan_radius` (mặc định 48) quanh vị trí. Càng nhiều lồng, cấp càng cao: 1-2 = cấp 2, 3-4 = cấp 3, 5-7 = cấp 4, 8+ = cấp 5 ("Spawner Nest II..V").
- Cấp cuối cùng là cấp cao nhất giữa công trình, spawner và biome (bằng nhau: công trình > spawner > biome).
- Khóa mới trong `smart_auto_settings`: `analyze_spawners` (true), `spawner_scan_radius` (48), `spawner_level_thresholds` (`{"2":1,"3":3,"4":5,"5":8}`, cấp -> số spawner tối thiểu). Config cũ không có các khóa này vẫn chạy với mặc định; file config đã sinh sẵn không tự thêm khóa.
- Đếm bằng block entity của chunk đang tải (không quét từng block, không ép tải chunk), cache vị trí theo chunk trong `DangerCache`, hết hạn sau 60 giây và bị xóa khi đặt/phá spawner hoặc chunk bị gỡ.
- `/hrd scan` (khi gõ bởi người chơi) hiện số spawner quanh bạn, cấp do spawner và spawner có phải yếu tố quyết định không; file báo cáo thêm mục `spawner_analysis`.

## Title gọn hơn, tự nạp config

- Title đổi vùng mặc định là chữ nhỏ phía trên màn hình (`style: "compact"`, chỉnh `scale`, `y_offset`); `style: "vanilla"` để quay lại title to giữa màn hình. Thời gian hiện mặc định 70 -> 40 tick.
- Không báo lại cùng một vùng trong `repeat_cooldown_seconds` (mặc định 60 giây).
- Config tự nạp lại: mỗi khi vào world/bật server và trong lúc chơi khi file được lưu (kiểm tra mỗi 5 giây). `/hrd reload` vẫn còn.

## Scale mob mạnh hơn

- Mặc định mỗi cấp độ khó: máu +25%, sát thương +20%, giáp +20% và +1 giáp cộng thẳng (mob giáp gốc 0 mới có tác dụng), tốc độ +2% (giữ thấp). Cấp 5 so với cấp 1: máu +144%, sát thương +107%.
- Config đã sinh sẵn trong `config/` không tự đổi: sửa tay hoặc xóa file để tạo lại.

## Rìu, scale mob theo %, scan tự động

- Advanced Sharpness dùng được trên **rìu** (đe, sách, rương, thủ thư), giống Sharpness vanilla.
- Scale mob: mỗi cấp độ khó mạnh hơn cấp trước X% (cộng dồn). Khóa mới `health/damage/armor/speed_percent_per_level` (mặc định 4 / 4 / 3 / 2). Sát thương đổi từ cộng cứng +1/cấp sang %. Khóa cũ `*_multiplier` bị bỏ qua.
- Scan công trình/biome/dimension chạy **tự động** khi server khởi động (`ServerStartingEvent`) và sau `/hrd reload`; kết quả vào log + file báo cáo, đồng thời nạp sẵn cache xếp hạng công trình. `/hrd scan` giữ lại để quét tay.

## Enchant Advanced xuất hiện ở thủ thư và rương dungeon (hiếm)

- Thủ thư: tỉ lệ đổi sách enchant thành sách Advanced giờ là `librarian_trade_chance` (mặc định 8%), không còn phụ thuộc tỉ lệ độ khó 1%. Giá cao hơn sách thường (20 emerald + 10 mỗi cấp, tối đa 64).
- Rương dungeon: thêm `dungeon_chest_chance` (mặc định 8%) và `dungeon_loot_tables` (mặc định `minecraft:chests/simple_dungeon`). Rương trúng sẽ nhận thêm 1 sách enchant Advanced. Trước đây rương dungeon gần như không bao giờ ra vì enchant chỉ gắn lên đồ enchant được.
- Config cũ không có 3 khóa mới vẫn chạy bình thường (dùng mặc định).

## Nâng cấp phân tích công trình / độ khó (smart-auto)

### Công trình
- `StructureClassifier` trả về cấp 0-5 (`Rating`) thay vì chỉ 2 mức cố định (dungeon 3 / boss 5). Cấp vanilla khớp file config mặc định (fortress, stronghold, monument, mansion = 4; bastion = 5...).
- Bastion Remnant, End City, Pillager Outpost trước đây bị bỏ qua -> nay được tính.
- Khớp theo **từ nguyên** thay vì chuỗi con: hết nhầm `dwelling`/`broadleaf`/`unstable`/`lighthouse` thành công trình an toàn; từ nguy hiểm thắng từ an toàn (`haunted_house`, `zombie_village`).
- Bỏ danh sách từ khóa chết (`temple_safe`, `stronghold_end`, `fortress_boss`) và các log debug mỗi lần phân loại; thêm cache theo id công trình.
- Trail Ruins không còn bị coi là dungeon cấp 3 (không có quái/spawner).

### Kết hợp công trình + biome
- Lấy cấp cao hơn thay vì "công trình luôn thắng". Trước đây đi vào Fortress (cấp 3) giữa biome Nether (cấp 5) làm độ khó **giảm**.
- Không còn tạo region cấp 1 (chỉ hiện title "Adventure I" mà không scale gì) cho biome lạ.

### Biome
- Bảng chính xác cho 64 biome vanilla; sửa `crimson_forest`, `warped_forest`, `stony_peaks`, `windswept_gravelly_hills` bị coi là an toàn do khớp từ "forest/stony/gravel" trước; `mushroom_fields` (không có quái) không còn bị coi là nguy hiểm; `deep_cold_ocean`/`deep_lukewarm_ocean` đúng cấp 4; `cherry_grove` an toàn; `soul_sand_valley` cấp 5 như các biome Nether khác.
- Biome mod: từ nguyên, rồi tag vanilla (`is_nether`, `is_end`, `is_deep_ocean`, `is_badlands`, `is_ocean`).
- Bỏ fallback "có quái thù địch = nguy hiểm" (gần như biome nào cũng có quái).

### Hỗ trợ mod world gen
- `/hrd scan`: quét toàn bộ công trình/biome/dimension trong world và ghi báo cáo `config/hardcore_region_difficulty_scan.json` (cấp, nguồn, danh sách chưa nhận ra, đoạn JSON để dán).
- Công trình mod không đoán được theo tên: nếu tự khai báo quái thù địch thì được cấp `structure_spawn_level` (3); tag `#minecraft:village` = an toàn.
- Region dimension thủ công là mức tối thiểu khi bật smart-auto (trước đây biome nguy hiểm cấp thấp hơn vẫn ghi đè).
- Scale mob: không còn scale boss mod (tag `forge:bosses`, máu gốc >= 150) và có `exclude_entities`.

### Cấu hình mới
- `smart_auto_settings`: `analyze_structures`, `analyze_biomes`, `unknown_structure_level`, `structure_spawn_level`, `structure_levels`, `biome_levels` (pattern id / `#tag` / `~regex`, cấp 0 = bỏ qua); `scaling_settings`: `exclude_entities`, `boss_health_threshold`. File cũ không có mục này vẫn chạy như mặc định.

## Bản sửa lỗi lớn (chỉ Forge)

### Lỗi khiến mod không build / không chạy
- `NetworkHandler`: `ResourceLocation.fromNamespaceAndPath` (chỉ có từ 1.21) -> `new ResourceLocation(...)`.
- Loot: thêm `AdvancedEnchantLootModifier`, đăng ký codec và `global_loot_modifiers.json`, thêm `conditions` vào file modifier. Trước đây rương không bao giờ nhận enchant.
- Villager: thêm listener `VillagerTradesEvent`; sách thủ thư giờ có thể thành sách Advanced.
- Sách enchant dùng `EnchantmentHelper.enchantItem` (chọn enchant ngẫu nhiên, bỏ qua enchant Advanced đã chọn) -> `EnchantedBookItem.addEnchantment`.
- Enchant chỉ chọn loại hợp với item, không xung đột với enchant sẵn có, cấp bị chặn theo cấp tối đa của enchant.

### Enchant Advanced
- Thêm hiệu ứng thật cho Protection, Feather Falling, Fire Aspect, Thorns, Looting, Efficiency, Knockback, Power, Unbreaking (trước chỉ Sharpness có tác dụng).
- Chuyển sang `DeferredRegister`/`RegistryObject`; lớp cơ sở `AdvancedEnchantmentBase` (không ra từ bàn enchant, random loot, vanilla trade).
- Enchant giáp/giày chỉ dùng slot giáp tương ứng.

### Hệ thống region
- Bật lại structure matching (dùng structure references + bounding box như DungeonDifficulty).
- Sửa thứ tự ưu tiên: structure > biome > biome tag > dimension (trước phụ thuộc thứ tự HashMap).
- Region khớp theo AND các điều kiện (khớp ý đồ của file mẫu `hardcore_region_difficulty.json`); trước đây OR nên cả sa mạc bị coi là "Ancient Desert Temple".
- Smart-auto thực sự chạy được (trước đây region dimension luôn khớp trước); thêm phân tích structure; cache theo biome.
- Hỗ trợ pattern `#tag`, `~regex`, `!phủ định`, `*`.
- Sửa `soul_sand_valley` bị xếp là biome an toàn; sửa tên biome chỉ viết hoa chữ đầu.

### Scale mob
- Dùng modifier vĩnh viễn + cờ NBT: không còn cộng dồn mỗi lần load chunk.
- Chỉ scale mob thù địch (không còn scale iron golem / snow golem); chỉ chạy phía server.
- Giữ tỉ lệ máu hiện tại; thêm `armor_flat_per_level` (mặc định 0, giữ nguyên cân bằng cũ).
- Mặc định trong code, file config và README đã thống nhất (0.02 / 0.15 / 1.0 / 0.05).

### Hiệu năng / ổn định
- Kiểm tra region mỗi 20 tick và chỉ tính một lần (trước: 4 lần mỗi tick).
- Dọn tracker khi người chơi thoát / server dừng.
- Config: đọc lỗi không còn làm crash hay ghi đè file; bỏ qua từng region lỗi; giữ thứ tự region.
- Title: hiện cả phụ đề và thời gian fade theo config; client-only code qua `DistExecutor`.

### Mới
- Lệnh `/hrd reload`, `/hrd region`.
- File config mặc định lấy từ file mẫu (11 region theo README).

### Đã xóa
- `ForgeConfig` (chỉ có `enabled` không dùng ở đâu, tạo ra file toml vô nghĩa).
- `INTEGRATION_NOTES.md` (việc cần làm đã xong), `ARCHITECTURE.md` / `FORGE_ARCHITECTURE.md` cũ (mô tả Fabric và code không còn) -> thay bằng `ARCHITECTURE.md` mới.
