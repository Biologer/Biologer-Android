package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.EntryAdapter;
import org.biologer.biologer.services.RecyclerOnClickListener;
import org.biologer.biologer.network.UploadRecords;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.EntryDb_;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class LandingFragment extends Fragment {

    private ArrayList<EntryDb> entries;
    String TAG = "Biologer.LandingFragment";
    EntryAdapter entriesAdapter;
    RecyclerView recyclerView;
    BroadcastReceiver broadcastReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_landing, container, false);

        // Load the entries from the database
        loadAllEntries();

        // If there are entries display the list with taxa
        recyclerView = rootView.findViewById(R.id.recycled_view_entries);
        entriesAdapter = new EntryAdapter(entries);
        recyclerView.setAdapter(entriesAdapter);
        recyclerView.setClickable(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.addOnItemTouchListener(
                new RecyclerOnClickListener(getActivity(), recyclerView, new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        recyclerView.setClickable(false);
                        EntryDb entryDb = entries.get(position);
                        long l = entryDb.getId();
                        Activity activity = getActivity();
                        if (activity != null) {
                            Intent intent = new Intent(activity.getApplicationContext(), EntryActivity.class);
                            intent.putExtra("IS_NEW_ENTRY", "NO");
                            intent.putExtra("ENTRY_ID", l);
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

        // Add the + button for making new records
        FloatingActionButton floatingActionButton = rootView.findViewById(R.id.float_button_new_entry);
        if (SettingsManager.isMailConfirmed()) {
            floatingActionButton.setEnabled(true);
            floatingActionButton.setAlpha(1f);
        } else {
            floatingActionButton.setEnabled(false);
            floatingActionButton.setAlpha(0.25f);
        }

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

        // Broadcast will watch if upload service is active
        // and run the command when the upload is complete
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(UploadRecords.TASK_COMPLETED);
                long entry_id = intent.getLongExtra("EntryID", 0);

                // This will be executed after upload is completed
                if (s != null) {
                    Log.i(TAG, "Uploading records returned the code: " + s);
                    Activity activity = getActivity();
                    if (activity != null) {
                        TextView textView = getActivity().findViewById(R.id.list_entries_info_text);
                        if (s.equals("success")) {
                            textView.setText(getString(R.string.entry_info_uploaded, SettingsManager.getDatabaseName()));
                            textView.setVisibility(View.VISIBLE);
                            ((LandingActivity) getActivity()).setMenuIconVisibility(false);
                        }
                        if (s.equals("failed_photo")) {
                            textView.setText(R.string.failed_to_upload_photo);
                            textView.setVisibility(View.VISIBLE);
                            ((LandingActivity) getActivity()).setMenuIconVisibility(true);
                        }
                        if (s.equals("failed_entry")) {
                            textView.setText(R.string.failed_to_upload_entry);
                            textView.setVisibility(View.VISIBLE);
                            ((LandingActivity) getActivity()).setMenuIconVisibility(true);
                        }
                        if (s.equals("id_uploaded")) {
                            Log.i(TAG, "The ID: " + entry_id + " is now uploaded, trying to remove it from the fragment.");
                            int index = getIndexFromID(entry_id);
                            entries.remove(index);
                            entriesAdapter.notifyItemRemoved(index);
                        }
                    }

                }
            }
        };

        return rootView;
    }

    private void newObservation() {
        Intent intent = new Intent(getActivity(), EntryActivity.class);
        intent.putExtra("IS_NEW_ENTRY", "YES");
        openEntry.launch(intent);
    }

    private void newTimedCount() {
        Intent intent = new Intent(getActivity(), TimedCountActivity.class);
        intent.putExtra("IS_NEW_ENTRY", "YES");
        openEntry.launch(intent);
    }

    private void loadAllEntries() {
        entries = (ArrayList<EntryDb>) App.get().getBoxStore().boxFor(EntryDb.class).getAll();
        // If there are no entries create an empty list
        if (entries == null) {
            entries = new ArrayList<>();
        } else {
            Collections.reverse(entries);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver((broadcastReceiver),
                    new IntentFilter(UploadRecords.TASK_COMPLETED)
            );
        }

        if (App.get().getBoxStore().boxFor(EntryDb.class).count() != entriesAdapter.getItemCount()) {
            loadAllEntries();
            entriesAdapter = new EntryAdapter(entries);
            recyclerView.setAdapter(entriesAdapter);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d(TAG, "Context item.");

        if (item.getItemId() == R.id.duplicate) {
            duplicateEntry(entriesAdapter.getPosition());
            return true;
        }
        if (item.getItemId() == R.id.delete) {
            deleteEntryAtPosition(entriesAdapter.getPosition());
            return true;
        }
        if (item.getItemId() == R.id.delete_all) {
            // Delete the entry
            buildAlertOnDeleteAll();
            return true;
        }
        return super.onContextItemSelected(item);
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

    private final ActivityResultLauncher<Intent> openEntry = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // If we need to do something with the result after EntryActivity...
                Log.d(TAG, "We got a result from the Entry Activity!");

                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "We got RESULT_OK code from the EntryActivity.");

                    if (result.getData() != null && result.getData().getBooleanExtra("IS_NEW_ENTRY", false)) {
                        long new_entry_id = result.getData().getLongExtra("ENTRY_LIST_ID", 0);
                        Log.d(TAG, "This is a new entry with id: " + new_entry_id);
                        addEntry(new_entry_id);
                    } else {
                        long old_data_id = result.getData().getLongExtra("ENTRY_LIST_ID", 0);
                        Log.d(TAG, "This was an existing entry with id: " + old_data_id);
                        updateEntry(old_data_id);
                    }
                }

                // Change the visibility of the Upload Icon
                Activity activity = getActivity();
                if (activity != null) {
                    ((LandingActivity) getActivity()).updateMenuIconVisibility();
                }

            });

    private void addEntry(long newEntryId) {
        // Load the entry from ObjectBox
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        Query<EntryDb> query = box.query(EntryDb_.id.equal(newEntryId)).build();
        EntryDb entryDb = query.findFirst();
        query.close();

        // Add the entry to the entry list (RecycleView)
        entries.add(0, entryDb);
        entriesAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);

        // Remove the info text that should be displayed if there are no entries
        Activity activity = getActivity();
        if (activity != null) {
            TextView textView = activity.findViewById(R.id.list_entries_info_text);
            textView.setVisibility(View.GONE);
        }
    }

    private void updateEntry(long oldDataId) {
        // Load the entry from the ObjectBox
        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        Query<EntryDb> query = box.query(EntryDb_.id.equal(oldDataId)).build();
        EntryDb entryDb = query.findFirst();
        query.close();

        int entry_id = getIndexFromID(oldDataId);

        // Update the entry to in the entry list (RecycleView)
        entries.set(entry_id, entryDb);
        entriesAdapter.notifyItemChanged(entry_id);
    }

    // Find the entry’s index ID
    private int getIndexFromID(long entry_id) {

        int index_id = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).getId() == entry_id) {
                index_id = i;
            }
        }
        Log.d(TAG, "Entry " + entry_id + " index ID is " + index_id);
        return index_id;
    }

    public void duplicateEntry(int position) {
        Log.d(TAG, "You will now duplicate entry ID: " + position);

        // Create new entry based on current one
        EntryDb entry = new EntryDb(
                0,
                entries.get(position).getTaxonId(),
                entries.get(position).getTaxonSuggestion(),
                entries.get(position).getYear(),
                entries.get(position).getMonth(),
                entries.get(position).getDay(),
                entries.get(position).getComment(),
                null,"", null, null, "true", "",
                entries.get(position).getLattitude(),
                entries.get(position).getLongitude(),
                entries.get(position).getAccuracy(),
                entries.get(position).getElevation(),
                entries.get(position).getLocation(),
                null, null, null,
                entries.get(position).getProjectId(),
                entries.get(position).getFoundOn(),
                entries.get(position).getDataLicence(),
                entries.get(position).getImageLicence(),
                entries.get(position).getTime(),
                entries.get(position).getHabitat(),
                "");
        entries.add(0, entry);

        // Update ObjectBox
        Box<EntryDb> entryBox = App.get().getBoxStore().boxFor(EntryDb.class);
        entryBox.put(entry);
        int index_last = (int) App.get().getBoxStore().boxFor(EntryDb.class).count() - 1;
        long new_entry_id = App.get().getBoxStore().boxFor(EntryDb.class).getAll().get(index_last).getId();
        Log.d(TAG, "Entry will be saved in ObjectBox under ID " + new_entry_id);

        // Update the list in RecycleView
        entriesAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);

        // Open EntryActivity to edit the new record
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent(activity.getApplicationContext(), EntryActivity.class);
            intent.putExtra("IS_NEW_ENTRY", "NO");
            intent.putExtra("ENTRY_ID", new_entry_id);
            openEntry.launch(intent);
        }
    }

    public void deleteEntryAtPosition(int position) {
        Log.d(TAG, "You will now delete entry index ID: " + position);
        long number_in_database = entries.get(position).getId();
        Log.d(TAG, "You will now delete entry ObjectBox ID: " + number_in_database);
        Log.i(TAG, "Position: " + position + "; ObjectBox ID: " + number_in_database + "; Items in list: " + entries.size() + ", in adapter: " + entriesAdapter.getItemCount());
        Box<EntryDb> entryBox = App.get().getBoxStore().boxFor(EntryDb.class);
        entryBox.remove((int) number_in_database);
        entries.remove(position);
        entriesAdapter.notifyItemRemoved(position);
        Log.i(TAG, "Position: " + position + "; ObjectBox ID: " + number_in_database + "; Items in list: " + entries.size() + ", in adapter: " + entriesAdapter.getItemCount());

        // Print user a message
        int entryNo = position + 1;
        Toast.makeText(getContext(), getString(R.string.entry_deleted_msg1) + " " + entryNo + " " + getString(R.string.entry_deleted_msg2), Toast.LENGTH_SHORT).show();
        Activity activity = getActivity();
        if (activity != null) {
            ((LandingActivity)activity).updateMenuIconVisibility();
        }
    }

    protected void buildAlertOnDeleteAll() {
        Activity activity_alert = getActivity();
        if (activity_alert != null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.confirm_delete_all))
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.yes_delete), (dialog, id) -> {

                        Toast.makeText(LandingFragment.this.getContext(), LandingFragment.this.getString(R.string.entries_deleted_msg), Toast.LENGTH_SHORT).show();

                        int last_index = entries.size() - 1;
                        entries.clear();
                        App.get().getBoxStore().boxFor(EntryDb.class).removeAll();
                        ((LandingActivity)getActivity()).updateMenuIconVisibility();
                        Log.i(TAG, "There are " + last_index + " in the RecycleView to be deleted.");

                        Fragment replacementFragment = new LandingFragment();
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

}
