package com.xinyihl.functionalstoragelgeacy.block;

import com.xinyihl.functionalstoragelgeacy.DrawerType;
import com.xinyihl.functionalstoragelgeacy.block.tile.DrawerTile;
import com.xinyihl.functionalstoragelgeacy.util.DrawerWoodType;
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
 * Standard wooden drawer block.
 * Each variant is defined by a wood type and drawer type (1/2/4 slots).
 */
public class WoodDrawerBlock extends DrawerBlock {

    // Cached hit shapes per drawer type per facing
    public static final Map<DrawerType, Map<EnumFacing, List<AxisAlignedBB>>> CACHED_SHAPES = new HashMap<>();

    static {
        // X_1: Single slot covering the whole front face
        Map<EnumFacing, List<AxisAlignedBB>> x1 = new HashMap<>();
        x1.put(EnumFacing.NORTH, Collections.singletonList(new AxisAlignedBB(1/16D, 1/16D, 0, 15/16D, 15/16D, 1/16D)));
        x1.put(EnumFacing.SOUTH, Collections.singletonList(new AxisAlignedBB(1/16D, 1/16D, 15/16D, 15/16D, 15/16D, 1)));
        x1.put(EnumFacing.WEST, Collections.singletonList(new AxisAlignedBB(0, 1/16D, 1/16D, 1/16D, 15/16D, 15/16D)));
        x1.put(EnumFacing.EAST, Collections.singletonList(new AxisAlignedBB(15/16D, 1/16D, 1/16D, 1, 15/16D, 15/16D)));
        CACHED_SHAPES.put(DrawerType.X_1, x1);

        // X_2: Two slots stacked vertically (top slot = 0, bottom slot = 1)
        Map<EnumFacing, List<AxisAlignedBB>> x2 = new HashMap<>();
        x2.put(EnumFacing.NORTH, Arrays.asList(
                new AxisAlignedBB(1/16D, 9/16D, 0, 15/16D, 15/16D, 1/16D),   // Slot 0 (top)
                new AxisAlignedBB(1/16D, 1/16D, 0, 15/16D, 7/16D, 1/16D)     // Slot 1 (bottom)
        ));
        x2.put(EnumFacing.SOUTH, Arrays.asList(
                new AxisAlignedBB(1/16D, 9/16D, 15/16D, 15/16D, 15/16D, 1),
                new AxisAlignedBB(1/16D, 1/16D, 15/16D, 15/16D, 7/16D, 1)
        ));
        x2.put(EnumFacing.WEST, Arrays.asList(
                new AxisAlignedBB(0, 9/16D, 1/16D, 1/16D, 15/16D, 15/16D),
                new AxisAlignedBB(0, 1/16D, 1/16D, 1/16D, 7/16D, 15/16D)
        ));
        x2.put(EnumFacing.EAST, Arrays.asList(
                new AxisAlignedBB(15/16D, 9/16D, 1/16D, 1, 15/16D, 15/16D),
                new AxisAlignedBB(15/16D, 1/16D, 1/16D, 1, 7/16D, 15/16D)
        ));
        CACHED_SHAPES.put(DrawerType.X_2, x2);

        // X_4: Four slots in a 2x2 grid (0=TL, 1=TR, 2=BL, 3=BR)
        Map<EnumFacing, List<AxisAlignedBB>> x4 = new HashMap<>();
        x4.put(EnumFacing.NORTH, Arrays.asList(
                new AxisAlignedBB(9 / 16D, 9 / 16D, 0, 15 / 16D, 15 / 16D, 1 / 16D),   // Slot 0 (top-left)
                new AxisAlignedBB(1 / 16D, 9 / 16D, 0, 7 / 16D, 15 / 16D, 1 / 16D),    // Slot 1 (top-right)
                new AxisAlignedBB(9 / 16D, 1 / 16D, 0, 15 / 16D, 7 / 16D, 1 / 16D),    // Slot 2 (bottom-left)
                new AxisAlignedBB(1 / 16D, 1 / 16D, 0, 7 / 16D, 7 / 16D, 1 / 16D)      // Slot 3 (bottom-right)
        ));
        x4.put(EnumFacing.SOUTH, Arrays.asList(
                new AxisAlignedBB(1 / 16D, 9 / 16D, 15 / 16D, 7 / 16D, 15 / 16D, 1),   // Slot 0 (top-left)
                new AxisAlignedBB(9 / 16D, 9 / 16D, 15 / 16D, 15 / 16D, 15 / 16D, 1),  // Slot 1 (top-right)
                new AxisAlignedBB(1 / 16D, 1 / 16D, 15 / 16D, 7 / 16D, 7 / 16D, 1),    // Slot 2 (bottom-left)
                new AxisAlignedBB(9 / 16D, 1 / 16D, 15 / 16D, 15 / 16D, 7 / 16D, 1)    // Slot 3 (bottom-right)
        ));
        x4.put(EnumFacing.WEST, Arrays.asList(
                new AxisAlignedBB(0, 9 / 16D, 1 / 16D, 1 / 16D, 15 / 16D, 7 / 16D),    // Slot 0 (top-left)
                new AxisAlignedBB(0, 9 / 16D, 9 / 16D, 1 / 16D, 15 / 16D, 15 / 16D),   // Slot 1 (top-right)
                new AxisAlignedBB(0, 1 / 16D, 1 / 16D, 1 / 16D, 7 / 16D, 7 / 16D),     // Slot 2 (bottom-left)
                new AxisAlignedBB(0, 1 / 16D, 9 / 16D, 1 / 16D, 7 / 16D, 15 / 16D)     // Slot 3 (bottom-right)
        ));
        x4.put(EnumFacing.EAST, Arrays.asList(
                new AxisAlignedBB(15 / 16D, 9 / 16D, 9 / 16D, 1, 15 / 16D, 15 / 16D),  // Slot 0 (top-left)
                new AxisAlignedBB(15 / 16D, 9 / 16D, 1 / 16D, 1, 15 / 16D, 7 / 16D),   // Slot 1 (top-right)
                new AxisAlignedBB(15 / 16D, 1 / 16D, 9 / 16D, 1, 7 / 16D, 15 / 16D),   // Slot 2 (bottom-left)
                new AxisAlignedBB(15 / 16D, 1 / 16D, 1 / 16D, 1, 7 / 16D, 7 / 16D)     // Slot 3 (bottom-right)
        ));
        CACHED_SHAPES.put(DrawerType.X_4, x4);
    }

