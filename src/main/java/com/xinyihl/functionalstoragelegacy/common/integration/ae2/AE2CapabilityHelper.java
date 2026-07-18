package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import ae2.api.AECapabilities;
import com.xinyihl.functionalstoragelegacy.api.storage.IBigFluidHandler;
import com.xinyihl.functionalstoragelegacy.api.storage.IBigItemHandler;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import net.minecraftforge.common.capabilities.Capability;

/** Direct references to Supergiant are isolated here for optional loading. */
public final class AE2CapabilityHelper {

    private AE2CapabilityHelper() {
    }

    public static boolean isStorageAccessor(Capability<?> capability) {
        return capability == AECapabilities.ME_STORAGE;
    }

    public static Object createAccessor(ControllableDrawerTile tile) {
        IBigItemHandler itemHandler = tile.getItemHandler();
        IBigFluidHandler fluidHandler = tile.getFluidHandler();
        return itemHandler == null && fluidHandler == null ? null : new DrawerMEStorage(itemHandler, fluidHandler);
    }
}
