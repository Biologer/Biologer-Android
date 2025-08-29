package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.adapters.LandingFragmentAdapter;
import org.biologer.biologer.adapters.LandingFragmentItems;
import org.biologer.biologer.adapters.TimedCountViewModel;
import org.biologer.biologer.services.DateHelper;
import org.biologer.biologer.services.RecyclerOnClickListener;
import org.biologer.biologer.services.StageAndSexLocalization;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.EntryDb_;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.StageDb_;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class FragmentTimedCountEntries extends Fragment {
    String TAG = "Biologer.TCEntries";
    private ArrayList<LandingFragmentItems> items;
    RecyclerView recyclerView;
    LandingFragmentAdapter entriesAdapter;
    TimedCountViewModel timedCountViewModel;
    long taxonId;

    public interface OnItemChangeListener {
        void onTaxonChanged(Long old_id, Long new_id, String taxonSuggestion);
    }

    private FragmentTimedCountEntries.OnItemChangeListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnItemChangeListener) {
            listener = (OnItemChangeListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnItemChangeListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_timed_count_entries, container, false);

        timedCountViewModel = new ViewModelProvider(requireActivity()).get(TimedCountViewModel.class);

        // Load the entries from the database
        items = (ArrayList<LandingFragmentItems>) loadEntries(); // Load the entries from the database

        loadEntries();

        // If there are entries display the list with taxa
        recyclerView = rootView.findViewById(R.id.recycled_view_timed_count_entries);
        entriesAdapter = new LandingFragmentAdapter(items);
        recyclerView.setAdapter(entriesAdapter);
        recyclerView.setClickable(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.addOnItemTouchListener(
                new RecyclerOnClickListener(getActivity(), recyclerView, new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        recyclerView.setClickable(false);
                        LandingFragmentItems item = items.get(position);
                        long entry_id = item.getObservationId();
                        Box<EntryDb> entriesBox = App.get().getBoxStore().boxFor(EntryDb.class);
                        Query<EntryDb> query = entriesBox
                                .query(EntryDb_.id.equal(entry_id))
                                .build();
                        EntryDb taxon = query.findFirst();
                        query.close();
                        if (taxon != null) {
                            taxonId = taxon.getTaxonId();
                        }
                        Activity activity = getActivity();
                        if (activity != null) {
                            Log.d(TAG, "Species entry at position " + position + " clicked.");
                            Intent intent = new Intent(activity.getApplicationContext(), ActivityObservation.class);
                            intent.putExtra("IS_NEW_ENTRY", "NO");
                            intent.putExtra("ENTRY_ID", entry_id);
                            openEntry.launch(intent);
                        }
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Log.d(TAG, "Item " + position + " long pressed.");
                        entriesAdapter.setPosition(position);
                    }
                }));
        registerForContextMenu(recyclerView);

        return rootView;
    }

    private List<LandingFragmentItems> loadEntries() {
        // This list will hold all the items displayed
        List<LandingFragmentItems> items = new ArrayList<>();

        // Get the observation data for selected species
        if (timedCountViewModel.getTaxonId().getValue() != null &&
                timedCountViewModel.getTimedCountId().getValue() != null) {
            long taxon_id = timedCountViewModel.getTaxonId().getValue();
            int timed_count_id = timedCountViewModel.getTimedCountId().getValue();
            Box<EntryDb> entriesBox = App.get().getBoxStore().boxFor(EntryDb.class);
            Query<EntryDb> query = entriesBox
                    .query(EntryDb_.timedCoundId.equal(timed_count_id)
                            .and(EntryDb_.taxonId.equal(taxon_id)))
                    .build();
            ArrayList<EntryDb> entriesDb = (ArrayList<EntryDb>) query.find();
            query.close();
            for (EntryDb entry : entriesDb) {
                items.add(getItemFromEntry(entry));
            }
        }

        // Sort in descending order
        Collections.sort(items, (item1, item2) ->
                Long.compare(item2.getObservationId(), item1.getObservationId()));

        return items;
    }

    private final ActivityResultLauncher<Intent> openEntry = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // If we need to do something with the result after EntryActivity...
                Log.d(TAG, "We got a result from the Entry Activity!");

                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "We got RESULT_OK code from the EntryActivity.");
                    long old_data_id = 0;
                    if (result.getData() != null) {
                        old_data_id = result.getData().getLongExtra("ENTRY_LIST_ID", 0);
                    }
                    FragmentTimedCountEntries.this.updateEntry(old_data_id);
                }
            });

    private void updateEntry(long entry_id) {
        // Load the entry from the ObjectBox
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        Query<EntryDb> query = box.query(EntryDb_.id.equal(entry_id)).build();
        EntryDb entryDb = query.findFirst();
        query.close();

        int index_id = getIndexFromID(entry_id);
        Log.d(TAG, "Updating entry with ID " + index_id + ".");

        if (entryDb != null && entryDb.getTaxonId() == taxonId) {
            Log.d(TAG, "Taxon is the same. Updating just the current entry.");
            // Update the entry to in the entry list (RecycleView)
            items.set(index_id, getItemFromEntry(entryDb));
            entriesAdapter.notifyItemChanged(index_id);
        } else {
            Log.d(TAG, "Taxon is changed, removing it from this species entries.");
            items.remove(index_id);
            entriesAdapter.notifyItemRemoved(index_id);
            // Update the SpeciesCount in the main Activity
            if (listener != null) {
                if (entryDb != null) {
                    listener.onTaxonChanged(taxonId, entryDb.getTaxonId(), entryDb.getTaxonSuggestion());
                }
            }
        }
    }

    // Find the entry’s index ID
    private int getIndexFromID(long entry_id) {

        int index_id = 0;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).getObservationId() == entry_id) {
                index_id = i;
            }
        }
        Log.d(TAG, "Entry " + entry_id + " index ID is " + index_id);
        return index_id;
    }

    private LandingFragmentItems getItemFromEntry(EntryDb entry) {
        Long observationId = entry.getId();
        String title = entry.getTaxonSuggestion();

        String subtitle = "";
        Long stage_id = entry.getStage();
        if (stage_id != null) {
            Box<StageDb> stageBox = App.get().getBoxStore().boxFor(StageDb.class);
            Query<StageDb> queryStage = stageBox
                    .query(StageDb_.id.equal(stage_id))
                    .build();
            StageDb stage = queryStage.findFirst();
            queryStage.close();
            if (stage != null) {
                subtitle = StageAndSexLocalization.getStageLocale(getContext(), stage.getName());
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

        return new LandingFragmentItems(observationId, null, title, subtitle, image, calendar.getTime());
    }
}
