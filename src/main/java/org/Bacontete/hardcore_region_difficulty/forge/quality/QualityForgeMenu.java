package org.Bacontete.hardcore_region_difficulty.forge.quality;

import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.Bacontete.hardcore_region_difficulty.common.quality.ForgeMaterials;
import org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityConfig;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityType;

public class QualityForgeMenu extends AbstractContainerMenu {

    private static final String SEED_TAG = "HrdForgeSeed";
    private static final int ITEM_SLOT = 0;
    private static final int MATERIAL_SLOT = 1;
    private static final int RESULT_SLOT = 2;
    private static final int INV_START = 3;
    private static final int INV_END = 39;

    private final Container input = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            QualityForgeMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    private final DataSlot costData = DataSlot.standalone();
    private final DataSlot xpCostData = DataSlot.standalone();

    private QualityType pendingQuality = null;

    public QualityForgeMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(QualityForgeRegistry.QUALITY_FORGE_MENU.get(), id);
        this.access = access;
        this.player = inventory.player;
        this.costData.set(QualityConfig.MATERIAL_COST.get());
        addDataSlot(costData);
        this.xpCostData.set(QualityConfig.XP_COST.get());
        addDataSlot(xpCostData);

        addSlot(new Slot(input, ITEM_SLOT, 8, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ItemQuality.isEligible(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(input, MATERIAL_SLOT, 26, 48));
        addSlot(new Slot(result, 0, 98, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player p) {
                return p.getAbilities().instabuild || p.experienceLevel >= xpCostData.get();
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {

                access.execute((level, pos) -> {
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE, 
                            net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                });

                if (!stack.isEmpty()) {
                    ItemQuality.set(stack, pendingQuality);
                }

                input.setItem(ITEM_SLOT, ItemStack.EMPTY);
                input.getItem(MATERIAL_SLOT).shrink(costData.get());
                if (!taker.getAbilities().instabuild && xpCostData.get() > 0) {
                    taker.giveExperienceLevels(-xpCostData.get());
                }
                taker.getPersistentData().putLong(SEED_TAG, taker.getRandom().nextLong());
                createResult();
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public int getXpCost() {
        return xpCostData.get();
    }

    public int getMaterialCost() {
        return costData.get();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == input) {
            createResult();
        }
    }

    private void createResult() {
        if (player.level().isClientSide) {
            return;   
        }
        ItemStack item = input.getItem(ITEM_SLOT);
        ItemStack material = input.getItem(MATERIAL_SLOT);
        result.setItem(0, ItemStack.EMPTY);
        pendingQuality = null;
        if (!item.isEmpty() && !material.isEmpty()
                && material.getCount() >= costData.get()
                && ForgeMaterials.accepts(item, material, player.level())) {
            pendingQuality = ItemQuality.rollForge(RandomSource.create(rollSeed(item)), item, player.getLuck());
            if (pendingQuality == null) {
                // the category has no quality defined: nothing to forge
                broadcastChanges();
                return;
            }
            ItemStack forged = item.copy();

            if (forged.getTag() != null) {
                forged.getTag().remove(ItemQuality.TAG);
                if (forged.getTag().isEmpty()) {
                    forged.setTag(null);
                }
            }
            result.setItem(0, forged);
        }
        broadcastChanges();
    }

    private long rollSeed(ItemStack item) {
        if (!player.getPersistentData().contains(SEED_TAG)) {
            player.getPersistentData().putLong(SEED_TAG, player.getRandom().nextLong());
        }
        long seed = player.getPersistentData().getLong(SEED_TAG);
        seed ^= item.getItem().hashCode() * 31L;
        if (item.getTag() != null) {
            seed ^= item.getTag().hashCode();
        }
        return seed;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == RESULT_SLOT) {
            if (!slot.mayPickup(player)) {
                return ItemStack.EMPTY;
            }

            ItemStack moving = stack.copy();
            ItemQuality.set(moving, pendingQuality);
            if (!moveItemStackTo(moving, INV_START, INV_END, true) || !moving.isEmpty()) {
                return ItemStack.EMPTY;
            }
            slot.setByPlayer(ItemStack.EMPTY);
            slot.onTake(player, ItemStack.EMPTY);
            return original;
        } else if (index < RESULT_SLOT) {
            if (!moveItemStackTo(stack, INV_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, ITEM_SLOT, RESULT_SLOT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, QualityForgeRegistry.QUALITY_FORGE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, input));
    }
}
