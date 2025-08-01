package org.biologer.biologer.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String localeScript = Localisation.getLocaleScript();
        BoxStore boxStore = App.get().getBoxStore();

        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<TaxonDb> allTaxaLists = new ArrayList<>();
        String changedEnteredName = searchText.trim();

        // Query latin names
        Box<TaxonDb> taxonDataBox = boxStore.boxFor(TaxonDb.class);
        Query<TaxonDb> queryLatinName = taxonDataBox
                .query(TaxonDb_.latinName.contains(changedEnteredName, QueryBuilder.StringOrder.CASE_INSENSITIVE))
                .build();
        List<TaxonDb> latinNames = queryLatinName.find(0, 10);
        queryLatinName.close();

        for (int i = 0; i < latinNames.size(); i++) {
            Box<TaxaTranslationDb> taxaTranslationDataBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
            Query<TaxaTranslationDb> query_taxon_translation = taxaTranslationDataBox
                    .query(TaxaTranslationDb_.taxonId.equal(latinNames.get(i).getId())
                            .and(TaxaTranslationDb_.locale.equal(localeScript)))
                    .build();
            List<TaxaTranslationDb> nativeNames = query_taxon_translation.find();
            query_taxon_translation.close();

            if (!nativeNames.isEmpty()) {
                String native_name = nativeNames.get(0).getNativeName();
                if (native_name != null) {
                    TaxonDb taxon = latinNames.get(i);
                    taxon.setLatinName(taxon.getLatinName() + " (" + native_name + ")");
                    allTaxaLists.add(taxon);
                } else {
                    allTaxaLists.add(latinNames.get(i));

                }
            } else {
                allTaxaLists.add(latinNames.get(i));
            }
        }

        // Get tne native names for taxa
        Box<TaxaTranslationDb> taxaTranslationDataBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
        List<TaxaTranslationDb> nativeList;
        // For Serbian language we should also search for Latin and Cyrillic names
        if (localeScript.equals("sr")) {
            if (preferences.getBoolean("english_names", false)) {
                Query<TaxaTranslationDb> nativeQuery = taxaTranslationDataBox
                        .query(TaxaTranslationDb_.locale.equal("en")
                                .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName, QueryBuilder.StringOrder.CASE_INSENSITIVE))
                                .or(TaxaTranslationDb_.locale.equal("sr")
                                        .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName, QueryBuilder.StringOrder.CASE_INSENSITIVE)))
                                .or(TaxaTranslationDb_.locale.equal("sr-Latn")
                                        .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName, QueryBuilder.StringOrder.CASE_INSENSITIVE))))
                        .build();
                nativeList = nativeQuery.find(0, 10);
                nativeQuery.close();
            } else {
                Query<TaxaTranslationDb> nativeQuery = taxaTranslationDataBox
                        .query(TaxaTranslationDb_.locale.equal("sr")
                                .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName, QueryBuilder.StringOrder.CASE_INSENSITIVE))
                                .or(TaxaTranslationDb_.locale.equal("sr-Latn")
                                        .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName, QueryBuilder.StringOrder.CASE_INSENSITIVE))))
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
                                .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName, QueryBuilder.StringOrder.CASE_INSENSITIVE))
                                .or(TaxaTranslationDb_.locale.equal(localeScript)
                                        .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName, QueryBuilder.StringOrder.CASE_INSENSITIVE))))
                        .build();
                nativeList = nativeQuery.find(0, 10);
                nativeQuery.close();
            } else {
                Query<TaxaTranslationDb> nativeQuery = taxaTranslationDataBox
                        .query(TaxaTranslationDb_.locale.equal(localeScript)
                                .and(TaxaTranslationDb_.nativeName.contains(changedEnteredName, QueryBuilder.StringOrder.CASE_INSENSITIVE)))
                        .build();
                nativeList = nativeQuery.find(0, 10);
                nativeQuery.close();
            }
        }

        for (int i = 0; i < nativeList.size(); i++) {
            TaxaTranslationDb taxaTranslationData = nativeList.get(i);
            // Don’t add taxa if already on the list
            boolean duplicated = false;
            for (int j = 0; j < allTaxaLists.size(); j++) {
                if (allTaxaLists.get(j).getId() == taxaTranslationData.getTaxonId()) {
                    duplicated = true;
                }
            }
            if (!duplicated) {
                Box<TaxonDb> taxonDbBox = App.get().getBoxStore().boxFor(TaxonDb.class);
                Query<TaxonDb> taxonDbQuery = taxonDbBox
                        .query(TaxonDb_.id.equal(taxaTranslationData.getTaxonId()))
                        .build();
                TaxonDb taxon = taxonDbQuery.findFirst();
                taxonDbQuery.close();

                if (taxon != null) {
                    taxon.setLatinName(taxon.getLatinName() + " (" + taxaTranslationData.getNativeName() + ")");
                    allTaxaLists.add(taxon);
                }
            }
        }

        return allTaxaLists;
    }
}