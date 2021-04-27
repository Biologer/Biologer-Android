package org.biologer.biologer.gui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import org.biologer.biologer.R;

public class PreferencesTaxaGroupsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "Biologer.Preferences";

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // This is a workaround in order to change background color of the fragment
        getListView().setBackgroundResource(R.color.fragment_background);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_taxa_groups, rootKey);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Log.d(TAG, "Loading preferences taxa groups fragment.");

        CheckBoxPreference checkAll = new CheckBoxPreference(getContext());
        checkAll.setTitle(getString(R.string.select_all_groups));
        checkAll.setSummary(getString(R.string.select_all_groups_desc));
        checkAll.setChecked(true);
        checkAll.setIconSpaceReserved(false);

        preferenceScreen.addPreference(checkAll);

    }
}
