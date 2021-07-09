package org.biologer.biologer.adapters;

import android.util.Log;

import com.google.android.gms.common.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArrayHelper {

    private static final String TAG = "Biologer.ArrayHelper";

    public static int[] getArrayFromText(String observation_type_ids) {
        String[] strings = observation_type_ids.replace("[", "").replace("]", "").split(", ");
        ArrayList<Integer> temporary_array = new ArrayList<>();
        for (String string : strings) {
            if (!string.equals("")) {
                temporary_array.add(Integer.parseInt(string));
            }
        }

        int[] final_array = new int[temporary_array.size()];
        for (int i = 0; i < temporary_array.size(); i++) {
            final_array[i] = temporary_array.get(i);
        }
        return final_array;
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
