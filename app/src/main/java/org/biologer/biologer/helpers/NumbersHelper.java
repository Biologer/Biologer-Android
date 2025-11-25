package org.biologer.biologer.helpers;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumbersHelper {

    /**
     * Mathematically rounds a double value to a specified number of decimal places.
     * @param value The double value to round.
     * @param decimalPlaces The number of decimal places to retain.
     * @return The rounded double value.
     */
    public static double roundDouble(double value, int decimalPlaces) {
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("Decimal places must be non-negative.");
        }
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(value * factor) / factor;
    }

    public static String formatValueEnglish(double value, int decimalPlaces) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
        String pattern = createPattern(decimalPlaces);
        DecimalFormat df = new DecimalFormat(pattern, symbols);
        return df.format(value);
    }

    public static String formatValueLocalised(double value, int decimalPlaces) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag(Localisation.getLocaleScript()));
        String pattern = createPattern(decimalPlaces);
        DecimalFormat df = new DecimalFormat(pattern, symbols);
        return df.format(value);
    }

    private static String createPattern(int decimalPlaces) {
        StringBuilder pattern = new StringBuilder("0.");
        for (int i = 0; i < decimalPlaces; i++) {
            pattern.append("0");
        }
        return pattern.toString();
    }
}
