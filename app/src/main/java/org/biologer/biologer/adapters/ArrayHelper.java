package org.biologer.biologer.adapters;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArrayHelper {

    private static final String TAG = "Biologer.ArrayHelper";

    public static int[] getArrayFromText(String string) {

        String[] strings = string.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");

        // If observation types are not retrieved from server we need to handle this.
        // In those cases we save „[0]“ in SQL.
        int[] ints;
        if (strings[0].equals("0")) {
            ints = new int[1];
            ints[0] = 1;
        }

        // If everything is OK
        else {
            ints = new int[strings.length];

            for (int i = 0; i < strings.length; i++) {
                try {
                    ints[i] = Integer.parseInt(strings[i]);
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "Wrong ID number in Observation Types: " + strings[i]);
                }
            }
        }

        return ints;
    }

    public static int[] removeFromArray(int[] observation_type_ids, int id) {
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

    public static int[] insertIntoArray(int[] observation_type_ids, int new_id) {
        if (observation_type_ids == null) {
            int[] new_array = {new_id};
            Log.d(TAG, "The complete array of tag IDs looks like this: " + Arrays.toString(new_array));
            return new_array;
        } else {
            int len = observation_type_ids.length;
            int[] new_array = new int[len + 1];
            System.arraycopy(observation_type_ids, 0, new_array, 0, len);
            new_array[len] = new_id;
            Log.d(TAG, "The complete array of tag IDs looks like this: " + Arrays.toString(new_array));
            return new_array;
        }
    }

    public static boolean arrayContainsNumber(final int[] array, final int key) {
        boolean value = ArrayUtils.contains(array, key);
        Log.d(TAG, "Array " + Arrays.toString(array) + " is compared against number " + key + " and returned " + value);
        return value;
    }
}
