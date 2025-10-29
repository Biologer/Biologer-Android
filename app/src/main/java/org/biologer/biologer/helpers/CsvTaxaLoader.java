package org.biologer.biologer.helpers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.UpdateTaxa;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.SynonymsDb;
import org.biologer.biologer.sql.TaxaTranslationDb;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonGroupsDb;
import org.biologer.biologer.sql.TaxonGroupsTranslationDb;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loads local taxa datasets (from assets folder) into ObjectBox
 * if newer than the currently stored version.
 */
public class CsvTaxaLoader {

    private static final String TAG = "Biologer.TaxaLoader";

    private final Context context;

    public CsvTaxaLoader(Context context) {
        this.context = context;
    }

    /**
     * Loads taxa database from assets for the given Biologer instance,
     * replacing ObjectBox data and updating timestamp.
     */
    public void loadInternalTaxaDataset(@NonNull String databaseUrl, @NonNull String newTimestamp) {
        Log.i(TAG, "Updating ObjectBox taxa database using local copy from assets.");

        List<String[]> taxaCsv;
        List<String[]> groupsCsv;
        List<String[]> stagesCsv;

        switch (databaseUrl) {
            case "https://biologer.rs" -> {
                taxaCsv = readCsv("taxa/rs_taxa.csv");
                groupsCsv = readCsv("taxa/rs_groups.csv");
                stagesCsv = readCsv("taxa/rs_stages.csv");
            }
            case "https://biologer.hr" -> {
                taxaCsv = readCsv("taxa/hr_taxa.csv");
                groupsCsv = readCsv("taxa/hr_groups.csv");
                stagesCsv = readCsv("taxa/hr_stages.csv");
            }
            case "https://biologer.ba" -> {
                taxaCsv = readCsv("taxa/ba_taxa.csv");
                groupsCsv = readCsv("taxa/ba_groups.csv");
                stagesCsv = readCsv("taxa/ba_stages.csv");
            }
            case "https://biologer.me" -> {
                taxaCsv = readCsv("taxa/me_taxa.csv");
                groupsCsv = readCsv("taxa/me_groups.csv");
                stagesCsv = readCsv("taxa/me_stages.csv");
            }
            case "https://dev.biologer.org" -> {
                taxaCsv = readCsv("taxa/dev_taxa.csv");
                groupsCsv = readCsv("taxa/dev_groups.csv");
                stagesCsv = readCsv("taxa/dev_stages.csv");
            }
            default -> {
                Toast.makeText(context, R.string.database_url_empty, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Unsupported database URL: " + databaseUrl);
                return;
            }
        }

        if (taxaCsv == null || groupsCsv == null || stagesCsv == null) {
            Log.e(TAG, "Could not load asset CSV files.");
            return;
        }

        // Wipe old data
        ObjectBoxHelper.removeTaxaDatabase();
        Log.d(TAG, "Old taxonomic data cleared.");

        // --- Parse taxa CSV ---
        List<TaxonDb> taxaList = new ArrayList<>();
        List<TaxaTranslationDb> translationList = new ArrayList<>();
        List<SynonymsDb> synonymList = new ArrayList<>();

        for (int i = 1; i < taxaCsv.size(); i++) { // skip header i = 1, not 0
            String[] t = taxaCsv.get(i);
            long id = Long.parseLong(t[0]);
            String rank = t[1];
            String name = t[2];
            String author = t[3];
            boolean usesAtlasCodes = Objects.equals(t[4], "1");
            String translations = t[5];
            String stages = t[6];
            String groups = t[7];
            String synonyms = t[8];

            taxaList.add(new TaxonDb(id, 0, name, rank, 0, author,
                    false, usesAtlasCodes, null, groups, stages));

            // Translations
            if (!translations.isEmpty()) {
                String[] splitTranslations = translations.split(";");
                String[] locales = {"en", "sr", "sr-Latn", "hr", "ba", "me"};
                for (int tIndex = 0; tIndex < splitTranslations.length && tIndex < locales.length; tIndex++) {
                    if (!splitTranslations[tIndex].isEmpty()) {
                        translationList.add(new TaxaTranslationDb(0, id, locales[tIndex], splitTranslations[tIndex], name, ""));
                    }
                }
            }

            // Synonyms
            String trimmedSynonyms = synonyms.trim();
            if (!trimmedSynonyms.isEmpty()) {
                String[] splitSynonyms = trimmedSynonyms.split(";");
                for (String s : splitSynonyms) {
                    String trimmedS = s.trim();
                    if (!trimmedS.isEmpty()) {
                        synonymList.add(new SynonymsDb(0, id, trimmedS));
                    }
                }
            }
        }

        App.get().getBoxStore().boxFor(TaxonDb.class).put(taxaList);
        App.get().getBoxStore().boxFor(TaxaTranslationDb.class).put(translationList);
        App.get().getBoxStore().boxFor(SynonymsDb.class).put(synonymList);
        Log.d(TAG, "Inserted " + taxaList.size() + " taxa from assets.");

        // --- Parse taxon groups ---
        List<TaxonGroupsDb> groupList = new ArrayList<>();
        List<TaxonGroupsTranslationDb> groupTranslations = new ArrayList<>();
        for (int i = 1; i < groupsCsv.size(); i++) {
            String[] g = groupsCsv.get(i);
            long id = Long.parseLong(g[0]);
            long parentId = Long.parseLong(g[1]);
            String ba = g[2];
            String en = g[3];
            String hr = g[4];
            String me = g[5];
            String sr = g[6];
            String srLatin = g[7];

            groupList.add(new TaxonGroupsDb(id, parentId, en, ""));
            groupTranslations.add(new TaxonGroupsTranslationDb(0, id, "en", en, ""));
            groupTranslations.add(new TaxonGroupsTranslationDb(0, id, "sr", sr, ""));
            groupTranslations.add(new TaxonGroupsTranslationDb(0, id, "sr-Latn", srLatin, ""));
            groupTranslations.add(new TaxonGroupsTranslationDb(0, id, "hr", hr, ""));
            groupTranslations.add(new TaxonGroupsTranslationDb(0, id, "ba", ba, ""));
            groupTranslations.add(new TaxonGroupsTranslationDb(0, id, "me", me, ""));
        }

        App.get().getBoxStore().boxFor(TaxonGroupsDb.class).put(groupList);
        App.get().getBoxStore().boxFor(TaxonGroupsTranslationDb.class).put(groupTranslations);
        Log.d(TAG, "Inserted " + groupList.size() + " groups from assets.");

        // --- Parse stages ---
        List<StageDb> stageList = new ArrayList<>();
        for (int i = 1; i < stagesCsv.size(); i++) {
            String[] s = stagesCsv.get(i);
            long id = Long.parseLong(s[0]);
            String name = s[1];
            stageList.add(new StageDb(id, name));
        }

        App.get().getBoxStore().boxFor(StageDb.class).put(stageList);
        Log.d(TAG, "Inserted " + stageList.size() + " stages from assets.");

        // Update timestamp & trigger online check
        SettingsManager.setTaxaUpdatedAt(newTimestamp);
        Log.d(TAG, "Local taxa loaded. Checking online for newer version...");
        checkForUpdates(context);
    }

    private List<String[]> readCsv(String filename) {
        try (InputStream input = context.getAssets().open(filename);
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {
            return csvReader.readAll();
        } catch (IOException | CsvException e) {
            Log.e(TAG, "Error reading CSV: " + filename, e);
            return null;
        }
    }

    public static void checkForUpdates(Context context) {
        try {
            Intent intent = new Intent(context, UpdateTaxa.class);
            intent.setAction(UpdateTaxa.ACTION_DOWNLOAD);
            context.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "Could not start UpdateTaxa service: " + e.getMessage(), e);
        }
    }
}
