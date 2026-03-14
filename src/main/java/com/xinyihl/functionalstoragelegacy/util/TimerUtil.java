package com.xinyihl.functionalstoragelegacy.util;

import net.minecraft.nbt.NBTTagCompound;

public class TimerUtil {


    public static final String TIMER_KEY = "generation_timer";
    public static final int FAIL_COOLDOWN_TICKS = 100;

    /**
     * @param nbt      升级物品的 NBT 数据
     * @param interval 正常生成间隔 (ticks)
     * @param action   需要执行的具体生成逻辑
     */
    public static void updateAndExecute(NBTTagCompound nbt, int interval, TickAction action) {
        if (!nbt.hasKey(TIMER_KEY)) {
            nbt.setInteger(TIMER_KEY, interval);
        }

        int timer = nbt.getInteger(TIMER_KEY) - 1;
        nbt.setInteger(TIMER_KEY, timer);

        if (timer <= 0) {
            boolean success = action.execute();

            if (success) {
                nbt.setInteger(TIMER_KEY, interval);
            } else {
                nbt.setInteger(TIMER_KEY, FAIL_COOLDOWN_TICKS);
            }
        }
    }


    @FunctionalInterface
    public interface TickAction {
        boolean execute();
    }
}
