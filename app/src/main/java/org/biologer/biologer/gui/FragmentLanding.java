package org.biologer.biologer.gui;

import static org.biologer.biologer.adapters.LandingFragmentItems.compareUploadAndDate;
import static org.biologer.biologer.adapters.LandingFragmentItems.getItemFromEntry;
import static org.biologer.biologer.adapters.LandingFragmentItems.getItemFromTimedCount;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.LandingFragmentAdapter;
import org.biologer.biologer.adapters.LandingFragmentItems;
import org.biologer.biologer.databinding.FragmentLandingBinding;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.services.RecyclerOnClickListener;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.TimedCountDb;
import org.biologer.biologer.workers.ObservationsDownloadWorker;
import org.biologer.biologer.workers.PhotoDownloadWorker;
import org.biologer.biologer.workers.TimedCountsDownloadWorker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentLanding extends Fragment {

    String TAG = "Biologer.FragmentLanding";
    private FragmentLandingBinding binding;
    LandingFragmentAdapter entriesAdapter;
    private final Set<UUID> processedWorkerIds = new HashSet<>();
    private ActionMode actionMode;
    private int localObjectBoxObservationOffset = 0;
    private int localObjectBoxTimedCountsOffset = 0;
    private boolean isLoading = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLandingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        loadItemsForRecyclerView();
        setupFloatingActionButton();
        setupWorkerObservers(); // To track upload of data online
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

    private void loadItemsForRecyclerView() {
        localObjectBoxObservationOffset = 0;
        localObjectBoxTimedCountsOffset = 0;

        // Group 1. Data waiting for upload to the server
        ArrayList<LandingFragmentItems> allItems =
                LandingFragmentItems.loadAllLocalEntries(requireContext());

        // Group 2. If the list is short ask for more items
        ArrayList<EntryDb> syncedObservations = ObjectBoxHelper.getPagedObservations(25, 0);
        for (EntryDb observation : syncedObservations) {
            allItems.add(getItemFromEntry(requireContext(), observation));
        }
        ArrayList<TimedCountDb> syncedTimedCounts = ObjectBoxHelper.getPagedTimedCounts(25, 0);
        for (TimedCountDb timedCount : syncedTimedCounts) {
            allItems.add(getItemFromTimedCount(requireContext(), timedCount));
        }
        Collections.sort(allItems, compareUploadAndDate);

        localObjectBoxObservationOffset = syncedObservations.size();
        localObjectBoxTimedCountsOffset = syncedTimedCounts.size();

        entriesAdapter.submitList(allItems);

        // If the list is short get more data
        if (allItems.size() < 10) {
            loadNextBatch();
        }

        // Finally, download new data updated on the server if exist
        downloadNewerData();
    }

    private void reloadItemsForRecyclerView() {
        binding.recycledViewEntries.post(() -> {
            ArrayList<LandingFragmentItems> allItems =
                    LandingFragmentItems.loadAllLocalEntries(requireContext());

            ArrayList<EntryDb> syncedObservations =
                    ObjectBoxHelper.getPagedObservations(localObjectBoxObservationOffset, 0);
            for (EntryDb observation : syncedObservations) {
                allItems.add(getItemFromEntry(requireContext(), observation));
            }
            ArrayList<TimedCountDb> syncedTimedCounts =
                    ObjectBoxHelper.getPagedTimedCounts(localObjectBoxTimedCountsOffset, 0);
            for (TimedCountDb timedCount : syncedTimedCounts) {
                allItems.add(getItemFromTimedCount(requireContext(), timedCount));
            }
            Collections.sort(allItems, compareUploadAndDate);
            entriesAdapter.submitList(allItems);
            ((ActivityLanding) requireActivity()).updateMenuIconVisibility();
        });
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_entry_items_selected, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

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
            if (actionMode != null) {
                deselectAllItems();
            }
            actionMode = null;
        }
    };

    private int getSelectedItemsCount(List<LandingFragmentItems> list) {
        int count = 0;
        for (LandingFragmentItems item : list) {
            if (item.isMarked()) count++;
        }
        return count;
    }

    private void deselectAllItems() {
        List<LandingFragmentItems> current = new ArrayList<>(entriesAdapter.getCurrentList());
        boolean changed = false;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).isMarked()) {
                current.set(i, current.get(i).withMarked(false));
                changed = true;
            }
        }
        if (changed) entriesAdapter.submitList(current);
    }

    private void setupWorkerObservers() {
        WorkManager.getInstance(requireContext())
                .getWorkInfosByTagLiveData("UPLOAD_WORK")
                .observe(getViewLifecycleOwner(), workInfos -> {
                    if (workInfos == null || workInfos.isEmpty()) return;

                    int total = 0;
                    int finished = 0;
                    int failed = 0;
                    boolean shouldShowToast = false;

                    for (WorkInfo workInfo : workInfos) {
                            total++;
                            if (workInfo.getState().isFinished()) {
                                finished++;
                                if (workInfo.getState() == WorkInfo.State.FAILED) {
                                    long code = workInfo.getOutputData().getLong("error_code", 0);
                                    String message = workInfo.getOutputData().getString("error_message");
                                    Toast.makeText(getContext(), "Error " + code + ": " + message, Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Error " + code + ": " + message);
                                    failed++;
                                }

                                // Check if we've already handled this worker – be more efficient :)
                                if (!processedWorkerIds.contains(workInfo.getId())) {
                                    processedWorkerIds.add(workInfo.getId());
                                    if (workInfos.size() > 1 && finished == total) {
                                        Log.d(TAG, "Skipping historical worker toast: " + workInfo.getId());
                                    } else {
                                        shouldShowToast = true;
                                    }

                                    LinearLayoutManager lm = (LinearLayoutManager) binding.recycledViewEntries.getLayoutManager();
                                    if (lm != null && lm.findFirstVisibleItemPosition() < 5) {
                                        Log.d(TAG, "Scrolling to the top of the list (less than 5 items at the top).");
                                        long observationId = workInfo.getOutputData().getLong("uploadedObservationId", 0);
                                        long timedCountId = workInfo.getOutputData().getLong("uploadedTimeCountId", 0);
                                        reloadItemsForRecyclerView();
                                        Log.d(TAG, "Worker received Observation ID " + observationId + "; Timed Count ID " + timedCountId);
                                        binding.recycledViewEntries.post(() -> {
                                            if (ObjectBoxHelper.getUnsyncedCount() > 0) {
                                                ((ActivityLanding) requireActivity()).uploadRecords();
                                            }
                                            binding.recycledViewEntries.smoothScrollToPosition(0);
                                        });
                                    } else {
                                        reloadItemsForRecyclerView();
                                    }
                                }
                            }
                    }

                    if (total == 0) return;

                    int percentage = (finished * 100) / total;
                    Log.d(TAG, "Progress: " + percentage + "%");

                    if (finished < total) {
                        showUploadProgress(true, percentage);
                    } else {
                        showUploadProgress(false, 0);
                        if (shouldShowToast) {
                            if (failed > 0) {
                                Toast.makeText(getContext(), getResources().getQuantityString(
                                        R.plurals.upload_failed_items,
                                        failed,
                                        failed
                                ), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), getString(R.string.all_data_uploaded_successfully), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        WorkManager.getInstance(requireContext())
                .getWorkInfosByTagLiveData("PHOTO_DOWNLOAD")
                .observe(getViewLifecycleOwner(), workInfos -> {
                    if (workInfos != null && !workInfos.isEmpty()) {
                        WorkInfo workInfo = workInfos.get(0);
                        WorkInfo.State state = workInfo.getState();

                        if (state.isFinished()) {
                            isLoading = false;
                            progressBar(false);

                            if (!processedWorkerIds.contains(workInfo.getId()) && state == WorkInfo.State.SUCCEEDED) {
                                processedWorkerIds.add(workInfo.getId());
                                long[] ids = workInfo.getOutputData().getLongArray("updatedPhotoForObservationIds");
                                Log.d(TAG, "Should update these IDs after photos downloaded: " + Arrays.toString(ids));
                                updatePhotoForObservation(ids);
                            } else if (state == WorkInfo.State.FAILED && !processedWorkerIds.contains(workInfo.getId())) {
                                processedWorkerIds.add(workInfo.getId());
                                Toast.makeText(getContext(), "Sync failed. Check connection.", Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                });

        WorkManager.getInstance(requireContext())
                .getWorkInfosByTagLiveData("UPDATED_AT_SYNC")
                .observe(getViewLifecycleOwner(), workInfos -> {
                    if (workInfos != null && !workInfos.isEmpty()) {

                        boolean allFinished = true;
                        List<Long> observationsToUpdate = new ArrayList<>();
                        List<Long> timedCountsToUpdate = new ArrayList<>();

                        for (WorkInfo workInfo : workInfos) {
                            WorkInfo.State state = workInfo.getState();

                            if (state == WorkInfo.State.SUCCEEDED && !processedWorkerIds.contains(workInfo.getId())) {
                                processedWorkerIds.add(workInfo.getId());
                                long[] observationIds = workInfo.getOutputData().getLongArray("updatedObservationIds");
                                if (observationIds != null) {
                                    for (long id : observationIds) observationsToUpdate.add(id);
                                    Log.d(TAG, "Should update these observations downloaded by timestamp: "
                                            + Arrays.toString(observationIds));
                                }
                                long[] timedCountIds = workInfo.getOutputData().getLongArray("updatedTimedCountIds");
                                if (timedCountIds != null) {
                                    for (long id : timedCountIds) timedCountsToUpdate.add(id);
                                    Log.d(TAG, "Should update these observations downloaded by timestamp: "
                                            + Arrays.toString(timedCountIds));
                                }

                            } else if (state == WorkInfo.State.FAILED && !processedWorkerIds.contains(workInfo.getId())) {
                                processedWorkerIds.add(workInfo.getId());
                                Toast.makeText(getContext(), "Sync failed. Check connection.", Toast.LENGTH_SHORT).show();
                            }

                            if (!workInfo.getState().isFinished()) {
                                allFinished = false;
                                break;
                            }
                        }

                        if (!observationsToUpdate.isEmpty() || !timedCountsToUpdate.isEmpty()) {
                            applyBatchUpdates(observationsToUpdate, timedCountsToUpdate);
                        }

                        if (allFinished) {
                            Log.d(TAG, "All observations sync workers by timestamp finished. Updating UI.");
                            binding.swipeRefreshLayout.setRefreshing(false);
                            LinearLayoutManager lm = (LinearLayoutManager) binding.recycledViewEntries.getLayoutManager();
                            if (lm != null && lm.findFirstVisibleItemPosition() < 5) {
                                Log.d(TAG, "Scrolling to the top of the list (less than 5 items at the top).");
                                binding.recycledViewEntries.post(() -> {
                                    if (ObjectBoxHelper.getUnsyncedCount() > 0) {
                                        ((ActivityLanding) requireActivity()).uploadRecords();
                                    }
                                    binding.recycledViewEntries.smoothScrollToPosition(0);
                                });
                            }

                        }

                    }
                });

        WorkManager.getInstance(requireContext())
                .getWorkInfosByTagLiveData("UPDATED_BY_ID")
                .observe(getViewLifecycleOwner(), workInfos -> {
                    if (workInfos != null && !workInfos.isEmpty()) {

                        boolean allFinished = true;
                        List<Long> observationsToUpdate = new ArrayList<>();
                        List<Long> timedCountsToUpdate = new ArrayList<>();

                        for (WorkInfo workInfo : workInfos) {
                            WorkInfo.State state = workInfo.getState();

                            if (state == WorkInfo.State.SUCCEEDED && !processedWorkerIds.contains(workInfo.getId())) {
                                processedWorkerIds.add(workInfo.getId());

                                long[] observationIds = workInfo.getOutputData().getLongArray("updatedObservationIds");
                                if (observationIds != null) {
                                    for (long id : observationIds) observationsToUpdate.add(id);
                                    Log.d(TAG, "Should update these observations downloaded by timestamp: "
                                            + Arrays.toString(observationIds));
                                }
                                long[] timedCountIds = workInfo.getOutputData().getLongArray("updatedTimedCountIds");
                                if (timedCountIds != null) {
                                    for (long id : timedCountIds) timedCountsToUpdate.add(id);
                                    Log.d(TAG, "Should update these observations downloaded by timestamp: "
                                            + Arrays.toString(timedCountIds));
                                }

                            } else if (state == WorkInfo.State.FAILED && !processedWorkerIds.contains(workInfo.getId())) {
                                processedWorkerIds.add(workInfo.getId());
                                Toast.makeText(getContext(), "Sync failed. Check connection.", Toast.LENGTH_SHORT).show();
                            }

                            if (!workInfo.getState().isFinished()) {
                                allFinished = false;
                                break;
                            }
                        }

                        if (!observationsToUpdate.isEmpty() || !timedCountsToUpdate.isEmpty()) {
                            applyBatchUpdates(observationsToUpdate, timedCountsToUpdate);
                        }

                        if (allFinished) {
                            Log.d(TAG, "All observations sync workers by id finished. Updating UI.");
                            // Auto-scroll if user is already near top
                            LinearLayoutManager lm = (LinearLayoutManager) binding.recycledViewEntries.getLayoutManager();
                            if (lm != null && lm.findFirstVisibleItemPosition() < 5) {
                                binding.recycledViewEntries.post(() ->
                                        binding.recycledViewEntries.smoothScrollToPosition(0));                            }
                        }

                    }
                });
    }

    private void updatePhotoForObservation(long[] observationIds) {

        List<LandingFragmentItems> currentList = new ArrayList<>(entriesAdapter.getCurrentList());

        for (long id : observationIds) {
            int index = entriesAdapter.getItemIndexFromId(id, false);
            if (index == -1) {
                Log.e(TAG, "Photo update: item " + id + " not in list yet, skipping.");
                continue;
            }
            EntryDb entry = ObjectBoxHelper.getObservationById(id);
            if (entry == null) continue;
            currentList.set(index, getItemFromEntry(requireContext(), entry));
        }

        entriesAdapter.submitList(currentList);
    }

    private void setupFloatingActionButton() {
        // Add the + button for making new records
        FloatingActionButton floatingActionButton = binding.floatButtonNewEntry;
        boolean mail_confirmed = SettingsManager.isMailConfirmed();
        floatingActionButton.setEnabled(mail_confirmed);
        floatingActionButton.setAlpha(mail_confirmed ? 1f : 0.25f);

        floatingActionButton.setOnClickListener(v -> newObservation());

        floatingActionButton.setOnLongClickListener(v -> {
            // Show the context menu
            showContextMenu(v);
            return true;
        });
    }

    private void setupRecyclerView() {
        entriesAdapter = new LandingFragmentAdapter(false);
        binding.recycledViewEntries.setAdapter(entriesAdapter);
        binding.recycledViewEntries.setClickable(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recycledViewEntries.setLayoutManager(layoutManager);
        binding.recycledViewEntries.setItemAnimator(null);
        binding.recycledViewEntries.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Load more items on user scrolls down
                if (dy > 0) {
                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                    // Threshold: Load more when 5 items from the bottom
                    if (!isLoading && totalItemCount <= (lastVisibleItem + 5)) {
                        Log.d(TAG, "Scroll at the last 5 items. Starting loadNextBatch.");
                        loadNextBatch();
                    }
                }
            }
        });
        binding.recycledViewEntries.addOnItemTouchListener(
                new RecyclerOnClickListener(requireContext(), binding.recycledViewEntries, new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        // Select the notifications
                        if (actionMode != null) {
                            selectDeselect(position);
                        }
                        // Or open it in normal way...
                        else {
                            binding.recycledViewEntries.setClickable(false);
                            LandingFragmentItems item = entriesAdapter.getCurrentList().get(position);
                            if (item.isTimedCount()) {
                                openTimedCount(item.getLocalId());
                            } else {
                                openObservation(item.getLocalId());
                            }
                        }
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Log.d(TAG, "Item " + position + " long pressed.");
                        // Update Toolbar
                        if (actionMode == null) {
                            actionMode = ((AppCompatActivity) requireActivity())
                                    .startSupportActionMode(actionModeCallback);
                        }
                        selectDeselect(position);
                        entriesAdapter.notifyItemChanged(position);

                        entriesAdapter.setPosition(position);
                    }
                })
        );
        registerForContextMenu(binding.recycledViewEntries);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Swipe to refresh triggered.");
            downloadNewerData();
        });
    }

    private void progressBar(boolean show) {
        if (show) {
            binding.loadingProgressBar.setAlpha(1f);
            binding.loadingProgressBar.setVisibility(View.VISIBLE);
        } else {
            binding.loadingProgressBar.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> binding.loadingProgressBar.setVisibility(View.GONE));
        }
    }

    private void loadNextBatch() {
        if (isLoading) return;
        isLoading = true;
        progressBar(true);

        ArrayList<EntryDb> localObservationsBatch = ObjectBoxHelper.getPagedObservations(25, localObjectBoxObservationOffset);
        ArrayList<TimedCountDb> localTimedCountsBatch = ObjectBoxHelper.getPagedTimedCounts(25, localObjectBoxTimedCountsOffset);

        ArrayList<LandingFragmentItems> batchItems = new ArrayList<>();

        if (!localObservationsBatch.isEmpty()) {
            for (EntryDb entry : localObservationsBatch) {
                batchItems.add(getItemFromEntry(requireContext(), entry));
            }
            localObjectBoxObservationOffset += localObservationsBatch.size();
        }

        if (!localTimedCountsBatch.isEmpty()) {
            for (TimedCountDb timedCount : localTimedCountsBatch) {
                batchItems.add(getItemFromTimedCount(requireContext(), timedCount));
            }
            localObjectBoxTimedCountsOffset += localTimedCountsBatch.size();
        }

        if (!batchItems.isEmpty()) {
            // Sort only the new batch, then append — don't re-sort the whole list
            Collections.sort(batchItems, compareUploadAndDate);
            appendItems(batchItems);
            progressBar(false);
            isLoading = false;
        } else {
            Log.d(TAG, "No more local data. Fetching older data from server...");
            downloadOlderData();
        }
    }

    private void appendItems(List<LandingFragmentItems> newItems) {
        List<LandingFragmentItems> current = new ArrayList<>(entriesAdapter.getCurrentList());
        current.addAll(newItems);
        entriesAdapter.submitList(current, () -> isLoading = false);
    }

    private void downloadNewerData() {

        long updatedAt = SettingsManager.getObservationsUpdatedAt();
        Log.d(TAG, "Loading new data from Retrofit, timestamp: " + updatedAt);

        Data data = new Data.Builder()
                .putBoolean("isSyncOnScroll", false)
                .putLong("updatedAt", updatedAt)
                .build();

        OneTimeWorkRequest timedCountRequest = new OneTimeWorkRequest.Builder(TimedCountsDownloadWorker.class)
                .setInputData(data)
                .addTag("TIMED_COUNTS_DOWNLOAD_UPDATED_AT")
                .addTag("UPDATED_AT_SYNC")
                .build();

        OneTimeWorkRequest observationRequest = new OneTimeWorkRequest.Builder(ObservationsDownloadWorker.class)
                .setInputData(data)
                .addTag("OBSERVATIONS_DOWNLOAD_UPDATED_AT")
                .addTag("UPDATED_AT_SYNC")
                .build();

        OneTimeWorkRequest photoRequest = new OneTimeWorkRequest.Builder(PhotoDownloadWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .addTag("PHOTO_DOWNLOAD")
                .build();

        WorkManager.getInstance(requireContext())
                .beginUniqueWork(
                        "data_sync_chain",
                        ExistingWorkPolicy.KEEP,
                        timedCountRequest)
                .then(observationRequest)
                .then(photoRequest)
                .enqueue();
    }

    private void downloadOlderData() {
        Log.d(TAG, "Loading more data from Retrofit.");

        long beforeObservationId = ObjectBoxHelper.getMinObservationServerId();
        long beforeTimedCountId = ObjectBoxHelper.getMinTimedCountsServerId();

        OneTimeWorkRequest observationsRequest = new OneTimeWorkRequest.Builder(ObservationsDownloadWorker.class)
                .setInputData(new Data.Builder()
                        .putLong("beforeId", beforeObservationId)
                        .putBoolean("isSyncOnScroll", true)
                        .build())
                .addTag("OBSERVATIONS_DOWNLOAD_BY_ID")
                .addTag("UPDATED_BY_ID")
                .build();

        OneTimeWorkRequest timedCountsRequest = new OneTimeWorkRequest.Builder(TimedCountsDownloadWorker.class)
                .setInputData(new Data.Builder()
                        .putLong("beforeId", beforeTimedCountId)
                        .putBoolean("isSyncOnScroll", true)
                        .build())
                .addTag("TIMED_COUNTS_DOWNLOAD_BY_ID")
                .addTag("UPDATED_BY_ID")
                .build();

        OneTimeWorkRequest photoRequest = new OneTimeWorkRequest.Builder(PhotoDownloadWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .addTag("PHOTO_DOWNLOAD")
                .build();

        WorkManager.getInstance(requireContext())
                .beginUniqueWork(
                        "data_sync_chain",
                        ExistingWorkPolicy.KEEP,
                        Arrays.asList(observationsRequest, timedCountsRequest))
                .then(photoRequest)
                .enqueue();
    }

    private void selectDeselect(int position) {
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

    private void updateActionModeMenu(ActionMode mode, List<LandingFragmentItems> list, int selectedCount) {
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

    // Opens ActivityEntry to add new species observation
    private void newObservation() {
        Intent intent = new Intent(getActivity(), ActivityObservation.class);
        intent.putExtra("IS_NEW_ENTRY", true);
        observationLauncher.launch(intent);
    }

    // Opens ActivityEntry to update existing species observation
    private void openObservation(long entryId) {
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent(activity.getApplicationContext(), ActivityObservation.class);
            intent.putExtra("IS_NEW_ENTRY", false);
            intent.putExtra("ENTRY_ID", entryId);
            observationLauncher.launch(intent);
        }
    }

    // Opens ActivityTimedCount to add new timed count
    private void newTimedCount() {
        Intent intent = new Intent(getActivity(), ActivityTimedCount.class);
        intent.putExtra("IS_NEW_ENTRY", true);
        timedCountLauncher.launch(intent);
    }

    private void newDuplicateObservation(long new_entry_id) {
        Intent intent = new Intent(getActivity(), ActivityObservation.class);
        intent.putExtra("IS_NEW_ENTRY", false);
        intent.putExtra("ENTRY_ID", new_entry_id);
        intent.putExtra("IS_DUPLICATED_ENTRY", true);
        observationLauncher.launch(intent);
    }

    // Opens ActivityTimedCount to update existing timed count
    private void openTimedCount(long timedCountId) {
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent(activity.getApplicationContext(), ActivityTimedCount.class);
            intent.putExtra("IS_NEW_ENTRY", false);
            intent.putExtra("TIMED_COUNT_ID", timedCountId);
            timedCountLauncher.launch(intent);
        }
    }

    // Launch new Entry Activity
    private final ActivityResultLauncher<Intent> observationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "We got a result from the ActivityObservation!");

                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "We got RESULT_OK code from the ActivityObservation.");

                    boolean isNewEntry = result.getData() != null && result.getData().getBooleanExtra("IS_NEW_ENTRY", false);
                    boolean isDuplicatedEntry = result.getData() != null && result.getData().getBooleanExtra("IS_DUPLICATED_ENTRY", false);

                    if (isNewEntry || isDuplicatedEntry) {
                        long entry_id = result.getData().getLongExtra("ENTRY_ID", 0);
                        Log.d(TAG, "This is a new/duplicated entry with id: " + entry_id);
                        addObservationItem(entry_id);
                    } else {
                        long old_data_id = 0;
                        if (result.getData() != null) {
                            old_data_id = result.getData().getLongExtra("ENTRY_ID", 0);
                        }
                        Log.d(TAG, "This was an existing entry edit with id: " + old_data_id);
                        reloadItemsForRecyclerView();
                    }
                }

                updateUploadIconVisibility();

            });

    // Launch new Timed Count Activity
    private final ActivityResultLauncher<Intent> timedCountLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Log.d(TAG, "We got a result from the Timed Count Activity!");
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            if (result.getData() != null && result.getData().getBooleanExtra("IS_NEW_ENTRY", false)) {
                                long timeCountId = result.getData().getLongExtra("TIMED_COUNT_ID", 0);
                                addTimedCountItem((int) timeCountId);
                            } else {
                                Log.d(TAG, "This was an existing Timed Count. Should not change the UI.");
                            }

                            updateUploadIconVisibility();

                        }
                    });

    private void addObservationItem(long entryId) {
        // Load observation entry
        EntryDb entryDb = ObjectBoxHelper.getObservationById(entryId);
        if (entryDb == null) {
            Log.e(TAG, "New entry ID " + entryId + " not found in database.");
            return;
        }

        // Display observation in the RecyclerView
        Log.d(TAG, "Adding entry " + entryId + " to the list.");
        LandingFragmentItems newItem = getItemFromEntry(requireContext(), entryDb);
        addItemToRecyclerView(newItem);

        // Update UI element
        ((ActivityLanding) requireActivity()).updateMenuIconVisibility();
    }

    private void addTimedCountItem(int timedCountId) {
        TimedCountDb timedCount = ObjectBoxHelper.getTimedCountById(timedCountId);
        if (timedCount == null) {
            Log.e(TAG, "New entry ID " + timedCountId + " not found in database.");
            return;
        }

        LandingFragmentItems newItem = getItemFromTimedCount(getContext(), timedCount);
        addItemToRecyclerView(newItem);
        ((ActivityLanding) requireActivity()).updateMenuIconVisibility();
    }

    private void addItemToRecyclerView(LandingFragmentItems item) {
        List<LandingFragmentItems> currentList = new ArrayList<>(entriesAdapter.getCurrentList());
        currentList.add(0, item);
        Log.d(TAG, "There are " + entriesAdapter.getItemCount() + " items in the adapter, " + currentList.size() + " in the list.");
        entriesAdapter.submitList(currentList, () ->
                binding.recycledViewEntries.post(() ->
                        binding.recycledViewEntries.smoothScrollToPosition(0)));
    }

    // Method to show the context menu on + button long press
    private void showContextMenu(View view) {
        // Create the context menu and set its items
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_add_entry_long_press, popupMenu.getMenu());

        // Handle item selection
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_entry_timed_count) {
                newTimedCount();
                return true;
            } else if (item.getItemId() == R.id.menu_entry_observation) {
                newObservation();
                return true;
            } else {
                return false;
            }
        });

        popupMenu.show();
    }

    // Change the visibility of the Upload Icon
    private void updateUploadIconVisibility() {
        Activity activity = getActivity();
        if (activity != null) {
            ((ActivityLanding) getActivity()).updateMenuIconVisibility();
        }
    }

    private void showUploadProgress(boolean shown, int percentage) {
        if (shown) {
            binding.uploadProgressBar.setVisibility(View.VISIBLE);
            binding.uploadProgressBar.setProgress(percentage);
        } else {
            binding.uploadProgressBar.setVisibility(View.GONE);
        }
    }

    public void duplicateEntry(int position) {
        LandingFragmentItems item = entriesAdapter.getCurrentList().get(position);
        if (item.isTimedCount()) {
            Log.d(TAG, "Timed counts cannot be duplicated.");
            actionMode.finish();
            return;
        }

        Log.d(TAG, "You will now duplicate entry at position " + position);
        EntryDb entry_from = ObjectBoxHelper.getObservationById(item.getLocalId());

        // Create new entry based on current one
        if (entry_from != null) {
            EntryDb entry = new EntryDb(
                    0,
                    null,
                    false,
                    false,
                    entry_from.getTaxonId(),
                    entry_from.getTimedCoundId(),
                    entry_from.getTaxonSuggestion(),
                    entry_from.getYear(),
                    entry_from.getMonth(),
                    entry_from.getDay(),
                    entry_from.getComment(),
                    null,
                    "",
                    null,
                    null,
                    "true",
                    "",
                    entry_from.getLattitude(),
                    entry_from.getLongitude(),
                    entry_from.getAccuracy(),
                    entry_from.getElevation(),
                    entry_from.getLocation(),
                    null,
                    null,
                    null,
                    entry_from.getProjectId(),
                    entry_from.getFoundOn(),
                    entry_from.getDataLicence(),
                    entry_from.getImageLicence(),
                    entry_from.getTime(),
                    entry_from.getHabitat(),
                    ""
            );

            // Update ObjectBox
            long new_entry_id = ObjectBoxHelper.setObservation(entry);

            // Open EntryActivity to edit the new record
            newDuplicateObservation(new_entry_id);
        }

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void deleteSelectedEntries() {
        List<LandingFragmentItems> snapshot = new ArrayList<>(entriesAdapter.getCurrentList());
        List<LandingFragmentItems> toDelete = new ArrayList<>();

        for (LandingFragmentItems item : snapshot) {
            if (item.isMarked()) toDelete.add(item);
        }

        if (toDelete.isEmpty()) return;

        int count = toDelete.size();

        // Delete request
        for (LandingFragmentItems item : toDelete) {
            if (item.isUploaded()) {
                sendDeleteRequestToServer(item);
            } else {
                deleteFromObjectBox(item, false);
            }
        }

        // Get the deleted filed that require refresh
        Set<Long> deletedIds = new HashSet<>();
        for (LandingFragmentItems item : toDelete) {
            if (!item.isUploaded()) { // remove non-uploaded from UI
                deletedIds.add(item.getLocalId());
            }
        }

        // Create new list for recycler view
        List<LandingFragmentItems> newList = new ArrayList<>();
        for (LandingFragmentItems item : snapshot) {
            if (!deletedIds.contains(item.getLocalId())) {
                // Keep item, but deselect it
                newList.add(item.isMarked() ? item.withMarked(false) : item);
            }
        }

        entriesAdapter.submitList(newList);
        updateUploadIconVisibility();

        String message = getResources().getQuantityString(
                R.plurals.entries_deleted_count, count, count);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

        ActionMode mode = actionMode;
        actionMode = null;
        if (mode != null) mode.finish();
    }

    private void deleteFromObjectBox(LandingFragmentItems item, boolean reload) {
        Long id = item.getLocalId();
        if (id == null) return;
        if (item.isTimedCount()) {
            ObjectBoxHelper.removeObservationsForTimedCountId(id);
            ObjectBoxHelper.removeTimedCountById(id);
        } else {
            ObjectBoxHelper.removeObservationById(id);
        }

        if (reload) {
            reloadItemsForRecyclerView();
        }
    }

    private void sendDeleteRequestToServer(LandingFragmentItems item) {

        Call<Void> call = null;
        if (item.isTimedCount()) {
            TimedCountDb timedCount = ObjectBoxHelper.getTimedCountById(item.getLocalId());
            if (timedCount != null) {
                call = RetrofitClient.getService(SettingsManager.getDatabaseName())
                        .deleteTimedCountObservation(timedCount.getServerId());
            }
        } else {
            EntryDb entry = ObjectBoxHelper.getObservationById(item.getLocalId());
            if (entry != null) {
                call = RetrofitClient.getService(SettingsManager.getDatabaseName())
                        .deleteFieldObservation(entry.getServerId());
            }
        }

        if (call != null) {
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        if (item.isTimedCount()) {
                            Log.d(TAG, "Timed Count ID " + item.getLocalId() + " deleted from server.");
                        } else {
                            Log.d(TAG, "Observation ID " + item.getLocalId() + " deleted from server.");
                        }
                        deleteFromObjectBox(item, true);
                    } else {
                        if (response.code() == 404 || response.code() == 403) {
                            deleteFromObjectBox(item, true);
                            Log.e(TAG, "File already deleted on the server: " + response.code());
                        } else {
                            Toast.makeText(getContext(), "Error deleting (server).", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error deleting file from server: " + response.code());
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "Network error deleting the file: " + t.getMessage());
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void applyBatchUpdates(List<Long> observationIds, List<Long> timedCountIds) {
        // 1. Fetch the list ONCE
        List<LandingFragmentItems> currentList = new ArrayList<>(entriesAdapter.getCurrentList());
        boolean hasChanges = false;

        // 2. Process all Observation updates
        if (observationIds != null && !observationIds.isEmpty()) {
            for (long id : new HashSet<>(observationIds)) {
                EntryDb entry = ObjectBoxHelper.getObservationById(id);
                if (entry != null) {
                    LandingFragmentItems newItem = getItemFromEntry(requireContext(), entry);
                    int index = findItemIndex(currentList, id, false);
                    if (index != -1) currentList.set(index, newItem);
                    else currentList.add(newItem);
                    hasChanges = true;
                }
            }
        }

        // 3. Process all Timed Count updates
        if (timedCountIds != null && !timedCountIds.isEmpty()) {
            for (long id : new HashSet<>(timedCountIds)) {
                TimedCountDb timedCount = ObjectBoxHelper.getTimedCountById(id);
                if (timedCount != null) {
                    LandingFragmentItems newItem = getItemFromTimedCount(requireContext(), timedCount);
                    int index = findItemIndex(currentList, id, true);
                    if (index != -1) currentList.set(index, newItem);
                    else currentList.add(newItem);
                    hasChanges = true;
                }
            }
        }

        // 4. Sort and submit ONCE
        if (hasChanges) {
            Collections.sort(currentList, LandingFragmentItems.compareUploadAndDate);
            entriesAdapter.submitList(currentList);
            ((ActivityLanding) requireActivity()).updateMenuIconVisibility();
        }
    }

    private int findItemIndex(List<LandingFragmentItems> list, long id, boolean isTimedCount) {
        for (int i = 0; i < list.size(); i++) {
            LandingFragmentItems item = list.get(i);
            long localId = item.getLocalId() != null ? item.getLocalId() : -1L;
            if (item.isTimedCount() == isTimedCount && localId == id) {
                return i;
            }
        }
        return -1;
    }

}
