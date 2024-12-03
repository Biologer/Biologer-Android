package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.adapters.ArrayHelper;
import org.biologer.biologer.network.FetchTaxa;
import org.biologer.biologer.network.InternetConnection;
import org.biologer.biologer.sql.TaxaTranslationDb;
import org.biologer.biologer.sql.TaxaTranslationDb_;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonDb_;
import org.biologer.biologer.sql.TaxonGroupsDb;
import org.biologer.biologer.sql.TaxonGroupsDb_;
import org.biologer.biologer.sql.TaxonGroupsTranslationDb;
import org.biologer.biologer.sql.TaxonGroupsTranslationDb_;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class PreferencesTaxaGroupsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "Biologer.PreferencesT";
    ArrayList<CheckBoxPreference> checkBoxes = new ArrayList<>();

    String locale = Localisation.getLocaleScript();
    String how_to_use_network;

    ArrayList<String> checked_checkbox = new ArrayList<>();
    ArrayList<String> unchecked_checkbox = new ArrayList<>();
    ArrayList<String> remove_from_sql = new ArrayList<>();
    ArrayList<String> add_to_sql = new ArrayList<>();

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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(preferenceScreen.getContext());
        how_to_use_network = preferences.getString("auto_download", "wifi");

        // Query Parent groups from SQL
        Box<TaxonGroupsDb> taxonGroupsDataBox = App.get().getBoxStore().boxFor(TaxonGroupsDb.class);
        Query<TaxonGroupsDb> query = taxonGroupsDataBox
                .query(TaxonGroupsDb_.parentId.equal(0))
                .build();
        List<TaxonGroupsDb> listParents = query.find();
        query.close();

        for (int i = 0; i < listParents.size(); i++) {
            Log.d(TAG, "Parent group: " + listParents.get(i).getName());
            Long id = listParents.get(i).getId();
            String name = listParents.get(i).getName();

            // Get the translation for Parent groups
            Box<TaxonGroupsTranslationDb> taxonGroupsTranslationDataBox = App.get().getBoxStore().boxFor(TaxonGroupsTranslationDb.class);
            Query<TaxonGroupsTranslationDb> groups_translation = taxonGroupsTranslationDataBox
                    .query(TaxonGroupsTranslationDb_.locale.equal(locale)
                            .and(TaxonGroupsTranslationDb_.viewGroupId.equal(id)))
                    .build();
            List<TaxonGroupsTranslationDb> listParentTranslation = groups_translation.find();
            groups_translation.close();

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
            Box<TaxonGroupsDb> taxonGroupsDataBox1 = App.get().getBoxStore().boxFor(TaxonGroupsDb.class);
            Query<TaxonGroupsDb> children = taxonGroupsDataBox1
                    .query(TaxonGroupsDb_.parentId.equal(id))
                    .build();
            List<TaxonGroupsDb> listChildren = children.find();
            children.close();

            for (int j = 0; j < listChildren.size(); j++) {
                Long child_id = listChildren.get(j).getId();
                String child_name = listChildren.get(j).getName();
                Log.d(TAG, "Child group: " + child_name + " (" + child_id + "); parent: " + name + " (" + id + ")");

                // Get the translation for Children groups
                Box<TaxonGroupsTranslationDb> taxonGroupsTranslationDataBox1 = App.get().getBoxStore().boxFor(TaxonGroupsTranslationDb.class);
                Query<TaxonGroupsTranslationDb> children_translation = taxonGroupsTranslationDataBox1
                        .query(TaxonGroupsTranslationDb_.locale.equal(locale)
                                .and(TaxonGroupsTranslationDb_.viewGroupId.equal(child_id)))
                        .build();
                List<TaxonGroupsTranslationDb> listChildTranslation = children_translation.find();
                children_translation.close();

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

    // Add individual checkbox to the list
    private void populateGroup(PreferenceScreen preferenceScreen, String name, String key, Boolean reserve_icon_space) {
        Context context = getContext();
        CheckBoxPreference checkBoxPreference = null;
        if (context != null) {
            checkBoxPreference = new CheckBoxPreference(context);
        }
        checkBoxes.add(checkBoxPreference);
        int last = checkBoxes.size() - 1;
        checkBoxes.get(last).setKey(key);
        checkBoxes.get(last).setTitle(name);
        checkBoxes.get(last).setChecked(true);
        checkBoxes.get(last).setIconSpaceReserved(reserve_icon_space);

        preferenceScreen.addPreference(checkBoxes.get(last));

        // Query for taxa groups selection before the user selects anything
        List<TaxonGroupsDb> allTaxaGroups = App.get().getBoxStore().boxFor(TaxonGroupsDb.class).getAll();
        for (int i = 0; i < allTaxaGroups.size(); i++) {
            int id = (int)allTaxaGroups.get(i).getId();
            Activity activity = getActivity();
            if (activity != null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                boolean checked = preferences.getBoolean(String.valueOf(id), true);
                if (checked) {
                    //Log.d(TAG, "Checkbox for taxa group ID " + id + " is checked.");
                    checked_checkbox.add(String.valueOf(id));
                } else {
                    unchecked_checkbox.add(String.valueOf(id));
                }
            }
        }

        checkBoxes.get(last).setOnPreferenceChangeListener((preference, newValue) -> {
            int id = Integer.parseInt(preference.getKey());
            Log.d(TAG, "Item " + preference.getTitle() + " (" + id + ") clicked.");

            // Query to determine if this is a child or parent checkbox
            Box<TaxonGroupsDb> taxonGroupsDataBox = App.get().getBoxStore().boxFor(TaxonGroupsDb.class);
            Query<TaxonGroupsDb> query = taxonGroupsDataBox
                    .query(TaxonGroupsDb_.id.equal(id))
                    .build();
            List<TaxonGroupsDb> listSelected = query.find();
            query.close();

            if (!listSelected.isEmpty()) {
                if (listSelected.get(0).getParentId() == 0) {
                    Log.d(TAG, "This checkbox preference is a parent.");

                    // Save the value of the clicked parent
                    String key_parent = String.valueOf(listSelected.get(0).getId());
                    saveCheckedState(key_parent);

                    // Query to get all the children
                    Box<TaxonGroupsDb> taxonGroupsDataBox1 = App.get().getBoxStore().boxFor(TaxonGroupsDb.class);
                    Query<TaxonGroupsDb> query1 = taxonGroupsDataBox1
                            .query(TaxonGroupsDb_.parentId.equal(id))
                            .build();
                    List<TaxonGroupsDb> listChildren = query1.find();
                    query1.close();

                    for (int i = 0; i < listChildren.size(); i++) {
                        String key1 = String.valueOf(listChildren.get(i).getId());
                        saveCheckedOnParentChangeState(key_parent, key1);
                        CheckBoxPreference temp = findPreference(key1);
                        if (temp != null) {
                            temp.setChecked(!((CheckBoxPreference) preference).isChecked());
                        }
                    }

                }
                else {
                    Log.d(TAG, "This checkbox preference is a child.");

                    // Save the value of the clicked child
                    String key_child = String.valueOf(listSelected.get(0).getId());
                    saveCheckedState(key_child);
                }
            }

            return true;
        });

    }

    private void saveCheckedOnParentChangeState(String parent_key, String child_key) {
        CheckBoxPreference parent = findPreference(parent_key);
        CheckBoxPreference child = findPreference(child_key);
        if (parent != null) {
            if (parent.isChecked()) {
                // If preference not checked - delete from SQL
                if (!remove_from_sql.contains(child_key)) remove_from_sql.add(child_key);
                Log.d(TAG, "Adding key " + child_key + " to the list for deleting from sql.");
            } else {
                // If preference checked - download from server
                if (child != null) {
                    if (child.isChecked()) {
                        Log.d(TAG, "The key " + child_key + " is already downloaded, ignoring.");
                    } else {
                        if (!add_to_sql.contains(child_key)) add_to_sql.add(child_key);
                        Log.d(TAG, "Adding key " + child_key + " to the list for downloading from server.");
                    }
                }
            }
        }
    }

    private void saveCheckedState(String key) {
        CheckBoxPreference clicked_child = findPreference(key);
        if (clicked_child != null) {
            // WTF! This returns true if not checked!
            if(clicked_child.isChecked()) {
                // If preference not checked
                if (!remove_from_sql.contains(key)) remove_from_sql.add(key);
                add_to_sql.remove(key);
                remove_from_sql.removeAll(unchecked_checkbox);
                Log.d(TAG, "Adding key " + key + " to the list for deleting from sql.");
            } else {
                // If preference checked
                if (!add_to_sql.contains(key)) add_to_sql.add(key);
                remove_from_sql.remove(key);
                add_to_sql.removeAll(checked_checkbox);
                Log.d(TAG, "Adding key " + key + " to the list for downloading from server.");
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        Activity activity = getActivity();
        if (activity != null) {
            Log.d(TAG, "Delete this: " + remove_from_sql);
            if (!remove_from_sql.isEmpty()) {
                for (int i = 0; i < remove_from_sql.size(); i++) {
                    String key = remove_from_sql.get(i);
                    Log.d(TAG, "Attempting to delete key " + key + " from SQL database");

                    // Delete taxa
                    Box<TaxonDb> taxonData = App.get().getBoxStore().boxFor(TaxonDb.class);
                    Query<TaxonDb> query = taxonData
                            .query(TaxonDb_.groups.contains(key))
                            .build();
                    List<TaxonDb> deleteTaxaList = query.find();
                    taxonData.remove(deleteTaxaList);
                    query.close();

                    List<Long> deleteThisIds = new ArrayList<>();

                    // A large list could not be deleted at once! We have to make this workaround
                    if (deleteTaxaList.size() > 500) {
                        int k = 0;
                        for (int j = 0; j < deleteTaxaList.size(); j++) {
                            deleteThisIds.add(deleteTaxaList.get(j).getId());
                            k++;
                            if (k == 500) {
                                // Delete after each 500th entry
                                deleteTaxaTranslationFromSQL(deleteThisIds);
                                // Reset for the further loop...
                                k = 0;
                                deleteThisIds.clear();
                            }
                        }
                        if (k != 0) {
                            deleteTaxaTranslationFromSQL(deleteThisIds);
                        }
                    } else {
                        for (int j = 0; j < deleteTaxaList.size(); j++) {
                            deleteThisIds.add(deleteTaxaList.get(j).getId());
                        }
                        deleteTaxaTranslationFromSQL(deleteThisIds);
                    }
                }
            }

            Log.d(TAG, "Download this: " + add_to_sql.toString());
            if (FetchTaxa.isInstanceCreated()) {
                Log.d(TAG, "Already running!");
                buildAlertOKMessage(getString(R.string.already_fetching_taxa));
            } else {
                if (!add_to_sql.isEmpty()) {
                    // Check if there is network available and download the data...
                    Context context = getContext();
                    if (context != null) {
                        String network_type = InternetConnection.networkType(context);
                        if (network_type != null) {
                            if (InternetConnection.isConnected(context) || network_type.equals("wifi")) {
                                if (how_to_use_network.equals("all") ||
                                        (how_to_use_network.equals("wifi") && network_type.equals("wifi"))) {
                                    fetchTaxa(activity);
                                } else {
                                    buildAlertMessage(getString(R.string.message_should_download),
                                            getString(R.string.download),
                                            getString(R.string.skip),
                                            activity);
                                }
                            } else {
                                Log.d(TAG, "There is no network available. Application will not be able to get new data from the server.");
                            }
                        }
                    }
                }
            }
        }
    }

    private void deleteTaxaTranslationFromSQL(List<Long> deleteThisIds) {

        Log.d(TAG, "List of taxa to be deleted: " + deleteThisIds.toString());
        long[] delete_this_ids = ArrayHelper.listToArray(deleteThisIds);

        Box<TaxaTranslationDb> taxaTranslationDataBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
        Query<TaxaTranslationDb> query = taxaTranslationDataBox
                .query(TaxaTranslationDb_.taxonId.oneOf(delete_this_ids))
                .build();
        List<TaxaTranslationDb> deleteTaxaList = query.find();
        taxaTranslationDataBox.remove(deleteTaxaList);
        query.close();
    }

    @Override
    public void onStart() {
        super.onStart();
        remove_from_sql = new ArrayList<>();
        add_to_sql = new ArrayList<>();
    }

    protected void buildAlertOKMessage(String message) {
        Activity activity = getActivity();
        if (activity != null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(message)
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.OK), (dialog, id) -> dialog.dismiss());
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }

    protected void buildAlertMessage(String message, String yes_string, String no_string, Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(yes_string, (dialog, id) -> {
                    Log.d(TAG, "Positive button clicked");
                    fetchTaxa(activity);
                })
                .setNegativeButton(no_string, (dialog, id) -> {
                    Log.d(TAG, "Negative button clicked");
                    dialog.cancel();
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void fetchTaxa(Activity activity) {
        final Intent fetchTaxa = new Intent(activity, FetchTaxa.class);
        fetchTaxa.setAction(FetchTaxa.ACTION_START);
        fetchTaxa.putStringArrayListExtra("groups", add_to_sql);
        activity.startService(fetchTaxa);
    }

}
