package com.xinyihl.functionalstoragelegacy.proxy;

import com.xinyihl.functionalstoragelegacy.Tags;
import com.xinyihl.functionalstoragelegacy.block.tile.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        // Register TileEntities
        GameRegistry.registerTileEntity(DrawerTile.class, new ResourceLocation(Tags.MOD_ID, "drawer"));
        GameRegistry.registerTileEntity(CompactingDrawerTile.class, new ResourceLocation(Tags.MOD_ID, "compacting_drawer"));
        GameRegistry.registerTileEntity(SimpleCompactingDrawerTile.class, new ResourceLocation(Tags.MOD_ID, "simple_compacting_drawer"));
        GameRegistry.registerTileEntity(FluidDrawerTile.class, new ResourceLocation(Tags.MOD_ID, "fluid_drawer"));
        GameRegistry.registerTileEntity(EnderDrawerTile.class, new ResourceLocation(Tags.MOD_ID, "ender_drawer"));
        GameRegistry.registerTileEntity(ArmoryCabinetTile.class, new ResourceLocation(Tags.MOD_ID, "armory_cabinet"));
        GameRegistry.registerTileEntity(StorageControllerTile.class, new ResourceLocation(Tags.MOD_ID, "storage_controller"));
        GameRegistry.registerTileEntity(ControllerExtensionTile.class, new ResourceLocation(Tags.MOD_ID, "controller_extension"));
    }

    public void init(FMLInitializationEvent event) {
    }

    public void postInit(FMLPostInitializationEvent event) {
    }
}
