package org.biologer.biologer.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import org.apache.commons.lang3.ArrayUtils;
import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.GetTaxaGroups;
import org.biologer.biologer.network.InternetConnection;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.UpdateLicenses;
import org.biologer.biologer.network.UpdateTaxa;
import org.biologer.biologer.sql.UserDb;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreferencesFragment extends PreferenceFragmentCompat {

    private static final String TAG = "Biologer.Preferences";
    private Preference preferenceButton;
    Fragment fragment;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String message = intent.getStringExtra("org.biologer.biologer.network.FetchTaxa.TASK_COMPLETED");
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
        Log.i(TAG, "Configured locale set to: " + AppCompatDelegate.getApplicationLocales());

        // Fetch taxa groups from server
        Activity activity = getActivity();
        if (activity != null) {
            if (InternetConnection.isConnected(activity)) {
                final Intent getTaxaGroups = new Intent(activity, GetTaxaGroups.class);
                Activity getGroups = getActivity();
                getGroups.startService(getTaxaGroups);
            }
        }

        PreferenceScreen taxaGroups = findPreference("species_groups");
        if (taxaGroups != null) {
            taxaGroups.setOnPreferenceClickListener(preference -> {
                Log.d(TAG, "Preferences for taxa groups clicked.");
                fragment = new PreferencesTaxaGroupsFragment();
                FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
                fragmentTransaction.add(R.id.content_frame, fragment);
                fragmentTransaction.addToBackStack("new fragment");
                fragmentTransaction.commit();
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

        Preference deleteAccount = findPreference("delete_account");
        if (deleteAccount != null) {
            deleteAccount.setOnPreferenceClickListener(preference -> {

                final CheckBox checkboxDeleteData = new CheckBox(requireContext());
                checkboxDeleteData.setText(R.string.delete_field_observations);
                LinearLayout container = new LinearLayout(requireContext());
                LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                container.setLayoutParams(containerParams);
                container.setPadding(48, 12, 24, 24);
                container.addView(checkboxDeleteData);

                // Build the alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

                List<UserDb> userDataList = App.get().getBoxStore().boxFor(UserDb.class).getAll();
                UserDb userData = userDataList.get(0);
                String username = userData.getUsername();
                String database = SettingsManager.getDatabaseName();

                builder.setTitle(R.string.delete_account_alert_title)
                        .setMessage(getString(R.string.delete_msg1) + username +
                                getString(R.string.delete_msg2) + database +
                                getString(R.string.delete_msg3) +
                                getString(R.string.delete_msg4))
                        .setView(container)
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            boolean deleteData = checkboxDeleteData.isChecked();
                            deleteAccount(deleteData);
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

                final AlertDialog alert = builder.create();
                alert.setOnShowListener(new DialogInterface.OnShowListener() {
                    private static final int AUTO_DISMISS_MILLIS = 10000;
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        final Button defaultButton = alert.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                        defaultButton.setEnabled(false);
                        final CharSequence negativeButtonText = defaultButton.getText();
                        new CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
                            @Override
                            public void onTick(long l) {
                                defaultButton.setText(String.format(
                                        Locale.getDefault(), "%s (%d)",
                                        negativeButtonText,
                                        TimeUnit.MILLISECONDS.toSeconds(l) + 1
                                ));
                            }

                            @Override
                            public void onFinish() {
                                if (alert.isShowing()) {
                                    defaultButton.setEnabled(true);
                                    defaultButton.setText(negativeButtonText);
                                }
                            }
                        }.start();
                    }
                });
                alert.show();

                return true;
            });
        }

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
        assert preference != null;
        if (UpdateTaxa.isInstanceCreated()) {
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

    private void deleteAccount(boolean deleteData) {
        Call<ResponseBody> deleteUser = RetrofitClient
                .getService(SettingsManager.getDatabaseName()).deleteUser(deleteData);
        deleteUser.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Delete user preferences and database
                    App.get().deleteAllBoxes();
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.get());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();

                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle(R.string.account_deleted)
                            .setMessage(getString(R.string.your_user_account_has_been_deleted_from)
                                    + " " + SettingsManager.getDatabaseName() + ".")
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                // Go back to login activity and exit the dialog
                                Intent intent = new Intent(getActivity(), LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                dialog.dismiss();
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    int code = response.code();
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle(R.string.error)
                            .setMessage(getString(R.string.delete_account_error1) + " " + code + ".")
                            .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(R.string.error)
                        .setMessage(getString(R.string.delete_account_error2) + " " + t)
                        .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                    new IntentFilter("org.biologer.biologer.network.FetchTaxa.TASK_COMPLETED"));
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
