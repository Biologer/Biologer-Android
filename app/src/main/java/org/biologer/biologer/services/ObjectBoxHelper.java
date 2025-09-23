package org.biologer.biologer.services;

import android.util.Log;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.EntryDb_;
import org.biologer.biologer.sql.ObservationTypesDb;
import org.biologer.biologer.sql.ObservationTypesDb_;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.StageDb_;
import org.biologer.biologer.sql.TaxaTranslationDb;
import org.biologer.biologer.sql.TaxaTranslationDb_;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonDb_;
import org.biologer.biologer.sql.TimedCountDb;
import org.biologer.biologer.sql.TimedCountDb_;
import org.biologer.biologer.sql.UserDb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class ObjectBoxHelper {

    public static ArrayList<EntryDb> getObservations() {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        Query<EntryDb> query = box.query(EntryDb_.timedCoundId.isNull()).build();
        ArrayList<EntryDb> observations = (ArrayList<EntryDb>) query.find();
        query.close();
        Log.i("Biologer.ObjectBox", "There are " + observations.size() + " observations in the database.");
        return observations;
    }

    public static EntryDb getObservationById(long entryId) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        Query<EntryDb> query = box.query(EntryDb_.id.equal(entryId)).build();
        EntryDb entry = query.findFirst();
        query.close();
        if (entry != null) {
            Log.i("Biologer.ObjectBox", "Observation with ID " + entryId + " found in the database");
            return entry;
        } else {
            Log.i("Biologer.ObjectBox", "There is no observation with ID " + entryId + " in the database");
            return null;
        }
    }

    public static long setObservation(EntryDb entry) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        long id = box.put(entry);
        Log.d("Biologer.ObjectBox", "Observation will be saved in EntryDb under ID " + id);
        return id;
    }

    public static ArrayList<TimedCountDb> getTimedCounts() {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        Query<TimedCountDb> query = box.query().build();
        ArrayList<TimedCountDb> timedCounts = (ArrayList<TimedCountDb>) query.find();
        query.close();
        Log.i("Biologer.ObjectBox", "There are " + timedCounts.size() + " timed counts in the database.");
        return timedCounts;
    }

    public static TimedCountDb getTimedCountById(long timedCountId) {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        Query<TimedCountDb> query = box.query(TimedCountDb_.timedCountId.equal(timedCountId)).build();
        TimedCountDb timedCount = query.findFirst();
        query.close();
        if (timedCount != null) {
            Log.i("Biologer.ObjectBox", "Timed count with ID " + timedCountId + " found in the database");
        } else {
            Log.i("Biologer.ObjectBox", "There is no timed cound with ID " + timedCountId + " in the database");
        }
        return timedCount;
    }

    public static TaxonDb getTaxonById(long taxonId) {
        Box<TaxonDb> box = App.get().getBoxStore().boxFor(TaxonDb.class);
        Query<TaxonDb> query = box.query(TaxonDb_.id.equal(taxonId)).build();
        TaxonDb taxon = query.findFirst();
        query.close();
        if (taxon != null) {
            Log.i("Biologer.ObjectBox", "Taxon with ID " + taxonId + " found in the database");
        } else {
            Log.i("Biologer.ObjectBox", "There is no taxon with ID " + taxonId + " in the database");
        }
        return taxon;
    }

    public static TaxaTranslationDb getTaxonTranslationByTaxonId(long taxonId) {
        Box<TaxaTranslationDb> box = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
        Query<TaxaTranslationDb> query = box.query(TaxaTranslationDb_.taxonId.equal(taxonId)
                        .and(TaxaTranslationDb_.locale.equal(Localisation.getLocaleScript())))
                .build();
        TaxaTranslationDb translation = query.findFirst();
        query.close();
        if (translation != null) {
            Log.i("Biologer.ObjectBox", "There is translation of the selected taxon.");
            return translation;
        } else {
            Log.i("Biologer.ObjectBox", "There is no translation of the selected taxon.");
            return null;
        }
    }

    public static long getIdFromTimeCountId(int timeCountId) {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        Query<TimedCountDb> query = box.query(TimedCountDb_.timedCountId.equal(timeCountId)).build();
        TimedCountDb timedCount = query.findFirst();
        query.close();
        if (timedCount != null) {
            Log.i("Biologer.ObjectBox",
                    "ObjectBox ID of the timed count is " + timedCount.getId()
                            + " (Timed count ID (" + timeCountId + ")).");
            return timedCount.getId();
        } else {
            Log.i("Biologer.ObjectBox", "Could not find ObjectBox id for time cound ID " + timeCountId + ".");
            return 0;
        }
    }

    public static int getUniqueTimedCountID() {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        Query<TimedCountDb> query = box.query().build();
        if (query.count() == 0) {
            Log.i("Biologer.ObjectBox", "Timed counts database is empty, returning ID 1.");
            query.close();
            return 1;
        } else {
            int maxId = (int) query.property(TimedCountDb_.timedCountId).max();
            query.close();
            Log.i("Biologer.ObjectBox", "The last timed count in the database has ID " + maxId + ".");
            return ++maxId;
        }
    }

    public static ArrayList<EntryDb> getTimedCountObservations(int timedCountId) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        Query<EntryDb> query = box.query(EntryDb_.timedCoundId.equal(timedCountId)).build();
        ArrayList<EntryDb> observations = (ArrayList<EntryDb>) query.find();
        query.close();
        Log.i("Biologer.ObjectBox", "There are " + observations.size() + " observations in the database.");
        return observations;
    }

    public static StageDb getStageById(long stageId) {
        Box<StageDb> box = App.get().getBoxStore().boxFor(StageDb.class);
        Query<StageDb> query = box.query(StageDb_.id.equal(stageId)).build();
        StageDb stage = query.findFirst();
        query.close();
        if (stage != null) {
            Log.i("Biologer.ObjectBox", "Stage with ID " + stageId + " found in the database");
        } else {
            Log.i("Biologer.ObjectBox", "There is no stage with ID " + stageId + " in the database");
        }
        return stage;
    }

    public static ArrayList<StageDb> getStagesForTaxonId(long taxonId) {
        Box<StageDb> box = App.get().getBoxStore().boxFor(StageDb.class);
        Query<StageDb> query = box.query(StageDb_.id.equal(taxonId)).build();
        ArrayList<StageDb> stages = (ArrayList<StageDb>) query.find();
        query.close();
        Log.i("Biologer.ObjectBox", "There are " + stages.size() + " stages for taxon ID " + taxonId + ".");
        return stages;
    }

    public static int getStagesForTaxonIdCount(long taxonId) {
        ArrayList<StageDb> stages = getStagesForTaxonId(taxonId);
        return stages.size();
    }

    public static Long getAdultStageIdForTaxon(TaxonDb taxon) {
        String stages = taxon.getStages();
        Long stageId = null;
        if (stages != null) {
            if (!stages.isEmpty()) {
                Log.i("Biologer.ObjectBox", "Taxon contains stages.");
                String[] all_stages = stages.split(";");

                Box<StageDb> box = App.get().getBoxStore().boxFor(StageDb.class);
                Query<StageDb> query = box.query(StageDb_.name.equal("adult")).build();
                StageDb stage = query.findFirst();
                query.close();
                if (stage != null) {
                    String s = String.valueOf(stage.getId());
                    if (Arrays.asList(all_stages).contains(s)) {
                        stageId = stage.getId();
                        Log.i("Biologer.ObjectBox", "Taxon contains adult stage (ID: " + stageId + ").");
                    }
                }
            }
        }
        return stageId;
    }

    public static List<ObservationTypesDb> getObservationTypes() {
        Box<ObservationTypesDb> box = App.get().getBoxStore().boxFor(ObservationTypesDb.class);
        Query<ObservationTypesDb> query = box.query(ObservationTypesDb_.locale.equal(Localisation.getLocaleScript())).build();
        List<ObservationTypesDb> list = query.find();
        Log.i("Biologer.ObjectBox", "There are " + list.size() + " observation types for locale " + Localisation.getLocaleScript() + ".");
        query.close();
        return list;
    }

    public static Long getIdForObservedTag() {
        Box<ObservationTypesDb> box = App.get().getBoxStore().boxFor(ObservationTypesDb.class);
        Query<ObservationTypesDb> query = box.query(ObservationTypesDb_.slug.equal("observed")).build();
        ObservationTypesDb observationTypes = query.findFirst();
        query.close();

        Long observed_id = null;
        if (observationTypes != null) {
            observed_id = observationTypes.getObservationId();
            Log.i("Biologer.ObjectBox", "Observed tag has ID " + observed_id + ".");
        }
        return observed_id;
    }

    public static int getDataLicense() {
        List<UserDb> user = App.get().getBoxStore().boxFor(UserDb.class).getAll();
        if (user != null) {
            if (!user.isEmpty()) {
                return user.get(0).getDataLicense();
            }
            return 0;
        }
        return 0;
    }

    public static int getImageLicense() {
        List<UserDb> user = App.get().getBoxStore().boxFor(UserDb.class).getAll();
        if (user != null) {
            if (!user.isEmpty()) {
                return user.get(0).getImageLicense();
            }
            return 0;
        }
        return 0;
    }

    public static Long getIdForPhotographedTag() {
        Box<ObservationTypesDb> box = App.get().getBoxStore().boxFor(ObservationTypesDb.class);
        Query<ObservationTypesDb> query = box.query(ObservationTypesDb_.slug.equal("photographed")).build();
        ObservationTypesDb observationTypes = query.findFirst();
        query.close();

        Long observed_id = null;
        if (observationTypes != null) {
            observed_id = observationTypes.getObservationId();
        }
        return observed_id;
    }

    public static void removeObservationsForTimedCountId(int timedCountId) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        long removedCount;
        try (Query<EntryDb> query = box.query(EntryDb_.timedCoundId.equal(timedCountId)).build()) {
            removedCount = query.remove();
        }
        Log.i("Biologer.ObjectBox", "Removed " + removedCount + " objects for time count ID " + timedCountId + ".");
    }

    public static void removeObservationById(long observationId) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        long removedCount;
        try (Query<EntryDb> query = box.query(EntryDb_.id.equal(observationId)).build()) {
            removedCount = query.remove();
        }
        Log.i("Biologer.ObjectBox", "Removed " + removedCount + " observation with ID " + observationId + ".");
    }

    public static void removeTimedCountById(int timedCountId) {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        long removedCount;
        try (Query<TimedCountDb> query = box.query(TimedCountDb_.timedCountId.equal(timedCountId)).build()) {
            removedCount = query.remove();
        }
        Log.i("Biologer.ObjectBox", "Removed " + removedCount + " time count with ID " + timedCountId + ".");
    }

    public static void removeAllObservations() {
        App.get().getBoxStore().boxFor(EntryDb.class).removeAll();
    }

    public static void removeAllTimedCounts() {
        App.get().getBoxStore().boxFor(TimedCountDb.class).removeAll();
    }

    public static void removeAllEntries() {
        removeAllObservations();
        removeAllTimedCounts();
    }

    public static Boolean hasAtlasCode(long taxonId) {
        TaxonDb taxon = getTaxonById(taxonId);
        if (taxon != null) {
            if (taxon.isUseAtlasCode()) {
                Log.d("Biologer.ObjectBox", "There is atlas code for taxon ID: " + taxonId);
                return true;
            } else {
                Log.d("Biologer.ObjectBox", "There is no atlas code for taxon ID: " + taxonId);
                return false;
            }
        } else {
            return false;
        }
    }
}