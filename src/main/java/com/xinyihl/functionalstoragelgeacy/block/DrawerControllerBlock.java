package com.xinyihl.functionalstoragelgeacy.block;

import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.block.tile.StorageControllerTile;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * Block for the drawer controller.
 * Aggregates connected drawers into a unified item/fluid handler.
 */
public class DrawerControllerBlock extends DrawerBlock {

    public DrawerControllerBlock() {
        super(Material.IRON);
        this.setRegistryName("storage_controller");
        this.setTranslationKey("functionalstoragelgeacy.storage_controller");
        this.setHardness(5.0F);
        this.setResistance(10.0F);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new StorageControllerTile();
    }

    @Override
    public int getHitSlot(IBlockState state, World world, BlockPos pos, EntityPlayer player) {
        return -1; // Controller has no slots
    }

    @Override
    public Collection<AxisAlignedBB> getHitBoxes(IBlockState state) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack createStackWithTileData(ControllableDrawerTile tile) {
        return new ItemStack(this);
    }
}
