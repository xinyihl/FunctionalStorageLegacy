package com.xinyihl.functionalstoragelegacy.block;

import com.xinyihl.functionalstoragelegacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelegacy.block.tile.ControllerExtensionTile;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Block for the controller extension.
 * Provides additional access point to a linked drawer controller.
 */
public class ControllerExtensionBlock extends DrawerBlock {

    public ControllerExtensionBlock() {
        super(Material.IRON);
        this.setRegistryName("controller_extension");
        this.setTranslationKey("functionalstoragelegacy.controller_extension");
        this.setHardness(5.0F);
        this.setResistance(10.0F);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new ControllerExtensionTile();
    }

    @Override
    public ItemStack createStackWithTileData(ControllableDrawerTile tile) {
        return new ItemStack(this);
    }
}
