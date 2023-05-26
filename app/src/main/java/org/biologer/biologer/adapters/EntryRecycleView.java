package org.biologer.biologer.adapters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.gui.LandingActivity;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.StageDb_;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class EntryRecycleView
        extends RecyclerView.Adapter<EntryRecycleView.ViewHolder> {
    private final List<EntryDb> myEntries;
    private int position;
    private static final String TAG = "Biologer.EntryAdapter";
    View view;

    public static class ViewHolder
            extends RecyclerView.ViewHolder
            implements View.OnCreateContextMenuListener {

        public ImageView imageEntry;
        public TextView textTaxonName;
        public TextView textTaxonStage;

        public ViewHolder(View view) {
            super(view);

            // Define click listener for the ViewHolder's View
            imageEntry = view.findViewById(R.id.entry_image);
            textTaxonName = view.findViewById(R.id.entry_taxon_name);
            textTaxonStage = view.findViewById(R.id.entry_stage);

            // Add the entry items menu
            view.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
            Log.d(TAG, "On create context menu from EntryRecycleView fragment.");
            menu.add(Menu.NONE, R.id.delete,
                    Menu.NONE, R.string.delete_entry);
            menu.add(Menu.NONE, R.id.delete_all,
                    Menu.NONE, R.string.delete_all_entries);
        }
    }

    public EntryRecycleView(ArrayList<EntryDb> entries) {
        myEntries = entries;
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

        EntryDb entry = myEntries.get(position);

        // Get the taxon name
        TextView taxonName = viewHolder.textTaxonName;
        String taxon_name = entry.getTaxonSuggestion();
        if (taxon_name != null) {
            taxonName.setText(taxon_name);
        } else {
            taxonName.setText("");
        }

        // Get the taxon stage
        TextView taxonStage = viewHolder.textTaxonStage;
        Long stage_id = entry.getStage();
        if (stage_id != null) {
            Box<StageDb> stageBox = ObjectBox.get().boxFor(StageDb.class);

            Query<StageDb> query = stageBox
                    .query(StageDb_.stageId.equal(stage_id))
                    .build();
            List<StageDb> results = query.find();
            String s = results.get(0).getName();
            query.close();
            taxonStage.setText(StageAndSexLocalization.getStageLocale(taxonStage.getContext(), s));
        } else {
            taxonStage.setText("");
        }

        ImageView entryImage = viewHolder.imageEntry;
        // Get the image
        String useImage = null;
        if (entry.getSlika3() != null) {
            useImage = entry.getSlika3();
        }
        if (entry.getSlika2() != null) {
            useImage = entry.getSlika2();
        }
        if (entry.getSlika1() != null) {
            useImage = entry.getSlika1();
        }

        if (useImage == null) {
            Drawable defaultImage = ContextCompat.getDrawable(view.getContext(), R.mipmap.ic_kornjaca_kocka);
            Glide.with(view.getContext())
                    .load(defaultImage)
                    .into(entryImage);
        } else {
            Glide.with(view.getContext())
                    .load(useImage)
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

    public void removeAll() {
        int last_index = this.getItemCount();
        myEntries.clear();
        for (int i = 0; i < last_index; i++) {
            this.notifyItemRemoved(i);
        }
    }

    @Override
    public void onViewRecycled(ViewHolder viewHolder) {
        viewHolder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(viewHolder);
    }

}
