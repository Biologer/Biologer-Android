package org.biologer.biologer.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.R;

import java.util.ArrayList;
import java.util.List;

/**
 * This adapter is used to display the species name and number of individuals
 * observed in the RecyclerView of the Timed Count.
 */

public class TimedCountAdapter
        extends RecyclerView.Adapter<TimedCountAdapter.ViewHolder> {
    private final List<SpeciesCount> observedSpecies;
    private int position;
    private static final String TAG = "Biologer.TCEntryAdapter";
    View view;
    public TimedCountAdapter(ArrayList<SpeciesCount> entries) {
        observedSpecies = entries;
    }

    public interface OnItemClickListener {
        void onPlusClick(SpeciesCount position);
        void onSpeciesClick(SpeciesCount position);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {
//            implements View.OnCreateContextMenuListener {

        public TextView speciesCount;
        public ImageView plusImage;
        public TextView speciesName;

        public ViewHolder(View view) {
            super(view);

            // Define click listener for the ViewHolder's View
            speciesCount = view.findViewById(R.id.species_count_no);
            plusImage = view.findViewById(R.id.species_count_plus);
            speciesName = view.findViewById(R.id.species_count_name);

            // Add the entry items menu
//            view.setOnCreateContextMenuListener(this);
        }

//        @Override
//        public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
//            Log.d(TAG, "On create context menu from EntryRecycleView fragment.");
//            menu.add(Menu.NONE, R.id.duplicate,
//                    Menu.NONE, R.string.duplicate_entry);
//            menu.add(Menu.NONE, R.id.delete,
//                    Menu.NONE, R.string.delete_entry);
//            menu.add(Menu.NONE, R.id.delete_all,
//                    Menu.NONE, R.string.delete_all_entries);
 //       }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        // Create a new view, which defines the UI of the list item
        view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.species_count_list, viewGroup, false);

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        SpeciesCount species = observedSpecies.get(position);
        viewHolder.speciesName.setText(species.getSpeciesName());
        viewHolder.speciesCount.setText(String.valueOf(species.getNumberOfIndividuals()));

        viewHolder.plusImage.setOnClickListener(view -> {
            int adapterPosition = viewHolder.getAdapterPosition();

            Log.d(TAG, "Plus button clicked for species " + species.getSpeciesName());
            int individuals = species.getNumberOfIndividuals();
            individuals++;
            species.setNumberOfIndividuals(individuals);

            notifyItemChanged(adapterPosition);

            if (listener != null) {
                listener.onPlusClick(species);
            }
        });

        viewHolder.speciesCount.setOnClickListener(view -> {
            int adapterPosition = viewHolder.getAdapterPosition();

            Log.d(TAG, "Number of species clicked for species " + species.getSpeciesName());
            int individuals = species.getNumberOfIndividuals();
            individuals++;
            species.setNumberOfIndividuals(individuals);

            notifyItemChanged(adapterPosition);

            if (listener != null) {
                listener.onPlusClick(species);
            }
        });

        viewHolder.speciesName.setOnClickListener(v -> {
            int adapterPosition = viewHolder.getAdapterPosition();

            Log.d(TAG, "Species name clicked for species " + species.getSpeciesName() +
                    "(position " + adapterPosition + ")");

            if (listener != null) {
                listener.onSpeciesClick(species);
            }

        });

//        viewHolder.itemView.setOnLongClickListener(v -> {
//            Log.d(TAG, "Long click on " + viewHolder.getLayoutPosition());
//            return false;
//        });
    }

    @Override
    public int getItemCount() {
        if (observedSpecies == null) {
            return 0;
        } else {
            return observedSpecies.size();
        }
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean hasSpeciesWithID(long speciesID) {
        if (observedSpecies == null) {
            return false;
        }
        for (SpeciesCount species : observedSpecies) {
            if (species.getSpeciesID() == speciesID) {
                return true;
            }
        }
        return false;
    }

    public void addToSpeciesCount(long speciesID) {
        if (observedSpecies == null) {
            return;
        }
        for (int i = 0; i < observedSpecies.size(); i++) {
            SpeciesCount species = observedSpecies.get(i);
            if (species.getSpeciesID() == speciesID) {
                int individuals = species.getNumberOfIndividuals() + 1;
                species.setNumberOfIndividuals(individuals);
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void removeFromSpeciesCount(long speciesID) {
        if (observedSpecies == null) {
            return;
        }
        for (int i = 0; i < observedSpecies.size(); i++) {
            SpeciesCount species = observedSpecies.get(i);
            if (species.getSpeciesID() == speciesID) {
                int individuals = species.getNumberOfIndividuals() - 1;
                if (individuals == 0) {
                    observedSpecies.remove(i);
                    notifyItemRemoved(i);
                } else {
                    species.setNumberOfIndividuals(individuals);
                    notifyItemChanged(i);
                }
                return;
            }
        }
    }

//    @Override
//    public void onViewRecycled(ViewHolder viewHolder) {
//        viewHolder.itemView.setOnLongClickListener(null);
//        super.onViewRecycled(viewHolder);
//    }

}
