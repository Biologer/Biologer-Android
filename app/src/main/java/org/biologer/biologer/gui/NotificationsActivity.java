package org.biologer.biologer.gui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.adapters.NotificationsAdapter;
import org.biologer.biologer.adapters.NotificationsHelper;
import org.biologer.biologer.adapters.RecyclerOnClickListener;
import org.biologer.biologer.sql.UnreadNotificationsDb;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationsActivity extends AppCompatActivity {
    private static final String TAG = "Biologer.NotySActivity";

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
                        String realId = result.getData().getStringExtra("real_notification_id");
                        long notificationId = result.getData().getLongExtra("notification_id", 0);

                        // Remove notification from the Recycler View and mark it as read
                        if (downloaded != null) {
                            Log.i(TAG, "Activity returned result: " + downloaded);
                            if (downloaded.equals("yes")) {
                                Log.i(TAG, "Removing notification no. " + index + " from RecyclerView.");
                                notifications.remove( (int) index);
                                notificationsAdapter.notifyItemRemoved((int) index);
                                NotificationsHelper.setOnlineNotificationAsRead(realId);
                                if (notificationId != 0) {
                                    NotificationsHelper.deleteNotificationPhotos(NotificationsActivity.this, notificationId);
                                    NotificationsHelper.deleteNotificationFromObjectBox(notificationId);
                                }
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

    // Add Save button in the right part of the toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notifications_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.getOnBackPressedDispatcher().onBackPressed();
        }
        if (id == R.id.noty_all_read) {
            buildAlertOnReadAll();
        }
        return true;
    }

    protected void buildAlertOnReadAll() {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.confirm_read_all))
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.yes_read_add), (dialog, id) -> {

                        Toast.makeText(this, getString(R.string.all_notifications_are_read), Toast.LENGTH_SHORT).show();
                        NotificationsHelper.setAllOnlineNotificationsAsRead(this); // Remove notifications online
                        int size = notifications.size();
                        notifications.clear(); // Remove notifications from the list
                        notificationsAdapter.notifyItemRangeRemoved(0, size); // Remove notifications from RecycleView

                    })
                    .setNegativeButton(getString(R.string.no_delete), (dialog, id) -> dialog.cancel());
            final AlertDialog alert = builder.create();

            alert.setOnShowListener(new DialogInterface.OnShowListener() {
                private static final int AUTO_DISMISS_MILLIS = 4000;
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
