package org.biologer.biologer.gui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.databinding.FragmentDataLicense3Binding;

public class FragmentDataLicense3 extends Fragment {

    private FragmentDataLicense3Binding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDataLicense3Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        TextView textView = requireActivity().findViewById(R.id.textView_data_license_description_3);
        textView.setText(getString(R.string.partially_open_data_license_text, SettingsManager.getDatabaseName()));

        binding.buttonLicense3.setOnClickListener(view1 -> requireActivity().getSupportFragmentManager().popBackStack());
    }

}
