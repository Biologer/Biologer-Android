package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.EntryAdapter;
import org.biologer.biologer.adapters.RecyclerOnClickListener;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_landing, container, false);

        // Load the entries from the database
        entries = (ArrayList<EntryDb>) ObjectBox.get().boxFor(EntryDb.class).getAll();
        Collections.reverse(entries);
        // If there are no entries create an empty list
        if (entries == null) {
            entries = new ArrayList<>();
        }

        // If there are entries display the list with taxa
        recyclerView = rootView.findViewById(R.id.recycled_view_entries);
        entriesAdapter = new EntryAdapter(entries);
        recyclerView.setAdapter(entriesAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.addOnItemTouchListener(
                new RecyclerOnClickListener(getActivity(), recyclerView, new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        EntryDb entryDb = entries.get(position);
                        long l = entryDb.getId();
                        Activity activity13 = getActivity();
                        if (activity13 != null) {
                            Intent intent = new Intent(activity13.getApplicationContext(), EntryActivity.class);
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
            Intent intent = new Intent(getActivity(), EntryActivity.class);
            intent.putExtra("IS_NEW_ENTRY", "YES");
            openEntry.launch(intent);
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d(TAG, "Context item.");

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
                    ((LandingActivity) getActivity()).setMenuIconVisibility();
                }

            });

    private void addEntry(long newEntryId) {
        // Load the entry from ObjectBox
        Box<EntryDb> box = ObjectBox.get().boxFor(EntryDb.class);
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
        Box<EntryDb> box = ObjectBox.get().boxFor(EntryDb.class);
        Query<EntryDb> query = box.query(EntryDb_.id.equal(oldDataId)).build();
        EntryDb entryDb = query.findFirst();
        query.close();

        // Find the entry’s index ID
        int entry_id = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId() == oldDataId) {
                entry_id = i;
            }
        }
        Log.d(TAG, "Entry index ID is " + entry_id);

        // Add the entry to the entry list (RecycleView)
        entries.set(entry_id, entryDb);
        Log.i(TAG, "Images in entry " + entry_id + ": " +
                entries.get(entry_id).getSlika1() + ", " +
                entries.get(entry_id).getSlika2() + ", " +
                entries.get(entry_id).getSlika3() + ".");
        entriesAdapter.notifyItemChanged(entry_id);
    }

    private void deleteEntryAtPosition(int position) {
        Log.d(TAG, "You will now delete entry index ID: " + position);
        long number_in_objectbox = entries.get(position).getId();
        Log.d(TAG, "You will now delete entry ObjectBox ID: " + number_in_objectbox);
        Log.i(TAG, "Position: " + position + "; ObjectBox ID: " + number_in_objectbox + "; Items in list: " + entries.size() + ", in adapter: " + entriesAdapter.getItemCount());
        Box<EntryDb> entryBox = ObjectBox.get().boxFor(EntryDb.class);
        entryBox.remove((int) number_in_objectbox);
        entries.remove(position);
        entriesAdapter.notifyItemRemoved(position);
        Log.i(TAG, "Position: " + position + "; ObjectBox ID: " + number_in_objectbox + "; Items in list: " + entries.size() + ", in adapter: " + entriesAdapter.getItemCount());

        // Print user a message
        int entryNo = position + 1;
        Toast.makeText(getContext(), getString(R.string.entry_deleted_msg1) + " " + entryNo + " " + getString(R.string.entry_deleted_msg2), Toast.LENGTH_SHORT).show();
        Activity activity = getActivity();
        if (activity != null) {
            ((LandingActivity)activity).setMenuIconVisibility();
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
                        ObjectBox.get().boxFor(EntryDb.class).removeAll();
                        ((LandingActivity)getActivity()).setMenuIconVisibility();
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
