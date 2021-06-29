package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.adapters.EntriesList;
import org.biologer.biologer.bus.DeleteEntryFromList;
import org.biologer.biologer.sql.Entry;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class LandingFragment extends Fragment {

    private static final int REQ_CODE_NEW_ENTRY = 1001;
    private EntriesList entriesList;
    private ArrayList<Entry> entries;
    ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_landing, container, false);
        // Load the entries from the database
        entries = (ArrayList<Entry>) App.get().getDaoSession().getEntryDao().loadAll();
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
            Entry entry = entriesList.getItem(position);
            long l = entry.getId();
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
        floatingActionButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EntryActivity.class);
            intent.putExtra("IS_NEW_ENTRY", "YES");
            startActivityForResult(intent, REQ_CODE_NEW_ENTRY);
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DeleteEntryFromList deleteEntryFromList) {
        entriesList.addAll(App.get().getDaoSession().getEntryDao().loadAll(), true);
    }

    void updateEntries(ArrayList<Entry> entries_list) {
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
            int index = info.position;

            // Delete the entry
            App.get().getDaoSession().getEntryDao().deleteByKey(entriesList.getItem(index).getId());
            entriesList.removeItem(index);
            int entryNo = index + 1;
            Toast.makeText(getContext(), getString(R.string.entry_deleted_msg1) + " " + entryNo + " " + getString(R.string.entry_deleted_msg2), Toast.LENGTH_SHORT).show();
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
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        entries = (ArrayList<Entry>) App.get().getDaoSession().getEntryDao().loadAll();
        entriesList.addAll(entries, true);
    }

    protected void buildAlertOnDeleteAll() {
        Activity activity_alert = getActivity();
        if (activity_alert != null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.confirm_delete_all))
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.yes_delete), (dialog, id) -> {
                        Toast.makeText(getContext(), getString(R.string.entries_deleted_msg), Toast.LENGTH_SHORT).show();
                        App.get().getDaoSession().getEntryDao().deleteAll();
                        entriesList.removeAll();
                    })
                    .setNegativeButton(getString(R.string.no_delete), (dialog, id) -> dialog.cancel());
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }
}
