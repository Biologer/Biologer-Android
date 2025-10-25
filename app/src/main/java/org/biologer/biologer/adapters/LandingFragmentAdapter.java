package org.biologer.biologer.adapters;

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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.biologer.biologer.R;

import java.util.ArrayList;
import java.util.List;

public class LandingFragmentAdapter
        extends RecyclerView.Adapter<LandingFragmentAdapter.ViewHolder> {
    private final List<LandingFragmentItems> myEntries;
    private int position;
    private static final String TAG = "Biologer.EntryAdapter";
    View view;

    public List<LandingFragmentItems> getData() {
        return myEntries;
    }

    public LandingFragmentAdapter(ArrayList<LandingFragmentItems> entries) {
        myEntries = entries;
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder
            implements View.OnCreateContextMenuListener {

        public ImageView imageView;
        public TextView textTitle;
        public TextView textSubtitle;

        public ViewHolder(View view) {
            super(view);

            // Define click listener for the ViewHolder's View
            imageView = view.findViewById(R.id.entry_image);
            textTitle = view.findViewById(R.id.entry_taxon_name);
            textSubtitle = view.findViewById(R.id.entry_stage);

            // Add the entry items menu
            view.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
            Log.d(TAG, "On create context menu from EntryRecycleView fragment.");
            menu.add(Menu.NONE, R.id.duplicate,
                    Menu.NONE, R.string.duplicate_entry);
            menu.add(Menu.NONE, R.id.delete,
                    Menu.NONE, R.string.delete_entry);
            menu.add(Menu.NONE, R.id.delete_all,
                    Menu.NONE, R.string.delete_all_entries);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        // Create a new view, which defines the UI of the list item
        view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.entries_list, viewGroup, false);

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        LandingFragmentItems entry = myEntries.get(position);

        // Get the title
        TextView titleText = viewHolder.textTitle;
        String title = entry.getTitle();
        titleText.setText(title);

        // Get the subtitle
        TextView subtitleText = viewHolder.textSubtitle;
        String subtitle = entry.getSubtitle();
        subtitleText.setText(subtitle);

        ImageView entryImage = viewHolder.imageView;
        entryImage.setImageDrawable(null); // Clear the previous image
        String image = entry.getImage();

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
            Glide.with(view.getContext())
                    .load(image)
                    .override(40, 40)
                    .into(entryImage);
        }

        viewHolder.itemView.setOnLongClickListener(v -> {
            Log.d(TAG, "Long click on " + viewHolder.getLayoutPosition());
            return false;
        });
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

}
