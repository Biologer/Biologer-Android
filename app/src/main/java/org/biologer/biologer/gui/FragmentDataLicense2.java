package org.biologer.biologer.gui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.biologer.biologer.databinding.FragmentDataLicense2Binding;

public class FragmentDataLicense2 extends Fragment {

    private FragmentDataLicense2Binding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDataLicense2Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.buttonLicense2.setOnClickListener(view1 -> requireActivity().getSupportFragmentManager().popBackStack());
    }

}
