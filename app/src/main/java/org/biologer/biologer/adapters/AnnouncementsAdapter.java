package org.biologer.biologer.adapters;

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
    private int position;
    private static final String TAG = "Biologer.EntryAdapter";

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
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        AnnouncementsDb announcement = myAnnouncements.get(position);

        TextView textAnnouncementTitle = viewHolder.textAnnouncementTitle;
        textAnnouncementTitle.setText(announcement.getTitle());
        TextView textAnnouncementText = viewHolder.textAnnouncementText;
        textAnnouncementText.setText(announcement.getMessage());
    }

    @Override
    public int getItemCount() {
        return myAnnouncements.size();
    }

}
