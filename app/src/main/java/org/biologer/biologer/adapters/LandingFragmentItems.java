package org.biologer.biologer.adapters;

import android.content.Context;
import android.util.Log;

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
    private Long localId;
    private Long serverId;
    private boolean uploaded;
    private boolean modified;
    private boolean isTimedCount;
    private String title;
    private String subtitle;
    private String image;
    private Date date;
    private boolean marked = false;

    public LandingFragmentItems(Long localId, Long serverId, boolean isTimedCount,
                                boolean uploaded, boolean modified,
                                String title, String subtitle, String image,
                                Date date) {
        this.localId = localId;
        this.serverId = serverId;
        this.isTimedCount = isTimedCount;
        this.uploaded = uploaded;
        this.modified = modified;
        this.title = title;
        this.subtitle = subtitle;
        this.image = image;
        this.date = date;
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
    public static ArrayList<LandingFragmentItems> loadAllLocalEntries(Context context) {
        // This list will hold all the items displayed
        ArrayList<LandingFragmentItems> items = new ArrayList<>();

        // Get the ObjectBox entries containing regular species observation data
        ArrayList<EntryDb> observations = ObjectBoxHelper.getObservationsForUpload();
        Log.d(TAG, "There are " + observations.size() + " regular observations awaiting upload.");
        for (EntryDb entry : observations) {
            items.add(getItemFromEntry(context, entry));
        }

        // Get the ObjectBox entries containing timed count data
        ArrayList<TimedCountDb> timedCounts = ObjectBoxHelper.getTimedCountsForUpload();
        Log.d(TAG, "There are " + timedCounts.size() + " timed counts awaiting upload.");
        for (TimedCountDb timedCountDb : timedCounts) {
            items.add(getItemFromTimedCount(context, timedCountDb));
        }

        // Sort by date descending (newest first)
        Collections.sort(items, (item1, item2) ->
                item2.getDate().compareTo(item1.getDate()));

        return items;
    }

    public static LandingFragmentItems getItemFromEntry(Context context, EntryDb entry) {

        if (entry.getId() == 0) {
            Log.e(TAG, "Building UI item for Entry with ID 0! This observation will be un-clickable.");
        }

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
            Log.d(TAG, "Entry " + entry.getId() + " has image path: " + image);
        }

        Calendar calendar = DateHelper.getCalendar(
                Integer.parseInt(entry.getYear()),
                Integer.parseInt(entry.getMonth()),
                Integer.parseInt(entry.getDay()),
                entry.getTime());

        return new LandingFragmentItems(
                entry.getId(),
                entry.getServerId(),
                false,
                entry.isUploaded(),
                entry.isModified(),
                entry.getTaxonSuggestion(),
                subtitle,
                image,
                calendar.getTime()
        );
    }

    public static LandingFragmentItems getItemFromTimedCount(Context context, TimedCountDb timed_count) {

        if (timed_count.getId() == 0) {
            Log.e(TAG, "Building UI item for Entry with ID 0! This timed count will be un-clickable.");
        }

        String title = context.getString(R.string.timed_count);
        String image = "timed_count";
        String subtitle;
        Calendar calendar = DateHelper.getCalendar(timed_count.getYear(),
                timed_count.getMonth(), timed_count.getDay(), timed_count.getStartTime());
        subtitle = DateHelper.getLocalizedCalendarDate(calendar) + " " +
                context.getString(R.string.at_time) + " " +
                DateHelper.getLocalizedCalendarTime(calendar);

        return new LandingFragmentItems(
                timed_count.getId(),
                timed_count.getServerId(),
                true,
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

        return Objects.equals(localId, that.localId) &&
                Objects.equals(isTimedCount, that.isTimedCount) &&
                Objects.equals(title, that.title) &&
                Objects.equals(subtitle, that.subtitle) &&
                Objects.equals(image, that.image) &&
                uploaded == that.uploaded &&
                modified == that.modified &&
                (date != null && that.date != null
                        ? date.getTime() == that.date.getTime()
                        : date == that.date);
    }

    @Override
    public int hashCode() {
        int result = localId != null ? localId.hashCode() : 0;
        result = 31 * result + (isTimedCount ? 1231 : 1237);
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

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    public boolean isTimedCount() {
        return isTimedCount;
    }
}
