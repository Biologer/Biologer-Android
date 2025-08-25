package org.biologer.biologer.gui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.biologer.biologer.R;

import java.util.Calendar;

public class FragmentDatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    public interface OnDateSelectedListener {
        void onDateSelected(int year, int month, int day);
    }

    private OnDateSelectedListener listener;
    private Calendar selectedDateFromBundle;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // Instantiate the OnDateSelectedListener so we can send events to the host
            listener = (OnDateSelectedListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw an exception
            throw new ClassCastException(context + " must implement OnDateSelectedListener");
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

        int year = selectedDateFromBundle.get(Calendar.YEAR);
        int month = selectedDateFromBundle.get(Calendar.MONTH);
        int day = selectedDateFromBundle.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(requireActivity(), this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        // Get the current date and time for comparison.
        Calendar now = Calendar.getInstance();

        // Create a new Calendar object for the date the user selected.
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, day);

        // Create a calendar for today with time fields zeroed out for date-only comparison.
        Calendar todayOnly = (Calendar) now.clone();
        todayOnly.set(Calendar.HOUR_OF_DAY, 0);
        todayOnly.set(Calendar.MINUTE, 0);
        todayOnly.set(Calendar.SECOND, 0);
        todayOnly.set(Calendar.MILLISECOND, 0);

        // Create a calendar for the selected day with time fields zeroed out.
        Calendar selectedDayOnly = (Calendar) selectedDate.clone();
        selectedDayOnly.set(Calendar.HOUR_OF_DAY, 0);
        selectedDayOnly.set(Calendar.MINUTE, 0);
        selectedDayOnly.set(Calendar.SECOND, 0);
        selectedDayOnly.set(Calendar.MILLISECOND, 0);

        // First check: Is the selected date in the future?
        if (selectedDayOnly.after(todayOnly)) {
            Toast.makeText(getActivity(), R.string.cannot_select_a_future_date, Toast.LENGTH_LONG).show();
            return;
        }

        // Second check: If the selected date is today, is the selected time in the future?
        if (selectedDayOnly.equals(todayOnly) && selectedDateFromBundle.after(now)) {
            Toast.makeText(getActivity(), R.string.cannot_select_a_future_time, Toast.LENGTH_LONG).show();
        } else {
            // The date and time are valid, so pass it back to the Activity.
            if (listener != null) {
                listener.onDateSelected(year, month, day);
            }
        }
    }

}