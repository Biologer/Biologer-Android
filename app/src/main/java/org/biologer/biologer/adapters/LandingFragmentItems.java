package org.biologer.biologer.adapters;

import android.content.Context;
import android.util.Log;

import org.biologer.biologer.R;
import org.biologer.biologer.services.DateHelper;
import org.biologer.biologer.services.ObjectBoxHelper;
import org.biologer.biologer.services.StageAndSexLocalization;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.TimedCountDb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

public class LandingFragmentItems {
    private Long observationId;
    private Integer timedCountId;
    private String title;
    private String subtitle;
    private String image;
    private Date date;

    public LandingFragmentItems(Long observationId, Integer timedCountId,
                                String title, String subtitle, String image,
                                Date date) {
        this.observationId = observationId;
        this.timedCountId = timedCountId;
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
        Log.d("Biologer.LandingItems", "There are " + observations.size() + " regular observations.");
        for (EntryDb entry : observations) {
            items.add(getItemFromEntry(context, entry));
        }

        // Get the ObjectBox entries containing timed count data
        ArrayList<TimedCountDb> timedCounts = ObjectBoxHelper.getTimedCounts();
        Log.d("Biologer.LandingItems", "There are " + timedCounts.size() + " timed counts.");
        for (TimedCountDb timedCountDb : timedCounts) {
            items.add(getItemFromTimedCount(context, timedCountDb));
        }

        // Sort items in descending order
        Collections.sort(items, (item1, item2) ->
                item2.getDate().compareTo(item1.getDate()));

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
                subtitle = StageAndSexLocalization.getStageLocale(context, stage.getName());
            }
        }

        String image = null;
        if (entry.getSlika3() != null) {
            image = entry.getSlika3();
        }
        if (entry.getSlika2() != null) {
            image = entry.getSlika2();
        }
        if (entry.getSlika1() != null) {
            image = entry.getSlika1();
        }

        Calendar calendar = DateHelper.getCalendar(entry.getYear(),
                entry.getMonth(), entry.getDay(), entry.getTime());

        return new LandingFragmentItems(observationId, null,
                title, subtitle, image, calendar.getTime());
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

        return new LandingFragmentItems(null, timed_count.getTimedCountId(),
                title, subtitle, image, calendar.getTime());
    }

}
