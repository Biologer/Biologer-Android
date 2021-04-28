package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import org.biologer.biologer.FetchTaxa;
import org.biologer.biologer.GetTaxaGroups;
import org.biologer.biologer.R;

public class PreferencesTaxaGroupsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "Biologer.Preferences";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String message = intent.getStringExtra("org.biologer.biologer.GetTaxaGroups.TASK_COMPLETED");
                if (message != null) {
                    Log.d(TAG, "Fetching taxonomic data returned the code: " + message);
                    if (message.equals("done")) {
                        Log.d(TAG, "Fetching successful");
                        populatePreferences();
                    }
                    if (message.equals("no_network")) {
                        Log.d(TAG, "No network");
                    }
                }
            }
        }
    };

    private void populatePreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        CheckBoxPreference checkAll = new CheckBoxPreference(getContext());
        checkAll.setTitle(getString(R.string.select_all_groups));
        checkAll.setSummary(getString(R.string.select_all_groups_desc));
        checkAll.setChecked(true);
        checkAll.setIconSpaceReserved(false);
        preferenceScreen.addPreference(checkAll);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // This is a workaround in order to change background color of the fragment
        getListView().setBackgroundResource(R.color.fragment_background);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_taxa_groups, rootKey);
        Log.d(TAG, "Starting fragment for taxa groups preferences.");

        // Fetch taxa groups from server
        final Intent getTaxaGroups = new Intent(getActivity(), GetTaxaGroups.class);
        getTaxaGroups.setAction(FetchTaxa.ACTION_START);
        Activity activity = getActivity();
        activity.startService(getTaxaGroups);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                    new IntentFilter("org.biologer.biologer.FetchTaxa.TASK_COMPLETED"));
        }
        Log.d(TAG, "Resuming Preferences Taxa Groups Fragment.");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        }
        Log.d(TAG, "Pausing Preferences Taxa Groups Fragment.");
    }
}
