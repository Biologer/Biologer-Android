package org.biologer.biologer.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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
import io.objectbox.query.QueryCondition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaxonSearchHelper {

    private final Context context;

    public TaxonSearchHelper(Context context) {
        this.context = context;
    }
    private static final String TAG = "Biologer.TaxaSearch";


    public List<TaxonDb> searchTaxa(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<TaxonDb> allTaxaLists = new ArrayList<>();
        Set<Long> allTaxaIds = new HashSet<>(); // For more efficient duplicate search
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
            allTaxaIds.add(taxon.getId());
        }

        // Part 2: Query native names
        //
        List<TaxaTranslationDb> nativeNames = getTaxaFromNativeName(changedEnteredName);

        // Check all taxon translations and add only those with IDs that were not
        // already queried by latin name
        for (int i = 0; i < nativeNames.size(); i++) {
            TaxaTranslationDb taxonTranslation = nativeNames.get(i);
            // Don’t add taxa if already on the list
            if (!allTaxaIds.contains(taxonTranslation.getId())) {
                TaxonDb taxon = getTaxonById(taxonTranslation.getTaxonId());
                if (taxon != null) {
                    taxon.setLatinName(getLocalisedLatinName(taxon));
                    allTaxaLists.add(taxon);
                    allTaxaIds.add(taxon.getId());
                } else {
                    Log.e(TAG, "Taxon is null for native name " + taxonTranslation.getNativeName() + "!");
                }
            }
        }

        // Part 3: Query synonyms
        //
        List<SynonymsDb> synonymsList = getTaxaFromSynonymName(changedEnteredName);

        for (int i = 0; i < synonymsList.size(); i++) {
            SynonymsDb synonym = synonymsList.get(i);
            // Don’t add taxa if already on the list
            if (!allTaxaIds.contains(synonym.getTaxonId())) {
                TaxonDb taxon = getTaxonById(synonym.getTaxonId());
                if (taxon != null) {
                    taxon.setLatinName(getLocalisedLatinName(synonym));
                    allTaxaLists.add(taxon);
                    allTaxaIds.add(taxon.getId());
                } else {
                    Log.e(TAG, "Taxon is null for synonym  " + synonym.getName() + "!");
                }
            }
        }

        return allTaxaLists;
    }

    public List<TaxonDb> searchTaxaByGroup(String searchText, long group) {

        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<TaxonDb> allTaxaLists = new ArrayList<>();
        Set<Long> allTaxaIds = new HashSet<>(); // For more efficient duplicate search
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
            allTaxaIds.add(taxon.getId());
        }

        // Part 2: Query native names
        //
        List<TaxaTranslationDb> nativeNames = getTaxaFromNativeName(changedEnteredName);
        // Check all taxon translations and add only those with IDs that were not
        // already queried by latin name
        for (int i = 0; i < nativeNames.size(); i++) {
            TaxaTranslationDb taxonTranslation = nativeNames.get(i);
            // Don’t add taxa if already on the list
            if (!allTaxaIds.contains(taxonTranslation.getId())) {
                // Check if the taxon belongs to the group and add it
                TaxonDb taxon = getTaxonById(taxonTranslation.getTaxonId());
                if (taxon != null && taxon.getGroups().contains(String.valueOf(group))) {
                    taxon.setLatinName(getLocalisedLatinName(taxon));
                    allTaxaLists.add(taxon);
                    allTaxaIds.add(taxon.getId());
                } else {
                    Log.e(TAG, "Trying to add null taxon for native name " + taxonTranslation.getNativeName() + "!");
                }
            }
        }

        // Part 3: Query synonyms
        //
        List<SynonymsDb> synonymsList = getTaxaFromSynonymName(changedEnteredName);
        for (int i = 0; i < synonymsList.size(); i++) {
            SynonymsDb synonym = synonymsList.get(i);
            // Don’t add taxa if already on the list
            if (!allTaxaIds.contains(synonym.getTaxonId())) {
                TaxonDb taxon = getTaxonById(synonym.getTaxonId());
                // Check if the taxon belongs to the group and add it
                if (taxon != null && taxon.getGroups().contains(String.valueOf(group))) {
                    taxon.setLatinName(getLocalisedLatinName(synonym));
                    allTaxaLists.add(taxon);
                    allTaxaIds.add(taxon.getId());
                } else {
                    Log.e(TAG, "Trying to add null taxon for synonym " + synonym.getName() + "!");
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

        Box<TaxaTranslationDb> box = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);

        boolean includeEnglish = preferences.getBoolean("english_names", false);

        QueryCondition<TaxaTranslationDb> cond;

        if ("sr".equals(localeScript)) {
            // Grouped: (sr AND name) OR (sr-Latn AND name) [OR (en AND name)]
            QueryCondition<TaxaTranslationDb> cSr = TaxaTranslationDb_.locale.equal("sr")
                    .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                            QueryBuilder.StringOrder.CASE_INSENSITIVE));
            QueryCondition<TaxaTranslationDb> cSrLatn = TaxaTranslationDb_.locale.equal("sr-Latn")
                    .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                            QueryBuilder.StringOrder.CASE_INSENSITIVE));

            if (includeEnglish) {
                QueryCondition<TaxaTranslationDb> cEn = TaxaTranslationDb_.locale.equal("en")
                        .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                                QueryBuilder.StringOrder.CASE_INSENSITIVE));
                cond = cEn.or(cSr).or(cSrLatn);
            } else {
                cond = cSr.or(cSrLatn);
            }

        } else {
            // Grouped: (local AND name) [OR (en AND name)]
            QueryCondition<TaxaTranslationDb> cLocal = TaxaTranslationDb_.locale.equal(localeScript)
                    .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                            QueryBuilder.StringOrder.CASE_INSENSITIVE));

            if (includeEnglish) {
                QueryCondition<TaxaTranslationDb> cEn = TaxaTranslationDb_.locale.equal("en")
                        .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName,
                                QueryBuilder.StringOrder.CASE_INSENSITIVE));
                cond = cEn.or(cLocal);
            } else {
                cond = cLocal;
            }
        }

        try (Query<TaxaTranslationDb> query = box.query(cond).build()) {
            return query.find(0, 10);
        }
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