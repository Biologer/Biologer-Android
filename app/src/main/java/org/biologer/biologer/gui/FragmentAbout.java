package org.biologer.biologer.gui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.databinding.FragmentAboutBinding;

public class FragmentAbout extends Fragment {

    private FragmentAboutBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();

        // Get the name of the database currently in use
        if (view != null) {
            binding.textViewDatabase.setText(SettingsManager.getDatabaseName());
        }

        // Add onClick to the biologer.org link
        binding.textViewUrl.setOnClickListener(view1 -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://biologer.org"));
            startActivity(browserIntent);
        });

    }
}
