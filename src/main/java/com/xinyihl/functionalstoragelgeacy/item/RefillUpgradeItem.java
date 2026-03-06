package com.xinyihl.functionalstoragelgeacy.item;

import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.config.FunctionalStorageConfig;
import com.xinyihl.functionalstoragelgeacy.inventory.BigInventoryHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RefillUpgradeItem extends MFSUpgradeItem {

    private static final String TAG_REFILL_TARGET = "RefillTarget";

    private final boolean dimensional;

    public RefillUpgradeItem(boolean dimensional) {
        this.dimensional = dimensional;
    }

    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(getRefillTarget(stack).getDisplayText());
    }

    @Override
    protected void doWork(ControllableDrawerTile tile, ItemStack upgradeStack, int upgradeSlot, MFSUpgradeDataManager data) {
        if (tile.getWorld() == null || tile.getWorld().getMinecraftServer() == null) {
            return;
        }

        UUID ownerId = getOwner(upgradeStack);
        if (ownerId == null) {
            return;
        }

        EntityPlayerMP owner = tile.getWorld().getMinecraftServer().getPlayerList().getPlayerByUUID(ownerId);
        if (owner == null) {
            return;
        }

        if (!dimensional && owner.dimension != tile.getWorld().provider.getDimension()) {
            return;
        }

        IItemHandler drawerHandler = tile.getItemHandler();
        if (drawerHandler == null) {
            return;
        }

        FilterConfiguration filter = getFilterConfiguration(upgradeStack);
        RefillTarget target = getRefillTarget(upgradeStack);
        List<Integer> selectedSlots = getSelectedSlots(upgradeStack, Math.min(4, drawerHandler.getSlots()));

        for (SlotAccess access : target.getSlots(owner)) {
            ItemStack current = access.get();
            if (current.isEmpty() || current.getCount() >= current.getMaxStackSize() || !filter.test(current)) {
                continue;
            }

            for (int selectedSlot : selectedSlots) {
                if (selectedSlot < 0 || selectedSlot >= drawerHandler.getSlots()) {
                    continue;
                }
                ItemStack extracted = drawerHandler.extractItem(selectedSlot, 1, true);
                if (extracted.isEmpty() || !BigInventoryHandler.areItemStacksEqual(extracted, current)) {
                    continue;
                }

                ItemStack moved = drawerHandler.extractItem(selectedSlot, 1, false);
                if (moved.isEmpty()) {
                    continue;
                }

                ItemStack updated = access.get().copy();
                updated.grow(1);
                access.set(updated);
                owner.inventoryContainer.detectAndSendChanges();
                return;
            }
        }
    }

    @Override
    protected int getBaseSpeed() {
        return dimensional ? FunctionalStorageConfig.MFS_BASE_DIMENSIONAL_REFILL_SPEED : FunctionalStorageConfig.MFS_BASE_REFILL_SPEED;
    }

    @Override
    public boolean hasOwner() {
        return true;
    }

    @Override
    public boolean hasDirection() {
        return false;
    }

    public boolean isDimensional() {
        return dimensional;
    }

    public static RefillTarget getRefillTarget(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG_REFILL_TARGET)) {
            try {
                return RefillTarget.valueOf(stack.getTagCompound().getString(TAG_REFILL_TARGET));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return RefillTarget.HOTBAR;
    }

    public static void setRefillTarget(ItemStack stack, RefillTarget target) {
        NBTTagCompound tag = getOrCreateTag(stack);
        tag.setString(TAG_REFILL_TARGET, target.name());
    }

    public enum RefillTarget {
        HOTBAR("gui.functionalstoragelgeacy.mfs_upgrade.refill.hotbar"),
        INVENTORY("gui.functionalstoragelgeacy.mfs_upgrade.refill.inventory"),
        ENDER_CHEST("gui.functionalstoragelgeacy.mfs_upgrade.refill.ender");

        private final String displayKey;

        RefillTarget(String displayKey) {
            this.displayKey = displayKey;
        }

        public String getDisplayText() {
            return net.minecraft.util.text.TextFormatting.YELLOW
                    + new net.minecraft.util.text.TextComponentTranslation("gui.functionalstoragelgeacy.mfs_upgrade.refill_target").getUnformattedText()
                    + " "
                    + net.minecraft.util.text.TextFormatting.AQUA
                    + new net.minecraft.util.text.TextComponentTranslation(displayKey).getUnformattedText();
        }

        public RefillTarget next() {
            RefillTarget[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public List<SlotAccess> getSlots(EntityPlayerMP player) {
            List<SlotAccess> accesses = new ArrayList<>();
            switch (this) {
                case INVENTORY:
                    for (int i = 9; i < player.inventory.mainInventory.size(); i++) {
                        final int slot = i;
                        accesses.add(new SlotAccess(() -> player.inventory.mainInventory.get(slot), stack -> player.inventory.mainInventory.set(slot, stack)));
                    }
                    break;
                case ENDER_CHEST:
                    InventoryEnderChest chest = player.getInventoryEnderChest();
                    for (int i = 0; i < chest.getSizeInventory(); i++) {
                        final int slot = i;
                        accesses.add(new SlotAccess(() -> chest.getStackInSlot(slot), stack -> chest.setInventorySlotContents(slot, stack)));
                    }
                    break;
                case HOTBAR:
                default:
                    for (int i = 0; i < 9; i++) {
                        final int slot = i;
                        accesses.add(new SlotAccess(() -> player.inventory.mainInventory.get(slot), stack -> player.inventory.mainInventory.set(slot, stack)));
                    }
                    break;
            }
            return accesses;
        }
    }

    public static class SlotAccess {
        private final java.util.function.Supplier<ItemStack> getter;
        private final java.util.function.Consumer<ItemStack> setter;

        public SlotAccess(java.util.function.Supplier<ItemStack> getter, java.util.function.Consumer<ItemStack> setter) {
            this.getter = getter;
            this.setter = setter;
        }

        public ItemStack get() {
            return getter.get();
        }

        public void set(ItemStack stack) {
            setter.accept(stack);
        }
    }
}
