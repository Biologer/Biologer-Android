package org.biologer.biologer.gui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.biologer.biologer.R;

public class FragmentLicense extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_licenses, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {

            Activity activity = getActivity();
            if (activity != null) {
                TextView textView_data1 = activity.findViewById(R.id.textView_data_license1);
                textView_data1.setOnClickListener(view1 -> {
                    Fragment fragment = new FragmentDataLicense1();
                    openFragment(fragment);
                });

                TextView textView_data2 = activity.findViewById(R.id.textView_data_license2);
                textView_data2.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentDataLicense2();
                    openFragment(fragment);
                });

                TextView textView_data3 = activity.findViewById(R.id.textView_data_license3);
                textView_data3.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentDataLicense3();
                    openFragment(fragment);
                });

                TextView textView_data4 = activity.findViewById(R.id.textView_data_license4);
                textView_data4.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentDataLicense4();
                    openFragment(fragment);
                });

                TextView textView_data5 = activity.findViewById(R.id.textView_data_license5);
                textView_data5.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentDataLicense5();
                    openFragment(fragment);
                });

                TextView textView_image1 = activity.findViewById(R.id.textView_image_license1);
                textView_image1.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentImageLicense1();
                    openFragment(fragment);
                });

                TextView textView_image2 = activity.findViewById(R.id.textView_image_license2);
                textView_image2.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentImageLicense2();
                    openFragment(fragment);
                });

                TextView textView_image3 = activity.findViewById(R.id.textView_image_license3);
                textView_image3.setOnClickListener(view12 -> {
                    Fragment fragment = new FragmentImageLicense3();
                    openFragment(fragment);
                });

                TextView textView_image4 = activity.findViewById(R.id.textView_image_license4);
                textView_image4.setOnClickListener(view12 -> {
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
