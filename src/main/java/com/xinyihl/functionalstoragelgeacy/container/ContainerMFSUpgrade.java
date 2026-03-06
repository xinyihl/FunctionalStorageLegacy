package com.xinyihl.functionalstoragelgeacy.container;

import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.item.BreakerUpgradeItem;
import com.xinyihl.functionalstoragelgeacy.item.FilterConfiguration;
import com.xinyihl.functionalstoragelgeacy.item.MFSUpgradeItem;
import com.xinyihl.functionalstoragelgeacy.item.SpeedUpgradeAugmentItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class ContainerMFSUpgrade extends Container {

    public static final int AUGMENT_SLOT = 0;
    public static final int TOOL_SLOT = 1;
    public static final int FILTER_START = 2;
    public static final int FILTER_COUNT = 9;
    public static final int PLAYER_START = FILTER_START + FILTER_COUNT;

    private final ControllableDrawerTile tile;
    private final int upgradeSlot;
    private final ItemStackHandler augmentInventory;
    private final ItemStackHandler toolInventory;
    private final InventoryBasic filterInventory;

    public ContainerMFSUpgrade(InventoryPlayer playerInventory, ControllableDrawerTile tile, int upgradeSlot) {
        this.tile = tile;
        this.upgradeSlot = upgradeSlot;

        ItemStack upgradeStack = getUpgradeStack();
        this.augmentInventory = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.getItem() instanceof SpeedUpgradeAugmentItem;
            }

            @Override
            protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
                return 6;
            }

            @Override
            protected void onContentsChanged(int slot) {
                saveUpgradeData();
            }
        };
        this.toolInventory = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return getUpgradeStack().getItem() instanceof BreakerUpgradeItem;
            }

            @Override
            protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                saveUpgradeData();
            }
        };
        this.filterInventory = new InventoryBasic("mfs_filters", false, FILTER_COUNT) {
            @Override
            public int getInventoryStackLimit() {
                return 1;
            }

            @Override
            public void markDirty() {
                super.markDirty();
                saveUpgradeData();
            }
        };

        augmentInventory.deserializeNBT(MFSUpgradeItem.getAugmentInventory(upgradeStack).serializeNBT());
        toolInventory.setStackInSlot(0, MFSUpgradeItem.getToolStack(upgradeStack));
        FilterConfiguration filterConfiguration = MFSUpgradeItem.getFilterConfiguration(upgradeStack);
        for (int i = 0; i < FILTER_COUNT; i++) {
            filterInventory.setInventorySlotContents(i, filterConfiguration.getFilter(i));
        }

        addSlotToContainer(new SlotItemHandler(augmentInventory, 0, 8, 18));
        addSlotToContainer(new SlotItemHandler(toolInventory, 0, 30, 18));
        for (int i = 0; i < FILTER_COUNT; i++) {
            addSlotToContainer(new GhostFilterSlot(filterInventory, i, 8 + (i % 3) * 18, 44 + (i / 3) * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 112 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 170));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return playerIn.getDistanceSq(tile.getPos()) <= 64;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        saveUpgradeData();
    }

    @Nonnull
    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        if (slotId >= FILTER_START && slotId < FILTER_START + FILTER_COUNT) {
            Slot slot = inventorySlots.get(slotId);
            ItemStack carried = player.inventory.getItemStack();
            if (carried.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                ItemStack ghost = carried.copy();
                ghost.setCount(1);
                slot.putStack(ghost);
            }
            detectAndSendChanges();
            return player.inventory.getItemStack();
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getStack();
        itemstack = stackInSlot.copy();

        if (index < PLAYER_START) {
            if (!mergeItemStack(stackInSlot, PLAYER_START, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stackInSlot.getItem() instanceof SpeedUpgradeAugmentItem) {
                if (!mergeItemStack(stackInSlot, AUGMENT_SLOT, AUGMENT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (getUpgradeStack().getItem() instanceof BreakerUpgradeItem) {
                if (!mergeItemStack(stackInSlot, TOOL_SLOT, TOOL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        return itemstack;
    }

    public void saveUpgradeData() {
        ItemStack stack = getUpgradeStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof MFSUpgradeItem)) {
            return;
        }

        MFSUpgradeItem.setAugmentInventory(stack, augmentInventory);
        MFSUpgradeItem.setToolStack(stack, toolInventory.getStackInSlot(0));

        FilterConfiguration filterConfiguration = new FilterConfiguration();
        for (int i = 0; i < FILTER_COUNT; i++) {
            filterConfiguration.setFilter(i, filterInventory.getStackInSlot(i));
        }
        FilterConfiguration current = MFSUpgradeItem.getFilterConfiguration(stack);
        filterConfiguration.setBlacklist(current.isBlacklist());
        filterConfiguration.setMatchNBT(current.isMatchNBT());
        MFSUpgradeItem.setFilterConfiguration(stack, filterConfiguration);

        tile.markDirty();
        tile.sendUpdatePacket();
    }

    public ControllableDrawerTile getTile() {
        return tile;
    }

    public int getUpgradeSlot() {
        return upgradeSlot;
    }

    public ItemStack getUpgradeStack() {
        return upgradeSlot >= 0 && upgradeSlot < tile.getUtilityUpgrades().getSlots()
                ? tile.getUtilityUpgrades().getStackInSlot(upgradeSlot)
                : ItemStack.EMPTY;
    }

    private static class GhostFilterSlot extends Slot {
        private GhostFilterSlot(InventoryBasic inventory, int index, int xPosition, int yPosition) {
            super(inventory, index, xPosition, yPosition);
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }
}
