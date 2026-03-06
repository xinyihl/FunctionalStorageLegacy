package com.xinyihl.functionalstoragelgeacy.item;

import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.config.FunctionalStorageConfig;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

public class PlacerUpgradeItem extends MFSUpgradeItem {

    @Override
    protected void doWork(ControllableDrawerTile tile, ItemStack upgradeStack, int upgradeSlot, MFSUpgradeDataManager data) {
        World world = tile.getWorld();
        if (world == null) {
            return;
        }

        IItemHandler drawerHandler = tile.getItemHandler();
        if (drawerHandler == null) {
            return;
        }

        FakePlayer fakePlayer = getFakePlayer(world, upgradeStack);
        if (fakePlayer == null) {
            return;
        }

        BlockPos targetPos = tile.getPos().offset(getDirection(tile, upgradeStack));
        if (!world.getBlockState(targetPos).getBlock().isReplaceable(world, targetPos)) {
            return;
        }

        FilterConfiguration filter = getFilterConfiguration(upgradeStack);
        List<Integer> selectedSlots = getSelectedSlots(upgradeStack, Math.min(4, drawerHandler.getSlots()));
        for (int slot : selectedSlots) {
            if (slot < 0 || slot >= drawerHandler.getSlots()) {
                continue;
            }

            ItemStack candidate = drawerHandler.extractItem(slot, 1, true);
            if (candidate.isEmpty() || !(candidate.getItem() instanceof ItemBlock) || !filter.test(candidate)) {
                continue;
            }

            BlockPos supportPos = targetPos.offset(getDirection(tile, upgradeStack).getOpposite());
            ItemStack placingStack = candidate.copy();
            fakePlayer.setHeldItem(EnumHand.MAIN_HAND, placingStack);
            EnumActionResult result = placingStack.onItemUse(fakePlayer, world, supportPos, EnumHand.MAIN_HAND,
                    getDirection(tile, upgradeStack), 0.5F, 0.5F, 0.5F);

            if (result == EnumActionResult.SUCCESS) {
                drawerHandler.extractItem(slot, 1, false);
                return;
            }
        }
    }

    @Override
    protected int getBaseSpeed() {
        return FunctionalStorageConfig.MFS_BASE_PLACER_SPEED;
    }

    @Override
    public boolean hasOwner() {
        return true;
    }
}
