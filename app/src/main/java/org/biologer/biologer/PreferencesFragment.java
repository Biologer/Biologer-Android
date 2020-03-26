package org.biologer.biologer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.View;

public class PreferencesFragment extends PreferenceFragmentCompat {

    private static final String TAG = "Biologer.Preferences";

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // This is a workaround in order to change background color of the fragment
        getListView().setBackgroundColor(Color.WHITE);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.fragment_preferences, rootKey);
        Preference preferences = getPreferenceScreen();
        if (preferences != null) {
            preferences.setIconSpaceReserved(false);
        }
        Log.d(TAG, "Loading preferences fragment");

        ListPreference dataLicense = (ListPreference) findPreference("data_license");
        ListPreference imageLicense = (ListPreference) findPreference("image_license");
        ListPreference autoDownload = (ListPreference) findPreference("auto_download");

        if (dataLicense != null || imageLicense != null) {
            getLicences(dataLicense);
            getLicences(imageLicense);
        }

        if (autoDownload != null) {
            getAutoDownload(autoDownload);
        }

        // Add button fot taxa sync process
        final Preference button = findPreference("taxa_button");
        // If already fetching taxa disable the fetch taxa button
        if (FetchTaxa.isInstanceCreated()) {
            assert button != null;
            button.setEnabled(false);
            button.setSummary(getString(R.string.updating_taxa_be_patient));
        }

        assert button != null;
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Disable the button first
                button.setEnabled(false);
                button.setSummary(getString(R.string.updating_taxa_be_patient));
                // Start the service for fetching taxa
                final Intent fetchTaxa = new Intent(getActivity(), FetchTaxa.class);
                fetchTaxa.setAction(FetchTaxa.ACTION_START_NEW);
                Activity activity = getActivity();
                if(activity != null) {
                    // If the fetching is paused we would prefer to resume the activity rather than fetching data from the first page.
                    activity.startService(fetchTaxa);
                }

                // Start a thread to monitor taxa update and set user interface after the update is finished
                Thread waitForTaxaUpdate = new Thread() {
                    @Override
                    public void run() {
                        try {
                            while (FetchTaxa.isInstanceCreated()) {
                                sleep(5000);
                                // Run this loop on every 2 seconds while updating taxa
                                Log.d(TAG, "Running the empty loop until taxa updated...");
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            Activity activity = getActivity();
                            if(activity != null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        button.setEnabled(true);
                                        button.setSummary(getString(R.string.update_taxa_desc));
                                    }
                                });
                            }
                        }
                    }
                };
                waitForTaxaUpdate.start();

                return true;
            }
        });

        if (dataLicense != null) {
            dataLicense.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(TAG, "Data license changed to: " + newValue);
                    updateLicense();
                    return true;
                }
            });
        }

        if (imageLicense != null) {
            imageLicense.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(TAG, "Data license changed to: " + newValue);
                    updateLicense();
                    return true;
                }
            });
        }

        if (autoDownload != null) {
            autoDownload.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(TAG, "Data license changed to: " + newValue);
                    updateLicense();
                    return true;
                }
            });
        }
    }

    private void getAutoDownload(ListPreference listPreference) {
        CharSequence[] entries = {getString(R.string.only_wifi), getString(R.string.any_network), getString(R.string.always_ask)};
        CharSequence[] entryValues = {"wifi", "all", "ask"};
        listPreference.setEntries(entries);
        listPreference.setDefaultValue("0");
        listPreference.setEntryValues(entryValues);
    }

    private void getLicences(ListPreference listpreference) {
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

    private void updateLicense() {
        Activity activity = getActivity();
        if (activity != null) {
            Intent update_licences = new Intent(activity, UpdateLicenses.class);
            activity.startService(update_licences);
        }
    }
}
