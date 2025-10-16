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
import org.biologer.biologer.databinding.FragmentDataLicense4Binding;

public class FragmentDataLicense4 extends Fragment {

    private FragmentDataLicense4Binding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDataLicense4Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        TextView textView = requireActivity().findViewById(R.id.textView_data_license_description_4);
        textView.setText(getString(R.string.temporary_closed_data_license_text, SettingsManager.getDatabaseName()));

        binding.buttonLicense4.setOnClickListener(view1 -> requireActivity().getSupportFragmentManager().popBackStack());
    }

}
