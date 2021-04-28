package org.biologer.biologer.gui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.sql.TaxonGroupsData;
import org.biologer.biologer.sql.TaxonGroupsDataDao;
import org.biologer.biologer.sql.TaxonGroupsTranslationData;
import org.biologer.biologer.sql.TaxonGroupsTranslationDataDao;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

public class PreferencesTaxaGroupsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "Biologer.Preferences";

    String locale = Localisation.getLocaleScript();

    /*
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

    }
     */

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // This is a workaround in order to change background color of the fragment
        getListView().setBackgroundResource(R.color.fragment_background);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_taxa_groups);
        PreferenceScreen preferenceScreen = this.getPreferenceScreen();
        Log.d(TAG, "Starting fragment for taxa groups preferences.");

        PreferenceCategory preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
        preferenceCategory.setTitle(getString(R.string.groups_of_taxa));
        preferenceCategory.setIconSpaceReserved(false);
        preferenceScreen.addPreference(preferenceCategory);

        preferenceCategory.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(TAG, "Something changed");
                return false;
            }
        });

        // Query group names from SQL
        QueryBuilder<TaxonGroupsData> groups = App.get().getDaoSession().getTaxonGroupsDataDao().queryBuilder();
        groups.where(TaxonGroupsDataDao.Properties.Name.isNotNull());
        List<TaxonGroupsData> taxonGroupsData = groups.list();

        for (int i = 0; i < taxonGroupsData.size(); i++) {
            Log.d(TAG, "System locale set to " + locale);

            Long id = taxonGroupsData.get(i).getId();
            String name = taxonGroupsData.get(i).getName();

            QueryBuilder<TaxonGroupsTranslationData> groups_translation =
                    App.get().getDaoSession().getTaxonGroupsTranslationDataDao().queryBuilder();
            groups_translation.where(
                    groups_translation.and(
                            TaxonGroupsTranslationDataDao.Properties.Locale.eq(locale),
                            TaxonGroupsTranslationDataDao.Properties.ViewGroupId.eq(id)));
            List<TaxonGroupsTranslationData> translation = groups_translation.list();

            if (translation.size() >= 1) {
                String localised_name = translation.get(0).getNative_name();
                if (localised_name != null) {
                    // Log.d(TAG, "Taxon ID: " + id + "; name: " + name + "; translation: " + localised_name);
                    CheckBoxPreference checkBoxPreference = new CheckBoxPreference(getContext());
                    checkBoxPreference.setTitle(localised_name);
                    checkBoxPreference.setChecked(true);
                    checkBoxPreference.setIconSpaceReserved(false);
                    preferenceScreen.addPreference(checkBoxPreference);
                }
            } else {
                // Log.d(TAG, "Taxon ID: " + id + "; name: " + name + "; translation: null");
                CheckBoxPreference checkBoxPreference = new CheckBoxPreference(getContext());
                checkBoxPreference.setTitle(name);
                checkBoxPreference.setChecked(true);
                checkBoxPreference.setIconSpaceReserved(false);
                preferenceScreen.addPreference(checkBoxPreference);
            }

        }
    }

    /*
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
     */
}
