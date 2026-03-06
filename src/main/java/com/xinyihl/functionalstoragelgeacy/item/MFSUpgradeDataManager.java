package com.xinyihl.functionalstoragelgeacy.item;

import net.minecraft.nbt.NBTTagCompound;

public class MFSUpgradeDataManager {

    private NBTTagCompound data = new NBTTagCompound();

    public boolean getBoolean(String key) {
        return data.getBoolean(key);
    }

    public void setBoolean(String key, boolean value) {
        data.setBoolean(key, value);
    }

    public int getInteger(String key) {
        return data.getInteger(key);
    }

    public void setInteger(String key, int value) {
        data.setInteger(key, value);
    }

    public String getString(String key) {
        return data.getString(key);
    }

    public void setString(String key, String value) {
        data.setString(key, value);
    }

    public NBTTagCompound getCompound(String key) {
        return data.getCompoundTag(key);
    }

    public void setCompound(String key, NBTTagCompound value) {
        data.setTag(key, value);
    }

    public NBTTagCompound serializeNBT() {
        return data.copy();
    }

    public void deserializeNBT(NBTTagCompound tag) {
        data = tag.copy();
    }
}
