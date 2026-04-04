package org.biologer.biologer.adapters;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.biologer.biologer.R;

import java.io.File;
import java.util.List;

public class LandingFragmentAdapter
        extends ListAdapter<LandingFragmentItems, LandingFragmentAdapter.ViewHolder> {

    public LandingFragmentAdapter(boolean isTimedCountObservation) {
        super(new LandingDiffCallback());
        this.isTimedCountObservation = isTimedCountObservation;
    }
    private int position;
    View view;
    private final boolean isTimedCountObservation;

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

        LandingFragmentItems item = getItem(position);
        viewHolder.setIsTimedCount(item.getServerId() != null);

        // Get the title
        TextView titleText = viewHolder.textTitle;
        String title = item.getTitle();
        titleText.setText(title);

        // Get the subtitle
        TextView subtitleText = viewHolder.textSubtitle;
        String subtitle = item.getSubtitle();
        subtitleText.setText(subtitle);

        ImageView entryImage = viewHolder.thumbnailImage;
        String image = item.getImage();

        if (image == null) {
            Glide.with(view.getContext())
                    .load(R.mipmap.ic_kornjaca_kocka)
                    .into(entryImage);

        } else if (image.equals("timed_count")) {
            Glide.with(view.getContext())
                    .load(R.drawable.ic_timer)
                    .into(entryImage);

        } else {
            File file = image.startsWith("file://")
                    ? new File(image.replace("file://", ""))
                    : new File(image);

            Glide.with(view.getContext())
                    .load(file)
                    .override(40, 40)
                    .dontAnimate()
                    .into(entryImage);
        }

        if (viewHolder.uploadStatus != null) {
            if (item.isUploaded()) {
                viewHolder.uploadStatus.setVisibility(View.VISIBLE);

                if (item.isModified()) {
                    // Case 1. Modified entry
                    viewHolder.uploadStatus.setImageResource(R.drawable.ic_modified);
                    int grayColor = ContextCompat.getColor(viewHolder.itemView.getContext(), R.color.icon_color);
                    viewHolder.uploadStatus.setColorFilter(grayColor, PorterDuff.Mode.SRC_IN);
                    setAlpha(viewHolder, 1.0f);
                } else {
                    // Case 2. Uploaded entry
                    viewHolder.uploadStatus.setImageResource(R.drawable.ic_uploaded);
                    int okColor = ContextCompat.getColor(viewHolder.itemView.getContext(), R.color.colorPrimaryDark);
                    viewHolder.uploadStatus.setColorFilter(okColor, PorterDuff.Mode.SRC_IN);
                    setAlpha(viewHolder, 0.6f);
                }
            } else {
                // Case 3. New entry, not uploaded
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

    public static class LandingDiffCallback extends DiffUtil.ItemCallback<LandingFragmentItems> {
        @Override
        public boolean areItemsTheSame(@NonNull LandingFragmentItems oldItem,
                                       @NonNull LandingFragmentItems newItem) {
            if (oldItem.isTimedCount() != newItem.isTimedCount()) return false;
            if (oldItem.getLocalId() != null && newItem.getLocalId() != null) {
                return oldItem.getLocalId().equals(newItem.getLocalId());
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull LandingFragmentItems oldItem,
                                          @NonNull LandingFragmentItems newItem) {
            return oldItem.equals(newItem);
        }
    }

    public int getItemIndexFromId(long id, boolean isTimedCount) {
        List<LandingFragmentItems> current = getCurrentList();
        for (int i = current.size() - 1; i >= 0; i--) {
            LandingFragmentItems item = current.get(i);
            long localId = item.getLocalId() != null ? item.getLocalId() : -1L;
            if (item.isTimedCount() == isTimedCount && localId == id) {
                return i;
            }
        }
        return -1;
    }

}
