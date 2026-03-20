package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.biologer.biologer.App;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentLanding extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    String TAG = "Biologer.LandingFragment";
    private FragmentLandingBinding binding;
    private ArrayList<LandingFragmentItems> items;
    LandingFragmentAdapter entriesAdapter;
    private ActionMode actionMode;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLandingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        items = LandingFragmentItems.loadAllEntries(requireContext()); // Load the entries from the database
        setupRecyclerView();
        setupFloatingActionButton();
        setupWorkerObserver(); // To track upload of data online

        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);

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

    private void setupWorkerObserver() {
        WorkManager.getInstance(requireContext())
                .getWorkInfosByTagLiveData("UPLOAD_WORK")
                .observe(getViewLifecycleOwner(), workInfos -> {
                    // Remove text if no workers area active
                    if (workInfos == null || workInfos.isEmpty()) {
                        return;
                    }

                    List<LandingFragmentItems> updatedItems = LandingFragmentItems.loadAllEntries(requireContext());
                    if (entriesAdapter != null) {
                        entriesAdapter.updateData(updatedItems);
                    }

                    int totalWorkers = workInfos.size();
                    int finishedWorkers = 0;
                    int failedWorkers = 0;

                    for (WorkInfo info : workInfos) {
                        if (info.getState().isFinished()) {
                            finishedWorkers++;
                            if (info.getState() == WorkInfo.State.FAILED) {
                                failedWorkers++;
                            }
                        }
                    }
                    int percentage = (finishedWorkers * 100) / totalWorkers;
                    Log.d(TAG, "Progress: " + percentage + "%");

                    if (finishedWorkers < totalWorkers) {
                        showUploadProgress(true, percentage);
                    } else {
                        if (failedWorkers > 0) {
                            Toast.makeText(getContext(), getResources().getQuantityString(
                                    R.plurals.upload_failed_items,
                                    failedWorkers,
                                    failedWorkers
                            ), Toast.LENGTH_LONG).show();
                            showUploadProgress(false, 0);
                        } else {
                            Toast.makeText(getContext(), getString(R.string.all_data_uploaded_successfully), Toast.LENGTH_SHORT).show();
                            showUploadProgress(false, 0);
                        }
                    }
                });
    }

    private void setupFloatingActionButton() {
        // Add the + button for making new records
        FloatingActionButton floatingActionButton = binding.floatButtonNewEntry;
        boolean mail_confirmed = SettingsManager.isMailConfirmed();
        floatingActionButton.setEnabled(mail_confirmed);
        floatingActionButton.setAlpha(mail_confirmed ? 1f : 0.25f);

        floatingActionButton.setOnClickListener(v -> {
            // When user opens the EntryActivity for the first time set this to true.
            // This is used to display intro message for new users.
            if (!SettingsManager.getEntryOpen()) {
                SettingsManager.setEntryOpen(true);
            }
            // Start the new Entry Activity.
            newObservation();
        });

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
        binding.recycledViewEntries.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycledViewEntries.setItemAnimator(new DefaultItemAnimator());
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
                            if (item.getTimedCountId() != null) {
                                openTimedCount(item.getTimedCountId());
                            } else {
                                openObservation(item.getObservationId());
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
                        updateEntry(old_data_id);
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
        LandingFragmentItems newItem = LandingFragmentItems.getItemFromEntry(requireContext(), entryDb);
        int insertionIndex = LandingFragmentItems.findSortedInsertionIndex(
                requireContext(), items, newItem);
        items.add(insertionIndex, newItem);
        entriesAdapter.notifyItemInserted(insertionIndex);
        binding.recycledViewEntries.smoothScrollToPosition(insertionIndex);

        // Update UI element
        ((ActivityLanding) requireActivity()).updateMenuIconVisibility();
    }

    private void addTimedCountItem(Intent data) {
        Integer new_timed_count_id = data.getIntExtra("TIMED_COUNT_ID", 0);
        String time = data.getStringExtra("TIMED_COUNT_START_TIME");
        String day = data.getStringExtra("TIMED_COUNT_DAY");
        String month = data.getStringExtra("TIMED_COUNT_MONTH");
        String year = data.getStringExtra("TIMED_COUNT_YEAR");
        Log.d(TAG, "This was a new Timed Count with ID: " + new_timed_count_id);

        String title = getString(R.string.timed_count);
        String image = "timed_count";
        String subtitle;
        Calendar calendar = DateHelper.getCalendar(year,
                month, day, Objects.requireNonNull(time));
        subtitle = DateHelper.getLocalizedCalendarDate(calendar) + " " +
                getString(R.string.at_time) + " " +
                DateHelper.getLocalizedCalendarTime(calendar);
        LandingFragmentItems item = new LandingFragmentItems(
                null,
                new_timed_count_id,
                false,
                false,
                title,
                subtitle,
                image,
                calendar.getTime());
        addItemToRecyclerView(item);
    }

    private void addItemToRecyclerView(LandingFragmentItems item) {
        // If the user choose to sort by name we should insert the
        // entry in the right place.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String sortBy = prefs.getString("sort_observations", "time");

        if ("name".equals(sortBy)) {
            // Alphabetical insert with binary search
            Comparator<LandingFragmentItems> comparator =
                    (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle());

            int index = Collections.binarySearch(items, item, comparator);
            if (index < 0) index = -index - 1;

            items.add(index, item);
            entriesAdapter.notifyItemInserted(index);
            binding.recycledViewEntries.smoothScrollToPosition(index);
        } else {
            // Time sort → always newest first at top
            items.add(0, item);
            entriesAdapter.notifyItemInserted(0);
            binding.recycledViewEntries.smoothScrollToPosition(0);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();

        if (App.get().getBoxStore().boxFor(EntryDb.class).count()
                != entriesAdapter.getItemCount()) {
            items = LandingFragmentItems.loadAllEntries(context);
            entriesAdapter = new LandingFragmentAdapter(items, false);
            binding.recycledViewEntries.setAdapter(entriesAdapter);
        }
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

    private void updateEntry(long oldDataId) {
        // Load the entry from the ObjectBox
        EntryDb entryDb = ObjectBoxHelper.getObservationById(oldDataId);
        int entry_index = getIndexFromID(oldDataId);

        if (entry_index != -1 && entryDb != null) {
            Log.d(TAG, "Updating existing entry with ID " + oldDataId + " at index " + entry_index);
            items.set(entry_index, LandingFragmentItems.getItemFromEntry(requireContext(), entryDb));
            entriesAdapter.notifyItemChanged(entry_index);
        } else {
            Log.e(TAG, "Cannot update entry with ID " + oldDataId +
                    ". It was not found in the fragment list (index: " + entry_index +
                    ") or database entry is null. Reloading list.");
            reloadRecyclerView();
        }
    }

    // Find the entry’s index by ObservationId
    private int getIndexFromID(long entry_id) {
        for (int i = items.size() - 1; i >= 0; i--) {
            Long entryId = items.get(i).getObservationId();
            if (entryId != null && entryId == entry_id) {
                Log.d(TAG, "Entry " + entry_id + " index ID is " + i);
                return i;
            }
        }
        Log.d(TAG, "Entry " + entry_id + " not found in items.");
        return -1;
    }

    public void duplicateEntry(int position) {
        // TODO handle timed counts
        Log.d(TAG, "You will now duplicate entry ID: " + position);
        EntryDb entry_from = ObjectBoxHelper.getObservationById(items.get(position).getObservationId());

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

        actionMode.finish();
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
                count ++;
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

        if (item.getTimedCountId() != null) {
            int id = item.getTimedCountId();
            ObjectBoxHelper.removeObservationsForTimedCountId(id);
            ObjectBoxHelper.removeTimedCountById(id);
        } else {
            Long id = item.getObservationId();
            ObjectBoxHelper.removeObservationById(id);
        }

        items.remove(currentPosition);
        entriesAdapter.notifyItemRemoved(currentPosition);
    }

    private void sendDeleteRequestToServer(LandingFragmentItems item) {

        Call<Void> call = null;
        if (item.getTimedCountId() != null) {
            TimedCountDb timedCount = ObjectBoxHelper.getTimedCountById(item.getTimedCountId());
            if (timedCount != null) {
                call = RetrofitClient.getService(SettingsManager.getDatabaseName())
                        .deleteTimedCountObservation(timedCount.getServerId());
            }
        } else {
            EntryDb entry = ObjectBoxHelper.getObservationById(item.getObservationId());
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
                        if (item.getTimedCountId() != null) {
                            Log.d(TAG, "Timed Count ID " + item.getTimedCountId() + " deleted from server.");
                        } else {
                            Log.d(TAG, "Observation ID " + item.getObservationId() + " deleted from server.");
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

    protected void buildAlertOnDeleteAll() {
        Activity activity_alert = getActivity();
        if (activity_alert != null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.confirm_delete_all))
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.yes_delete), (dialog, id) -> {

                        Toast.makeText(FragmentLanding.this.getContext(), FragmentLanding.this.getString(R.string.entries_deleted_msg), Toast.LENGTH_SHORT).show();

                        int last_index = items.size() - 1;
                        items.clear();
                        ObjectBoxHelper.removeAllEntries();
                        ((ActivityLanding)getActivity()).updateMenuIconVisibility();
                        Log.i(TAG, "There are " + last_index + " in the RecycleView to be deleted.");

                        Fragment replacementFragment = new FragmentLanding();
                        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.add(R.id.content_frame , replacementFragment);
                        fragmentTransaction.commit();

                    })
                    .setNegativeButton(getString(R.string.no_delete), (dialog, id) -> dialog.cancel());
            final AlertDialog alert = builder.create();

            alert.setOnShowListener(new DialogInterface.OnShowListener() {
                private static final int AUTO_DISMISS_MILLIS = 10000;
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    final Button defaultButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                    defaultButton.setEnabled(false);
                    final CharSequence negativeButtonText = defaultButton.getText();
                    new CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
                        @Override
                        public void onTick(long l) {
                            defaultButton.setText(String.format(
                                    Locale.getDefault(), "%s (%d)",
                                    negativeButtonText,
                                    TimeUnit.MILLISECONDS.toSeconds(l) + 1
                            ));
                        }

                        @Override
                        public void onFinish() {
                            if (alert.isShowing()) {
                                defaultButton.setEnabled(true);
                                defaultButton.setText(negativeButtonText);
                            }
                        }
                    }.start();
                }
            });

            alert.show();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("sort_observations".equals(key)) {
            if (isAdded()) {
                reloadRecyclerView();
            }
        }
    }

    private void reloadRecyclerView() {
        if (!isAdded()) return;

        List<LandingFragmentItems> updatedItems = LandingFragmentItems.loadAllEntries(requireContext());
        if (entriesAdapter == null) {
            entriesAdapter = new LandingFragmentAdapter(new ArrayList<>(updatedItems), false);
            binding.recycledViewEntries.setAdapter(entriesAdapter);
        } else {
            entriesAdapter.updateData(updatedItems);
        }
    }

}
