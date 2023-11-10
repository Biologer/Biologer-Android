package org.biologer.biologer.gui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.adapters.NotificationsAdapter;
import org.biologer.biologer.adapters.RecyclerOnClickListener;
import org.biologer.biologer.sql.UnreadNotificationsDb;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "Biologer.NotifyFragment";
    RecyclerView recyclerView;
    List<UnreadNotificationsDb> notifications;
    NotificationsAdapter notificationsAdapter;
    int current_size;
    int index;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_notifications, container, false);

        notifications = getNotifications();
        recyclerView = rootView.findViewById(R.id.recycled_view_notifications);
        updateRecycleView(notifications);

        return rootView;
    }

    private void updateRecycleView(List<UnreadNotificationsDb> notifications) {
        notificationsAdapter = new NotificationsAdapter(notifications);
        recyclerView.setAdapter(notificationsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addOnItemTouchListener(
                new RecyclerOnClickListener(getContext(), recyclerView, new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        index = position;
                        UnreadNotificationsDb notification = notifications.get(index);
                        long notificationId = notification.getId();
                        Log.d(TAG, "Opening notification with index: " + index + "; ID: " + notificationId + ".");

                        Intent intent = new Intent(getContext(), NotificationActivity.class);
                        intent.putExtra("notification_id", notificationId);
                        startActivity(intent);
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
    public void onResume() {
        super.onResume();
        Log.i(TAG, "Notifications Fragment resumed.");
        notifications = getNotifications();
        updateRecycleView(notifications);
    }
}
