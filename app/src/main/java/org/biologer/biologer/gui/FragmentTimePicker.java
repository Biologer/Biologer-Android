package org.biologer.biologer.gui;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.format.DateFormat;
import android.widget.TimePicker;
import android.widget.Toast;

import org.biologer.biologer.R;
import org.biologer.biologer.adapters.ObservationViewModel;

import java.util.Calendar;

public class FragmentTimePicker extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Retrieve the Calendar object from the bundle
        Calendar calendarFromActivity = getCalendar();

        // Use the current time as the default values for the picker
        int hour = calendarFromActivity.get(Calendar.HOUR_OF_DAY);
        int minute = calendarFromActivity.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    // Call the listener's method in onTimeSet()
    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar now = Calendar.getInstance();

        // Clone current calendar from ViewModel (preserve date fields)
        Calendar selectedDateTime = (Calendar) getCalendar().clone();
        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        selectedDateTime.set(Calendar.MINUTE, minute);
        selectedDateTime.set(Calendar.SECOND, 0);
        selectedDateTime.set(Calendar.MILLISECOND, 0);

        // Strip time from both for date-only comparison
        Calendar todayOnly = (Calendar) now.clone();
        todayOnly.set(Calendar.HOUR_OF_DAY, 0);
        todayOnly.set(Calendar.MINUTE, 0);
        todayOnly.set(Calendar.SECOND, 0);
        todayOnly.set(Calendar.MILLISECOND, 0);

        Calendar selectedDayOnly = (Calendar) selectedDateTime.clone();
        selectedDayOnly.set(Calendar.HOUR_OF_DAY, 0);
        selectedDayOnly.set(Calendar.MINUTE, 0);
        selectedDayOnly.set(Calendar.SECOND, 0);
        selectedDayOnly.set(Calendar.MILLISECOND, 0);

        if (selectedDayOnly.after(todayOnly)) {
            // User somehow picked a future day (shouldn’t happen if DatePicker is used)
            Toast.makeText(requireActivity(), R.string.cannot_select_a_future_date, Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedDayOnly.equals(todayOnly) && selectedDateTime.after(now)) {
            // Same day, but time is in the future
            Toast.makeText(requireActivity(), R.string.cannot_select_a_future_time, Toast.LENGTH_LONG).show();
            return;
        }

        // Valid date & time and save to ViewModel
        ObservationViewModel observationViewModel =
                new ViewModelProvider(requireActivity()).get(ObservationViewModel.class);
        observationViewModel.setCalendar(selectedDateTime);
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