package org.biologer.biologer.gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.adapters.NotificationsAdapter;
import org.biologer.biologer.adapters.NotificationsHelper;
import org.biologer.biologer.adapters.RecyclerOnClickListener;
import org.biologer.biologer.network.UpdateUnreadNotifications;
import org.biologer.biologer.sql.UnreadNotificationsDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class NotificationsActivity extends AppCompatActivity {
    private static final String TAG = "Biologer.NotySActivity";
    RecyclerView recyclerView;
    BroadcastReceiver downloadNotifications;
    List<UnreadNotificationsDb> notifications;
    NotificationsAdapter notificationsAdapter;
    boolean make_selection;
    static Menu notyMenu;

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

        make_selection = false;

        registerDownloadNotificationsReceiver();

        // If downloading is disables, we should ask user to download notifications
        if (!LandingActivity.shouldDownload(this)) {
            TextView textView = findViewById(R.id.recycled_view_notifications_text);
            recyclerView.setVisibility(View.GONE);
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.downloading_notifications_disabled)
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.download), (dialog, id) -> {
                        final Intent update_notifications = new Intent(this, UpdateUnreadNotifications.class);
                        update_notifications.putExtra("download", true);
                        startService(update_notifications);
                    })
                    .setNegativeButton(getString(R.string.ignore), (dialog, id) -> {
                        textView.setVisibility(View.VISIBLE);
                        dialog.cancel();
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (make_selection) {
                    make_selection = false;
                    deselectAllNotifications();
                } else {
                    finish();
                }
            }
        });

    }

    private void registerDownloadNotificationsReceiver() {
        downloadNotifications = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String received = intent.getStringExtra(UpdateUnreadNotifications.NOTIFICATIONS_DOWNLOADED);
                if (received != null) {
                    if (received.equals("downloaded")) {
                        Toast.makeText(NotificationsActivity.this, getString(R.string.notifications_downloaded), Toast.LENGTH_SHORT).show();
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                }
            }
        };
    }

    private void selectedNotificationsMenuItemsEnabled(boolean selected) {
        updateMenuItemEnabled(0, selected);
        updateMenuItemEnabled(1, selected);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((downloadNotifications),
                new IntentFilter(UpdateUnreadNotifications.NOTIFICATIONS_DOWNLOADED)
        );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadNotifications);
        super.onStop();
    }

    // Update visibility by menu index
    // 0 = Mark read menu
    // 1 = Clear selection menu
    // 2 = Clear all notifications
    private void updateMenuItemEnabled(int index, boolean active) {
        if (active) {
            getMenu().getItem(index).setEnabled(true);
            Objects.requireNonNull(getMenu().getItem(index).getIcon()).setAlpha(255);
            changeMenuItemColor(getMenu().getItem(index), true);
        } else {
            getMenu().getItem(index).setEnabled(false);
            Objects.requireNonNull(getMenu().getItem(index).getIcon()).setAlpha(100);
            changeMenuItemColor(getMenu().getItem(index), false);
        }
    }

    private void changeMenuItemColor(MenuItem item, boolean enabled) {
        SpannableString s = new SpannableString(item.getTitle());
        if (enabled) {
            s.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s.length(), 0);
        } else {
            s.setSpan(new ForegroundColorSpan(Color.GRAY), 0, s.length(), 0);
        }
        item.setTitle(s);
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
                                selectedNotificationsMenuItemsEnabled(false);
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
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setClickable(true);
        recyclerView.addOnItemTouchListener(
                new RecyclerOnClickListener(this, recyclerView, new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        // Select the notifications
                        if (make_selection) {
                            selectDeselect(position);
                            notificationsAdapter.notifyItemChanged(position);
                        }
                        // Or open it in normal way...
                        else {
                            recyclerView.setClickable(false);
                            openNotification(position);
                        }
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        make_selection = true;
                        selectedNotificationsMenuItemsEnabled(true);
                        selectDeselect(position);
                        notificationsAdapter.notifyItemChanged(position);
                    }
                }));
    }

    private void selectDeselect(int position) {
        if (notifications.get(position).getMarked() == 0) {
            Log.d(TAG, "Notification " + position + " not selected. Selecting now.");
            notifications.get(position).setMarked(1);
        } else {
            Log.d(TAG, "Notification " + position + " already selected. Deselecting.");
            notifications.get(position).setMarked(0);
            // If some notification is selected continue selection,...
            int selected = 0;
            for (int i = 0; i < notifications.size(); i++) {
                if (notifications.get(i).getMarked() == 1) {
                    selected++;
                }
            }
            Log.d(TAG, "There are " + selected + " notifications already selected.");
            if (selected == 0) {
                make_selection = false;
                selectedNotificationsMenuItemsEnabled(false);
            }
        }
    }

    private void deselectAllNotifications() {
        selectedNotificationsMenuItemsEnabled(false);
        for (int i = 0; i < notifications.size(); i++) {
            if (notifications.get(i).getMarked() == 1) {
                notifications.get(i).setMarked(0);
                notificationsAdapter.notifyItemChanged(i);
            }
        }
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
        }
        return notifications;
    }

    // Add Save button in the right part of the toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notifications_menu, menu);
        notyMenu = menu;
        selectedNotificationsMenuItemsEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }

    private static Menu getMenu() {
        return notyMenu;
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
        if (id == R.id.noty_read) {
            deleteSelectedNotifications();
        }
        if (id == R.id.noty_deselect) {
            deselectAllNotifications();
        }
        return true;
    }

    private void deleteSelectedNotifications() {
        make_selection = false;
        selectedNotificationsMenuItemsEnabled(false);
        List<Integer> remove_indexes = new ArrayList<>();
        List<UnreadNotificationsDb> remove_list = new ArrayList<>();
        for (int i = 0; i < notifications.size(); i++) {
            if (notifications.get(i).getMarked() == 1) {
                Log.d(TAG, "Removing notification using item index " + i + ".");
                NotificationsHelper.setOnlineNotificationAsRead(notifications.get(i).getRealId());
                NotificationsHelper.deleteNotificationPhotos(NotificationsActivity.this, notifications.get(i).getId());
                NotificationsHelper.deleteNotificationFromObjectBox(notifications.get(i).getId());
                remove_indexes.add(i);
                remove_list.add(notifications.get(i));
            }
        }
        Log.d(TAG, "There are " + remove_indexes.size() + " entries to remove.");

        // Finally remove notification from RecycleView
        notifications.removeAll(remove_list);
        Collections.sort(remove_indexes);
        Log.d(TAG, "Removing notifications from index " + Collections.min(remove_indexes) + " to " + Collections.max(remove_indexes));
        Log.d(TAG, "Removing notifications from index " + remove_indexes.get(0) + " to " + remove_indexes.get(remove_indexes.size() - 1));
        for (int i = remove_indexes.size() - 1; i >=0; i--) {
            Log.d(TAG, "Removing notification " + remove_indexes.get(i) + " from adapter.");
            notificationsAdapter.notifyItemRemoved(remove_indexes.get(i));
        }
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
                        selectedNotificationsMenuItemsEnabled(false);

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
