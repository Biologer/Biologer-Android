package org.biologer.biologer.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.StageDb_;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class EntryRecycleView
        extends RecyclerView.Adapter<EntryRecycleView.ViewHolder> {
    private final List<EntryDb> myEntries;
    private int position;
    private static final String TAG = "Biologer.EntryAdapter";

    public static class ViewHolder
            extends RecyclerView.ViewHolder
            implements View.OnCreateContextMenuListener {

        public ImageView imageEntry;
        public TextView textTaxonName;
        public TextView textTaxonStage;

        public ViewHolder(View view) {
            super(view);

            // Define click listener for the ViewHolder's View
            imageEntry = (ImageView) view.findViewById(R.id.entry_image);
            textTaxonName = (TextView) view.findViewById(R.id.entry_taxon_name);
            textTaxonStage = (TextView) view.findViewById(R.id.entry_stage);
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
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.entries_list, viewGroup, false);

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        EntryDb entry = myEntries.get(position);

        // Get the taxon name
        TextView taxonName = viewHolder.textTaxonName;
        String taxon_name = entry.getTaxonSuggestion();
        taxonName.setText(Objects.requireNonNullElse(taxon_name, ""));

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
        if (entry.getSlika1() != null) {
            useImage = entry.getSlika1();
        }
        if (entry.getSlika2() != null) {
            useImage = entry.getSlika2();
        }
        if (entry.getSlika3() != null) {
            useImage = entry.getSlika3();
        }

        if (useImage != null) {
            try {
                Bitmap bitmap = loadBitmap(useImage);
                entryImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        viewHolder.itemView.setOnLongClickListener(v -> {
            Log.d(TAG, "Long click on " + viewHolder.getLayoutPosition());
            setPosition(viewHolder.getLayoutPosition());
            return false;
        });
    }

    public static Bitmap loadBitmap(String url) throws IOException {
        Bitmap bitmap;
        URL newurl = new URL(url);
        bitmap = BitmapFactory.decodeStream(newurl.openConnection().getInputStream());
        return bitmap;
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
    public void onViewRecycled(ViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

}
