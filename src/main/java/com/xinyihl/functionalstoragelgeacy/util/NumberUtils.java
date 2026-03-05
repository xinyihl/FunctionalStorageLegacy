package com.xinyihl.functionalstoragelgeacy.util;

import java.text.DecimalFormat;

public class NumberUtils {

    private static final DecimalFormat formatterWithUnits = new DecimalFormat("####0.#");
    private static final DecimalFormat blankFormatter = new DecimalFormat();

    public static String getFormattedNumber(int number) {
        return blankFormatter.format(number);
    }

    public static String getFormattedFluid(int number) {
        return blankFormatter.format(number) + "mb";
    }

    public static String getFormatedBigNumber(int number) {
        return getFormattedBigNumber(number);
    }

    public static String getFormattedBigNumber(long number) {
        if (number >= 1000000000L) {
            float numb = number / 1000000000F;
            return formatterWithUnits.format(numb) + "B";
        } else if (number >= 1000000L) {
            float numb = number / 1000000F;
            if (number > 100000000L) numb = Math.round(numb);
            return formatterWithUnits.format(numb) + "M";
        } else if (number >= 1000L) {
            float numb = number / 1000F;
            if (number > 100000L) numb = Math.round(numb);
            return formatterWithUnits.format(numb) + "K";
        }
        return String.valueOf(number);
    }

    public static String getFormatedFluidBigNumber(int number) {
        if (number < 1000) return number + " mB";
        if (number >= 1000000000) {
            float numb = number / 1000000000F;
            return formatterWithUnits.format(numb) + "M B";
        } else if (number >= 1000000) {
            float numb = number / 1000000F;
            if (number > 100000000) numb = Math.round(numb);
            return formatterWithUnits.format(numb) + "K B";
        } else if (number >= 1000) {
            float numb = number / 1000F;
            if (number > 100000) numb = Math.round(numb);
            return formatterWithUnits.format(numb) + " B";
        }
        return number + " B";
    }
}
