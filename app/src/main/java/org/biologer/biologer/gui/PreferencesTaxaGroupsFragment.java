package org.biologer.biologer.gui;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.User;
import org.biologer.biologer.sql.TaxonGroupsData;
import org.biologer.biologer.sql.TaxonGroupsDataDao;
import org.biologer.biologer.sql.TaxonGroupsTranslationData;
import org.biologer.biologer.sql.TaxonGroupsTranslationDataDao;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

public class PreferencesTaxaGroupsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "Biologer.Preferences";
    ArrayList<CheckBoxPreference> checkBoxes = new ArrayList<>();

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

        // User.resetPreferences(getContext());

        PreferenceCategory preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
        preferenceCategory.setTitle(getString(R.string.groups_of_taxa));
        preferenceCategory.setIconSpaceReserved(false);
        preferenceScreen.addPreference(preferenceCategory);

        // Query Parent groups from SQL
        QueryBuilder<TaxonGroupsData> groups = App.get().getDaoSession().getTaxonGroupsDataDao().queryBuilder();
        groups.where(
                groups.and(TaxonGroupsDataDao.Properties.Name.isNotNull(),
                        TaxonGroupsDataDao.Properties.PrentId.isNull()));
        List<TaxonGroupsData> listParents = groups.list();

        for (int i = 0; i < listParents.size(); i++) {
            Log.d(TAG, "Parent group: " + listParents.get(i).getName());
            Long id = listParents.get(i).getId();
            String name = listParents.get(i).getName();

            // Get the translation for Parent groups
            QueryBuilder<TaxonGroupsTranslationData> groups_translation = App.get().getDaoSession().getTaxonGroupsTranslationDataDao().queryBuilder();
            groups_translation.where(
                    groups_translation.and(
                            TaxonGroupsTranslationDataDao.Properties.Locale.eq(locale),
                            TaxonGroupsTranslationDataDao.Properties.ViewGroupId.eq(id)));
            List<TaxonGroupsTranslationData> listParentTranslation = groups_translation.list();

            if (listParentTranslation.size() >= 1) {
                String localised_name = listParentTranslation.get(0).getNative_name();
                if (localised_name != null) {
                    // Log.d(TAG, "Taxon ID: " + id + "; name: " + name + "; translation: " + localised_name);
                    populateGroup(preferenceScreen, localised_name, String.valueOf(id), false);
                } else {
                    populateGroup(preferenceScreen, name, String.valueOf(id),false);
                }
            } else {
                populateGroup(preferenceScreen, name,String.valueOf(id),false);
            }

            // List all children within parent
            QueryBuilder<TaxonGroupsData> children = App.get().getDaoSession().getTaxonGroupsDataDao().queryBuilder();
            children.where(TaxonGroupsDataDao.Properties.PrentId.eq(id));
            List<TaxonGroupsData> listChildren = children.list();

            for (int j = 0; j < listChildren.size(); j++) {
                Long child_id = listChildren.get(j).getId();
                String child_name = listChildren.get(j).getName();
                Log.d(TAG, "Child group: " + child_name + " (" + child_id + "); parent: " + name + " (" + id + ")");

                // Get the translation for Children groups
                QueryBuilder<TaxonGroupsTranslationData> children_translation = App.get().getDaoSession().getTaxonGroupsTranslationDataDao().queryBuilder();
                children_translation.where(
                        children_translation.and(
                                TaxonGroupsTranslationDataDao.Properties.Locale.eq(locale),
                                TaxonGroupsTranslationDataDao.Properties.ViewGroupId.eq(child_id)));
                List<TaxonGroupsTranslationData> listChildTranslation = children_translation.list();

                if (listChildTranslation.size() >= 1) {
                    String localised_child_name = listChildTranslation.get(0).getNative_name();
                    if (localised_child_name != null) {
                        // Log.d(TAG, "Taxon ID: " + id + "; name: " + name + "; translation: " + localised_name);
                        populateGroup(preferenceScreen, localised_child_name, String.valueOf(child_id), true);
                    } else {
                        populateGroup(preferenceScreen, name, String.valueOf(child_id), true);
                    }
                } else {
                    populateGroup(preferenceScreen, name, String.valueOf(child_id), true);
                }

            }
        }
    }

    private void populateGroup(PreferenceScreen preferenceScreen, String name, String key, Boolean reserve_icon_space) {
        Context context = getContext();
        CheckBoxPreference checkBoxPreference = new CheckBoxPreference(context);
        checkBoxes.add(checkBoxPreference);
        int last = checkBoxes.size() - 1;
        checkBoxes.get(last).setKey(key);
        checkBoxes.get(last).setTitle(name);
        checkBoxes.get(last).setChecked(true);
        checkBoxes.get(last).setIconSpaceReserved(reserve_icon_space);

        preferenceScreen.addPreference(checkBoxes.get(last));

        checkBoxes.get(last).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int id = Integer.parseInt(preference.getKey());
                Log.d(TAG, "Item " + preference.getTitle() + " (" + id + ") clicked.");

                // Query to determine if this is a child or parent checkbox
                QueryBuilder<TaxonGroupsData> query = App.get().getDaoSession().getTaxonGroupsDataDao().queryBuilder();
                query.where(TaxonGroupsDataDao.Properties.Id.eq(id));
                List<TaxonGroupsData> listSelected = query.list();

                if (!listSelected.isEmpty()) {
                    if (listSelected.get(0).getPrentId() == null) {
                        Log.d(TAG, "This checkbox preference is a parent.");

                        // Query to get all the children
                        QueryBuilder<TaxonGroupsData> children = App.get().getDaoSession().getTaxonGroupsDataDao().queryBuilder();
                        children.where(TaxonGroupsDataDao.Properties.PrentId.eq(id));
                        List<TaxonGroupsData> listChildren = children.list();

                        for (int i = 0; i < listChildren.size(); i++) {
                            if (!listChildren.isEmpty()) {
                                String key = listChildren.get(i).getId().toString();
                                CheckBoxPreference temp = findPreference(key);
                                temp.setChecked(!((CheckBoxPreference) preference).isChecked());
                            }
                        }

                        }
                    else {
                        Log.d(TAG, "This checkbox preference is a child.");
                    }
                }


                //boolean blnIsReg = Boolean.getBoolean(newValue.toString());
                //Editor e = _prefs.edit();
                //e.putBoolean("isReg", blnIsReg);
                //e.commit();

                return true;
            }
        });

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
