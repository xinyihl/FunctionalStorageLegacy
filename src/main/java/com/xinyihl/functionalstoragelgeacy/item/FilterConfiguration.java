package com.xinyihl.functionalstoragelgeacy.item;

import com.xinyihl.functionalstoragelgeacy.inventory.BigInventoryHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;

public class FilterConfiguration {

    private static final String TAG_FILTERS = "Filters";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_STACK = "Stack";
    private static final String TAG_BLACKLIST = "Blacklist";
    private static final String TAG_MATCH_NBT = "MatchNBT";

    private final ItemStack[] filters = new ItemStack[9];
    private boolean blacklist;
    private boolean matchNBT;

    public FilterConfiguration() {
        for (int i = 0; i < filters.length; i++) {
            filters[i] = ItemStack.EMPTY;
        }
    }

    public ItemStack getFilter(int slot) {
        if (slot < 0 || slot >= filters.length) {
            return ItemStack.EMPTY;
        }
        return filters[slot];
    }

    public void setFilter(int slot, @Nonnull ItemStack stack) {
        if (slot < 0 || slot >= filters.length) {
            return;
        }
        ItemStack copy = stack.copy();
        if (!copy.isEmpty()) {
            copy.setCount(1);
        }
        filters[slot] = copy;
    }

    public int getSize() {
        return filters.length;
    }

    public boolean isBlacklist() {
        return blacklist;
    }

    public void setBlacklist(boolean blacklist) {
        this.blacklist = blacklist;
    }

    public boolean isMatchNBT() {
        return matchNBT;
    }

    public void setMatchNBT(boolean matchNBT) {
        this.matchNBT = matchNBT;
    }

    public boolean hasAnyFilter() {
        for (ItemStack filter : filters) {
            if (!filter.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean test(@Nonnull ItemStack stack) {
        if (!hasAnyFilter()) {
            return true;
        }

        boolean matched = false;
        for (ItemStack filter : filters) {
            if (filter.isEmpty()) {
                continue;
            }
            boolean same = matchNBT
                    ? BigInventoryHandler.areItemStacksEqual(filter, stack)
                    : filter.getItem() == stack.getItem() && filter.getMetadata() == stack.getMetadata();
            if (same) {
                matched = true;
                break;
            }
        }

        return blacklist ? !matched : matched;
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < filters.length; i++) {
            ItemStack filter = filters[i];
            if (filter.isEmpty()) {
                continue;
            }
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger(TAG_SLOT, i);
            entry.setTag(TAG_STACK, filter.writeToNBT(new NBTTagCompound()));
            list.appendTag(entry);
        }
        tag.setTag(TAG_FILTERS, list);
        tag.setBoolean(TAG_BLACKLIST, blacklist);
        tag.setBoolean(TAG_MATCH_NBT, matchNBT);
        return tag;
    }

    public void deserializeNBT(NBTTagCompound tag) {
        for (int i = 0; i < filters.length; i++) {
            filters[i] = ItemStack.EMPTY;
        }

        NBTTagList list = tag.getTagList(TAG_FILTERS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int slot = entry.getInteger(TAG_SLOT);
            if (slot >= 0 && slot < filters.length) {
                filters[slot] = new ItemStack(entry.getCompoundTag(TAG_STACK));
            }
        }

        blacklist = tag.getBoolean(TAG_BLACKLIST);
        matchNBT = tag.getBoolean(TAG_MATCH_NBT);
    }
}
