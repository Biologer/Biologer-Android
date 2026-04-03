package org.biologer.biologer.helpers;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.biologer.biologer.App;
import org.biologer.biologer.sql.AnnouncementTranslationsDb;
import org.biologer.biologer.sql.AnnouncementTranslationsDb_;
import org.biologer.biologer.sql.AnnouncementsDb;
import org.biologer.biologer.sql.AnnouncementsDb_;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.EntryDb_;
import org.biologer.biologer.sql.ObservationTypesDb;
import org.biologer.biologer.sql.ObservationTypesDb_;
import org.biologer.biologer.sql.PhotoDb;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.StageDb_;
import org.biologer.biologer.sql.SynonymsDb;
import org.biologer.biologer.sql.TaxaTranslationDb;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonDb_;
import org.biologer.biologer.sql.TaxonGroupsDb;
import org.biologer.biologer.sql.TaxonGroupsTranslationDb;
import org.biologer.biologer.sql.TimedCountDb;
import org.biologer.biologer.sql.TimedCountDb_;
import org.biologer.biologer.sql.UserDb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class ObjectBoxHelper {

    private static final String TAG = "Biologer.ObjectBoxHelper";

    public static ArrayList<EntryDb> getObservations() {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        try (Query<EntryDb> query = box.query(EntryDb_.timedCoundId.isNull()).build()) {
            ArrayList<EntryDb> observations = (ArrayList<EntryDb>) query.find();
            Log.i(TAG, "There are " + observations.size() + " observations in the database.");
            return observations;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving observations from database!", e);
            return new ArrayList<>();
        }
    }

    public static int getObservationsCount() {
        return (int) App.get().getBoxStore().boxFor(EntryDb.class).count();
    }

    public static ArrayList<EntryDb> getObservationsForUpload() {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        try (Query<EntryDb> query = box.query(
                EntryDb_.timedCoundId.isNull()
                        .and(EntryDb_.serverId.isNull())
        ).build()) {
            ArrayList<EntryDb> observations = (ArrayList<EntryDb>) query.find();
            Log.i(TAG, "There are " + observations.size() + " observations awaiting upload in the database.");
            return observations;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving observations from database!", e);
            return new ArrayList<>();
        }
    }

    public static ArrayList<EntryDb> getPagedObservations(int limit, int offset) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        try(Query<EntryDb> query = box.query()
                .isNull(EntryDb_.timedCoundId)
                .orderDesc(EntryDb_.serverId)
                .build()) {
            ArrayList<EntryDb> observations = (ArrayList<EntryDb>) query.find(offset, limit);
            Log.i(TAG, "There are " + observations.size() + " observations with served ID in the ObjectBox query.");
            return observations;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving observations from database!", e);
            return new ArrayList<>();
        }
    }

    public static EntryDb getObservationById(long entryId) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        try (Query<EntryDb> query = box.query(EntryDb_.id.equal(entryId)).build()) {
            EntryDb entry = query.findFirst();
            if (entry != null) {
                Log.i(TAG, "Observation with ID " + entryId + " found in the database");
            } else {
                Log.i(TAG, "There is no observation with ID " + entryId + " in the database");
            }
            return entry;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving observation by ID: " + entryId, e);
            return null;
        }
    }

    public static EntryDb getObservationByServerId(long id) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        try (Query<EntryDb> query = box.query(EntryDb_.serverId.equal(id)).build()) {
            return query.findFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error in ObjectBox observation query for serverId: " + id, e);
            return null;
        }
    }

    // Get the minimal server ID of the ObjectBox EntryDb items
    public static long getMinObservationServerId() {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        if (box.isEmpty()) {
            return -1L;
        }
        try (Query<EntryDb> query = box.query().build()) {
            return query.property(EntryDb_.serverId).min();
        } catch (Exception e) {
            Log.e(TAG,"Error retrieving minimal Server ID of ObjectBox observations!" + e);
            return -1L;
        }
    }

    public static long setObservation(EntryDb entry) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        long id = box.put(entry);
        Log.d(TAG, "Observation will be saved in EntryDb under ID " + id);
        return id;
    }

    public static ArrayList<TimedCountDb> getTimedCounts() {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        try (Query<TimedCountDb> query = box.query().build()) {
            ArrayList<TimedCountDb> timedCounts = (ArrayList<TimedCountDb>) query.find();
            Log.i(TAG, "There are " + timedCounts.size() + " timed counts in the database.");
            return timedCounts;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving timed counts from database!", e);
            return new ArrayList<>();
        }
    }

    public static int getTimedCountsCount() {
        return (int) App.get().getBoxStore().boxFor(TimedCountDb.class).count();
    }

    public static ArrayList<TimedCountDb> getPagedTimedCounts(int limit, int offset) {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        try(Query<TimedCountDb> query = box.query().orderDesc(TimedCountDb_.serverId).build()) {
            ArrayList<TimedCountDb> timedCounts = (ArrayList<TimedCountDb>) query.find(offset, limit);
            Log.i(TAG, "There are " + timedCounts.size() + " timed counts with served ID in the ObjectBox query.");
            return timedCounts;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving timed counts from database!", e);
            return new ArrayList<>();
        }
    }

    // Get the minimal server ID of the ObjectBox EntryDb items
    public static long getMinTimedCountsServerId() {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        if (box.isEmpty()) {
            return -1L;
        }
        try (Query<TimedCountDb> query = box.query().build()) {
            return query.property(TimedCountDb_.serverId).min();
        } catch (Exception e) {
            Log.e(TAG,"Error retrieving minimal Server ID of ObjectBox timed count!" + e);
            return -1L;
        }
    }

    public static ArrayList<TimedCountDb> getTimedCountsForUpload() {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        try (Query<TimedCountDb> query = box.query(
                TimedCountDb_.serverId.isNull()
        ).build()) {
            ArrayList<TimedCountDb> timedCounts = (ArrayList<TimedCountDb>) query.find();
            Log.i(TAG, "There are " + timedCounts.size() + " timed counts awaiting upload in the database.");
            return timedCounts;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving timed counts from database!", e);
            return new ArrayList<>();
        }
    }

    public static TimedCountDb getTimedCountById(long timedCountId) {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        try (Query<TimedCountDb> query = box.query(TimedCountDb_.id.equal(timedCountId)).build()) {
            TimedCountDb timedCount = query.findFirst();
            if (timedCount != null) {
                Log.i(TAG, "Timed count with ID " + timedCountId + " found in the database");
            } else {
                Log.i(TAG, "There is no timed count with ID " + timedCountId + " in the database");
            }
            return timedCount;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving timed count by ID: " + timedCountId, e);
            return null;
        }
    }

    public static TimedCountDb getTimedCountByServerId(long id) {
        Box<TimedCountDb> timedCountBox = App.get().getBoxStore().boxFor(TimedCountDb.class);
        try (Query<TimedCountDb> query = timedCountBox.query(
                        TimedCountDb_.serverId.equal(id))
                .build()) {
            return query.findFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error in ObjectBox timed count query for serverId: " + id, e);
            return null;
        }
    }

    /**
     * Checks if there are any observations or timed counts that
     * 1) are not been uploaded or 2) are modified.
     * @return the total count of records that needs sync with server.
     */
    public static long getUnsyncedCount() {
        Box<EntryDb> entryBox = App.get().getBoxStore().boxFor(EntryDb.class);
        Box<TimedCountDb> timedBox = App.get().getBoxStore().boxFor(TimedCountDb.class);

        // Count observations: serverId is 0 OR modified flag is true
        long unsyncedEntries = entryBox.query(
                EntryDb_.uploaded.equal(false)
                        .or(EntryDb_.modified.equal(true))
        ).build().count();

        // Count timed counts: serverId is 0 OR modified flag is true
        long unsyncedTimedCounts = timedBox.query(
                TimedCountDb_.uploaded.equal(false)
                        .or(TimedCountDb_.modified.equal(true))
        ).build().count();

        return unsyncedEntries + unsyncedTimedCounts;
    }

    public static TaxonDb getTaxonById(long taxonId) {
        Box<TaxonDb> box = App.get().getBoxStore().boxFor(TaxonDb.class);
        try (Query<TaxonDb> query = box.query(TaxonDb_.id.equal(taxonId)).build()) {
            TaxonDb taxon = query.findFirst();
            if (taxon != null) {
                Log.i(TAG, "Taxon with ID " + taxonId + " found in the database");
            } else {
                Log.i(TAG, "There is no taxon with ID " + taxonId + " in the database");
            }
            return taxon;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving taxon by ID: " + taxonId, e);
            return null;
        }
    }

    public static long getIdFromServerId(long serverId) {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        try (Query<TimedCountDb> query = box.query(TimedCountDb_.serverId.equal(serverId)).build()) {
            TimedCountDb timedCount = query.findFirst();
            if (timedCount != null) {
                Log.i(TAG,
                        "ObjectBox ID of the timed count is " + timedCount.getId()
                                + " (Timed count ID (" + serverId + ")).");
                return timedCount.getId();
            } else {
                Log.i(TAG, "Could not find ObjectBox id for time count server ID " + serverId + ".");
                return 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving ObjectBox ID from time count server ID: " + serverId, e);
            return 0;
        }
    }

    public static int getUniqueTimedCountID() {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        try (Query<TimedCountDb> query = box.query().build()) {
            if (query.count() == 0) {
                Log.i(TAG, "Timed counts database is empty, returning ID 1.");
                return 1;
            } else {
                // Note: property().max() internally handles query closing correctly, but having
                // the query declared in try-with-resources is safer overall.
                int maxId = (int) query.property(TimedCountDb_.id).max();
                Log.i(TAG, "The last timed count in the database has ID " + maxId + ".");
                return ++maxId;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique timed count ID!", e);
            return 1; // Default to 1 on failure
        }
    }

    public static ArrayList<EntryDb> getTimedCountObservations(long timedCountId) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        try (Query<EntryDb> query = box.query(EntryDb_.timedCoundId.equal(timedCountId)).build()) {
            ArrayList<EntryDb> observations = (ArrayList<EntryDb>) query.find();
            Log.i(TAG, "There are " + observations.size() + " observations in the database.");
            return observations;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving timed count observations by ID: " + timedCountId, e);
            return new ArrayList<>();
        }
    }

    public static long setTimedCount(TimedCountDb timedCount) {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        long id = box.put(timedCount);
        Log.d(TAG, "TimedCount will be saved in TimedCountDb under ID " + id);
        return id;
    }

    public static Location calculateCentroidLocation(Long timedCountId) {
        List<EntryDb> observations = getTimedCountObservations(timedCountId);

        if (observations.isEmpty()) {
            return null;
        }

        double sumLat = 0.0;
        double sumLon = 0.0;
        double sumAlt = 0.0;
        int count = 0;

        // Get all the data from timed count entries
        for (EntryDb entry : observations) {
            if (entry.getLattitude() != 0 && entry.getLongitude() != 0) {
                sumLat += entry.getLattitude();
                sumLon += entry.getLongitude();
                if (entry.getElevation() != 0) {
                    sumAlt += entry.getElevation();
                }
                count++;
            }
        }

        // Create average value
        if (count > 0) {
            Location centralLocation = new Location("TimedCountCentroid");
            centralLocation.setLatitude(sumLat / count);
            centralLocation.setLongitude(sumLon / count);
            if (sumAlt > 0) {
                centralLocation.setAltitude(sumAlt / count);
            } else {
                // If no altitude data, perhaps set it to 0 or null depending on your model
                centralLocation.setAltitude(0.0);
            }
            return centralLocation;
        }

        return null;
    }

    public static StageDb getStageById(long stageId) {
        Box<StageDb> box = App.get().getBoxStore().boxFor(StageDb.class);
        try (Query<StageDb> query = box.query(StageDb_.id.equal(stageId)).build()) {
            StageDb stage = query.findFirst();
            if (stage != null) {
                Log.i(TAG, "Stage with ID " + stageId + " found in the database");
            } else {
                Log.i(TAG, "There is no stage with ID " + stageId + " in the database");
            }
            return stage;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving stage by ID: " + stageId, e);
            return null;
        }
    }

    public static ArrayList<StageDb> getStagesForTaxonId(long taxonId) {
        Box<StageDb> box = App.get().getBoxStore().boxFor(StageDb.class);
        try (Query<StageDb> query = box.query(StageDb_.id.equal(taxonId)).build()) {
            ArrayList<StageDb> stages = (ArrayList<StageDb>) query.find();
            Log.i(TAG, "There are " + stages.size() + " stages for taxon ID " + taxonId + ".");
            return stages;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving stages for taxon ID: " + taxonId, e);
            return new ArrayList<>();
        }
    }

    public static int getStagesForTaxonIdCount(long taxonId) {
        // This method relies on getStagesForTaxonId, which is now safe, so no change needed here.
        ArrayList<StageDb> stages = getStagesForTaxonId(taxonId);
        return stages.size();
    }

    public static Long getAdultStageIdForTaxon(TaxonDb taxon) {
        String stages = taxon.getStages();
        Long stageId = null;
        if (stages != null && !stages.isEmpty()) {
            Log.i(TAG, "Taxon contains stages.");
            String[] all_stages = stages.split(";");

            Box<StageDb> box = App.get().getBoxStore().boxFor(StageDb.class);

            try (Query<StageDb> query = box.query(StageDb_.name.equal("adult")).build()) {
                StageDb stage = query.findFirst();
                if (stage != null) {
                    String s = String.valueOf(stage.getId());
                    if (Arrays.asList(all_stages).contains(s)) {
                        stageId = stage.getId();
                        Log.i(TAG, "Taxon contains adult stage (ID: " + stageId + ").");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting adult stage ID for taxon!", e);
                return null;
            }
        }
        return stageId;
    }

    public static List<ObservationTypesDb> getObservationTypes() {
        Box<ObservationTypesDb> box = App.get().getBoxStore().boxFor(ObservationTypesDb.class);
        try (Query<ObservationTypesDb> query = box.query(ObservationTypesDb_.locale.equal(Localisation.getLocaleScript())).build()) {
            List<ObservationTypesDb> list = query.find();
            Log.i(TAG, "There are " + list.size() + " observation types for locale " + Localisation.getLocaleScript() + ".");
            return list;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving observation types!", e);
            return new ArrayList<>();
        }
    }

    public static Long getIdForObservedTag() {
        Box<ObservationTypesDb> box = App.get().getBoxStore().boxFor(ObservationTypesDb.class);
        try (Query<ObservationTypesDb> query = box.query(ObservationTypesDb_.slug.equal("observed")).build()) {
            ObservationTypesDb observationTypes = query.findFirst();

            Long observed_id = null;
            if (observationTypes != null) {
                observed_id = observationTypes.getObservationId();
                Log.i(TAG, "Observed tag has ID " + observed_id + ".");
            }
            return observed_id;
        } catch (Exception e) {
            Log.e(TAG, "Error getting ID for observed tag!", e);
            return null;
        }
    }

    public static Long getIdForPhotographedTag() {
        Box<ObservationTypesDb> box = App.get().getBoxStore().boxFor(ObservationTypesDb.class);
        try (Query<ObservationTypesDb> query = box.query(ObservationTypesDb_.slug.equal("photographed")).build()) {
            ObservationTypesDb observationTypes = query.findFirst();
            return (observationTypes != null) ? observationTypes.getObservationId() : null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting ID for photographed tag!", e);
            return null;
        }
    }

    public static void removePhoto(long photoId) {
        if (photoId > 0) {
            Box<PhotoDb> box = App.get().getBoxStore().boxFor(PhotoDb.class);
            box.remove(photoId);
            Log.d(TAG, "Photo with ID " + photoId + " removed from database.");
        }
    }

    public static AnnouncementsDb getAnnouncementById(long id) {
        Box<AnnouncementsDb> box = App.get().getBoxStore().boxFor(AnnouncementsDb.class);
        try (Query<AnnouncementsDb> query = box
                .query(AnnouncementsDb_.id.equal(id))
                .build()) {
            return query.findFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving announcement by ID: " + id, e);
            return null;
        }
    }

    public static AnnouncementTranslationsDb getTranslatedAnnouncementById(long announcementId) {
        String locale = Localisation.getLocaleScript();
        Box<AnnouncementTranslationsDb> transBox = App.get().getBoxStore().boxFor(AnnouncementTranslationsDb.class);
        try (Query<AnnouncementTranslationsDb> transQuery = transBox
                .query(AnnouncementTranslationsDb_.announcementId.equal(announcementId)
                        .and(AnnouncementTranslationsDb_.locale.equal(locale)))
                .build()) {
            return transQuery.findFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving translated announcement by ID: " + announcementId, e);
            return null;
        }
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

    public static String getUserName() {
        UserDb user = getUser();
        return (user != null) ? user.getUsername() : null;
    }

    public static String getUserEmail() {
        UserDb user = getUser();
        return (user != null) ? user.getEmail() : null;
    }

    public static Integer getUserId() {
        UserDb user = getUser();
        return (user != null) ? user.getUserId() : null;
    }

    // Get the data from ObjectBox database
    public static UserDb getUser() {
        Box<UserDb> box = App.get().getBoxStore().boxFor(UserDb.class);
        try (Query<UserDb> query = box.query().build()) {
            return query.findFirst();
        } catch (RuntimeException e) {
            Log.e(TAG, "Error retrieving user from database!", e);
            return null;
        }
    }

    public static void removeObservationsForTimedCountId(long timedCountId) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        long removedCount;
        try (Query<EntryDb> query = box.query(EntryDb_.timedCoundId.equal(timedCountId)).build()) {
            removedCount = query.remove();
        }
        Log.i(TAG, "Removed " + removedCount + " objects for time count ID " + timedCountId + ".");
    }

    public static void removeObservationById(long observationId) {
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        long removedCount;
        try (Query<EntryDb> query = box.query(EntryDb_.id.equal(observationId)).build()) {
            removedCount = query.remove();
        }
        Log.i(TAG, "Removed " + removedCount + " observation with ID " + observationId + ".");
    }

    public static void removeTimedCountById(long timedCountId) {
        Box<TimedCountDb> box = App.get().getBoxStore().boxFor(TimedCountDb.class);
        try (Query<TimedCountDb> query = box.query(TimedCountDb_.id.equal(timedCountId)).build()) {
            long removedCount = query.remove();
            Log.i(TAG, "Removed " + removedCount + " timed count with ID " + timedCountId + ".");
        }
    }

    public static void removeAllObservations() {
        App.get().getBoxStore().boxFor(PhotoDb.class).removeAll();
        App.get().getBoxStore().boxFor(EntryDb.class).removeAll();
    }

    public static void removeAllTimedCounts() {
        App.get().getBoxStore().boxFor(TimedCountDb.class).removeAll();
    }

    public static void removeAllEntries() {
        removeAllObservations();
        removeAllTimedCounts();
    }

    public static void removeTaxaDatabase() {
        App.get().getBoxStore().boxFor(TaxonDb.class).removeAll();
        App.get().getBoxStore().boxFor(TaxaTranslationDb.class).removeAll();
        App.get().getBoxStore().boxFor(SynonymsDb.class).removeAll();
        App.get().getBoxStore().boxFor(TaxonGroupsDb.class).removeAll();
        App.get().getBoxStore().boxFor(TaxonGroupsTranslationDb.class).removeAll();
        App.get().getBoxStore().boxFor(StageDb.class).removeAll();
        App.get().getBoxStore().boxFor(ObservationTypesDb.class).removeAll();
    }

    public static void removeAllData(Context context) {
        // 1. Remove user data
        App.get().getBoxStore().boxFor(UserDb.class).removeAll();

        // 2. Remove announcements and notifications
        removeAnnouncements();
        NotificationsHelper.deleteAllNotificationsLocally(context);

        // Remove taxa and observations
        removeAllEntries();
        removeTaxaDatabase();
    }

    private static void removeAnnouncements() {
        App.get().getBoxStore().boxFor(AnnouncementsDb.class).removeAll();
        App.get().getBoxStore().boxFor(AnnouncementTranslationsDb.class).removeAll();
    }

    public static Boolean hasAtlasCode(long taxonId) {
        TaxonDb taxon = getTaxonById(taxonId);
        if (taxon != null) {
            if (taxon.isUseAtlasCode()) {
                Log.d(TAG, "There is atlas code for taxon ID: " + taxonId);
                return true;
            } else {
                Log.d(TAG, "There is no atlas code for taxon ID: " + taxonId);
                return false;
            }
        } else {
            return false;
        }
    }

}