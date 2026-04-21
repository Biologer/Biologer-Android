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

public class TimeCountAdapter
        extends RecyclerView.Adapter<TimeCountAdapter.ViewHolder> {
    private final List<SpeciesCountItems> observedSpecies;
    private int position;
    private static final String TAG = "Biologer.TCEntryAdapter";
    View view;
    public TimeCountAdapter(ArrayList<SpeciesCountItems> entries) {
        observedSpecies = entries;
    }

    public interface OnItemClickListener {
        void onPlusClick(SpeciesCountItems position);
        void onSpeciesClick(SpeciesCountItems position);
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

        }

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
        SpeciesCountItems species = observedSpecies.get(position);
        viewHolder.speciesName.setText(species.getSpeciesName());
        viewHolder.speciesCount.setText(String.valueOf(species.getNumberOfIndividuals()));

        viewHolder.plusImage.setOnClickListener(view -> {
            int adapterPosition = viewHolder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

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
            int adapterPosition = viewHolder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

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
            int adapterPosition = viewHolder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

            Log.d(TAG, "Species name clicked for species " + species.getSpeciesName() +
                    "(position " + adapterPosition + ")");

            if (listener != null) {
                listener.onSpeciesClick(species);
            }

        });

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
        for (SpeciesCountItems species : observedSpecies) {
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
            SpeciesCountItems species = observedSpecies.get(i);
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
            SpeciesCountItems species = observedSpecies.get(i);
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

}