    private final DrawerType drawerType;
    private final DrawerWoodType woodType;

    public WoodDrawerBlock(DrawerWoodType woodType, DrawerType drawerType) {
        super(Material.WOOD);
        this.woodType = woodType;
        this.drawerType = drawerType;
        this.setRegistryName(woodType.getName() + "_" + drawerType.getSlots());
        this.setTranslationKey("functionalstoragelgeacy." + woodType.getName() + "_" + drawerType.getSlots());
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new DrawerTile(drawerType, woodType);
    }

    @Override
    public int getHitSlot(IBlockState state, World world, BlockPos pos, EntityPlayer player) {
        EnumFacing facing = state.getValue(FACING);
        List<AxisAlignedBB> hitBoxes = CACHED_SHAPES.getOrDefault(drawerType, Collections.emptyMap())
                .getOrDefault(facing, Collections.emptyList());

        if (hitBoxes.isEmpty()) return -1;

        // Ray trace to find which slot box was hit
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        double reach = 5.0D;
        Vec3d end = start.add(look.x * reach, look.y * reach, look.z * reach);

        // Check if the player is looking at the front face
        EnumFacing hitFace = getHitFace(state, world, pos, player);
        if (hitFace != facing) return -1;

        // Determine which slot based on hit position on the front face
        RayTraceResult result = this.collisionRayTrace(state, world, pos, start, end);
        if (result == null || result.typeOfHit != RayTraceResult.Type.BLOCK) return -1;

        Vec3d hitVec = result.hitVec.subtract(pos.getX(), pos.getY(), pos.getZ());

        for (int i = 0; i < hitBoxes.size(); i++) {
            AxisAlignedBB box = hitBoxes.get(i);
            if (hitVec.x >= box.minX - 0.001 && hitVec.x <= box.maxX + 0.001 &&
                hitVec.y >= box.minY - 0.001 && hitVec.y <= box.maxY + 0.001 &&
                hitVec.z >= box.minZ - 0.001 && hitVec.z <= box.maxZ + 0.001) {
                return i;
            }
        }

        return -1;
    }

    private EnumFacing getHitFace(IBlockState state, World world, BlockPos pos, EntityPlayer player) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        double reach = 5.0D;
        Vec3d end = start.add(look.x * reach, look.y * reach, look.z * reach);
        RayTraceResult result = this.collisionRayTrace(state, world, pos, start, end);
        if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) {
            return result.sideHit;
        }
        return EnumFacing.NORTH;
    }

    @Override
    public Collection<AxisAlignedBB> getHitBoxes(IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        return CACHED_SHAPES.getOrDefault(drawerType, Collections.emptyMap())
                .getOrDefault(facing, Collections.emptyList());
    }

    public DrawerType getDrawerType() {
        return drawerType;
    }

    public DrawerWoodType getWoodType() {
        return woodType;
    }
}
