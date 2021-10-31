package org.biologer.biologer.gui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.biologer.biologer.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterPrivacyPolicy extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register_privacy_policy, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Format the text within a paragraph with nice line breaks
        String text = getString(R.string.privacy_policy);
        SpannableString spannableString = new SpannableString(text);
        Matcher matcher = Pattern.compile("\n\n").matcher(text);
        while (matcher.find()) {
            spannableString.setSpan(new AbsoluteSizeSpan(12, true), matcher.start() + 1, matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        View view = getView();
        if (view != null) {

            Activity activity = getActivity();
            if (activity != null) {
                Button button = activity.findViewById(R.id.button_privacy_policy_back);
                button.setOnClickListener(view1 -> requireActivity().getSupportFragmentManager().popBackStack());
            }
        }
    }
}
