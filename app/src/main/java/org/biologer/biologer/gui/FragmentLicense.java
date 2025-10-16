package org.biologer.biologer.gui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.biologer.biologer.R;
import org.biologer.biologer.databinding.FragmentLicenseBinding;

public class FragmentLicense extends Fragment {

    private FragmentLicenseBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLicenseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {

            Activity activity = getActivity();
            if (activity != null) {
                binding.textViewDataLicense1.setOnClickListener(view1 -> {
                    Fragment fragment = new FragmentDataLicense1();
                    openFragment(fragment);
                });

                binding.textViewDataLicense2.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentDataLicense2();
                    openFragment(fragment);
                });

                binding.textViewDataLicense3.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentDataLicense3();
                    openFragment(fragment);
                });

                binding.textViewDataLicense4.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentDataLicense4();
                    openFragment(fragment);
                });

                binding.textViewDataLicense5.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentDataLicense5();
                    openFragment(fragment);
                });

                binding.textViewImageLicense1.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentImageLicense1();
                    openFragment(fragment);
                });

                binding.textViewImageLicense2.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentImageLicense2();
                    openFragment(fragment);
                });

                binding.textViewImageLicense3.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentImageLicense3();
                    openFragment(fragment);
                });

                binding.textViewImageLicense4.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentImageLicense4();
                    openFragment(fragment);
                });

                Button button = activity.findViewById(R.id.button_licenses);
                button.setOnClickListener(view1 -> requireActivity().getSupportFragmentManager().popBackStack());

            }
        }
    }

    private void openFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        int containerGroupID = ((ViewGroup) requireView().getParent()).getId();
        fragmentTransaction.replace(containerGroupID, fragment);
        fragmentTransaction.addToBackStack("License fragment");
        fragmentTransaction.commit();
    }
}
