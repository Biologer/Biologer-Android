package org.biologer.biologer.gui;

import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.NotificationsAdapter;
import org.biologer.biologer.databinding.ActivityNotificationsBinding;
import org.biologer.biologer.workers.NotificationSyncWorker;
import org.biologer.biologer.services.NotificationsHelper;
import org.biologer.biologer.services.RecyclerOnClickListener;
import org.biologer.biologer.sql.UnreadNotificationsDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ActivityNotifications extends AppCompatActivity {
    private static final String TAG = "Biologer.NotySActivity";
    private ActivityNotificationsBinding binding;
    BroadcastReceiver downloadNotifications;
    List<UnreadNotificationsDb> notifications;
    NotificationsAdapter notificationsAdapter;
    boolean make_selection;
    static Menu notyMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Add a toolbar to the Activity
        addToolbar();

        notifications = getNotifications();
        initiateRecycleView(notifications);

        make_selection = false;

        updateNotifications();

        // If downloading is disables, we should ask user to download notifications
        if (!ActivityLanding.shouldDownload(this)) {
            binding.recyclerViewNotifications.setVisibility(View.GONE);
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.downloading_notifications_disabled)
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.download), (dialog, id) -> updateNotifications())
                    .setNegativeButton(getString(R.string.ignore), (dialog, id) -> {
                        binding.textViewNotifications.setVisibility(View.VISIBLE);
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

    private void addToolbar() {
        setSupportActionBar(binding.toolbar.toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(R.string.notifications);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }
    }

    private void updateNotifications() {
        long timestamp = Long.parseLong(SettingsManager.getNotificationsUpdatedAt());
        NotificationSyncWorker.enqueueNow(getApplicationContext(), timestamp);
    }

    private void selectedNotificationsMenuItemsEnabled(boolean selected) {
        updateMenuItemEnabled(0, selected);
        updateMenuItemEnabled(1, selected);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateNotifications();
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
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getData() != null) {
                        long index = result.getData().getIntExtra("index_id", 0);
                        String downloaded = result.getData().getStringExtra("downloaded");
                        Log.d(TAG, "Notification ID is " + result.getData().getLongExtra("notification_id", 0));
                        String realId = result.getData().getStringExtra("real_notification_id");
                        long notificationId = result.getData().getLongExtra("notification_id", 0);

                        // Remove notification from the Recycler View and mark it as read
                        if (downloaded != null) {
                            Log.i(TAG, "Activity returned result: " + downloaded);
                            if (downloaded.equals("yes")) {
                                Log.i(TAG, "Removing notification no. " + index + " from RecyclerView.");
                                notifications.remove((int) index);
                                notificationsAdapter.notifyItemRemoved((int) index);

                                NotificationsHelper.setOnlineNotificationAsRead(realId);
                                NotificationsHelper.deletePhotosFromNotification(ActivityNotifications.this, notificationId);
                                NotificationsHelper.deleteNotificationFromObjectBox(notificationId);
                            }
                        }

                        // If result code is 2, we should open next notification
                        if (result.getResultCode() == 2) {
                            if (notifications.isEmpty()) {
                                Log.i(TAG, "No more notifications");
                                selectedNotificationsMenuItemsEnabled(false);
                            }
                            if (notifications.size() == index) {
                                openNotification(0);
                            }
                            if (notifications.size() > index) {
                                openNotification((int) index);
                            }
                        }
                    }
                }
            });

    private void initiateRecycleView(List<UnreadNotificationsDb> notifications) {
        notificationsAdapter = new NotificationsAdapter(this, notifications);
        binding.recyclerViewNotifications.setAdapter(notificationsAdapter);
        binding.recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewNotifications.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerViewNotifications.setClickable(true);
        binding.recyclerViewNotifications.addOnItemTouchListener(
                new RecyclerOnClickListener(this,
                        binding.recyclerViewNotifications,
                        new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        // Select the notifications
                        if (make_selection) {
                            selectDeselect(position);
                            notificationsAdapter.notifyItemChanged(position);
                        }
                        // Or open it in normal way...
                        else {
                            binding.recyclerViewNotifications.setClickable(false);
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
        Log.d(TAG, "Opening notification with index: " + index + "; ID: " + notification.getId() + "; Real ID: " + notification.getRealId());

        Intent intent = new Intent(ActivityNotifications.this, ActivityNotification.class);
        intent.putExtra("real_notification_id", notification.getRealId());
        intent.putExtra("from_recycler_view", true);
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
                NotificationsHelper.deletePhotosFromNotification(ActivityNotifications.this, notifications.get(i).getId());
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
