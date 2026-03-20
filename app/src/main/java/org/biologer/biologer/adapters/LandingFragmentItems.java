package org.biologer.biologer.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.biologer.biologer.R;
import org.biologer.biologer.helpers.DateHelper;
import org.biologer.biologer.helpers.Localisation;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.TimedCountDb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

public class LandingFragmentItems {
    private static final String TAG = "Biologer.LandingItems";
    private Long observationId;
    private boolean uploaded;
    private boolean modified;
    private Integer timedCountId;
    private String title;
    private String subtitle;
    private String image;
    private Date date;
    private boolean marked = false;

    public LandingFragmentItems(Long observationId, Integer timedCountId,
                                boolean uploaded, boolean modified,
                                String title, String subtitle, String image,
                                Date date) {
        this.observationId = observationId;
        this.timedCountId = timedCountId;
        this.uploaded = uploaded;
        this.modified = modified;
        this.title = title;
        this.subtitle = subtitle;
        this.image = image;
        this.date = date;
    }

    public Long getObservationId() {
        return observationId;
    }

    public void setObservationId(Long observationId) {
        this.observationId = observationId;
    }

    public Integer getTimedCountId() {
        return timedCountId;
    }

    public void setTimedCountId(Integer timedCountId) {
        this.timedCountId = timedCountId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    // Loads all LandingFragmentItems into the RecyclerView list of items
    public static ArrayList<LandingFragmentItems> loadAllEntries(Context context) {
        // This list will hold all the items displayed
        ArrayList<LandingFragmentItems> items = new ArrayList<>();

        // Get the ObjectBox entries containing regular species observation data
        ArrayList<EntryDb> observations = ObjectBoxHelper.getObservations();
        Log.d(TAG, "There are " + observations.size() + " regular observations.");
        for (EntryDb entry : observations) {
            items.add(getItemFromEntry(context, entry));
        }

        // Get the ObjectBox entries containing timed count data
        ArrayList<TimedCountDb> timedCounts = ObjectBoxHelper.getTimedCounts();
        Log.d(TAG, "There are " + timedCounts.size() + " timed counts.");
        for (TimedCountDb timedCountDb : timedCounts) {
            items.add(getItemFromTimedCount(context, timedCountDb));
        }

        // Sort items based of preference settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sortBy = prefs.getString("sort_observations", "time");

        if ("name".equals(sortBy)) {
            // Sort alphabetically by title (case-insensitive)
            Collections.sort(items, (item1, item2) ->
                    item1.getTitle().compareToIgnoreCase(item2.getTitle()));
        } else {
            // Sort by date descending (newest first)
            Collections.sort(items, (item1, item2) ->
                    item2.getDate().compareTo(item1.getDate()));
        }

        return items;
    }

    public static LandingFragmentItems getItemFromEntry(Context context, EntryDb entry) {

        Long observationId = entry.getId();
        String title = entry.getTaxonSuggestion();

        String subtitle = "";
        Long stage_id = entry.getStage();
        if (stage_id != null) {
            StageDb stage = ObjectBoxHelper.getStageById(stage_id);
            if (stage != null) {
                subtitle = Localisation.getStageLocale(context, stage.getName());
            }
        }

        String image = null;
        if (entry.photos != null && !entry.photos.isEmpty()) {
            image = entry.photos.get(0).getLocalPath();
        }

        Calendar calendar = DateHelper.getCalendar(entry.getYear(),
                entry.getMonth(), entry.getDay(), entry.getTime());

        return new LandingFragmentItems(
                observationId,
                null,
                entry.isUploaded(),
                entry.isModified(),
                title,
                subtitle,
                image,
                calendar.getTime()
        );
    }

    /**
     * Finds the correct index to insert a new item into an already sorted list,
     * based on the user's current sorting preference ("time" or "name").
     *
     * @param context The application context to access SharedPreferences.
     * @param items The existing, sorted list of LandingFragmentItems.
     * @param newItem The new item to be inserted.
     * @return The index where the newItem should be inserted.
     */
    public static int findSortedInsertionIndex(Context context, ArrayList<LandingFragmentItems> items, LandingFragmentItems newItem) {

        if (items.isEmpty()) {
            return 0;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sortBy = prefs.getString("sort_observations", "time");

        if ("name".equals(sortBy)) {
            // Sort by name
            for (int i = 0; i < items.size(); i++) {
                // Check if the new item's title comes before the current item's title
                if (newItem.getTitle().compareToIgnoreCase(items.get(i).getTitle()) < 0) {
                    return i;
                }
            }
        } else {
            // Sort by date descending
            for (int i = 0; i < items.size(); i++) {
                if (newItem.getDate().compareTo(items.get(i).getDate()) > 0) {
                    return i;
                }
            }
        }

        return items.size();
    }

    public static LandingFragmentItems getItemFromTimedCount(Context context, TimedCountDb timed_count) {
        String title = context.getString(R.string.timed_count);
        String image = "timed_count";
        String subtitle;
        Calendar calendar = DateHelper.getCalendar(timed_count.getYear(),
                timed_count.getMonth(), timed_count.getDay(), timed_count.getStartTime());
        subtitle = DateHelper.getLocalizedCalendarDate(calendar) + " " +
                context.getString(R.string.at_time) + " " +
                DateHelper.getLocalizedCalendarTime(calendar);

        return new LandingFragmentItems(
                null,
                timed_count.getTimedCountId(),
                timed_count.isUploaded(),
                timed_count.isModified(),
                title,
                subtitle,
                image,
                calendar.getTime()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LandingFragmentItems that = (LandingFragmentItems) o;

        if (!Objects.equals(observationId, that.observationId))
            return false;
        if (!Objects.equals(timedCountId, that.timedCountId))
            return false;
        if (!Objects.equals(title, that.title)) return false;
        if (!Objects.equals(subtitle, that.subtitle)) return false;
        if (!Objects.equals(image, that.image)) return false;
        return Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        int result = observationId != null ? observationId.hashCode() : 0;
        result = 31 * result + (timedCountId != null ? timedCountId.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (subtitle != null ? subtitle.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }
}
