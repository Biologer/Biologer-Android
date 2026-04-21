package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.LandingFragmentAdapter;
import org.biologer.biologer.adapters.LandingFragmentItems;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.TimedCountDb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class BaseObservationListFragment extends Fragment {

    protected LandingFragmentAdapter entriesAdapter;
    protected ActionMode actionMode;
    protected abstract RecyclerView getRecyclerView();
    protected abstract void loadItemsForRecyclerView();

    protected final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_entry_items_selected, menu);
            return true;
        }
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return true; }
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.delete) {
                deleteSelectedEntries();
                return true;
            } else if (item.getItemId() == R.id.duplicate) {
                for (int i = 0; i < entriesAdapter.getCurrentList().size(); i++) {
                    if (entriesAdapter.getCurrentList().get(i).isMarked()) {
                        duplicateEntry(i);
                        break;
                    }
                }
                mode.finish();
                return true;
            }
            return false;
        }
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (actionMode != null) deselectAllItems();
            actionMode = null;
        }
    };

    protected void selectDeselect(int position) {
        List<LandingFragmentItems> currentList = new ArrayList<>(entriesAdapter.getCurrentList());
        LandingFragmentItems item = currentList.get(position);
        currentList.set(position, item.withMarked(!item.isMarked()));
        int selected = getSelectedItemsCount(currentList);
        entriesAdapter.submitList(currentList);

        if (selected == 0) {
            if (actionMode != null) actionMode.finish();
        } else if (actionMode != null) {
            actionMode.setTitle(String.valueOf(selected));
            updateActionModeMenu(actionMode, currentList, selected);
        }
    }

    protected int getSelectedItemsCount(List<LandingFragmentItems> list) {
        int count = 0;
        for (LandingFragmentItems item : list) {
            if (item.isMarked()) count++;
        }
        return count;
    }

    protected void updateActionModeMenu(ActionMode mode, List<LandingFragmentItems> list, int selectedCount) {
        Menu menu = mode.getMenu();
        if (menu == null) return;
        MenuItem duplicateItem = menu.findItem(R.id.duplicate);
        if (duplicateItem != null) {
            if (selectedCount == 1) {
                for (LandingFragmentItems i : list) {
                    if (i.isMarked()) {
                        duplicateItem.setVisible(!i.isTimedCount());
                        break;
                    }
                }
            } else {
                duplicateItem.setVisible(false);
            }
        }
    }

    protected void deselectAllItems() {
        List<LandingFragmentItems> currentList = new ArrayList<>(entriesAdapter.getCurrentList());
        boolean changed = false;
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).isMarked()) {
                currentList.set(i, currentList.get(i).withMarked(false));
                changed = true;
            }
        }
        if (changed) entriesAdapter.submitList(currentList);
    }

    public void duplicateEntry(int position) {
        LandingFragmentItems item = entriesAdapter.getCurrentList().get(position);
        EntryDb entry_from = ObjectBoxHelper.getObservationById(item.getLocalId());
        if (entry_from != null) {
            EntryDb entry = new EntryDb(0, null, false, false,
                    entry_from.getTaxonId(), entry_from.getTimeCountId(),
                    entry_from.getTaxonSuggestion(), entry_from.getYear(),
                    entry_from.getMonth(), entry_from.getDay(),
                    entry_from.getComment(), null, "", null, null, "true", "",
                    entry_from.getLattitude(), entry_from.getLongitude(),
                    entry_from.getAccuracy(), entry_from.getElevation(),
                    entry_from.getLocation(), null, null, null,
                    entry_from.getProjectId(), entry_from.getFoundOn(),
                    entry_from.getDataLicence(), entry_from.getImageLicence(),
                    entry_from.getTime(), entry_from.getHabitat(), "");
            long new_entry_id = ObjectBoxHelper.setObservation(entry);
            newDuplicateObservation(new_entry_id);
        }
        if (actionMode != null) actionMode.finish();
    }

    protected void newDuplicateObservation(long new_entry_id) {
        Intent intent = new Intent(getActivity(), ActivityObservation.class);
        intent.putExtra("IS_NEW_ENTRY", false);
        intent.putExtra("ENTRY_ID", new_entry_id);
        intent.putExtra("IS_DUPLICATED_ENTRY", true);
        observationLauncher.launch(intent);
    }

    protected void addObservationItem(long entryId) {
        EntryDb entryDb = ObjectBoxHelper.getObservationById(entryId);
        if (entryDb == null) return;

        LandingFragmentItems newItem = LandingFragmentItems.getItemFromEntry(requireContext(), entryDb);
        List<LandingFragmentItems> currentList = new ArrayList<>(entriesAdapter.getCurrentList());
        currentList.add(0, newItem);
        entriesAdapter.submitList(currentList, () ->
                getRecyclerView().post(() ->
                        getRecyclerView().smoothScrollToPosition(0)));
    }

    protected void deleteSelectedEntries() {
        List<LandingFragmentItems> snapshot = new ArrayList<>(entriesAdapter.getCurrentList());
        List<LandingFragmentItems> toDelete = new ArrayList<>();
        for (LandingFragmentItems item : snapshot) {
            if (item.isMarked()) toDelete.add(item);
        }
        if (toDelete.isEmpty()) return;
        int count = toDelete.size();

        // Delete request
        for (LandingFragmentItems item : toDelete) {
            if (item.isUploaded()) sendDeleteRequestToServer(item);
            else deleteFromObjectBox(item, false);
        }

        // Get the deleted files that require refresh
        Set<Long> deletedIds = new HashSet<>();
        for (LandingFragmentItems item : toDelete) {
            if (!item.isUploaded()) deletedIds.add(item.getLocalId());
        }

        // Create new list for recycler view
        List<LandingFragmentItems> newList = new ArrayList<>();
        for (LandingFragmentItems item : snapshot) {
            if (!deletedIds.contains(item.getLocalId()))
                newList.add(item.isMarked() ? item.withMarked(false) : item);
        }

        entriesAdapter.submitList(newList);
        loadItemsForRecyclerView();
        Toast.makeText(getContext(), getResources().getQuantityString(
                R.plurals.entries_deleted_count, count, count), Toast.LENGTH_SHORT).show();
        ActionMode mode = actionMode;
        actionMode = null;
        if (mode != null) mode.finish();
    }

    protected void deleteFromObjectBox(LandingFragmentItems item, boolean reload) {
        Long id = item.getLocalId();
        if (id == null) return;

        if (item.isTimedCount()) {
            ObjectBoxHelper.removeObservationsForTimedCountId(id);
            ObjectBoxHelper.removeTimedCountById(id);
        } else {
            ObjectBoxHelper.removeObservationById(id);
        }

        if (reload) loadItemsForRecyclerView();
    }

    protected void sendDeleteRequestToServer(LandingFragmentItems item) {
        Call<Void> call = null;
        if (item.isTimedCount()) {
            TimedCountDb timedCount = ObjectBoxHelper.getTimeCountById(item.getLocalId());
            if (timedCount != null)
                call = RetrofitClient.getService(SettingsManager.getDatabaseName())
                        .deleteTimedCountObservation(timedCount.getServerId());
        } else {
            EntryDb entry = ObjectBoxHelper.getObservationById(item.getLocalId());
            if (entry != null)
                call = RetrofitClient.getService(SettingsManager.getDatabaseName())
                        .deleteFieldObservation(entry.getServerId());
        }

        if (call != null) {
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        deleteFromObjectBox(item, true);
                    } else if (response.code() == 404 || response.code() == 403) {
                        deleteFromObjectBox(item, true);
                    } else {
                        Toast.makeText(getContext(), "Error deleting (server).", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e("BaseObservationList", "Network error deleting: " + t.getMessage());
                }
            });
        }
    }

    protected final ActivityResultLauncher<Intent> observationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            boolean isNew = result.getData().getBooleanExtra("IS_NEW_ENTRY", false);
                            boolean isDup = result.getData().getBooleanExtra("IS_DUPLICATED_ENTRY", false);
                            long entryId = result.getData().getLongExtra("ENTRY_ID", 0);
                            if (isNew || isDup) addObservationItem(entryId);
                            else loadItemsForRecyclerView();
                        }
                    });
}