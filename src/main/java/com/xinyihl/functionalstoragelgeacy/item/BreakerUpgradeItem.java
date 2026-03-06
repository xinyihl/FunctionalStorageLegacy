package com.xinyihl.functionalstoragelgeacy.item;

import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.config.FunctionalStorageConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class BreakerUpgradeItem extends MFSUpgradeItem {

    @Override
    protected void doWork(ControllableDrawerTile tile, ItemStack upgradeStack, int upgradeSlot, MFSUpgradeDataManager data) {
        World world = tile.getWorld();
        if (world == null) {
            return;
        }

        ItemStack tool = getToolStack(upgradeStack);
        if (tool.isEmpty()) {
            return;
        }

        IItemHandler drawerHandler = tile.getItemHandler();
        if (drawerHandler == null) {
            return;
        }

        BlockPos targetPos = tile.getPos().offset(getDirection(tile, upgradeStack));
        IBlockState state = world.getBlockState(targetPos);
        if (state.getBlock().isAir(state, world, targetPos)) {
            return;
        }

        if (!state.getMaterial().isToolNotRequired() && !tool.canHarvestBlock(state)) {
            return;
        }

        FilterConfiguration filter = getFilterConfiguration(upgradeStack);
        NonNullList<ItemStack> drops = NonNullList.create();
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool);
        Block block = state.getBlock();
        block.getDrops(drops, world, targetPos, state, fortune);

        if (drops.isEmpty()) {
            return;
        }

        for (ItemStack drop : drops) {
            if (!filter.test(drop)) {
                return;
            }
        }

        List<Integer> selectedSlots = getSelectedSlots(upgradeStack, Math.min(4, drawerHandler.getSlots()));
        List<Runnable> actions = new ArrayList<>();
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack drop : drops) {
            remaining.add(drop.copy());
        }

        for (int selectedSlot : selectedSlots) {
            if (selectedSlot < 0 || selectedSlot >= drawerHandler.getSlots()) {
                continue;
            }
            attemptInsert(drawerHandler, selectedSlot, remaining, actions);
            if (remaining.isEmpty()) {
                break;
            }
        }

        if (!remaining.isEmpty()) {
            return;
        }

        for (Runnable action : actions) {
            action.run();
        }
        world.destroyBlock(targetPos, false);
    }

    @Override
    protected int getBaseSpeed() {
        return FunctionalStorageConfig.MFS_BASE_BREAKER_SPEED;
    }

    @Override
    public boolean hasOwner() {
        return true;
    }

    private void attemptInsert(IItemHandler drawerHandler, int slot, List<ItemStack> drops, List<Runnable> actions) {
        for (int i = 0; i < drops.size(); i++) {
            ItemStack drop = drops.get(i);
            ItemStack remainder = drawerHandler.insertItem(slot, drop, true);
            if (remainder.getCount() < drop.getCount()) {
                ItemStack toInsert = drop.copy();
                toInsert.setCount(drop.getCount() - remainder.getCount());
                actions.add(() -> drawerHandler.insertItem(slot, toInsert, false));
                if (remainder.isEmpty()) {
                    drops.remove(i);
                } else {
                    drops.set(i, remainder);
                }
                i--;
            }
        }
    }
}
