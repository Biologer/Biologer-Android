package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

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
    private SwipeMenuListView listView;

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

        SwipeMenuCreator creator = menu -> {
            // create "delete" item
            Activity activity1 = getActivity();
            if (activity1 != null) {
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        activity1.getApplicationContext());
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xFF,
                        0xC2, 0xB3)));
                deleteItem.setWidth(200);
                deleteItem.setIcon(R.drawable.ic_delete);
                menu.addMenuItem(deleteItem);
            }
        };

        // set creator
        listView.setMenuCreator(creator);

        listView.setOnMenuItemClickListener((position, menu, index) -> {
            if (index == 0) {// delete button
                App.get().getDaoSession().getEntryDao().deleteByKey(entriesList.getItem(position).getId());
                entries = (ArrayList<Entry>) App.get().getDaoSession().getEntryDao().loadAll();
                if (entries == null) {
                    entries = new ArrayList<>();
                }
                Activity activity12 = getActivity();
                if (activity12 != null) {
                    entriesList = new EntriesList(activity12.getApplicationContext(), entries);
                    listView.setAdapter(entriesList);
                }
            }
            // false : close the menu; true : not close the menu
            return false;
        });

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

        FloatingActionButton fbtn_add = rootView.findViewById(R.id.fbtn_add);
        fbtn_add.setOnClickListener(v -> {
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

    void updateData() {
        entries = (ArrayList<Entry>) App.get().getDaoSession().getEntryDao().loadAll();
        if (entries == null) {
            entries = new ArrayList<>();
        }
        entriesList.addAll(entries, true);
    }
}
