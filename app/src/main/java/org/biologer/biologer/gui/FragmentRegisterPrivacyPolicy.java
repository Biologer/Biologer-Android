package org.biologer.biologer.gui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.databinding.FragmentRegisterPrivacyPolicyBinding;

public class FragmentRegisterPrivacyPolicy extends Fragment {

    private FragmentRegisterPrivacyPolicyBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterPrivacyPolicyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {

            Activity activity = getActivity();
            if (activity != null) {

                binding.textViewPolicy.setText(getString(R.string.privacy_policy2, SettingsManager.getDatabaseName()));

                binding.buttonBack.setOnClickListener(view1 -> requireActivity().getSupportFragmentManager().popBackStack());
            }
        }
    }
}
