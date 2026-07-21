package com.xinyihl.functionalstoragelegacy.common.storage;

import com.xinyihl.functionalstoragelegacy.common.block.DrawerWoodType;
import com.xinyihl.functionalstoragelegacy.common.tile.WoodDrawerTile;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DrawerLayoutNbtTest {

    @BeforeAll
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void itemTileDataUsesStableStringIdentifiers() {
        WoodDrawerTile tile = new WoodDrawerTile(DrawerLayout.X_2, DrawerWoodType.SPRUCE);

        NBTTagCompound data = tile.saveTileToNBT();

        assertEquals("1x2", data.getString("DrawerLayout"));
        assertEquals("spruce", data.getString("DrawerWood"));
        assertFalse(data.hasKey("DrawerType"));
        assertFalse(data.hasKey("WoodType"));

        WoodDrawerTile restored = new WoodDrawerTile();
        restored.loadTileFromNBT(data);
        assertEquals(DrawerLayout.X_2, restored.getDrawerLayout());
        assertEquals(DrawerWoodType.SPRUCE, restored.getWoodType());
    }

    @Test
    public void legacyOrdinalKeysAreIgnored() {
        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setInteger("DrawerType", DrawerLayout.X_4.ordinal());
        legacy.setInteger("WoodType", DrawerWoodType.DARK_OAK.ordinal());

        WoodDrawerTile tile = new WoodDrawerTile();
        tile.loadTileFromNBT(legacy);

        assertEquals(DrawerLayout.X_1, tile.getDrawerLayout());
        assertEquals(DrawerWoodType.OAK, tile.getWoodType());
    }
}
