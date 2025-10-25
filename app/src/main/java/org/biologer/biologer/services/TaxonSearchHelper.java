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

    public List<TaxonDb> searchTaxa(String searchText, long group) {
        List<TaxonDb> allTaxaLists = new ArrayList<>();
        Set<Long> allTaxaIds = new HashSet<>();
        String changedEnteredName = searchText.trim();

        // Part 1: Query latin names
        List<TaxonDb> latinNames = getTaxaFromLatinName(changedEnteredName);
        for (TaxonDb taxon : latinNames) {
            addTaxonToListSafely(taxon, allTaxaLists, allTaxaIds, group);
        }

        // Part 2: Query native names
        List<TaxaTranslationDb> nativeNames = getTaxaFromNativeName(changedEnteredName);
        for (TaxaTranslationDb taxonTranslation : nativeNames) {
            TaxonDb taxon = getTaxonById(taxonTranslation.getTaxonId());
            if (taxon != null) {
                addTaxonToListSafely(taxon, allTaxaLists, allTaxaIds, group);
            } else {
                Log.e(TAG, "Taxon is null for native name " + taxonTranslation.getNativeName() + "!");
            }
        }

        // Part 3: Query synonyms
        List<SynonymsDb> synonymsList = getTaxaFromSynonymName(changedEnteredName);
        for (SynonymsDb synonym : synonymsList) {
            TaxonDb taxon = getTaxonById(synonym.getTaxonId());
            if (taxon != null) {
                taxon.setLatinName(synonym.getName());
                addTaxonToListSafely(taxon, allTaxaLists, allTaxaIds, group);
            } else {
                Log.e(TAG, "Taxon is null for synonym  " + synonym.getName() + "!");
            }
        }

        return allTaxaLists;
    }

    /**
     * Safely adds a TaxonDb object to the results list if it meets all criteria.
     * @param taxon Taxon object to add (must not be null).
     * @param allTaxaLists The list to add the taxon to.
     * @param allTaxaIds The Set for duplicate checking.
     * @param requiredGroup The group ID to filter by, or 0 if no group filtering is required.
     */
    private void addTaxonToListSafely(TaxonDb taxon, List<TaxonDb> allTaxaLists,
                                      Set<Long> allTaxaIds, long requiredGroup) {
        // Exit if there is no taxon
        if (taxon == null) {
            return;
        }

        // Exit on duplicated taxon ID
        if (allTaxaIds.contains(taxon.getId())) {
            return;
        }

        // Check for group if required – 0 returns without checking
        if (requiredGroup > 0) {
            if (!taxon.getGroups().contains(String.valueOf(requiredGroup))) {
                return;
            }
        }

        // Localize the name
        String localizedName = getLocalisedLatinName(taxon);
        taxon.setLatinName(localizedName);

        // 4. Add to results
        allTaxaLists.add(taxon);
        allTaxaIds.add(taxon.getId());
    }

    private List<SynonymsDb> getTaxaFromSynonymName(String name) {
        Box<SynonymsDb> synonymsDbBox = App.get().getBoxStore().boxFor(SynonymsDb.class);

        try (Query<SynonymsDb> synonymsQuery = synonymsDbBox
                .query(SynonymsDb_.name.contains(name,
                        QueryBuilder.StringOrder.CASE_INSENSITIVE))
                .build()) {
            return synonymsQuery.find(0, 10);
        }
    }

    private List<TaxonDb> getTaxaFromLatinName(String name) {
        Box<TaxonDb> taxonDataBox = App.get().getBoxStore().boxFor(TaxonDb.class);
        try (Query<TaxonDb> queryLatinName = taxonDataBox
                .query(TaxonDb_.latinName.contains(name,
                        QueryBuilder.StringOrder.CASE_INSENSITIVE))
                .build()) {
            return queryLatinName.find(0, 10);
        }
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
        try (Query<TaxonDb> taxonDbQuery = taxonDbBox
                .query(TaxonDb_.id.equal(taxon_id))
                .build()) {
            return taxonDbQuery.findFirst();
        }
    }

    public static String getLocalisedLatinName(TaxonDb taxon) {
        // Query native name
        String nativeName = getTaxonTranslationStringById(taxon.getId());

        // Return the value
        return formatLatinAndNativeName(taxon.getLatinName(), nativeName);
    }

    public static String getLocalisedLatinName(long taxonId) {
        // Query taxon
        TaxonDb taxon = getTaxonById(taxonId);
        // Query native name
        String nativeName = getTaxonTranslationStringById(taxonId);
        // Return the value
        if (taxon != null) {
            return formatLatinAndNativeName(taxon.getLatinName(), nativeName);
        } else {
            return null;
        }
    }

    private static String getTaxonTranslationStringById(long taxonId) {
        TaxaTranslationDb nativeName = getTaxonTranslationById(taxonId);
        String nativeNameString = (nativeName != null) ? nativeName.getNativeName() : null;
        return (nativeNameString == null || nativeNameString.isEmpty()) ? null : nativeNameString;
    }

    private static TaxaTranslationDb getTaxonTranslationById(long taxonId) {
        String localeScript = Localisation.getLocaleScript();

        Box<TaxaTranslationDb> taxaTranslationDataBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
        try (Query<TaxaTranslationDb> query_taxon_translation = taxaTranslationDataBox
                .query(TaxaTranslationDb_.taxonId.equal(taxonId)
                        .and(TaxaTranslationDb_.locale.equal(localeScript)))
                .build()) {
            return query_taxon_translation.findFirst();
        }
    }

    private static String formatLatinAndNativeName(String latinName, String nativeName) {
        if (nativeName != null) {
            return latinName + " (" + nativeName + ")";
        } else {
            return latinName;
        }
    }
}