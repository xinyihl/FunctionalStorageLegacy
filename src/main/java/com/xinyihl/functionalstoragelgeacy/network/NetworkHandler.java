package com.xinyihl.functionalstoragelgeacy.network;

import com.xinyihl.functionalstoragelgeacy.Tags;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {

    public static SimpleNetworkWrapper CHANNEL;
    private static int packetId = 0;

    public static void init() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);
        CHANNEL.registerMessage(PacketMFSUpgradeAction.Handler.class, PacketMFSUpgradeAction.class, packetId++, Side.SERVER);
    }
}
