package org.biologer.biologer.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.sql.SynonymsDb;
import org.biologer.biologer.sql.SynonymsDb_;
import org.biologer.biologer.sql.TaxaTranslationDb;
import org.biologer.biologer.sql.TaxaTranslationDb_;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonDb_;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import java.util.ArrayList;
import java.util.List;

public class TaxonSearchHelper {

    private final Context context;

    public TaxonSearchHelper(Context context) {
        this.context = context;
    }

    public List<TaxonDb> searchTaxa(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<TaxonDb> allTaxaLists = new ArrayList<>();
        String changedEnteredName = searchText.trim();

        // Part 1: Query latin names
        //
        List<TaxonDb> latinNames = getTaxaFromLatinName(changedEnteredName);
        // Get the latin (native) name for the output
        for (int i = 0; i < latinNames.size(); i++) {
            TaxonDb taxon = latinNames.get(i);
            String name = getLocalisedLatinName(taxon);
            taxon.setLatinName(name);
            allTaxaLists.add(taxon);
        }

        // Part 2: Query native names
        //
        List<TaxaTranslationDb> nativeNames = getTaxaFromNativeName(changedEnteredName);

        // Check all taxon translations and add only those with IDs that were not
        // already queried by latin name
        for (int i = 0; i < nativeNames.size(); i++) {
            TaxaTranslationDb taxonTranslation = nativeNames.get(i);
            // Don’t add taxa if already on the list
            boolean duplicated = false;
            for (int j = 0; j < allTaxaLists.size(); j++) {
                if (allTaxaLists.get(j).getId() == taxonTranslation.getTaxonId()) {
                    duplicated = true;
                }
            }
            if (!duplicated) {
                TaxonDb taxon = getTaxonById(taxonTranslation.getTaxonId());
                taxon.setLatinName(getLocalisedLatinName(taxon));
                allTaxaLists.add(taxon);
            }
        }

        // Part 3: Query synonyms
        //
        List<SynonymsDb> synonymsList = getTaxaFromSynonymName(changedEnteredName);

        for (int i = 0; i < synonymsList.size(); i++) {
            SynonymsDb synonym = synonymsList.get(i);
            // Don’t add taxa if already on the list
            boolean duplicated = false;
            for (int j = 0; j < allTaxaLists.size(); j++) {
                if (allTaxaLists.get(j).getId() == synonym.getTaxonId()) {
                    duplicated = true;
                }
            }
            if (!duplicated) {
                TaxonDb taxon = getTaxonById(synonym.getTaxonId());
                taxon.setLatinName(getLocalisedLatinName(synonym));
                allTaxaLists.add(taxon);
            }
        }

        return allTaxaLists;
    }

    public List<TaxonDb> searchTaxaByGroup(String searchText, long group) {

        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<TaxonDb> allTaxaLists = new ArrayList<>();
        String changedEnteredName = searchText.trim();

        // Part 1: Query latin names
        //
        List<TaxonDb> latinNames = getTaxaFromLatinName(changedEnteredName, group);
        // Get the latin (native) name for the output
        for (int i = 0; i < latinNames.size(); i++) {
            TaxonDb taxon = latinNames.get(i);
            String name = getLocalisedLatinName(taxon);
            taxon.setLatinName(name);
            allTaxaLists.add(taxon);
        }

        // Part 2: Query native names
        //
        List<TaxaTranslationDb> nativeNames = getTaxaFromNativeName(changedEnteredName);

        // Check all taxon translations and add only those with IDs that were not
        // already queried by latin name
        for (int i = 0; i < nativeNames.size(); i++) {
            TaxaTranslationDb taxonTranslation = nativeNames.get(i);
            // Don’t add taxa if already on the list
            boolean duplicated = false;
            for (int j = 0; j < allTaxaLists.size(); j++) {
                if (allTaxaLists.get(j).getId() == taxonTranslation.getTaxonId()) {
                    duplicated = true;
                }
            }
            if (!duplicated) {
                TaxonDb taxon = getTaxonById(taxonTranslation.getTaxonId());
                // Check if the taxon belongs to the group and add it
                if (taxon.getGroups().contains(String.valueOf(group))) {
                    taxon.setLatinName(getLocalisedLatinName(taxon));
                    allTaxaLists.add(taxon);
                }
            }
        }

        // Part 3: Query synonyms
        //
        List<SynonymsDb> synonymsList = getTaxaFromSynonymName(changedEnteredName);

        for (int i = 0; i < synonymsList.size(); i++) {
            SynonymsDb synonym = synonymsList.get(i);
            // Don’t add taxa if already on the list
            boolean duplicated = false;
            for (int j = 0; j < allTaxaLists.size(); j++) {
                if (allTaxaLists.get(j).getId() == synonym.getTaxonId()) {
                    duplicated = true;
                }
            }
            if (!duplicated) {
                TaxonDb taxon = getTaxonById(synonym.getTaxonId());
                // Check if the taxon belongs to the group and add it
                if (taxon.getGroups().contains(String.valueOf(group))) {
                    taxon.setLatinName(getLocalisedLatinName(synonym));
                    allTaxaLists.add(taxon);
                }
            }
        }
        return allTaxaLists;
    }

