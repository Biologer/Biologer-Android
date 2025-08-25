package org.biologer.biologer.gui;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;
import android.widget.Toast;

import org.biologer.biologer.R;

import java.util.Calendar;

public class FragmentTimePicker extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    // Define an interface to communicate with the host Activity
    public interface OnTimeSelectedListener {
        void onTimeSelected(int hourOfDay, int minute);
    }

    private OnTimeSelectedListener listener;
    private Calendar selectedDateFromBundle;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (OnTimeSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTimeSelectedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Retrieve the Calendar object from the bundle
        if (getArguments() != null) {
            selectedDateFromBundle = (Calendar) getArguments().getSerializable("selected_date");
        }

        // If the date wasn't passed, default to the current time
        if (selectedDateFromBundle == null) {
            selectedDateFromBundle = Calendar.getInstance();
        }

        // Use the current time as the default values for the picker
        int hour = selectedDateFromBundle.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDateFromBundle.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    // Call the listener's method in onTimeSet()
    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Get the current time for comparison
        Calendar now = Calendar.getInstance();

        // Create a new Calendar object by combining the selected date
        // from the bundle with the new time from the picker.
        Calendar selectedDateTime = (Calendar) selectedDateFromBundle.clone();
        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        selectedDateTime.set(Calendar.MINUTE, minute);
        selectedDateTime.set(Calendar.SECOND, 0);
        selectedDateTime.set(Calendar.MILLISECOND, 0);

        if (selectedDateTime.after(now)) {
            Toast.makeText(getActivity(), R.string.cannot_select_a_future_time, Toast.LENGTH_LONG).show();
            // Ignore user input
        } else {
            // The time is valid, so pass it back to the Activity
            if (listener != null) {
                listener.onTimeSelected(hourOfDay, minute);
            }
        }
    }
}