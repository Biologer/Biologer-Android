package org.biologer.biologer.gui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;

public class DataLicenseFragment5 extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.license_data_5, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        TextView textView = requireActivity().findViewById(R.id.textView_data_license_description_5);
        textView.setText(getString(R.string.closed_data_license_text, SettingsManager.getDatabaseName()));

        Button button = requireActivity().findViewById(R.id.button_data_license5);
        button.setOnClickListener(view1 -> requireActivity().getSupportFragmentManager().popBackStack());
    }
}
