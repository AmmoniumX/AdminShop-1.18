package com.ammonium.adminshop.money;

import com.ammonium.adminshop.setup.Config;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

public class MoneyFormat {
    public static final double FORMAT_START = 1000000;
//    private final DecimalFormat decimalFormat = new DecimalFormat("#,###");

    public enum FormatType {
        FULL,
        SHORT,
        RAW
    }

    private MoneyFormat() {
//        decimalFormat.setRoundingMode(RoundingMode.DOWN);
//        decimalFormat.getNumberInstance(Locale.US);
    }

    // Format value based on config
    public static String cfgformat(long value) {
        boolean displayFormat = (Config.displayFormat.get() != null) ? Config.displayFormat.get() : true;
        FormatType formattype = displayFormat ? FormatType.SHORT : FormatType.FULL;
        return format(value, formattype, FormatType.RAW);
//        return format(value, FormatType.SHORT);
    }

    // Format is always X irrespective of shift
    public static String forcedFormat(long value, FormatType forced) {
        return format(value, forced, forced);
    }

    // If noShift not specified -> noShift = RAW
    public static String format(long value, FormatType noShift) {
        return format(value, noShift, FormatType.RAW);
    }

    public static String format(long value, FormatType noShift, FormatType onShift) {
        NumberName name = NumberName.findName(value);
        if(name == null || Math.abs(value) < FORMAT_START) return NumberFormat.getNumberInstance(Locale.US).format(value);
        return Screen.hasShiftDown() ? doFormat(value, name, onShift) : doFormat(value, name, noShift);
    }

    public static String doFormat(long value, NumberName name, FormatType formattype) {
//        AdminShop.LOGGER.debug("Doing format "+value+", "+name+", "+formattype);
        if (formattype == FormatType.SHORT) {
//            AdminShop.LOGGER.debug("Short Format: "+getShort(value)+" "+name.getName(true));
            return getShort(value) + name.getName(true);
        }
        else if (formattype == FormatType.FULL) {
//            AdminShop.LOGGER.debug("Full Format: "+getShort(value)+" "+name.getName(false));
            return getShort(value) + String.format(" %s", name.getName(false));
        }
        else {
//            AdminShop.LOGGER.debug("Number Format: "+NumberFormat.getNumberInstance(Locale.US).format(value));
            return NumberFormat.getNumberInstance(Locale.US).format(value);
        }
    }

    public static String getShort(long value) {
        boolean isNegative = value < 0;
        String str = String.valueOf(Math.abs(value));
        int len = str.length();

        if (len < 4) {
            return isNegative ? "-" + str : str;
        }

        String sig = str.substring(0, len % 3 == 0 ? 3 : len % 3);
        String dec = str.substring(sig.length(), Math.min(sig.length() + 2, len));

        String result = String.format("%s.%s", sig, dec);
        return isNegative ? "-" + result : result;
    }



    public enum NumberName {
        MILLION(1e6, "M"),
        BILLION(1e9, "B"),
        TRILLION(1e12, "T"),
        QUADRILLION(1e15, "Qa"),
        QUINTILLION(1e18, "Qi"),
        SEXTILLION(1e21, "Sx"),
        SEPTILLION(1e24, "Sp"),
        OCTILLION(1e27, "O"),
        NONILLION(1e30, "N"),
        DECILLION(1e33, "D"),
        UNDECILLION(1e36, "U"),
        DUODECILLION(1e39, "Du"),
        TREDECILLION(1e42, "Tr"),
        QUATTUORDECILLION(1e45, "Qt"),
        QUINDECILLION(1e48, "Qd"),
        SEXDECILLION(1e51, "Sd"),
        SEPTENDECILLION( 1e54, "St"),
        OCTODECILLION(1e57, "Oc"),
        NOVEMDECILLION(1e60, "No");

        public static final NumberName[] VALUES = values();

        private final double value;
        private final String shortName;
        NumberName(double val, String shortName) {
            this.value  = val;
            this.shortName = shortName;
        }

        public String getName() {
            return getName(false);
        }

        public String getName(boolean getShort) {
            return getShort ? shortName : (name().charAt(0) + name().toLowerCase(Locale.US).substring(1)).trim();
        }

        public double getValue() {return value;}

        static @Nullable NumberName findName(long value) {
            return Arrays.stream(VALUES).filter(v -> Math.abs(value) >= v.getValue()).reduce((first, second) -> second).orElse(null);
        }

    }
}