package org.biologer.biologer.adapters;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.biologer.biologer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LandingFragmentAdapter
        extends RecyclerView.Adapter<LandingFragmentAdapter.ViewHolder> {
    private final List<LandingFragmentItems> myEntries;
    private int position;
    private static final String TAG = "Biologer.EntryAdapter";
    View view;
    private final boolean isTimedCountObservation;

    public List<LandingFragmentItems> getData() {
        return myEntries;
    }

    public LandingFragmentAdapter(ArrayList<LandingFragmentItems> entries, boolean isTimedCountObservation) {
        myEntries = entries;
        this.isTimedCountObservation = isTimedCountObservation;
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        public ImageView thumbnailImage;
        public ImageView uploadStatus;
        public TextView textTitle;
        public TextView textSubtitle;
        private boolean isTimedCount = false;
        private final boolean isTimedCountObservation;

        public ViewHolder(View view, boolean isTimedCountObservation) {
            super(view);

            // Define click listener for the ViewHolder's View
            thumbnailImage = view.findViewById(R.id.entry_image);
            uploadStatus = view.findViewById(R.id.image_view_entry_upload_status);
            textTitle = view.findViewById(R.id.entry_taxon_name);
            textSubtitle = view.findViewById(R.id.entry_stage);

            // Add the entry items menu
            //view.setOnCreateContextMenuListener(this);
            this.isTimedCountObservation = isTimedCountObservation;
        }

        public void setIsTimedCount(boolean isTimedCount) {
            this.isTimedCount = isTimedCount;
        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        // Create a new view, which defines the UI of the list item
        view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.entries_list, viewGroup, false);

        return new ViewHolder(view, isTimedCountObservation);

    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        LandingFragmentItems item = myEntries.get(position);
        viewHolder.setIsTimedCount(item.getTimedCountId() != null);

        // Get the title
        TextView titleText = viewHolder.textTitle;
        String title = item.getTitle();
        titleText.setText(title);

        // Get the subtitle
        TextView subtitleText = viewHolder.textSubtitle;
        String subtitle = item.getSubtitle();
        subtitleText.setText(subtitle);

        ImageView entryImage = viewHolder.thumbnailImage;
        entryImage.setImageDrawable(null); // Clear the previous image
        String image = item.getImage();

        if (image == null) {
            Drawable defaultImage = ContextCompat.getDrawable(view.getContext(), R.mipmap.ic_kornjaca_kocka);
            Glide.with(view.getContext())
                    .load(defaultImage)
                    .into(entryImage);
        } else if (image.equals("timed_count")) {
            Drawable defaultImage = ContextCompat.getDrawable(view.getContext(), R.drawable.ic_timer);
            Glide.with(view.getContext())
                    .load(defaultImage)
                    .into(entryImage);
        } else {
            Log.d(TAG, "Loading thumbnail image: " + image);
            File file;
            if (image.startsWith("file://")) {
                file = new File(image.replace("file://", ""));
            } else {
                file = new File(image);
            }

            Glide.with(view.getContext())
                    .load(file)
                    .override(40, 40)
                    .into(entryImage);
        }

        if (viewHolder.uploadStatus != null) {
            viewHolder.uploadStatus.clearColorFilter();
            setAlpha(viewHolder, 1.0f);
            if (item.isUploaded()) {
                viewHolder.uploadStatus.setVisibility(View.VISIBLE);
                if (item.isModified()) {
                    // Modified entry
                    viewHolder.uploadStatus.setImageResource(R.drawable.ic_modified);
                    int grayColor = ContextCompat.getColor(viewHolder.itemView.getContext(), R.color.icon_color);
                    viewHolder.uploadStatus.setColorFilter(grayColor, PorterDuff.Mode.SRC_IN);
                    setAlpha(viewHolder, 1.0f);
                } else {
                    // Uploaded entry
                    viewHolder.uploadStatus.setImageResource(R.drawable.ic_uploaded);
                    int okColor = ContextCompat.getColor(viewHolder.itemView.getContext(), R.color.colorPrimaryDark);
                    viewHolder.uploadStatus.setColorFilter(okColor, PorterDuff.Mode.SRC_IN);
                    setAlpha(viewHolder, 0.6f);
                }
            } else {
                // New entry, not uploaded
                viewHolder.uploadStatus.setVisibility(View.GONE);
                setAlpha(viewHolder, 1.0f);
            }
        }

        if (item.isMarked()) {
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(viewHolder.itemView.getContext(), R.color.colorPrimaryLight));
        } else {
            viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

    }

    private void setAlpha(ViewHolder viewHolder, float alpha) {
        if (viewHolder.thumbnailImage != null) viewHolder.thumbnailImage.setAlpha(alpha);
        if (viewHolder.textTitle != null) viewHolder.textTitle.setAlpha(alpha);
        if (viewHolder.textSubtitle != null) viewHolder.textSubtitle.setAlpha(alpha);
    }

    @Override
    public int getItemCount() {
        return myEntries.size();
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public void onViewRecycled(ViewHolder viewHolder) {
        viewHolder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(viewHolder);
    }

    public static class LandingDiffCallback extends DiffUtil.Callback {
        private final List<LandingFragmentItems> oldList;
        private final List<LandingFragmentItems> newList;

        public LandingDiffCallback(List<LandingFragmentItems> oldList, List<LandingFragmentItems> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            LandingFragmentItems oldItem = oldList.get(oldItemPosition);
            LandingFragmentItems newItem = newList.get(newItemPosition);

            // Of Observation, compare ObservationId
            if (oldItem.getObservationId() != null && newItem.getObservationId() != null) {
                return oldItem.getObservationId().equals(newItem.getObservationId());
            }
            // If Timed Count, compare TimedCountId
            if (oldItem.getTimedCountId() != null && newItem.getTimedCountId() != null) {
                return oldItem.getTimedCountId().equals(newItem.getTimedCountId());
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    public void updateData(List<LandingFragmentItems> newItems) {
        LandingDiffCallback diffCallback = new LandingDiffCallback(this.myEntries, newItems);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.myEntries.clear();
        this.myEntries.addAll(newItems);

        diffResult.dispatchUpdatesTo(this);
    }

}
