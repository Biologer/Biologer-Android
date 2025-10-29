package org.biologer.biologer.helpers;

import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class ArrayHelper {

    private static final String TAG = "Biologer.ArrayHelper";

    public static int[] getArrayFromText(String string) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (string == null || string.trim().isEmpty()) {
                return new int[0];
            }

            // Split the string and map each part to an integer
            try {
                return Arrays.stream(string.replaceAll("[\\[\\]\\s]", "").split(","))
                        .mapToInt(Integer::parseInt)
                        .toArray();
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse array from string: " + string, e);
                return null;
            }
        } else {
            String[] strings = string
                    .replaceAll("\\[", "")
                    .replaceAll("\\]", "")
                    .replaceAll("\\s", "")
                    .split(",");

            int[] ints = new int[strings.length];

            for (int i = 0; i < strings.length; i++) {
                try {
                    ints[i] = Integer.parseInt(strings[i]);
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "Wrong ID number in Observation Types: " + strings[i]);
                    return null;
                }
            }

            return ints;
        }
    }

    public static long[] listToArray(List<Long> list) {
        if (list != null) {
            long[] result = new long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i);
            }
            return result;
        }
        return null;
    }

}
