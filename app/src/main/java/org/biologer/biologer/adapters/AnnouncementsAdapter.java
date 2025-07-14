package org.biologer.biologer.adapters;

import android.graphics.Typeface;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.R;
import org.biologer.biologer.sql.AnnouncementsDb;

import java.util.List;

public class AnnouncementsAdapter
        extends RecyclerView.Adapter<AnnouncementsAdapter.ViewHolder> {
    private final List<AnnouncementsDb> myAnnouncements;

    public AnnouncementsAdapter(List<AnnouncementsDb> myAnnouncements) {
        this.myAnnouncements = myAnnouncements;
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        public TextView textAnnouncementTitle;
        public TextView textAnnouncementText;

        public ViewHolder(View view) {
            super(view);

            // Define click listener for the ViewHolder's View
            textAnnouncementTitle = view.findViewById(R.id.announcement_name);
            textAnnouncementText = view.findViewById(R.id.announcement_text);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.announcements_list, viewGroup, false);

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Log.d("AnnouncementsAdapter", "Binding to the announcements adapter now!");

        AnnouncementsDb announcement = myAnnouncements.get(position);

        TextView textAnnouncementTitle = viewHolder.textAnnouncementTitle;
        textAnnouncementTitle.setText(announcement.getTitle());
        TextView textAnnouncementText = viewHolder.textAnnouncementText;
        textAnnouncementText.setText(Html.fromHtml(announcement.getMessage()));

        if (!announcement.isRead()) {
            textAnnouncementTitle.setTypeface(null, Typeface.BOLD);
            textAnnouncementText.setTypeface(null, Typeface.BOLD);
        }

    }

    @Override
    public int getItemCount() {
        if (myAnnouncements != null) {
            return myAnnouncements.size();
        } else {
            return 0;
        }
    }

}
