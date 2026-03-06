package com.xinyihl.functionalstoragelgeacy;

import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.client.gui.GuiDrawer;
import com.xinyihl.functionalstoragelgeacy.client.gui.GuiMFSUpgrade;
import com.xinyihl.functionalstoragelgeacy.container.ContainerDrawer;
import com.xinyihl.functionalstoragelgeacy.container.ContainerMFSUpgrade;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class GuiHandler implements IGuiHandler {

    public static final int GUI_DRAWER = 0;
    public static final int GUI_MFS_UPGRADE = 1;

    public static int encodeUpgradeGuiX(int x, int upgradeSlot) {
        return (x << 3) | (upgradeSlot & 7);
    }

    public static int decodeUpgradeSlot(int encodedX) {
        return encodedX & 7;
    }

    public static int decodeUpgradeX(int encodedX) {
        return encodedX >> 3;
    }

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = ID == GUI_MFS_UPGRADE ? new BlockPos(decodeUpgradeX(x), y, z) : new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        switch (ID) {
            case GUI_DRAWER:
                if (te instanceof ControllableDrawerTile) {
                    return new ContainerDrawer(player.inventory, (ControllableDrawerTile) te);
                }
                break;
            case GUI_MFS_UPGRADE:
                if (te instanceof ControllableDrawerTile) {
                    return new ContainerMFSUpgrade(player.inventory, (ControllableDrawerTile) te, decodeUpgradeSlot(x));
                }
                break;
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = ID == GUI_MFS_UPGRADE ? new BlockPos(decodeUpgradeX(x), y, z) : new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        switch (ID) {
            case GUI_DRAWER:
                if (te instanceof ControllableDrawerTile) {
                    return new GuiDrawer(new ContainerDrawer(player.inventory, (ControllableDrawerTile) te));
                }
                break;
            case GUI_MFS_UPGRADE:
                if (te instanceof ControllableDrawerTile) {
                    return new GuiMFSUpgrade(new ContainerMFSUpgrade(player.inventory, (ControllableDrawerTile) te, decodeUpgradeSlot(x)));
                }
                break;
        }
        return null;
    }
}
