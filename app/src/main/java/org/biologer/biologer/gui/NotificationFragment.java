package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.EntriesList;
import org.biologer.biologer.bus.DeleteEntryFromList;
import org.biologer.biologer.sql.EntryDb;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.objectbox.Box;

public class NotificationFragment extends Fragment {

    private EntriesList entriesList;
    private ArrayList<EntryDb> entries;
    ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_landing, container, false);
        // Load the entries from the database
        entries = (ArrayList<EntryDb>) ObjectBox.get().boxFor(EntryDb.class).getAll();
        // If there are no entries display the empty list
        if (entries == null) {entries = new ArrayList<>();}
        // If there are entries display the list with taxa
        Activity activity = getActivity();
        if (activity != null) {
            entriesList = new EntriesList(activity.getApplicationContext(), entries);
        }
        listView = rootView.findViewById(R.id.list_entries);
        listView.setAdapter(entriesList);

        listView.setOnItemClickListener((adapterView, view, position, id) -> {
            EntryDb entryDb = entriesList.getItem(position);
            long l = entryDb.getId();
            Activity activity13 = getActivity();
            if (activity13 != null) {
                Intent intent = new Intent(activity13.getApplicationContext(), EntryActivity.class);
                intent.putExtra("IS_NEW_ENTRY", "NO");
                intent.putExtra("ENTRY_ID", l);
                startActivity(intent);
            }
        });

        registerForContextMenu(listView);

        FloatingActionButton floatingActionButton = rootView.findViewById(R.id.fbtn_add);
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
        //EventBus.getDefault().register(this);
    }

    //@Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DeleteEntryFromList deleteEntryFromList) {
        entriesList.addAll(ObjectBox.get().boxFor(EntryDb.class).getAll(), true);
    }

    void updateEntries(ArrayList<EntryDb> entries_list) {
        entries = entries_list;
        if (entries == null) {
            entries = new ArrayList<>();
        }
        entriesList.addAll(entries, true);
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (view.getId()==R.id.list_entries) {
            Activity activity = getActivity();
            if (activity != null ) {
                MenuInflater inflater = activity.getMenuInflater();
                inflater.inflate(R.menu.entry_long_press, menu);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete) {
            // Get the clicked item from the listview
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            int index = 0;
            if (info != null) {
                index = info.position;
            }

            // Delete the entry
            Box<EntryDb> entryBox = ObjectBox.get().boxFor(EntryDb.class);
            entryBox.remove(entriesList.getItem(index).getId());
            entriesList.removeItem(index);
            int entryNo = index + 1;
            Toast.makeText(getContext(), getString(R.string.entry_deleted_msg1) + " " + entryNo + " " + getString(R.string.entry_deleted_msg2), Toast.LENGTH_SHORT).show();
            LandingActivity.setMenuIconVisibility();
            return true;
        }
        if (item.getItemId() == R.id.delete_all) {
            // Delete the entry
            buildAlertOnDeleteAll();
            return true;
        }
        return false;
    }

    @Override
    public void onStop() {
        super.onStop();
        //EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        entries = (ArrayList<EntryDb>) ObjectBox.get().boxFor(EntryDb.class).getAll();
        entriesList.addAll(entries, true);
    }

    protected void buildAlertOnDeleteAll() {
        Activity activity_alert = getActivity();
        if (activity_alert != null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.confirm_delete_all))
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.yes_delete), (dialog, id) -> {
                        Toast.makeText(NotificationFragment.this.getContext(), NotificationFragment.this.getString(R.string.entries_deleted_msg), Toast.LENGTH_SHORT).show();
                        ObjectBox.get().boxFor(EntryDb.class).removeAll();
                        entriesList.removeAll();
                        LandingActivity.setMenuIconVisibility();
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

    private final ActivityResultLauncher<Intent> openEntry = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // If we need to do something with the result after EntryActivity...
            });

}
