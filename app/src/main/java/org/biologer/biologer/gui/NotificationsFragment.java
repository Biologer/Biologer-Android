package org.biologer.biologer.gui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButton;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.DateHelper;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.UpdateUnreadNotifications;
import org.biologer.biologer.network.json.FieldObservationResponse;
import org.biologer.biologer.network.json.UnreadNotification;
import org.biologer.biologer.network.json.UnreadNotificationsResponse;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsFragment extends Fragment {
    private static final String TAG = "Biologer.NotificationsF";
    ImageView imageView1;
    ImageView imageView2;
    ImageView imageView3;
    FrameLayout frameLayout1;
    FrameLayout frameLayout2;
    FrameLayout frameLayout3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_notifications, container, false);

        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.notifications));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> {
            Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        });

        // If opening from Activity use index of taped list, else use bundle received from other Fragment
        long notificationId;
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            notificationId = bundle.getLong("notification_id");
            Log.d(TAG, "Opening notification with index ID: " + notificationId + ".");
        } else {
            int index = 0;
            if (((NotificationsActivity)getActivity()) != null) {
                index = ((NotificationsActivity)getActivity()).index;
            }
            List<UnreadNotificationsDb> notifications = App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).getAll();
            UnreadNotificationsDb notification = notifications.get(index);
            notificationId = notification.getId();
            Log.d(TAG, "Opening notification with index: " + index + "; ID: " + notificationId + ".");
        }

        // Get the notification from its ID
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore()
                .boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> queryNotification = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.equal(notificationId))
                .build();
        UnreadNotificationsDb notification = queryNotification.find().get(0);
        queryNotification.close();

        TextView textView = rootView.findViewById(R.id.notification_text);
        textView.setText(Html.fromHtml(getFormattedMessage(notification)));

        TextView textViewAllRead = rootView.findViewById(R.id.notification_all_read_text);

        frameLayout1 = rootView.findViewById(R.id.notification_view_imageFrame1);
        imageView1 = rootView.findViewById(R.id.notification_view_image1);
        frameLayout2 = rootView.findViewById(R.id.notification_view_imageFrame2);
        imageView2 = rootView.findViewById(R.id.notification_view_image2);
        frameLayout3 = rootView.findViewById(R.id.notification_view_imageFrame3);
        imageView3 = rootView.findViewById(R.id.notification_view_image3);

        MaterialButton buttonReadAll = rootView.findViewById(R.id.notification_view_read_all_button);
        buttonReadAll.setEnabled(true);
        buttonReadAll.setOnClickListener(v -> {
            Activity activity = getActivity();
            if (activity != null) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(getString(R.string.mark_all_notifications_read))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.yes), (dialog, id) -> {
                            buttonReadAll.setEnabled(false);
                            setAllNotificationsAsRead();
                            dialog.dismiss();
                        })
                        .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.dismiss()
                        );
                final AlertDialog alert = builder.create();
                alert.show();
            }
        });

        // Check if there is notification with larger ID
        Query<UnreadNotificationsDb> queryLargerId = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.greater(notificationId))
                .build();
        boolean is_last = queryLargerId.find().isEmpty();
        Log.d(TAG, "There are " + queryLargerId.find().size() + " IDs that are larger than the selected one! Reporting " + is_last);
        queryLargerId.close();

        MaterialButton buttonReadNext = rootView.findViewById(R.id.notification_view_read_next_button);
        buttonReadNext.setOnClickListener(v -> {
            buttonReadNext.setEnabled(false);
            if (is_last) {
                getFirstUnreadNotification();
            } else {
                getNextUnreadNotification(notification.getId());
            }
        });

        if (is_last) {
            Log.d(TAG, "This is the last notification.");
            buttonReadNext.setText(R.string.first_unread_notification);
        }

        if (unreadNotificationsDbBox.count() == 1) {
            Log.d(TAG, "There is only 1 notification, disabling buttons.");
            buttonReadNext.setEnabled(false);
            buttonReadNext.setVisibility(View.GONE);
            buttonReadAll.setEnabled(false);
            buttonReadAll.setVisibility(View.GONE);
            textViewAllRead.setVisibility(View.VISIBLE);
        }

        getPhotosApi((int) notification.getId(), notification.getRealId(), notification.getFieldObservationId());

        return rootView;
    }

    private String getFormattedMessage(UnreadNotificationsDb unreadNotification) {
        String text;
        String taxon = unreadNotification.getTaxonName();
        String author = getAuthor(unreadNotification);

        String action;
        String action1 = null;
        switch (unreadNotification.getType()) {
            case "App\\Notifications\\FieldObservationApproved":
                action = getString(R.string.approved_observation);
                break;
            case "App\\Notifications\\FieldObservationEdited":
                action = getString(R.string.changed_observation);
                break;
            case "App\\Notifications\\FieldObservationMarkedUnidentifiable":
                action = getString(R.string.marked_unidentifiable);
                action1 = getString(R.string.marked_unidentifiable2);
                break;
            default:
                action = getString(R.string.did_something_with_observation);
                break;
        }

        Date date = DateHelper.getDateFromJSON(unreadNotification.getUpdatedAt());
        String localized_date = DateHelper.getLocalizedDate(date, getActivity());
        String localized_time = DateHelper.getLocalizedTime(date, getActivity());

        if (action1 == null) {
            text = "<b>" + author + "</b> " +
                    action + " <i>" + taxon + "</i> " +
                    getString(R.string.on) + " " +
                    localized_date + " (" + localized_time + ").";
        } else {
            text = "<b>" + author + "</b> " +
                    action + " <i>" + taxon + "</i> " +
                    " " + action1 + " " +
                    getString(R.string.on) + " " +
                    localized_date + " (" + localized_time + ").";
        }
        return text;
    }

    private String getAuthor(UnreadNotificationsDb unreadNotificationsDb) {
        String author;
        if (unreadNotificationsDb.getCuratorName() != null) {
            author = unreadNotificationsDb.getCuratorName();
        } else {
            author = unreadNotificationsDb.getCauserName();
        }
        return author;
    }

    private void setAllNotificationsAsRead() {
        Call<ResponseBody> notificationRead = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .setAllNotificationAsRead(true);

        notificationRead.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        Log.d(TAG, "All notifications should be set to read now.");
                        App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).removeAll();
                        NotificationManagerCompat.from(activity).cancelAll();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(TAG, "Setting notification as read failed!");
                t.printStackTrace();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getFirstUnreadNotification() {
        // Get the next unread notification from the ObjectBox
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore().boxFor(UnreadNotificationsDb.class);
        if (!unreadNotificationsDbBox.isEmpty()) {
            UnreadNotificationsDb unreadNotification = unreadNotificationsDbBox.getAll().get(0);
            // Display the notification in the new NotificationView activity
            long first_notification_id = unreadNotification.getId();
            Log.d(TAG, "Opening the first notification with ID " + first_notification_id + ".");
            Fragment fragment = new NotificationsFragment();
            Bundle bundle = new Bundle();
            bundle.putLong("notification_id", first_notification_id);
            fragment.setArguments(bundle);
            Activity activity = getActivity();
            if (activity != null) {
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.notifications_frame, fragment);
                ft.commit();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getNextUnreadNotification(long previous_notification_id) {
        // Get the next unread notification from the ObjectBox
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore().boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.greater(previous_notification_id))
                .build();
        List<UnreadNotificationsDb> unreadNotification = query.find();
        query.close();

        // Display the notification in the new Fragment
        if (!unreadNotification.isEmpty()) {
            long next_notification_id = unreadNotification.get(0).getId();
            Log.d(TAG, "Opening the next notification with ID " + next_notification_id + ".");
            Fragment fragment = new NotificationsFragment();
            Bundle bundle = new Bundle();
            bundle.putLong("notification_id", next_notification_id);
            fragment.setArguments(bundle);
            Activity activity = getActivity();
            if (activity != null) {
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.notifications_frame, fragment);
                ft.commit();
            }
        } else {
            Log.d(TAG, "This is the last notification, you could only start from the first one!");
        }
    }

    private void getPhotosApi(int notificationID, String realNotificationID, int fieldObservationID) {
        // Get the data from Field observation (i.e. images) and display them
        Call<FieldObservationResponse> fieldObservation = RetrofitClient.getService(SettingsManager.getDatabaseName()).getFieldObservation(String.valueOf(fieldObservationID));
        fieldObservation.enqueue(new Callback<FieldObservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        setNotificationAsRead(notificationID, realNotificationID);

                        if (!response.body().getData()[0].getPhotos().isEmpty()) {
                            int photos = response.body().getData()[0].getPhotos().size();
                            for (int i = 0; i < photos; i++) {
                                String url = SettingsManager.getDatabaseName() + "/storage/" + response.body().getData()[0].getPhotos().get(i).getPath();
                                Log.d(TAG, "Loading image from: " + url);
                                if (i == 0) {
                                    updatePhoto(url, imageView1);
                                    frameLayout1.setVisibility(View.VISIBLE);
                                }
                                if (i == 1) {
                                    updatePhoto(url, imageView2);
                                    frameLayout2.setVisibility(View.VISIBLE);
                                } if (i == 2) {
                                    updatePhoto(url, imageView3);
                                    frameLayout3.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "Response body is null!");
                    }

                } else {
                    Log.d(TAG, "The response is not successful.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<FieldObservationResponse> call, @NonNull Throwable t) {
                Log.d(TAG, "Something is wrong!");
                t.printStackTrace();
            }
        });
    }

    private void setNotificationAsRead(int notification_id, String real_notification_id) {
        String[] notification = new String[1];
        notification[0] = real_notification_id;

        Call<ResponseBody> notificationRead = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .setNotificationAsRead(notification);
        notificationRead.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Notification " + real_notification_id + " should be set to read now.");
                    // Remove the notification from ObjectBox
                    // Get new notification from veb and display it
                    updateNotificationDatabase(notification_id);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(TAG, "Setting notification as read failed!");
                t.printStackTrace();
            }
        });

        updateNotificationDatabase(notification_id);
    }

    private void updatePhoto(String url, ImageView imageView) {

        Call<ResponseBody> photoResponse = RetrofitClient.getService(SettingsManager.getDatabaseName()).getPhoto(url);
        photoResponse.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        ResponseBody responseBody = response.body();
                        Log.d(TAG, "Image response obtained.");
                        InputStream inputStream = responseBody.byteStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        Log.d(TAG, bitmap.toString() + "; " + bitmap.getHeight() + "x" + bitmap.getWidth());
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(TAG, "Something is wrong with image response!");
                t.printStackTrace();
            }
        });
    }

    private void updateNotificationDatabase(long notification_id) {
        // Remove old notification from the ObjectBox database
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore()
                .boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.equal(notification_id))
                .build();
        query.remove();
        query.close();
        updateNotificationsListInActivity();

        Log.d(TAG, "Notification " + notification_id + " removed from local database, " + App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).count() + " notifications remain.");
    }

    private void updateNotificationsListInActivity() {
        Activity activity = ((NotificationsActivity)getActivity());
        if (activity != null) {
            ((NotificationsActivity)getActivity()).notifications = App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).getAll();
            ((NotificationsActivity)getActivity()).notificationsAdapter.notifyDataSetChanged();
        }
    }

}
