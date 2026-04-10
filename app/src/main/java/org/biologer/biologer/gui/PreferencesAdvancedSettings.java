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
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.biologer.biologer.R;
import org.biologer.biologer.network.UpdateTaxa;

import java.util.Objects;

public class PreferencesAdvancedSettings extends PreferenceFragmentCompat {
    private static final String TAG = "Biologer.PreferencesA";
    private Preference preferenceButton;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                // This code is used to listen if download is successfully completed.
                if (Objects.equals(intent.getAction(), UpdateTaxa.TASK_COMPLETED)) {
                    String message = intent.getStringExtra(UpdateTaxa.EXTRA_TASK_COMPLETED);
                    if (message != null) {
                        Log.d(TAG, "Fetching taxonomic data returned the code: " + message);
                        if (message.equals("success")) {
                            Log.d(TAG, "Re-enabling preferences entry for fetching taxa.");
                            preferenceButton.setEnabled(true);
                            preferenceButton.setSummary(getString(R.string.update_taxa_desc));
                        } else if (message.equals("failed")) {
                            preferenceButton.setEnabled(true);
                            preferenceButton.setSummary(getString(R.string.update_taxa_error));
                        }
                    }
                }

                // This code is used to listen how much data is downloaded (in %)
                else if (Objects.equals(intent.getAction(), UpdateTaxa.TASK_PERCENT)) {
                    int percent = intent.getIntExtra(UpdateTaxa.EXTRA_TASK_PERCENT, 0);
                    if (percent == 0) {
                        preferenceButton.setEnabled(true);
                        preferenceButton.setSummary(getString(R.string.update_taxa_desc));
                    } else {
                        preferenceButton.setEnabled(false);
                        preferenceButton.setSummary(getString(R.string.updating_taxa_be_patient) +
                                " " + getString(R.string.currently_downloaded) + " " + percent + "%.");
                    }
                }
            }
        }
    };


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // This is a workaround in order to change background color of the fragment
        getListView().setBackgroundResource(R.color.fragment_background);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_advanced);
        this.getPreferenceScreen();
        Log.d(TAG, "Starting fragment for account setup preferences.");

        // Add button fot taxa sync process
        preferenceButton = findPreference("taxa_button");
        toggleFetchTaxaButton(preferenceButton);
        preferenceButton.setOnPreferenceClickListener(preference -> {
            // Disable the button first
            preferenceButton.setEnabled(false);
            preferenceButton.setSummary(getString(R.string.updating_taxa_be_patient));
            // Start the service for fetching taxa
            Activity activity_fetch = getActivity();
            final Intent fetchTaxa = new Intent(activity_fetch, UpdateTaxa.class);
            fetchTaxa.setAction(UpdateTaxa.ACTION_DOWNLOAD_FROM_FIRST);
            if (activity_fetch != null) {
                activity_fetch.startService(fetchTaxa);
            }
            return true;
        });

        ListPreference sortObservations = findPreference("sort_observations");
        if (sortObservations != null) {
            getObservations(sortObservations);
            sortObservations.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d(TAG, "Sort observations changed to: " + newValue);
                if (getActivity() != null) {
                    Intent intent = new Intent("SORT_OBSERVATIONS_CHANGED");
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                }
                return true;
            });
        }

        ListPreference autoDownload = findPreference("auto_download");
        if (autoDownload != null) {
            getAutoDownload(autoDownload);
        }

        CheckBoxPreference showUploaded = findPreference("show_uploaded");
        if (showUploaded != null) {
            showUploaded.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d(TAG, "Sort observations changed to: " + newValue);
                if (getActivity() != null) {
                    Intent intent = new Intent("SHOW_UPLOADED_CHANGED");
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                }
                return true;
            });
        }

    }

    private void toggleFetchTaxaButton(Preference preference) {
        // If already fetching taxa disable the fetch taxa button
        if (preference != null) {
            if (UpdateTaxa.isInstanceCreated()) {
                preference.setEnabled(false);
                preference.setSummary(getString(R.string.updating_taxa_be_patient));
            } else {
                preference.setEnabled(true);
                preference.setSummary(getString(R.string.update_taxa_desc));
            }
        }
    }

    private void getObservations(ListPreference sortObservations) {
        CharSequence[] entries = {getString(R.string.sort_by_observation_time), getString(R.string.sort_by_species_name)};
        CharSequence[] entryValues = {"time", "name"};
        sortObservations.setEntries(entries);
        sortObservations.setDefaultValue("0");
        sortObservations.setEntryValues(entryValues);
    }

    private void getAutoDownload(ListPreference listPreference) {
        CharSequence[] entries = {getString(R.string.only_wifi), getString(R.string.any_network), getString(R.string.always_ask)};
        CharSequence[] entryValues = {"wifi", "all", "ask"};
        listPreference.setEntries(entries);
        listPreference.setDefaultValue("0");
        listPreference.setEntryValues(entryValues);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(UpdateTaxa.TASK_COMPLETED);
            filter.addAction(UpdateTaxa.TASK_PERCENT);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, filter);
        }
        Log.d(TAG, "Resuming Preferences Fragment.");
        toggleFetchTaxaButton(preferenceButton);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        }
        Log.d(TAG, "Pausing Preferences Fragment.");
    }

}
