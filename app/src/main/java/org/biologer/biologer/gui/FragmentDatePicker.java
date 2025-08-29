package org.biologer.biologer.gui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import org.biologer.biologer.R;
import org.biologer.biologer.adapters.ObservationViewModel;

import java.util.Calendar;

public class FragmentDatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Retrieve the Calendar object from ViewModel
        Calendar calendarFromActivity = getCalendar();

        int year = calendarFromActivity.get(Calendar.YEAR);
        int month = calendarFromActivity.get(Calendar.MONTH);
        int day = calendarFromActivity.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(requireActivity(), this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        // Get the current date and time for comparison.
        Calendar now = Calendar.getInstance();

        // Create a new Calendar object for the date the user selected.
        Calendar selectedDate = (Calendar) getCalendar().clone();
        selectedDate.set(Calendar.YEAR, year);
        selectedDate.set(Calendar.MONTH, month);
        selectedDate.set(Calendar.DAY_OF_MONTH, day);

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
        if (selectedDayOnly.equals(todayOnly) && selectedDate.after(now)) {
            Toast.makeText(getActivity(), R.string.cannot_select_a_future_time, Toast.LENGTH_LONG).show();
        } else {
            // The date and time are valid, so pass it back to the Activity through ViewModel.
            ObservationViewModel observationViewModel =
                    new ViewModelProvider(requireActivity()).get(ObservationViewModel.class);
            observationViewModel.setCalendar(selectedDate);
        }
    }

    public Calendar getCalendar() {
        ObservationViewModel observationViewModel =
                new ViewModelProvider(requireActivity()).get(ObservationViewModel.class);
        Calendar calendar = observationViewModel.getCalendar().getValue();

        if (calendar == null) {
            calendar = Calendar.getInstance();
        }

        return calendar;
    }
}