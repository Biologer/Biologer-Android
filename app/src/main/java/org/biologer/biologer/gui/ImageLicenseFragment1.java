package org.biologer.biologer.gui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.biologer.biologer.R;

public class ImageLicenseFragment1 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.license_image_1, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Button button = requireActivity().findViewById(R.id.button_image_license1);
        button.setOnClickListener(view1 -> requireActivity().getSupportFragmentManager().popBackStack());
    }

}
