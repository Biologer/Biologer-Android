package org.biologer.biologer.gui;

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
import org.biologer.biologer.helpers.DateHelper;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentLanding extends Fragment {

    String TAG = "Biologer.FragmentLanding";
    private FragmentLandingBinding binding;
    private ArrayList<LandingFragmentItems> items;
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

        loadItemsForRecyclerView();
        setupRecyclerView();
        setupFloatingActionButton();
        setupWorkerObservers(); // To track upload of data online
        downloadNewerData(); // If the data is added directly on the server

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
        if (items != null) {
            items.clear();
        } else {
            items = new ArrayList<>();
        }

        // Load new data not uploaded to the server
        items = LandingFragmentItems.loadAllLocalEntries(requireContext());

        ArrayList<LandingFragmentItems> syncedItems = new ArrayList<>();

        // If the list is short ask for more items
        ArrayList<EntryDb> syncedObservations = ObjectBoxHelper.getPagedObservations(25, 0);
        for (EntryDb observation : syncedObservations) {
            syncedItems.add(getItemFromEntry(requireContext(), observation));
        }
        ArrayList<TimedCountDb> syncedTimedCounts = ObjectBoxHelper.getPagedTimedCounts(25, 0);
        for (TimedCountDb timedCount : syncedTimedCounts) {
            syncedItems.add(getItemFromTimedCount(requireContext(), timedCount));
        }

        Collections.sort(syncedItems, (item1, item2) ->
                item2.getDate().compareTo(item1.getDate()));

        items.addAll(syncedItems);

        // Update the adapter
        if (entriesAdapter == null) {
            entriesAdapter = new LandingFragmentAdapter(items, false);
            binding.recycledViewEntries.setAdapter(entriesAdapter);
        } else {
            entriesAdapter.updateData(items);
        }

        // If the list is short check the server
        if (items.size() < 10) {
            loadNextBatch();
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_entry_items_selected, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            int selectedCount = getSelectedItemsCount();
            // Hide Duplicate if more than one item selected
            MenuItem duplicateItem = menu.findItem(R.id.duplicate);
            if (duplicateItem != null) {
                duplicateItem.setVisible(selectedCount == 1);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.delete) {
                deleteSelectedEntries();
                return true;
            } else if (item.getItemId() == R.id.duplicate) {
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).isMarked()) {
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
            deselectAllItems();
            actionMode = null;
        }
    };

    private int getSelectedItemsCount() {
        int count = 0;
        for (LandingFragmentItems item : items) {
            if (item.isMarked()) count++;
        }
        return count;
    }

    private void deselectAllItems() {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isMarked()) {
                items.get(i).setMarked(false);
                entriesAdapter.notifyItemChanged(i);
            }
        }
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
                                if (workInfo.getState() == WorkInfo.State.FAILED) failed++;

                                // Check if we've already handled this worker – be more efficient :)
                                if (!processedWorkerIds.contains(workInfo.getId())) {
                                    processedWorkerIds.add(workInfo.getId());
                                    if (workInfos.size() > 1 && finished == total) {
                                        Log.d(TAG, "Skipping historical worker toast: " + workInfo.getId());
                                    } else {
                                        shouldShowToast = true;
                                    }
                                    long observationId = workInfo.getOutputData().getLong("updatedObservationId", 0);
                                    if (observationId > 0) {
                                        updateSingleObservationItem(observationId);
                                    }
                                    long timedCountId = workInfo.getOutputData().getLong("updatedTimedCountId", 0);
                                    if (timedCountId > 0) {
                                        updateSingleTimeCountItem(timedCountId);
                                    }
                                    Log.d(TAG, "Observation ID: " + observationId + "; Timed Count ID: " + timedCountId);
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
                                long[] ids = workInfo.getOutputData().getLongArray("updatedObservationIds");
                                updateObservationItemsByIds(ids);
                            } else if (state == WorkInfo.State.FAILED && !processedWorkerIds.contains(workInfo.getId())) {
                                processedWorkerIds.add(workInfo.getId());
                                Toast.makeText(getContext(), "Sync failed. Check connection.", Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                });

        WorkManager.getInstance(requireContext())
                .getWorkInfosByTagLiveData("OBSERVATIONS_DOWNLOAD_UPDATED_AT")
                .observe(getViewLifecycleOwner(), workInfos -> {
                    if (workInfos != null && !workInfos.isEmpty()) {

                        boolean allFinished = true;
                        for (WorkInfo workInfo : workInfos) {
                            WorkInfo.State state = workInfo.getState();

                            if (state == WorkInfo.State.SUCCEEDED && !processedWorkerIds.contains(workInfo.getId())) {
                                processedWorkerIds.add(workInfo.getId());
                                long[] ids = workInfo.getOutputData().getLongArray("updatedObservationIds");
                                updateObservationItemsByIds(ids);
                            } else if (state == WorkInfo.State.FAILED && !processedWorkerIds.contains(workInfo.getId())) {
                                processedWorkerIds.add(workInfo.getId());
                                Toast.makeText(getContext(), "Sync failed. Check connection.", Toast.LENGTH_SHORT).show();
                            }

                            if (!workInfo.getState().isFinished()) {
                                allFinished = false;
                                break;
                            }
                        }

                        if (allFinished) {
                            Log.d(TAG, "All observations sync workers finished. Updating UI.");
                            binding.swipeRefreshLayout.setRefreshing(false);
                            // Auto-scroll if user is already near top
                            LinearLayoutManager lm = (LinearLayoutManager) binding.recycledViewEntries.getLayoutManager();
                            if (lm != null && lm.findFirstVisibleItemPosition() < 5) {
                                binding.recycledViewEntries.smoothScrollToPosition(0);
                            }
                        }

                    }
                });
    }

    private void updateObservationItemsByIds(long[] ids) {
        if (ids != null) {
            // Check for duplicated ids
            Set<Long> uniqueIds = new HashSet<>();
            for (long id : ids) {
                uniqueIds.add(id);
            }
            // Update the item
            for (long id : uniqueIds) {
                updateSingleObservationItem(id);
            }
        }
    }

    private void updateSingleObservationItem(long observationId) {

        int index = getItemIndex(observationId, false);

        EntryDb entry = ObjectBoxHelper.getObservationById(observationId);
        if (entry != null) {
            if (index != -1) {
                Log.d(TAG, "Updating existing entry with ID " + observationId + " at index " + index);
                items.set(index, getItemFromEntry(requireContext(), entry));
                entriesAdapter.notifyItemChanged(index);
            } else {
                Log.e(TAG, "Cannot update observation with ID " + observationId +
                        ". It was not found in the fragment list (index: " + index +
                        ") or database entry is null. Reloading list.");
                //loadItemsForRecyclerView();
            }
        }

    }

    private void updateSingleTimeCountItem(long timedCountId) {

        int index = getItemIndex(timedCountId, true);

        TimedCountDb timedCount = ObjectBoxHelper.getTimedCountById(timedCountId);
        if (timedCount != null) {
            if (index != -1) {
                Log.d(TAG, "Updating existing entry with ID " + timedCountId + " at index " + index);
                items.set(index, getItemFromTimedCount(requireContext(), timedCount));
                entriesAdapter.notifyItemChanged(index);
            } else {
                Log.e(TAG, "Cannot update timed count with ID " + timedCountId +
                        ". It was not found in the fragment list (index: " + index +
                        ") or database entry is null. Reloading list.");
                //loadItemsForRecyclerView();
            }
        }

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
        entriesAdapter = new LandingFragmentAdapter(items, false);
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
                            entriesAdapter.notifyItemChanged(position);
                        }
                        // Or open it in normal way...
                        else {
                            binding.recycledViewEntries.setClickable(false);
                            LandingFragmentItems item = items.get(position);
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

        // 1. Fetch observations and timed counts
        ArrayList<EntryDb> localObservationsBatch = ObjectBoxHelper.getPagedObservations(25, localObjectBoxObservationOffset);
        ArrayList<TimedCountDb> localTimedCountsBatch = ObjectBoxHelper.getPagedTimedCounts(25, localObjectBoxTimedCountsOffset);

        ArrayList<LandingFragmentItems> batchItems = new ArrayList<>();

        // Process Observations
        if (!localObservationsBatch.isEmpty()) {
            Log.d(TAG, "Observations found. Offset: " + localObjectBoxObservationOffset);
            for (EntryDb entry : localObservationsBatch) {
                batchItems.add(getItemFromEntry(requireContext(), entry));
            }
            localObjectBoxObservationOffset += localObservationsBatch.size();
        }

        // Process Timed Counts
        if (!localTimedCountsBatch.isEmpty()) {
            Log.d(TAG, "Timed counts found. Offset: " + localObjectBoxTimedCountsOffset);
            for (TimedCountDb timedCount : localTimedCountsBatch) {
                batchItems.add(getItemFromTimedCount(requireContext(), timedCount));
            }
            localObjectBoxTimedCountsOffset += localTimedCountsBatch.size();
        }

        // 2. Decide: Update UI or Go to Server
        if (!batchItems.isEmpty()) {
            Collections.sort(batchItems, (item1, item2) -> item2.getDate().compareTo(item1.getDate()));

            appendItems(batchItems);

            progressBar(false);
            isLoading = false;
        } else {
            Log.d(TAG, "No more local data. Fetching older data from server...");
            downloadOlderData();
        }
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
                .build();

        OneTimeWorkRequest observationRequest = new OneTimeWorkRequest.Builder(ObservationsDownloadWorker.class)
                .setInputData(data)
                .addTag("OBSERVATIONS_DOWNLOAD_UPDATED_AT")
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

        Data inputData = new Data.Builder()
                .putLong("beforeId", beforeObservationId)
                .putBoolean("isSyncOnScroll", true)
                .build();

        OneTimeWorkRequest observationsRequest = new OneTimeWorkRequest.Builder(ObservationsDownloadWorker.class)
                .setInputData(inputData)
                .addTag("DATA_SYNC")
                .build();

        OneTimeWorkRequest timedCountsRequest = new OneTimeWorkRequest.Builder(TimedCountsDownloadWorker.class)
                .setInputData(new Data.Builder()
                        .putLong("beforeId", beforeTimedCountId)
                        .putBoolean("isSyncOnScroll", true)
                        .build())
                .addTag("DATA_SYNC")
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
        items.get(position).setMarked(!items.get(position).isMarked());

        int selected = getSelectedItemsCount();

        if (selected == 0) {
            if (actionMode != null) {
                actionMode.finish();
            }
        } else {
            if (actionMode != null) {
                actionMode.setTitle(String.valueOf(selected));
                actionMode.invalidate();
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
    private final ActivityResultLauncher<Intent> observationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
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
                        updateSingleObservationItem(old_data_id);
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
                                addTimedCountItem(result.getData());
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
        Log.d(TAG, "Adding entry " + entryId + "to the list.");
        LandingFragmentItems newItem = getItemFromEntry(requireContext(), entryDb);
        addItemToRecyclerView(newItem);

        // Update UI element
        ((ActivityLanding) requireActivity()).updateMenuIconVisibility();
    }

    private void addTimedCountItem(Intent data) {
        int new_timed_count_id = data.getIntExtra("TIMED_COUNT_ID", 0);
        String time = data.getStringExtra("TIMED_COUNT_START_TIME");
        Integer day = data.getIntExtra("TIMED_COUNT_DAY", 0);
        Integer month = data.getIntExtra("TIMED_COUNT_MONTH", 0);
        Integer year = data.getIntExtra("TIMED_COUNT_YEAR", 0);
        Log.d(TAG, "This was a new Timed Count with ID: " + new_timed_count_id);
        Log.d(TAG, "Y-M-D" + year + "-" + month + "-" + day + " at " + time);

        String title = getString(R.string.timed_count);
        String image = "timed_count";
        String subtitle;
        Calendar calendar = DateHelper.getCalendar(
                year,
                month,
                day,
                Objects.requireNonNull(time));
        subtitle = DateHelper.getLocalizedCalendarDate(calendar) + " " +
                getString(R.string.at_time) + " " +
                DateHelper.getLocalizedCalendarTime(calendar);
        LandingFragmentItems item = new LandingFragmentItems(
                null,
                null,
                true,
                false,
                false,
                title,
                subtitle,
                image,
                calendar.getTime());
        addItemToRecyclerView(item);
    }

    private void addItemToRecyclerView(LandingFragmentItems item) {
        if (items == null) {
            items = new ArrayList<>();
            Log.e(TAG, "Items are null in the recycler view.");
        }

        items.add(0, item);
        Log.d(TAG, "There are " + entriesAdapter.getItemCount() + " items in the adapter, " + items.size() + " in the list.");
        if (entriesAdapter != null) {
            entriesAdapter.insertItem(item, 0);
            Log.d(TAG, "NotifyItemInserted. Items in the adapter now: " + entriesAdapter.getItemCount());
            binding.recycledViewEntries.smoothScrollToPosition(0);
        }

    }

    // Add new items to RecyclerView
    private void appendItems(List<LandingFragmentItems> newItems) {
        binding.recycledViewEntries.post(() -> {
            if (items == null) {
                items = new ArrayList<>();
            }

            int initialSize = items.size();
            for (LandingFragmentItems newItem : newItems) {
                boolean exists = false;
                for (LandingFragmentItems existingItem : items) {
                    if (Objects.equals(existingItem.getLocalId(), newItem.getLocalId()) &&
                            existingItem.isTimedCount() == newItem.isTimedCount()) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    items.add(newItem);
                }
            }

            int insertedCount = items.size() - initialSize;
            if (insertedCount > 0) {
                entriesAdapter.notifyItemRangeInserted(initialSize, insertedCount);
            }

            isLoading = false;
        });
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

    // Find the entry’s index by ObservationId
    private int getItemIndex(long id, boolean isTimedCount) {
        for (int i = items.size() - 1; i >= 0; i--) {
            LandingFragmentItems item = items.get(i);
            Log.d(TAG, "Checking item at " + i + ": ItemID=" + item.getLocalId() + ", SearchID=" + id + ", ServerID=" + item.getServerId() + ", Match=" + (item.getLocalId() == id));

            if (item.isTimedCount() == isTimedCount &&
                    item.getLocalId() != null && item.getLocalId() == id) {

                Log.d(TAG, (isTimedCount ? "Timed Count " : "Observation ") + id + " index is " + i);
                return i;
            }
        }
        Log.d(TAG, (isTimedCount ? "Timed Count " : "Observation ") + id + " not found in items.");
        return -1;
    }

    public void duplicateEntry(int position) {
        LandingFragmentItems item = items.get(position);
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
        int count = 0;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).isMarked()) {
                Log.d(TAG, "You will now delete entry index ID: " + i);
                // Delete from server
                if (items.get(i).isUploaded()) {
                    sendDeleteRequestToServer(items.get(i));
                } else {
                    // Just delete local files
                    deleteLocally(items.get(i));
                }
                count++;
            }
        }

        updateUploadIconVisibility();

        String message = getResources().getQuantityString(
                R.plurals.entries_deleted_count,
                count,
                count
        );
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

        actionMode.finish();
    }

    private void deleteLocally(LandingFragmentItems item) {
        int currentPosition = items.indexOf(item);
        Log.d(TAG, "Deleting recycler view item index ID " + currentPosition);

        if (currentPosition == -1) return; // Already deleted

        Long id = item.getLocalId();

        if (item.isTimedCount()) {
            ObjectBoxHelper.removeObservationsForTimedCountId(id);
            ObjectBoxHelper.removeTimedCountById(id);
        } else {
            ObjectBoxHelper.removeObservationById(id);
        }

        items.remove(currentPosition);
        entriesAdapter.notifyItemRemoved(currentPosition);
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
                        deleteLocally(item);
                    } else {
                        if (response.code() == 404 || response.code() == 403) {
                            deleteLocally(item);
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

}
