package com.xinyihl.functionalstoragelgeacy.util;

import net.minecraft.util.EnumFacing;

public enum RelativeDirection {
    FRONT,
    BACK,
    LEFT,
    RIGHT,
    UP,
    DOWN;

    public EnumFacing getAbsolute(EnumFacing drawerFacing) {
        switch (this) {
            case BACK:
                return drawerFacing.getOpposite();
            case LEFT:
                return drawerFacing.rotateY();
            case RIGHT:
                return drawerFacing.rotateYCCW();
            case UP:
                return EnumFacing.UP;
            case DOWN:
                return EnumFacing.DOWN;
            case FRONT:
            default:
                return drawerFacing;
        }
    }

    public RelativeDirection next() {
        RelativeDirection[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String getTranslationKey() {
        return "item.functionalstoragelgeacy.mfs_upgrade.direction." + name().toLowerCase();
    }
}
