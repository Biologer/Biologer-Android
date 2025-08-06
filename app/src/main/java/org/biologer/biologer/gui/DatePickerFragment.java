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

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

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
            throw new ClassCastException(context.toString() + " must implement OnDateSelectedListener");
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

        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(requireActivity(), this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        // Get the current date for comparison
        Calendar now = Calendar.getInstance();

        // Date and time obtained from the activity
        Calendar selectedDateTime = (Calendar) selectedDateFromBundle.clone();
        selectedDateTime.set(Calendar.YEAR, year);
        selectedDateTime.set(Calendar.MONTH, month);
        selectedDateTime.set(Calendar.DAY_OF_MONTH, day);

        // Live only date in current calendar
        Calendar nowDay = (Calendar) now.clone();
        nowDay.set(Calendar.HOUR_OF_DAY, 0);
        nowDay.set(Calendar.MINUTE, 0);
        nowDay.set(Calendar.SECOND, 0);
        nowDay.set(Calendar.MILLISECOND, 0);

        // Live only date in the calendar from the bundle
        Calendar selectedDay = (Calendar) selectedDateFromBundle.clone();
        selectedDay.set(Calendar.HOUR_OF_DAY, 0);
        selectedDay.set(Calendar.MINUTE, 0);
        selectedDay.set(Calendar.SECOND, 0);
        selectedDay.set(Calendar.MILLISECOND, 0);

        // First check: Is the selected date in the future
        if (selectedDay.after(nowDay)) {
            Toast.makeText(getActivity(), R.string.cannot_select_a_future_date, Toast.LENGTH_LONG).show();
            return; // Ignore the user's input
        }

        // And if the date is not in the future, but time, give different warning...
        if (selectedDateTime.after(now)) {
            Toast.makeText(getActivity(), R.string.cannot_select_a_future_time, Toast.LENGTH_LONG).show();
            // Ignore user input
        } else {
            // The time is valid, so pass it back to the Activity
            if (listener != null) {
                listener.onDateSelected(year, month, day);
            }
        }
    }



}