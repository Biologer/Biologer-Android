package org.biologer.biologer.services;

import android.os.Build;
import android.util.Log;

import com.google.android.gms.common.util.ArrayUtils;

import java.util.ArrayList;
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

    public static int[] removeFromArray(int[] observation_type_ids, int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (observation_type_ids == null) {
                return null;
            }
            return Arrays.stream(observation_type_ids)
                    .filter(val -> val != id)
                    .toArray();
        } else {
            List<Integer> new_list = new ArrayList<>();
            for (int observation_type_id : observation_type_ids) {
                if (observation_type_id != id) {
                    new_list.add(observation_type_id);
                }
            }
            int length = new_list.size();
            int[] new_array = new int[length];
            for (int i = 0; i < length; i++) {
                new_array[i] = new_list.get(i);
            }
            return new_array;
        }
    }

    public static int[] insertIntoArray(int[] observation_type_ids, int new_id) {
        if (observation_type_ids == null) {
            return new int[]{new_id};
        }

        int len = observation_type_ids.length;
        int[] new_array = Arrays.copyOf(observation_type_ids, len + 1);
        new_array[len] = new_id;
        return new_array;
    }

    public static boolean arrayContainsNumber(final int[] array, final int key) {
        boolean value = ArrayUtils.contains(array, key);
        Log.d(TAG, "Array " + Arrays.toString(array) + " is compared against number " + key + " and returned " + value);
        return value;
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

    public static int[] intListToArray(List<Integer> list) {
        if (list != null) {
            int[] result = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i);
            }
            return result;
        }
        return null;
    }

}
