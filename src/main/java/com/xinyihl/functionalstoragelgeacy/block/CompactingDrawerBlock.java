package com.xinyihl.functionalstoragelgeacy.block;

import com.xinyihl.functionalstoragelgeacy.block.tile.CompactingDrawerTile;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Block for the compacting drawer (3-slot nugget/ingot/block auto-compressing).
 */
public class CompactingDrawerBlock extends DrawerBlock {

    /**
     * Hit detection shapes for 3 compacting slots per facing direction.
     * Slot 0: top (full width), Slot 1: bottom-left, Slot 2: bottom-right
     */
    private static final Map<EnumFacing, List<AxisAlignedBB>> CACHED_SHAPES = new HashMap<>();

    static {
        // NORTH face
        CACHED_SHAPES.put(EnumFacing.NORTH, Arrays.asList(
                new AxisAlignedBB(1 / 16D, 9 / 16D, 0, 15 / 16D, 15 / 16D, 1 / 16D),  // slot 2 (bottom-right)
                new AxisAlignedBB(9 / 16D, 1 / 16D, 0, 15 / 16D, 7 / 16D, 1 / 16D),   // slot 1 (bottom-left)
                new AxisAlignedBB(1 / 16D, 1 / 16D, 0, 7 / 16D, 7 / 16D, 1 / 16D)     // slot 0 (top)
        ));
        CACHED_SHAPES.put(EnumFacing.SOUTH, Arrays.asList(
                new AxisAlignedBB(1 / 16D, 9 / 16D, 15 / 16D, 15 / 16D, 15 / 16D, 1),
                new AxisAlignedBB(1/16D, 1/16D, 15/16D, 7/16D, 7/16D, 1),
                new AxisAlignedBB(9 / 16D, 1 / 16D, 15 / 16D, 15 / 16D, 7 / 16D, 1)
        ));
        CACHED_SHAPES.put(EnumFacing.EAST, Arrays.asList(
                new AxisAlignedBB(15 / 16D, 9 / 16D, 1 / 16D, 1, 15 / 16D, 15 / 16D),
                new AxisAlignedBB(15/16D, 1/16D, 9/16D, 1, 7/16D, 15/16D),
                new AxisAlignedBB(15 / 16D, 1 / 16D, 1 / 16D, 1, 7 / 16D, 7 / 16D)
        ));
        CACHED_SHAPES.put(EnumFacing.WEST, Arrays.asList(
                new AxisAlignedBB(0, 9 / 16D, 1 / 16D, 1 / 16D, 15 / 16D, 15 / 16D),
                new AxisAlignedBB(0, 1/16D, 1/16D, 1/16D, 7/16D, 7/16D),
                new AxisAlignedBB(0, 1 / 16D, 9 / 16D, 1 / 16D, 7 / 16D, 15 / 16D)
        ));
    }

    public CompactingDrawerBlock() {
        super(Material.ROCK);
        this.setRegistryName("compacting_drawer");
        this.setTranslationKey("functionalstoragelgeacy.compacting_drawer");
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new CompactingDrawerTile();
    }

    @Override
    public int getHitSlot(IBlockState state, World world, BlockPos pos, EntityPlayer player) {
        EnumFacing facing = state.getValue(FACING);
        List<AxisAlignedBB> shapes = CACHED_SHAPES.get(facing);
        if (shapes == null) return -1;

        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
        Vec3d end = start.add(look.x * reach, look.y * reach, look.z * reach);

        for (int i = 0; i < shapes.size(); i++) {
            AxisAlignedBB box = shapes.get(i).offset(pos);
            RayTraceResult result = box.calculateIntercept(start, end);
            if (result != null) return i;
        }
        return -1;
    }

    @Override
    public Collection<AxisAlignedBB> getHitBoxes(IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        List<AxisAlignedBB> shapes = CACHED_SHAPES.get(facing);
        return shapes != null ? shapes : Collections.emptyList();
    }
}