    private List<SynonymsDb> getTaxaFromSynonymName(String name) {
        Box<SynonymsDb> synonymsDbBox = App.get().getBoxStore().boxFor(SynonymsDb.class);
        List<SynonymsDb> synonymsList;

        Query<SynonymsDb> synonymsQuery = synonymsDbBox
                .query(SynonymsDb_.name.contains(name,
                        QueryBuilder.StringOrder.CASE_INSENSITIVE))
                .build();
        synonymsList = synonymsQuery.find(0, 10);
        synonymsQuery.close();
        return synonymsList;
    }

    private List<TaxonDb> getTaxaFromLatinName(String name) {
        BoxStore boxStore = App.get().getBoxStore();
        Box<TaxonDb> taxonDataBox = boxStore.boxFor(TaxonDb.class);
        Query<TaxonDb> queryLatinName = taxonDataBox
                .query(TaxonDb_.latinName.contains(name,
                        QueryBuilder.StringOrder.CASE_INSENSITIVE))
                .build();
        List<TaxonDb> latinNames = queryLatinName.find(0, 10);
        queryLatinName.close();
        return latinNames;
    }

    private List<TaxonDb> getTaxaFromLatinName(String name, long group) {
        BoxStore boxStore = App.get().getBoxStore();
        Box<TaxonDb> taxonDataBox = boxStore.boxFor(TaxonDb.class);
        Query<TaxonDb> queryLatinName = taxonDataBox
                .query(TaxonDb_.latinName.contains(name,
                                QueryBuilder.StringOrder.CASE_INSENSITIVE)
                        .and(TaxonDb_.groups.contains(String.valueOf(group))))
                .build();
        List<TaxonDb> latinNames = queryLatinName.find(0, 10);
        queryLatinName.close();
        return latinNames;
    }

    private List<TaxaTranslationDb> getTaxaFromNativeName(String changedEnteredName) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String localeScript = Localisation.getLocaleScript();

        Box<TaxaTranslationDb> taxaTranslationDataBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
        List<TaxaTranslationDb> nativeList;

