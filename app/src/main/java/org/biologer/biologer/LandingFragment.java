package org.biologer.biologer;

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
import android.widget.AdapterView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import org.biologer.biologer.adapters.Adapter;
import org.biologer.biologer.bus.DeleteEntryFromList;
import org.biologer.biologer.model.Entry;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class LandingFragment extends Fragment {

    private static final int REQ_CODE_NEW_ENTRY = 1001;
    private Adapter adapter;
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
            adapter = new Adapter(activity.getApplicationContext(), entries);
        }
        listView = rootView.findViewById(R.id.list_entries);
        listView.setAdapter(adapter);

        SwipeMenuCreator creator = new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                // create "delete" item
                Activity activity = getActivity();
                if (activity != null) {
                    SwipeMenuItem deleteItem = new SwipeMenuItem(
                            activity.getApplicationContext());
                    deleteItem.setBackground(new ColorDrawable(Color.rgb(0xFF,
                            0xC2, 0xB3)));
                    deleteItem.setWidth(200);
                    deleteItem.setIcon(R.drawable.ic_delete);
                    menu.addMenuItem(deleteItem);
                }
            }
        };

        // set creator
        listView.setMenuCreator(creator);

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                if (index == 0) {// delete button
                    App.get().getDaoSession().getEntryDao().deleteByKey(adapter.getItem(position).getId());
                    entries = (ArrayList<Entry>) App.get().getDaoSession().getEntryDao().loadAll();
                    if (entries == null) {
                        entries = new ArrayList<>();
                    }
                    Activity activity = getActivity();
                    if (activity != null) {
                        adapter = new Adapter(activity.getApplicationContext(), entries);
                        listView.setAdapter(adapter);
                    }
                }
                // false : close the menu; true : not close the menu
                return false;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Entry entry = adapter.getItem(position);
                long l = entry.getId();
                Activity activity = getActivity();
                if (activity != null) {
                    Intent intent = new Intent(activity.getApplicationContext(), EntryActivity.class);
                    intent.putExtra("IS_NEW_ENTRY", "NO");
                    intent.putExtra("ENTRY_ID", l);
                    startActivity(intent);
                }
            }
        });

        FloatingActionButton fbtn_add = rootView.findViewById(R.id.fbtn_add);
        fbtn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), EntryActivity.class);
                intent.putExtra("IS_NEW_ENTRY", "YES");
                startActivityForResult(intent, REQ_CODE_NEW_ENTRY);
            }
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
        adapter.addAll(App.get().getDaoSession().getEntryDao().loadAll(), true);
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
        adapter.addAll(entries, true);
    }

    void updateData() {
        entries = (ArrayList<Entry>) App.get().getDaoSession().getEntryDao().loadAll();
        if (entries == null) {
            entries = new ArrayList<>();
        }
        adapter.addAll(entries, true);
    }
}
