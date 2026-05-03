package org.biologer.biologer.gui;

import static org.biologer.biologer.adapters.LandingFragmentItems.SORTER;
import static org.biologer.biologer.adapters.LandingFragmentItems.getItemFromEntry;
import static org.biologer.biologer.adapters.LandingFragmentItems.getItemFromTimedCount;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
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
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
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
import org.biologer.biologer.helpers.NetworkServicesHelper;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.services.RecyclerOnClickListener;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.TimedCountDb;
import org.biologer.biologer.workers.ObservationsDownloadWorker;
import org.biologer.biologer.workers.PhotoDownloadWorker;
import org.biologer.biologer.workers.TimeCountsDownloadWorker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FragmentLanding extends BaseObservationListFragment {

    String TAG = "Biologer.FragmentLanding";
    private FragmentLandingBinding binding;
    private final Set<UUID> processedWorkerIds = new HashSet<>();
    private int localObjectBoxObservationOffset = 0;
    private int localObjectBoxTimedCountsOffset = 0;
    private boolean isLoading = false;
    private boolean isDownloadingById = false;

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

        // If the user did not start the EntryActivity show a short help
        // If there are more than 5 entries the message will disappear
        if (!SettingsManager.isEntryCreated()) {
            binding.listEntriesInfoText.setText(R.string.entry_info_first_run);
            binding.listEntriesInfoText.setVisibility(View.VISIBLE);
            SettingsManager.setEntryCreated(true);
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Sort preference changed, reloading list.");
            reloadItemsForRecyclerView();
        }
    };

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

    @Override
    protected RecyclerView getRecyclerView() {
        return binding.recycledViewEntries;
    }

    @Override
    public void loadItemsForRecyclerView() {
        localObjectBoxObservationOffset = 0;
        localObjectBoxTimedCountsOffset = 0;

        // Group 1. Data waiting for upload to the server
        ArrayList<LandingFragmentItems> allItems =
                LandingFragmentItems.loadAllLocalEntries(requireContext());

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean showUploaded = sharedPreferences.getBoolean("show_uploaded", true);

        if (showUploaded) {
            // Group 2. Load first batch of items already uploaded
            ArrayList<EntryDb> syncedObservations = ObjectBoxHelper.getPagedObservations(25, 0);
            for (EntryDb observation : syncedObservations) {
                allItems.add(getItemFromEntry(requireContext(), observation));
            }
            ArrayList<TimedCountDb> syncedTimedCounts = ObjectBoxHelper.getPagedTimedCounts(25, 0);
            for (TimedCountDb timedCount : syncedTimedCounts) {
                allItems.add(getItemFromTimedCount(requireContext(), timedCount));
            }

            Collections.sort(allItems, SORTER);

            localObjectBoxObservationOffset = syncedObservations.size();
            localObjectBoxTimedCountsOffset = syncedTimedCounts.size();

            entriesAdapter.submitList(allItems);

            // If the list is short get more data
            if (allItems.size() < 10) {
                loadNextBatch();
            }

            // Finally, download new data updated on the server if exist
            if (NetworkServicesHelper.shouldDownload(getContext())) {
                downloadNewerData();
            }
        }
    }

    private void reloadItemsForRecyclerView() {
        binding.recycledViewEntries.post(() -> {
            ArrayList<LandingFragmentItems> allItems =
                    LandingFragmentItems.loadAllLocalEntries(requireContext());

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            boolean showUploaded = sharedPreferences.getBoolean("show_uploaded", true);

            if (showUploaded) {
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
                Collections.sort(allItems, SORTER);
            }
            entriesAdapter.submitList(allItems);
            ((ActivityLanding) requireActivity()).updateMenuIconVisibility();
        });
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

                        if (workInfo.getState() == WorkInfo.State.ENQUEUED && workInfo.getRunAttemptCount() > 0) {
                            Log.w(TAG, "Worker " + workInfo.getId() + " is retrying. Attempt: " + workInfo.getRunAttemptCount());
                            Toast.makeText(getContext(), "Retrying worker " + workInfo.getId() + " (" + workInfo.getRunAttemptCount() + ")", Toast.LENGTH_SHORT).show();
                        }

                        if (workInfo.getState() == WorkInfo.State.FAILED) {
                            long code = workInfo.getOutputData().getLong("error_code", 0);
                            String message = workInfo.getOutputData().getString("error_message");
                            Toast.makeText(getContext(), "Error " + code + ": " + message, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error " + code + ": " + message);
                            failed++;
                        }

                        if (workInfo.getState().isFinished()) {
                            finished++;

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
                                    binding.recycledViewEntries.post(() -> binding.recycledViewEntries.smoothScrollToPosition(0));
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
                            Log.d(TAG, "Displaying new observations from the server.");
                            //addItems(observationsToUpdate, timedCountsToUpdate);
                            reloadItemsForRecyclerView();
                        }

                        if (allFinished) {
                            Log.d(TAG, "All observations sync workers by timestamp finished. Updating UI.");

                            isDownloadingById = false;
                            isLoading = false;
                            progressBar(false);

                            if (getActivity() == null || binding == null) return;

                            binding.swipeRefreshLayout.setRefreshing(false);
                            LinearLayoutManager lm = (LinearLayoutManager) binding.recycledViewEntries.getLayoutManager();
                            if (lm != null && lm.findFirstVisibleItemPosition() < 5) {
                                Log.d(TAG, "Scrolling to the top of the list (less than 5 items at the top).");
                                binding.recycledViewEntries.post(() -> {
                                    if (!isUploadInProgress() &&
                                            ObjectBoxHelper.getUnsyncedCount() > 0 &&
                                            NetworkServicesHelper.shouldDownload(getContext())) {
                                        ((ActivityLanding) requireActivity()).uploadRecords();
                                    }
                                    binding.recycledViewEntries.smoothScrollToPosition(0);
                                });
                            }

                            if (entriesAdapter.getItemCount() > 5) {
                                binding.listEntriesInfoText.setVisibility(View.GONE);
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
                                isDownloadingById = false;
                                isLoading = false;
                                progressBar(false);
                                Toast.makeText(getContext(), "Sync failed. Check connection.", Toast.LENGTH_SHORT).show();
                            }

                            if (!workInfo.getState().isFinished()) {
                                allFinished = false;
                                break;
                            }
                        }

                        if (!observationsToUpdate.isEmpty() || !timedCountsToUpdate.isEmpty()) {
                            Log.d(TAG, "Displaying next batch of observations from the server.");
                            addItems(observationsToUpdate, timedCountsToUpdate);
                        }

                        if (allFinished) {
                            Log.d(TAG, "All observations sync workers by id finished. Updating UI.");
                            isLoading = false;
                            isDownloadingById = false;
                            progressBar(false);
                            // Auto-scroll if user is already near top
                            LinearLayoutManager lm = (LinearLayoutManager) binding.recycledViewEntries.getLayoutManager();
                            if (lm != null && lm.findFirstVisibleItemPosition() < 5) {
                                binding.recycledViewEntries.post(() ->
                                        binding.recycledViewEntries.smoothScrollToPosition(0));
                            }
                        }

                    }
                });
    }

    private void updatePhotoForObservation(long[] observationIds) {

        if (!isAdded() || getContext() == null || binding == null) {
            Log.d(TAG, "Fragment not attached or binding is null, skipping photo update.");
            return;
        }

        List<LandingFragmentItems> currentList = new ArrayList<>(entriesAdapter.getCurrentList());

        for (long id : observationIds) {
            int index = entriesAdapter.getItemIndexFromId(id, false);
            if (index == -1) {
                Log.e(TAG, "Photo update: item " + id + " not in list yet, skipping.");
                continue;
            }
            EntryDb entry = ObjectBoxHelper.getObservationById(id);
            if (entry == null) continue;
            currentList.set(index, getItemFromEntry(getContext(), entry));
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
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                boolean showUploaded = sharedPreferences.getBoolean("show_uploaded", true);

                if (showUploaded) {
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
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                boolean showUploaded = sharedPreferences.getBoolean("show_uploaded", true);
                if (showUploaded) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (!recyclerView.canScrollVertically(1) && !isLoading) {
                            Log.d(TAG, "Reached bottom of list while idle. Starting loadNextBatch.");
                            loadNextBatch();
                        }
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
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            boolean showUploaded = sharedPreferences.getBoolean("show_uploaded", true);
            if (showUploaded) {
                if (NetworkServicesHelper.shouldDownload(getContext())) {
                    FragmentLanding.this.downloadNewerData();
                } else {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.download_data_title)
                            .setMessage(R.string.download_data_message)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                Log.d(TAG, "User manually accepted a one-time data download.");
                                FragmentLanding.this.downloadNewerData();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> {
                                binding.swipeRefreshLayout.setRefreshing(false);
                                dialog.dismiss();
                            })
                            .setOnCancelListener(dialog -> binding.swipeRefreshLayout.setRefreshing(false))
                            .show();
                }
            }
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
                    .withEndAction(() -> {
                        if (binding != null) {
                            binding.loadingProgressBar.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private void loadNextBatch() {
        if (isLoading) return;
        isLoading = true;
        progressBar(true);

        Log.d(TAG, "Loading next page of data.");

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
            Log.d(TAG, "There are stored records. Loading data from ObjectBox...");
            reloadItemsForRecyclerView();
            progressBar(false);
            isLoading = false;
        } else {
            // Skip if user selected not to download data form the server
            if (!NetworkServicesHelper.shouldDownload(getContext())) {
                progressBar(false);
                isLoading = false;
                return;
            }

            Log.d(TAG, "No more local data. Fetching older data from server...");
            WorkManager.getInstance(requireContext())
                    .getWorkInfosByTag("UPDATED_AT_SYNC")
                    .addListener(() -> {
                        try {
                            List<WorkInfo> infos = WorkManager.getInstance(requireContext())
                                    .getWorkInfosByTag("UPDATED_AT_SYNC").get();
                            boolean newerSyncRunning = false;
                            for (WorkInfo i : infos) {
                                if (!i.getState().isFinished()) {
                                    newerSyncRunning = true;
                                    break;
                                }
                            }
                            if (!newerSyncRunning) {
                                downloadOlderData();
                            } else {
                                Log.d(TAG, "Newer data sync still running, skipping downloadOlderData.");
                                isLoading = false;
                                progressBar(false);
                            }
                        } catch (Exception e) {
                            isLoading = false;
                            progressBar(false);
                        }
                    }, ContextCompat.getMainExecutor(requireContext()));
        }
    }

    private void downloadNewerData() {
        if (isDownloadingById) return;
        isDownloadingById = true;

        long observationsUpdatedAt = SettingsManager.getObservationsUpdatedAt();
        long timeCountsUpdatedAt = SettingsManager.getTimeCountsUpdatedAt();
        Log.d(TAG, "Loading new data from Retrofit, timestamps: "
                + observationsUpdatedAt + ", " + timeCountsUpdatedAt);

        OneTimeWorkRequest timedCountRequest = new OneTimeWorkRequest.Builder(TimeCountsDownloadWorker.class)
                .setInputData(new Data.Builder()
                        .putBoolean("isSyncOnScroll", false)
                        .putLong("timeCountsUpdatedAt", timeCountsUpdatedAt)
                        .build())
                .addTag("TIMED_COUNTS_DOWNLOAD_UPDATED_AT")
                .addTag("UPDATED_AT_SYNC")
                .build();

        OneTimeWorkRequest observationRequest = new OneTimeWorkRequest.Builder(ObservationsDownloadWorker.class)
                .setInputData(new Data.Builder()
                        .putBoolean("isSyncOnScroll", false)
                        .putLong("observationsUpdatedAt", observationsUpdatedAt)
                        .build())
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
                        ExistingWorkPolicy.REPLACE,
                        timedCountRequest)
                .then(observationRequest)
                .then(photoRequest)
                .enqueue();
    }

    private void downloadOlderData() {
        Log.d(TAG, "Loading more data from Retrofit.");

        long beforeObservationId = ObjectBoxHelper.getMinObservationServerId(false);
        long beforeTimedCountId = ObjectBoxHelper.getMinTimedCountsServerId();

        OneTimeWorkRequest observationsRequest = new OneTimeWorkRequest.Builder(ObservationsDownloadWorker.class)
                .setInputData(new Data.Builder()
                        .putLong("beforeId", beforeObservationId)
                        .putBoolean("isSyncOnScroll", true)
                        .build())
                .addTag("OBSERVATIONS_DOWNLOAD_BY_ID")
                .addTag("UPDATED_BY_ID")
                .build();

        OneTimeWorkRequest timedCountsRequest = new OneTimeWorkRequest.Builder(TimeCountsDownloadWorker.class)
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

    private void addTimedCountItem(int timedCountId) {
        TimedCountDb timedCount = ObjectBoxHelper.getTimeCountById(timedCountId);
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
        Collections.sort(currentList, SORTER);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void addItems(List<Long> observationIds, List<Long> timedCountIds) {
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
                TimedCountDb timedCount = ObjectBoxHelper.getTimeCountById(id);
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
            Collections.sort(currentList, LandingFragmentItems.SORTER);
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

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(broadcastReceiver,
                        new IntentFilter("SORT_OBSERVATIONS_CHANGED"));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(broadcastReceiver);
    }

    private boolean isUploadInProgress() {
        try {
            List<WorkInfo> workInfos = WorkManager.getInstance(requireContext())
                    .getWorkInfosByTag("UPLOAD_WORK").get();
            for (WorkInfo info : workInfos) {
                if (info.getState() == WorkInfo.State.RUNNING ||
                        info.getState() == WorkInfo.State.ENQUEUED) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not check WorkManager status", e);
        }
        return false;
    }

    @Override
    protected void addObservationItem(long entryId) {
        super.addObservationItem(entryId);
        updateUploadIconVisibility();
    }

}
