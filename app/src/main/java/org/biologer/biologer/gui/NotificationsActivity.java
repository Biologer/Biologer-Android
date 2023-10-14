package org.biologer.biologer.gui;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.adapters.NotificationsAdapter;
import org.biologer.biologer.adapters.RecyclerOnClickListener;
import org.biologer.biologer.sql.UnreadNotificationsDb;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "Biologer.Notifications";

    RecyclerView recyclerView;
    List<UnreadNotificationsDb> notifications;
    NotificationsAdapter notificationsAdapter;
    int current_size;
    int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(R.string.notifications);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }

        notifications = getNotifications();

        recyclerView = findViewById(R.id.recycled_view_notifications);
        updateRecycleView(notifications);
    }

    private void updateRecycleView(List<UnreadNotificationsDb> notifications) {
        notificationsAdapter = new NotificationsAdapter(notifications);
        recyclerView.setAdapter(notificationsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnItemTouchListener(
                new RecyclerOnClickListener(this, recyclerView, new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        index = position;
                        Log.d(TAG, "Notification item " + position + " clicked");
                        Fragment fragment = new NotificationsFragment();
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.add(R.id.notifications_frame, fragment);
                        ft.addToBackStack("Notification fragment");
                        ft.commit();
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Log.d(TAG, "Notification item " + position + " long pressed");
                    }
                }));
    }

    private List<UnreadNotificationsDb> getNotifications() {
        List<UnreadNotificationsDb> notifications = App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).getAll();
        if (notifications == null) {
            notifications = new ArrayList<>();
            current_size = 0;
        } else {
            current_size = notifications.size();
        }
        return notifications;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return true;
    }

}
