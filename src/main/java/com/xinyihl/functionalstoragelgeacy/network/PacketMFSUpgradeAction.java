package com.xinyihl.functionalstoragelgeacy.network;

import com.xinyihl.functionalstoragelgeacy.FunctionalStorageLgeacy;
import com.xinyihl.functionalstoragelgeacy.GuiHandler;
import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.container.ContainerMFSUpgrade;
import com.xinyihl.functionalstoragelgeacy.item.FilterConfiguration;
import com.xinyihl.functionalstoragelgeacy.item.MFSUpgradeItem;
import com.xinyihl.functionalstoragelgeacy.item.RefillUpgradeItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMFSUpgradeAction implements IMessage {

    private BlockPos pos;
    private int upgradeSlot;
    private Action action;
    private int value;

    public PacketMFSUpgradeAction() {
    }

    public PacketMFSUpgradeAction(BlockPos pos, int upgradeSlot, Action action, int value) {
        this.pos = pos;
        this.upgradeSlot = upgradeSlot;
        this.action = action;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        upgradeSlot = buf.readInt();
        action = Action.values()[buf.readInt()];
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(upgradeSlot);
        buf.writeInt(action.ordinal());
        buf.writeInt(value);
    }

    public enum Action {
        OPEN_UPGRADE_GUI,
        OPEN_DRAWER_GUI,
        CYCLE_DIRECTION,
        CYCLE_REDSTONE,
        CYCLE_REFILL_TARGET,
        TOGGLE_SELECTED_SLOT,
        TOGGLE_BLACKLIST,
        TOGGLE_MATCH_NBT
    }

    public static class Handler implements IMessageHandler<PacketMFSUpgradeAction, IMessage> {
        @Override
        public IMessage onMessage(PacketMFSUpgradeAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            IThreadListener listener = player.getServerWorld();
            listener.addScheduledTask(() -> handle(player, message));
            return null;
        }

        private void handle(EntityPlayerMP player, PacketMFSUpgradeAction message) {
            if (message.action == Action.OPEN_DRAWER_GUI) {
                player.openGui(FunctionalStorageLgeacy.INSTANCE, GuiHandler.GUI_DRAWER, player.world, message.pos.getX(), message.pos.getY(), message.pos.getZ());
                return;
            }

            if (message.action == Action.OPEN_UPGRADE_GUI) {
                player.openGui(FunctionalStorageLgeacy.INSTANCE, GuiHandler.GUI_MFS_UPGRADE, player.world,
                        GuiHandler.encodeUpgradeGuiX(message.pos.getX(), message.upgradeSlot), message.pos.getY(), message.pos.getZ());
                return;
            }

            TileEntity te = player.world.getTileEntity(message.pos);
            if (!(te instanceof ControllableDrawerTile)) {
                return;
            }

            ControllableDrawerTile tile = (ControllableDrawerTile) te;
            if (message.upgradeSlot < 0 || message.upgradeSlot >= tile.getUtilityUpgrades().getSlots()) {
                return;
            }

            ItemStack stack = tile.getUtilityUpgrades().getStackInSlot(message.upgradeSlot);
            if (stack.isEmpty() || !(stack.getItem() instanceof MFSUpgradeItem)) {
                return;
            }

            MFSUpgradeItem upgrade = (MFSUpgradeItem) stack.getItem();
            switch (message.action) {
                case CYCLE_DIRECTION:
                    if (upgrade.hasDirection()) {
                        MFSUpgradeItem.setRelativeDirection(stack, MFSUpgradeItem.getRelativeDirection(stack).next());
                    }
                    break;
                case CYCLE_REDSTONE:
                    MFSUpgradeItem.setRedstoneMode(stack, MFSUpgradeItem.getRedstoneMode(stack).next());
                    break;
                case CYCLE_REFILL_TARGET:
                    if (stack.getItem() instanceof RefillUpgradeItem) {
                        RefillUpgradeItem.setRefillTarget(stack, RefillUpgradeItem.getRefillTarget(stack).next());
                    }
                    break;
                case TOGGLE_SELECTED_SLOT:
                    if (tile.getItemHandler() != null) {
                        MFSUpgradeItem.toggleSelectedSlot(stack, message.value, Math.min(4, tile.getItemHandler().getSlots()));
                    }
                    break;
                case TOGGLE_BLACKLIST: {
                    FilterConfiguration configuration = MFSUpgradeItem.getFilterConfiguration(stack);
                    configuration.setBlacklist(!configuration.isBlacklist());
                    MFSUpgradeItem.setFilterConfiguration(stack, configuration);
                    break;
                }
                case TOGGLE_MATCH_NBT: {
                    FilterConfiguration configuration = MFSUpgradeItem.getFilterConfiguration(stack);
                    configuration.setMatchNBT(!configuration.isMatchNBT());
                    MFSUpgradeItem.setFilterConfiguration(stack, configuration);
                    break;
                }
                default:
                    break;
            }

            if (player.openContainer instanceof ContainerMFSUpgrade) {
                ((ContainerMFSUpgrade) player.openContainer).saveUpgradeData();
            }
            tile.markDirty();
            tile.sendUpdatePacket();
        }
    }
}
