package com.xinyihl.functionalstoragelegacy.inventory.item;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DrawerStackCapabilityProvider implements ICapabilityProvider {

    @Nullable
    private final IItemHandler handler;

    public DrawerStackCapabilityProvider(@Nullable IItemHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && handler != null;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && handler != null) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handler);
        }
        return null;
    }
}
