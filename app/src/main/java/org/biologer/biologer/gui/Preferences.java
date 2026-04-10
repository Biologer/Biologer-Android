package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.UpdateLicenses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Preferences extends PreferenceFragmentCompat {

    private static final String TAG = "Biologer.Preferences";
    Fragment fragment;

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
        if (dataLicense != null) {
            getDataLicences(dataLicense);
            dataLicense.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d(TAG, "Data license changed to: " + newValue);
                updateLicense();
                return true;
            });
        }

        ListPreference imageLicense = findPreference("image_license");
        if (imageLicense != null) {
            getImageLicences(imageLicense);
            imageLicense.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d(TAG, "Data license changed to: " + newValue);
                updateLicense();
                return true;
            });
        }

        PreferenceScreen advancedSettings = findPreference("advanced_setup");
        if (advancedSettings != null) {
            advancedSettings.setOnPreferenceClickListener(preference -> {
                Log.d(TAG, "Preferences for advanced setup clicked.");

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        fragment = new PreferencesAdvancedSettings();
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.add(R.id.content_frame, fragment);
                        fragmentTransaction.addToBackStack("advanced preferences");
                        fragmentTransaction.commit();
                    });
                } else {
                    Log.e(TAG, "Activity is null in OnPreferenceClickListener!");
                }

                return true;
            });
        }

        ListPreference languageSettings = findPreference("language_settings");
        if (languageSettings != null) {
            getLanguages(languageSettings);
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

    private void getDataLicences(ListPreference listpreference) {
        CharSequence[] entries = {getString(R.string.license_default),
                getString(R.string.license10),
                getString(R.string.license20),
                getString(R.string.license30),
                getString(R.string.license_timed),
                getString(R.string.license40)};
        CharSequence[] entryValues = {"0", "10", "20", "30", "35", "40"};
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

    private void getLanguages(ListPreference listPreference) {

        // Define the Locales (Display Name: Value)
        Map<String, String> localeMap = new HashMap<>();

        // Add all user-defined locales
        localeMap.put(getString(R.string.locale_bosnia_and_herzegovina_latin), "bs");
        localeMap.put(getString(R.string.locale_bosnia_and_herzegovina_cyrillic), "sr-BA");
        localeMap.put(getString(R.string.locale_croatia), "hr");
        localeMap.put(getString(R.string.locale_english), "en");
        localeMap.put(getString(R.string.locale_hungarian), "hu");
        localeMap.put(getString(R.string.locale_macedonia), "mk");
        localeMap.put(getString(R.string.locale_montenegro_latin), "sr-Latn-ME");
        localeMap.put(getString(R.string.locale_montenegro_cyrillic), "sr-ME");
        localeMap.put(getString(R.string.locale_serbia_cyrilic), "sr");
        localeMap.put(getString(R.string.locale_serbia_latin), "sr-Latn");

        // Prepare and Sort the Display Names (Entries)
        List<String> sortedEntriesList = new ArrayList<>(localeMap.keySet());
        Collections.sort(sortedEntriesList, String.CASE_INSENSITIVE_ORDER); // Sort the display names alphabetically

        // Build the final CharSequence arrays, ensuring the "Default" option is first.
        List<CharSequence> finalEntries = new ArrayList<>();
        List<CharSequence> finalEntryValues = new ArrayList<>();

        // Add Default option first
        finalEntries.add(getString(R.string.defaults));
        finalEntryValues.add("");

        // Add all sorted entries and their corresponding values
        for (String entry : sortedEntriesList) {
            String value = localeMap.get(entry);

            finalEntries.add(entry);
            finalEntryValues.add(value);
            Log.i(TAG, "Added locale option: '" + entry + "' with value: '" + value + "'");
        }

        // Set the ListPreference
        listPreference.setEntries(finalEntries.toArray(new CharSequence[0]));
        listPreference.setEntryValues(finalEntryValues.toArray(new CharSequence[0]));
    }

    private void updateLicense() {
        Activity activity = getActivity();
        if (activity != null) {
            Intent update_licences = new Intent(activity, UpdateLicenses.class);
            activity.startService(update_licences);
        }
    }

}
