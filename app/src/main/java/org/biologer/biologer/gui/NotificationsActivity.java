package org.biologer.biologer.gui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
    private static final String TAG = "Biologer.NotifsActivity";

    RecyclerView recyclerView;
    List<UnreadNotificationsDb> notifications;
    NotificationsAdapter notificationsAdapter;
    int current_size;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Add a toolbar to the Activity
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
        initiateRecycleView(notifications);

    }

    private final ActivityResultLauncher<Intent> notificationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                    public void onActivityResult(ActivityResult result) {
                    if (result.getData() != null) {
                        long index = result.getData().getIntExtra("index_id", 0);
                        String downloaded = result.getData().getStringExtra("downloaded");

                        // Remove notification from the Recycler View
                        if (downloaded != null) {
                            Log.i(TAG, "Activity returned result: " + downloaded);
                            if (downloaded.equals("yes")) {
                                    Log.i(TAG, "Removing notification no. " + index + " from RecyclerView.");
                                    notifications.remove( (int) index);
                                    notificationsAdapter.notifyItemRemoved((int) index);
                            }
                        }

                        // If result code is 2, we should open next notification
                        if (result.getResultCode() == 2) {
                            if (notifications.isEmpty()) {
                                Log.i(TAG, "No more notifications");
                            } if (notifications.size() == index) {
                                openNotification(0);
                            } if (notifications.size() > index) {
                                openNotification((int) index);
                            }
                        }
                    }
                }
            });

    private void initiateRecycleView(List<UnreadNotificationsDb> notifications) {
        notificationsAdapter = new NotificationsAdapter(notifications);
        recyclerView.setAdapter(notificationsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setClickable(true);
        recyclerView.addOnItemTouchListener(
                new RecyclerOnClickListener(this, recyclerView, new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        recyclerView.setClickable(false);
                        openNotification(position);
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Log.d(TAG, "Notification item " + position + " long pressed");
                    }
                }));
    }

    private void openNotification(int index) {
        UnreadNotificationsDb notification = notifications.get(index);
        long notificationId = notification.getId();
        Log.d(TAG, "Opening notification with index: " + index + "; ID: " + notificationId + "; Real ID: " + notification.getRealId());

        Intent intent = new Intent(NotificationsActivity.this, NotificationActivity.class);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("index_id", index);
        notificationLauncher.launch(intent);
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
            this.getOnBackPressedDispatcher().onBackPressed();
        }
        return true;
    }

}
