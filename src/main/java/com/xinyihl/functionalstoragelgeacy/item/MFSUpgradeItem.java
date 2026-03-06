package com.xinyihl.functionalstoragelgeacy.item;

import com.mojang.authlib.GameProfile;
import com.xinyihl.functionalstoragelgeacy.block.DrawerBlock;
import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.config.FunctionalStorageConfig;
import com.xinyihl.functionalstoragelgeacy.util.RelativeDirection;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class MFSUpgradeItem extends UpgradeItem {

    public static final String TAG_OWNER = "Owner";
    public static final String TAG_DIRECTION = "RelativeDirection";
    public static final String TAG_REDSTONE_MODE = "RedstoneMode";
    public static final String TAG_SELECTED_SLOTS = "SelectedSlots";
    public static final String TAG_AUGMENTS = "Augments";
    public static final String TAG_TOOL = "Tool";
    public static final String TAG_FILTER = "Filter";

    protected MFSUpgradeItem() {
        super(Type.UTILITY, UtilityAction.NONE);
        setMaxStackSize(1);
    }

    @Override
    public void onUpdate(@Nonnull ItemStack stack, World worldIn, @Nonnull Entity entityIn, int itemSlot, boolean isSelected) {
        super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);
        if (!(entityIn instanceof EntityPlayer)) {
            return;
        }

        if (hasOwner() && !hasOwner(stack)) {
            setOwner(stack, ((EntityPlayer) entityIn).getUniqueID());
        }
        if (hasDirection() && !stack.hasTagCompound()) {
            setRelativeDirection(stack, RelativeDirection.FRONT);
        } else if (hasDirection() && !stack.getTagCompound().hasKey(TAG_DIRECTION)) {
            setRelativeDirection(stack, RelativeDirection.FRONT);
        }
    }

    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(TextFormatting.YELLOW + new TextComponentTranslation("item.functionalstoragelgeacy.mfs_upgrade.speed", getSpeed(stack)).getUnformattedText());
        tooltip.add(TextFormatting.YELLOW + new TextComponentTranslation("item.functionalstoragelgeacy.mfs_upgrade.redstone").getUnformattedText()
                + " " + TextFormatting.AQUA
                + new TextComponentTranslation(getRedstoneMode(stack).getTranslationKey()).getUnformattedText());

        if (hasDirection()) {
            tooltip.add(TextFormatting.YELLOW + new TextComponentTranslation("item.functionalstoragelgeacy.mfs_upgrade.direction").getUnformattedText()
                    + " " + TextFormatting.AQUA
                    + new TextComponentTranslation(getRelativeDirection(stack).getTranslationKey()).getUnformattedText());
        }
    }

    @Override
    public final void onTick(ControllableDrawerTile tile, ItemStack upgradeStack, int upgradeSlot) {
        if (tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }

        World world = tile.getWorld();
        BlockPos pos = tile.getPos();
        MFSUpgradeDataManager data = tile.getMfsUpgradeData(upgradeSlot);
        boolean powered = world.isBlockPowered(pos);
        boolean lastPowered = data.getBoolean("LastPowered");
        data.setBoolean("LastPowered", powered);

        RedstoneMode mode = getRedstoneMode(upgradeStack);
        boolean shouldRun;
        if (mode == RedstoneMode.PULSE) {
            shouldRun = powered && !lastPowered;
        } else {
            if (world.getTotalWorldTime() % getSpeed(upgradeStack) != 0) {
                return;
            }
            shouldRun = mode.canRun(powered);
        }

        if (shouldRun) {
            doWork(tile, upgradeStack, upgradeSlot, data);
        }
    }

    protected abstract void doWork(ControllableDrawerTile tile, ItemStack upgradeStack, int upgradeSlot, MFSUpgradeDataManager data);

    protected abstract int getBaseSpeed();

    public boolean hasOwner() {
        return false;
    }

    public boolean hasDirection() {
        return true;
    }

    public int getSpeed(ItemStack stack) {
        int augmentCount = getAugmentCount(stack);
        return Math.max(1, getBaseSpeed() - (augmentCount * FunctionalStorageConfig.MFS_SPEED_AUGMENT_REDUCTION));
    }

    protected int getAugmentCount(ItemStack stack) {
        return getAugmentInventory(stack).getStackInSlot(0).getCount();
    }

    public static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    public static void setOwner(ItemStack stack, UUID owner) {
        getOrCreateTag(stack).setUniqueId(TAG_OWNER, owner);
    }

    public static boolean hasOwner(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasUniqueId(TAG_OWNER);
    }

    @Nullable
    public static UUID getOwner(ItemStack stack) {
        return hasOwner(stack) ? stack.getTagCompound().getUniqueId(TAG_OWNER) : null;
    }

    public static RelativeDirection getRelativeDirection(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG_DIRECTION)) {
            int ordinal = stack.getTagCompound().getInteger(TAG_DIRECTION);
            if (ordinal >= 0 && ordinal < RelativeDirection.values().length) {
                return RelativeDirection.values()[ordinal];
            }
        }
        return RelativeDirection.FRONT;
    }

    public static void setRelativeDirection(ItemStack stack, RelativeDirection direction) {
        getOrCreateTag(stack).setInteger(TAG_DIRECTION, direction.ordinal());
    }

    public static EnumFacing getDirection(ControllableDrawerTile tile, ItemStack stack) {
        EnumFacing blockFacing = EnumFacing.NORTH;
        if (tile.getWorld() != null) {
            if (tile.getWorld().getBlockState(tile.getPos()).getProperties().containsKey(DrawerBlock.HORIZONTAL_FACING)) {
                blockFacing = tile.getWorld().getBlockState(tile.getPos()).getValue(DrawerBlock.HORIZONTAL_FACING);
            }
        }
        return getRelativeDirection(stack).getAbsolute(blockFacing);
    }

    public static RedstoneMode getRedstoneMode(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG_REDSTONE_MODE)) {
            try {
                return RedstoneMode.valueOf(stack.getTagCompound().getString(TAG_REDSTONE_MODE));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return RedstoneMode.IGNORE;
    }

    public static void setRedstoneMode(ItemStack stack, RedstoneMode mode) {
        getOrCreateTag(stack).setString(TAG_REDSTONE_MODE, mode.name());
    }

    public static List<Integer> getSelectedSlots(ItemStack stack, int maxSlots) {
        int[] selected = stack.hasTagCompound() ? stack.getTagCompound().getIntArray(TAG_SELECTED_SLOTS) : new int[0];
        if (selected.length == 0) {
            List<Integer> defaults = new ArrayList<>();
            for (int i = 0; i < maxSlots; i++) {
                defaults.add(i);
            }
            return defaults;
        }

        List<Integer> slots = new ArrayList<>();
        for (int value : selected) {
            if (value >= 0 && value < maxSlots) {
                slots.add(value);
            }
        }
        return slots;
    }

    public static void toggleSelectedSlot(ItemStack stack, int slot, int maxSlots) {
        List<Integer> current = getSelectedSlots(stack, maxSlots);
        if (current.contains(slot)) {
            current.remove((Integer) slot);
        } else {
            current.add(slot);
        }
        if (current.isEmpty()) {
            current.add(slot);
        }
        int[] encoded = new int[current.size()];
        for (int i = 0; i < current.size(); i++) {
            encoded[i] = current.get(i);
        }
        getOrCreateTag(stack).setIntArray(TAG_SELECTED_SLOTS, encoded);
    }

    public static ItemStackHandler getAugmentInventory(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(1);
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG_AUGMENTS)) {
            handler.deserializeNBT(stack.getTagCompound().getCompoundTag(TAG_AUGMENTS));
        }
        return handler;
    }

    public static void setAugmentInventory(ItemStack stack, ItemStackHandler handler) {
        getOrCreateTag(stack).setTag(TAG_AUGMENTS, handler.serializeNBT());
    }

    public static ItemStack getToolStack(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG_TOOL)) {
            return new ItemStack(stack.getTagCompound().getCompoundTag(TAG_TOOL));
        }
        return ItemStack.EMPTY;
    }

    public static void setToolStack(ItemStack stack, ItemStack toolStack) {
        NBTTagCompound tag = getOrCreateTag(stack);
        if (toolStack.isEmpty()) {
            tag.removeTag(TAG_TOOL);
        } else {
            ItemStack copy = toolStack.copy();
            copy.setCount(1);
            tag.setTag(TAG_TOOL, copy.writeToNBT(new NBTTagCompound()));
        }
    }

    public static FilterConfiguration getFilterConfiguration(ItemStack stack) {
        FilterConfiguration configuration = new FilterConfiguration();
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG_FILTER)) {
            configuration.deserializeNBT(stack.getTagCompound().getCompoundTag(TAG_FILTER));
        }
        return configuration;
    }

    public static void setFilterConfiguration(ItemStack stack, FilterConfiguration configuration) {
        getOrCreateTag(stack).setTag(TAG_FILTER, configuration.serializeNBT());
    }

    @Nullable
    protected FakePlayer getFakePlayer(World world, ItemStack stack) {
        UUID owner = getOwner(stack);
        if (!(world instanceof WorldServer) || owner == null) {
            return null;
        }
        return FakePlayerFactory.get((WorldServer) world, new GameProfile(owner, "[FunctionalStorage]"));
    }

    public enum RedstoneMode {
        IGNORE("item.functionalstoragelgeacy.mfs_upgrade.redstone.ignore"),
        HIGH("item.functionalstoragelgeacy.mfs_upgrade.redstone.high"),
        LOW("item.functionalstoragelgeacy.mfs_upgrade.redstone.low"),
        PULSE("item.functionalstoragelgeacy.mfs_upgrade.redstone.pulse");

        private final String translationKey;

        RedstoneMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public RedstoneMode next() {
            RedstoneMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public boolean canRun(boolean powered) {
            switch (this) {
                case HIGH:
                    return powered;
                case LOW:
                    return !powered;
                case PULSE:
                    return false;
                case IGNORE:
                default:
                    return true;
            }
        }
    }
}