        // For Serbian language we should also search for Latin and Cyrillic names
        if (localeScript.equals("sr")) {
            if (preferences.getBoolean("english_names", false)) {
                Query<TaxaTranslationDb> nativeQuery = taxaTranslationDataBox
                        .query(TaxaTranslationDb_.locale.equal("en")
                                .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                                        QueryBuilder.StringOrder.CASE_INSENSITIVE))
                                .or(TaxaTranslationDb_.locale.equal("sr")
                                        .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                                                QueryBuilder.StringOrder.CASE_INSENSITIVE)))
                                .or(TaxaTranslationDb_.locale.equal("sr-Latn")
                                        .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                                                QueryBuilder.StringOrder.CASE_INSENSITIVE))))
                        .build();
                nativeList = nativeQuery.find(0, 10);
                nativeQuery.close();
            } else {
                Query<TaxaTranslationDb> nativeQuery = taxaTranslationDataBox
                        .query(TaxaTranslationDb_.locale.equal("sr")
                                .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                                        QueryBuilder.StringOrder.CASE_INSENSITIVE))
                                .or(TaxaTranslationDb_.locale.equal("sr-Latn")
                                        .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                                                QueryBuilder.StringOrder.CASE_INSENSITIVE))))
                        .build();
                nativeList = nativeQuery.find(0, 10);
                nativeQuery.close();
            }
        }

        // For other languages it is more simple...
        else {
            if (preferences.getBoolean("english_names", false)) {
                Query<TaxaTranslationDb> nativeQuery = taxaTranslationDataBox
                        .query(TaxaTranslationDb_.locale.equal("en")
                                .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                                        QueryBuilder.StringOrder.CASE_INSENSITIVE))
                                .or(TaxaTranslationDb_.locale.equal(localeScript)
                                        .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                                                QueryBuilder.StringOrder.CASE_INSENSITIVE))))
                        .build();
                nativeList = nativeQuery.find(0, 10);
                nativeQuery.close();
            } else {
                Query<TaxaTranslationDb> nativeQuery = taxaTranslationDataBox
                        .query(TaxaTranslationDb_.locale.equal(localeScript)
                                .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                                        QueryBuilder.StringOrder.CASE_INSENSITIVE)))
                        .build();
                nativeList = nativeQuery.find(0, 10);
                nativeQuery.close();
            }
        }
        return nativeList;
    }

    private static TaxonDb getTaxonById(long taxon_id) {
        Box<TaxonDb> taxonDbBox = App.get().getBoxStore().boxFor(TaxonDb.class);
        Query<TaxonDb> taxonDbQuery = taxonDbBox
                .query(TaxonDb_.id.equal(taxon_id))
                .build();
        TaxonDb taxon = taxonDbQuery.findFirst();
        taxonDbQuery.close();
        return taxon;
    }

    public static String getLocalisedLatinName(long taxon_id) {

        String localeScript = Localisation.getLocaleScript();
        BoxStore boxStore = App.get().getBoxStore();

        // Query taxon
        Box<TaxonDb> taxonDataBox = boxStore.boxFor(TaxonDb.class);
        Query<TaxonDb> queryId = taxonDataBox
                .query(TaxonDb_.id.equal(taxon_id))
                .build();
        TaxonDb taxon = queryId.findFirst();
        queryId.close();

        // Query native name
        Box<TaxaTranslationDb> taxaTranslationDataBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
        Query<TaxaTranslationDb> query_taxon_translation = taxaTranslationDataBox
                .query(TaxaTranslationDb_.taxonId.equal(taxon_id)
                        .and(TaxaTranslationDb_.locale.equal(localeScript)))
                .build();
        TaxaTranslationDb nativeName = query_taxon_translation.findFirst();
        query_taxon_translation.close();

        // Return the value
        if (taxon != null) {
            if (nativeName != null) {
                return taxon.getLatinName() + " (" + nativeName.getNativeName() + ")";
            } else {
                return taxon.getLatinName();
            }
        } else {
            return null;
        }
    }

    public static String getLocalisedLatinName(TaxonDb taxon) {
        String localeScript = Localisation.getLocaleScript();

        // Query native name
        Box<TaxaTranslationDb> taxaTranslationDataBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
        Query<TaxaTranslationDb> query_taxon_translation = taxaTranslationDataBox
                .query(TaxaTranslationDb_.taxonId.equal(taxon.getId())
                        .and(TaxaTranslationDb_.locale.equal(localeScript)))
                .build();
        TaxaTranslationDb nativeName = query_taxon_translation.findFirst();
        query_taxon_translation.close();

        // Return the value
        if (nativeName != null) {
            return taxon.getLatinName() + " (" + nativeName.getNativeName() + ")";
        } else {
            return taxon.getLatinName();
        }
    }

    public static String getLocalisedLatinName(SynonymsDb synonym) {
        String localeScript = Localisation.getLocaleScript();

        // Query native name
        Box<TaxaTranslationDb> taxaTranslationDataBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
        Query<TaxaTranslationDb> query_taxon_translation = taxaTranslationDataBox
                .query(TaxaTranslationDb_.taxonId.equal(synonym.getTaxonId())
                        .and(TaxaTranslationDb_.locale.equal(localeScript)))
                .build();
        TaxaTranslationDb nativeName = query_taxon_translation.findFirst();
        query_taxon_translation.close();

        // Return the value
        if (nativeName != null) {
            return synonym.getName() + " (" + nativeName.getNativeName() + ")";
        } else {
            return synonym.getName();
        }
    }
}