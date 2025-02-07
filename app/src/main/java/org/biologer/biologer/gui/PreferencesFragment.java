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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import org.apache.commons.lang3.ArrayUtils;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.UpdateLicenses;
import org.biologer.biologer.network.UpdateTaxa;

import java.util.Arrays;
import java.util.Objects;

public class PreferencesFragment extends PreferenceFragmentCompat {

    private static final String TAG = "Biologer.Preferences";
    private Preference preferenceButton;
    Fragment fragment;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
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
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Log.d(TAG, "Loading preferences fragment.");
        Log.i(TAG, "Configured locale set to: " + AppCompatDelegate.getApplicationLocales());

        PreferenceScreen taxaGroups = findPreference("species_groups");
        if (taxaGroups != null) {
            taxaGroups.setOnPreferenceClickListener(preference -> {
                Log.d(TAG, "Preferences for taxa groups clicked.");

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        fragment = new PreferencesTaxaGroupsFragment();
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.add(R.id.content_frame, fragment);
                        fragmentTransaction.addToBackStack("taxa preferences");
                        fragmentTransaction.commit();
                    });
                } else {
                    Log.e(TAG, "Activity is null in OnPreferenceClickListener!");
                }

                return true;
            });
        }

        PreferenceScreen accountSetup = findPreference("account_setup");
        if (accountSetup != null) {
            accountSetup.setOnPreferenceClickListener(preference -> {
                Log.d(TAG, "Preferences for account setup clicked.");

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        fragment = new PreferencesAccountSetup();
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.add(R.id.content_frame, fragment);
                        fragmentTransaction.addToBackStack("account preferences");
                        fragmentTransaction.commit();
                    });
                } else {
                    Log.e(TAG, "Activity is null in OnPreferenceClickListener!");
                }

                return true;
            });
        }

        // Check if the location name is null and if true null the previous location
        EditTextPreference locationName = findPreference("location_name");
        if (locationName != null) {
            locationName.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue.toString().trim().isEmpty()) {
                    Log.d(TAG, "Setting previous location to null");
                    SettingsManager.setPreviousLocationLong(null);
                    SettingsManager.setPreviousLocationLat(null);
                }
                return true;
            });
        }

        setPreferenceScreen(preferenceScreen);

        ListPreference dataLicense = findPreference("data_license");
        ListPreference imageLicense = findPreference("image_license");
        ListPreference autoDownload = findPreference("auto_download");
        ListPreference languageSettings = findPreference("language_settings");

        if (dataLicense != null) {
            getDataLicences(dataLicense);
        }

        if (imageLicense != null) {
            getImageLicences(imageLicense);
        }

        if (autoDownload != null) {
            getAutoDownload(autoDownload);
        }

        if (languageSettings != null) {
            getLanguages(languageSettings);
        }

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

        if (languageSettings != null) {
            languageSettings.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d(TAG, "Language changed to: " + newValue);
                LocaleListCompat appLocale = LocaleListCompat.getEmptyLocaleList();
                if (newValue != "") {
                    appLocale = LocaleListCompat.forLanguageTags(newValue.toString());
                }
                AppCompatDelegate.setApplicationLocales(appLocale);
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

    private void getAutoDownload(ListPreference listPreference) {
        CharSequence[] entries = {getString(R.string.only_wifi), getString(R.string.any_network), getString(R.string.always_ask)};
        CharSequence[] entryValues = {"wifi", "all", "ask"};
        listPreference.setEntries(entries);
        listPreference.setDefaultValue("0");
        listPreference.setEntryValues(entryValues);
    }

    private void getDataLicences(ListPreference listpreference) {
        CharSequence[] entries = {getString(R.string.license_default),
                getString(R.string.license10),
                getString(R.string.license20),
                getString(R.string.license30),
                getString(R.string.license_timed),
                getString(R.string.license40)};
        CharSequence[] entryValues = {"0", "10", "20", "30", "11", "40"};
        listpreference.setEntries(entries);
        listpreference.setDefaultValue("0");
        listpreference.setEntryValues(entryValues);
    }

    private void getImageLicences(ListPreference listpreference) {
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

    private void updateLicense() {
        Activity activity = getActivity();
        if (activity != null) {
            Intent update_licences = new Intent(activity, UpdateLicenses.class);
            activity.startService(update_licences);
        }
    }

    private void getLanguages(ListPreference listPreference) {
        CharSequence[] default_entry = {getString(R.string.defaults)};
        CharSequence[] other_entries = {
                getString(R.string.locale_bosnia_and_herzegovina_latin),
                getString(R.string.locale_croatia),
                getString(R.string.locale_english),
                getString(R.string.locale_hungarian),
                //getString(R.string.locale_montenegro_latin),
                getString(R.string.locale_serbia_cyrilic),
                getString(R.string.locale_serbia_latin)};
        Arrays.sort(other_entries);
        CharSequence[] entries = ArrayUtils.addAll(default_entry, other_entries);

        CharSequence[] entryValues = new CharSequence[entries.length];
        entryValues[0] = ""; // First, empty line for default value
        for (int i = 0; i < entries.length; i++) {
            CharSequence entry = entries[i];
            if (entry.equals(getString(R.string.locale_croatia))) {
                CharSequence toAdd = "hr";
                entryValues[i] = toAdd;
                Log.i(TAG, "Adding locale: " + toAdd);
            }
            if (entry.equals(getString(R.string.locale_english))) {
                CharSequence toAdd = "en";
                entryValues[i] = toAdd;
                Log.i(TAG, "Adding locale: " + toAdd);
            }
            if (entry.equals(getString(R.string.locale_hungarian))) {
                CharSequence toAdd = "hu";
                entryValues[i] = toAdd;
                Log.i(TAG, "Adding locale: " + toAdd);
            }
            if (entry.equals(getString(R.string.locale_serbia_cyrilic))) {
                CharSequence toAdd = "sr";
                entryValues[i] = toAdd;
                Log.i(TAG, "Adding locale: " + toAdd);
            }
            if (entry.equals(getString(R.string.locale_serbia_latin))) {
                CharSequence toAdd = "sr-Latn";
                entryValues[i] = toAdd;
                Log.i(TAG, "Adding locale: " + toAdd);
            }
            if (entry.equals(getString(R.string.locale_bosnia_and_herzegovina_latin))) {
                CharSequence toAdd = "bs";
                entryValues[i] = toAdd;
                Log.i(TAG, "Adding locale: " + toAdd);
            }
        }
        listPreference.setEntries(entries);
        listPreference.setDefaultValue("2");
        listPreference.setEntryValues(entryValues);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(UpdateTaxa.TASK_COMPLETED);
            filter.addAction(UpdateTaxa.TASK_PERCENT);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
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
