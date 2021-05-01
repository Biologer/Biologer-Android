package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import android.util.Log;
import android.view.View;

import org.biologer.biologer.FetchTaxa;
import org.biologer.biologer.GetTaxaGroups;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.UpdateLicenses;

public class PreferencesFragment extends PreferenceFragmentCompat {

    private static final String TAG = "Biologer.Preferences";
    private Preference preferenceButton;
    Fragment fragment;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String message = intent.getStringExtra("org.biologer.biologer.FetchTaxa.TASK_COMPLETED");
                if (message != null) {
                    Log.d(TAG, "Fetching taxonomic data returned the code: " + message);
                    if (message.equals("fetched") || message.equals("paused") || message.equals("canceled")) {
                        Log.d(TAG, "Re-enabling preferences entry for fetching taxa.");
                        preferenceButton.setEnabled(true);
                        preferenceButton.setSummary(getString(R.string.update_taxa_desc));
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
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Log.d(TAG, "Loading preferences fragment.");

        // Fetch taxa groups from server
        ConnectivityManager connectivitymanager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivitymanager.getActiveNetworkInfo().isConnected()) {
            final Intent getTaxaGroups = new Intent(getActivity(), GetTaxaGroups.class);
            Activity getGroups = getActivity();
            getGroups.startService(getTaxaGroups);
        }

        PreferenceScreen taxaGroups = findPreference("species_groups");
        taxaGroups.setOnPreferenceClickListener(preference -> {
            Log.d(TAG, "Preferences for taxa groups clicked.");
            fragment = new PreferencesTaxaGroupsFragment();
            FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.content_frame, fragment);
            fragmentTransaction.addToBackStack("new fragment");
            fragmentTransaction.commit();
            return true;
        });

        setPreferenceScreen(preferenceScreen);

        ListPreference dataLicense = findPreference("data_license");
        ListPreference imageLicense = findPreference("image_license");
        ListPreference autoDownload = findPreference("auto_download");

        if (dataLicense != null || imageLicense != null) {
            getDataLicences(dataLicense);
            getImageLicences(imageLicense);
        }

        if (autoDownload != null) {
            getAutoDownload(autoDownload);
        }

        // Add button fot taxa sync process
        preferenceButton = findPreference("taxa_button");

        toggleFetchTaxaButton(preferenceButton);

        preferenceButton.setOnPreferenceClickListener(preference -> {
            // Disable the button first
            preferenceButton.setEnabled(false);
            preferenceButton.setSummary(getString(R.string.updating_taxa_be_patient));
            // Start the service for fetching taxa
            final Intent fetchTaxa = new Intent(getActivity(), FetchTaxa.class);
            fetchTaxa.setAction(FetchTaxa.ACTION_START_NEW);
            Activity activity = getActivity();
            assert activity != null;
            activity.startService(fetchTaxa);
            return true;
        });

        if (dataLicense != null) {
            dataLicense.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d(TAG, "Data license changed to: " + newValue);
                updateLicense();
                return true;
            });
        }

        if (imageLicense != null) {
            imageLicense.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d(TAG, "Data license changed to: " + newValue);
                updateLicense();
                return true;
            });
        }

        if (autoDownload != null) {
            autoDownload.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d(TAG, "Data license changed to: " + newValue);
                updateLicense();
                return true;
            });
        }
    }

    private void toggleFetchTaxaButton(Preference preference) {
        // If already fetching taxa disable the fetch taxa button
        assert preference != null;
        if (FetchTaxa.isInstanceCreated()) {
            preference.setEnabled(false);
            preference.setSummary(getString(R.string.updating_taxa_be_patient));
        } else {
            preference.setEnabled(true);
            preference.setSummary(getString(R.string.update_taxa_desc));
        }
    }

    private void getAutoDownload(ListPreference listPreference) {
        CharSequence[] entries = {getString(R.string.only_wifi), getString(R.string.any_network), getString(R.string.always_ask)};
        CharSequence[] entryValues = {"wifi", "all", "ask"};
        listPreference.setEntries(entries);
        listPreference.setDefaultValue("0");
        listPreference.setEntryValues(entryValues);
    }

    private void getDataLicences(ListPreference listpreference) {
        if(SettingsManager.getDatabaseName().equals("https://biologer.hr")) {
            CharSequence[] entries = {getString(R.string.license_default), getString(R.string.license_public), getString(R.string.license_timed)};
            CharSequence[] entryValues = {"0", "11", "35"};
            listpreference.setEntries(entries);
            listpreference.setDefaultValue("0");
            listpreference.setEntryValues(entryValues);
        } else {
            CharSequence[] entries = {getString(R.string.license_default), getString(R.string.license10), getString(R.string.license20), getString(R.string.license30), getString(R.string.license40)};
            CharSequence[] entryValues = {"0", "10", "20", "30", "40"};
            listpreference.setEntries(entries);
            listpreference.setDefaultValue("0");
            listpreference.setEntryValues(entryValues);
        }
    }

    private void getImageLicences(ListPreference listpreference) {
        if(SettingsManager.getDatabaseName().equals("https://biologer.hr")) {
            CharSequence[] entries = {getString(R.string.license_default),
                    getString(R.string.license_public),
                    getString(R.string.license_timed)};
            CharSequence[] entryValues = {"0", "11", "35"};
            listpreference.setEntries(entries);
            listpreference.setDefaultValue("0");
            listpreference.setEntryValues(entryValues);
        } else {
            CharSequence[] entries = {getString(R.string.license_default),
                    getString(R.string.license_image_10),
                    getString(R.string.license_image_20),
                    getString(R.string.license_image_30),
                    getString(R.string.license_image_40)};
            CharSequence[] entryValues = {"0", "10", "20", "30", "40"};
            listpreference.setEntries(entries);
            listpreference.setDefaultValue("0");
            listpreference.setEntryValues(entryValues);
        }
    }

    private void updateLicense() {
        Activity activity = getActivity();
        if (activity != null) {
            Intent update_licences = new Intent(activity, UpdateLicenses.class);
            activity.startService(update_licences);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                    new IntentFilter("org.biologer.biologer.FetchTaxa.TASK_COMPLETED"));
        }
        Log.d(TAG, "Resuming Preferences Fragment.");
        toggleFetchTaxaButton(preferenceButton);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        }
        Log.d(TAG, "Pausing Preferences Fragment.");
    }

}
