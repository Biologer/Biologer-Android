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

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;

public class RegisterPrivacyPolicy extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_privacy_policy, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {

            Activity activity = getActivity();
            if (activity != null) {

                TextView textView = activity.findViewById(R.id.textView_privacy_policy_p2);
                textView.setText(getString(R.string.privacy_policy2, SettingsManager.getDatabaseName()));

                Button button = activity.findViewById(R.id.button_privacy_policy_back);
                button.setOnClickListener(view1 -> requireActivity().getSupportFragmentManager().popBackStack());
            }
        }
    }
}
