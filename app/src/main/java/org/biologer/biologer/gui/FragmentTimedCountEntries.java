package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.adapters.LandingFragmentAdapter;
import org.biologer.biologer.adapters.LandingFragmentItems;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.viewmodels.TimedCountViewModel;
import org.biologer.biologer.databinding.FragmentTimedCountEntriesBinding;
import org.biologer.biologer.services.RecyclerOnClickListener;
import org.biologer.biologer.sql.EntryDb;

import java.util.ArrayList;
import java.util.List;

public class FragmentTimedCountEntries extends BaseObservationListFragment {
    String TAG = "Biologer.TCEntries";
    private FragmentTimedCountEntriesBinding binding;
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTimedCountEntriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        timedCountViewModel = new ViewModelProvider(requireActivity()).get(TimedCountViewModel.class);

        setupRecyclerView();
        loadItemsForRecyclerView();
        addBackPressedCallback();

    }

    private void addBackPressedCallback() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (actionMode != null) {
                    actionMode.finish();
                } else {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
    }

    private void setupRecyclerView() {
        entriesAdapter = new LandingFragmentAdapter(true);
        binding.recyclerViewTimedCounts.setAdapter(entriesAdapter);
        binding.recyclerViewTimedCounts.setClickable(true);
        binding.recyclerViewTimedCounts.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.recyclerViewTimedCounts.addOnItemTouchListener(
                new RecyclerOnClickListener(getActivity(), binding.recyclerViewTimedCounts, new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (actionMode != null) {
                            selectDeselect(position);
                        } else {
                            binding.recyclerViewTimedCounts.setClickable(false);
                            LandingFragmentItems item = entriesAdapter.getCurrentList().get(position);
                            long entry_id = item.getLocalId();
                            EntryDb entry = ObjectBoxHelper.getObservationById(entry_id);
                            if (entry != null) {
                                taxonId = entry.getTaxonId();
                            }
                            Activity activity = getActivity();
                            if (activity != null) {
                                Log.d(TAG, "Species entry at position " + position + " clicked.");
                                Intent intent = new Intent(activity.getApplicationContext(), ActivityObservation.class);
                                intent.putExtra("IS_NEW_ENTRY", false);
                                intent.putExtra("ENTRY_ID", entry_id);
                                openEntry.launch(intent);
                            }
                        }
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Log.d(TAG, "Item " + position + " long pressed.");
                        if (actionMode == null) {
                            actionMode = ((AppCompatActivity) requireActivity())
                                    .startSupportActionMode(actionModeCallback);
                        }
                        selectDeselect(position);
                        entriesAdapter.notifyItemChanged(position);
                        entriesAdapter.setPosition(position);
                    }
                }
                )
        );
        registerForContextMenu(binding.recyclerViewTimedCounts);
    }

    @Override
    protected RecyclerView getRecyclerView() {
        return binding.recyclerViewTimedCounts;
    }

    @Override
    public void loadItemsForRecyclerView() {

        ArrayList<LandingFragmentItems> items = LandingFragmentItems.loadTimeCountSpeciesEntries(
                requireContext(),
                timedCountViewModel.getObjectBoxId() != null ? timedCountViewModel.getObjectBoxId() : -1L,
                timedCountViewModel.getTaxonId() != null ? timedCountViewModel.getTaxonId() : -1L
                );

        entriesAdapter.submitList(items);

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
                        old_data_id = result.getData().getLongExtra("ENTRY_ID", 0);
                    }
                    FragmentTimedCountEntries.this.updateEntry(old_data_id);
                }
            });

    private void updateEntry(long entryId) {

        EntryDb entry = ObjectBoxHelper.getObservationById(entryId);
        List<LandingFragmentItems> currentList = new ArrayList<>(entriesAdapter.getCurrentList());

        int index = entriesAdapter.getItemIndexFromId(entryId, false);
        Log.d(TAG, "Updating entry with ID " + index + ".");

        if (entry != null && entry.getTaxonId() == taxonId) {
            Log.d(TAG, "Taxon is the same. Updating just the current entry.");
            currentList.set(index, LandingFragmentItems.getItemFromEntry(getContext(), entry));
            entriesAdapter.submitList(currentList);
        } else {
            Log.d(TAG, "Taxon is changed, removing it from this species entries.");
            currentList.remove(index);
            entriesAdapter.submitList(currentList);
            // Update the SpeciesCount in the main Activity
            if (listener != null) {
                if (entry != null) {
                    listener.onTaxonChanged(taxonId, entry.getTaxonId(), entry.getTaxonSuggestion());
                }
            }
        }
    }

    @Override
    protected void deleteFromObjectBox(LandingFragmentItems item, boolean reload) {
        super.deleteFromObjectBox(item, reload);
        timedCountViewModel.setSpeciesChanged(true);
    }

    @Override
    public void duplicateEntry(int position) {
        super.duplicateEntry(position);
        loadItemsForRecyclerView();
        timedCountViewModel.setSpeciesChanged(true);
    }
}
