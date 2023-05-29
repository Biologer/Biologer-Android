package org.biologer.biologer.gui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.button.MaterialButton;

import org.biologer.biologer.App;
import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.DateHelper;
import org.biologer.biologer.network.JSON.FieldObservationResponse;
import org.biologer.biologer.network.JSON.UnreadNotification;
import org.biologer.biologer.network.JSON.UnreadNotificationsResponse;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.UpdateUnreadNotifications;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;

import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "Biologer.NotificationV";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_view);

        Intent intent = getIntent();
        long notification_id = intent.getIntExtra("id", 0);
        Log.d(TAG, "Taped notification ID: " + notification_id);

        Box<UnreadNotificationsDb> unreadNotificationsDbBox = ObjectBox
                .get().boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.equal(notification_id))
                .build();
        List<UnreadNotificationsDb> unreadNotification = query.find();
        query.close();
        Query<UnreadNotificationsDb> queryLargerId = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.greater(notification_id))
                .build();
        boolean is_last = queryLargerId.find().isEmpty();
        Log.d(TAG, "There are " + queryLargerId.find().size() + " IDs that are larger than the selected one! Reporting " + is_last);
        queryLargerId.close();

        TextView textView = findViewById(R.id.notification_text);
        textView.setText(getFormattedMessage(unreadNotification));

        TextView textViewAllRead = findViewById(R.id.notification_all_read_text);

        MaterialButton buttonReadAll = findViewById(R.id.notification_view_read_all_button);
        buttonReadAll.setEnabled(true);
        buttonReadAll.setOnClickListener(v -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("This action will mark all the notifications as read including the ones on the web. Do you want to continue anyway?")
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
        });

        MaterialButton buttonReadNext = findViewById(R.id.notification_view_read_next_button);
        buttonReadNext.setOnClickListener(v -> {
            buttonReadNext.setEnabled(false);
            if (is_last) {
                getFirstUnreadNotification();
            } else {
                getNextUnreadNotification(notification_id);
            }
        });

        MaterialButton buttonGoBack = findViewById(R.id.notification_view_back_button);
        buttonGoBack.setOnClickListener(v -> {
            Intent landing = new Intent(NotificationActivity.this, LandingActivity.class);
            landing.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(landing);
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
            buttonGoBack.setVisibility(View.VISIBLE);
            textViewAllRead.setVisibility(View.VISIBLE);
        }

        int fieldObservationID = unreadNotification.get(0).getFieldObservationId();
        String realNotificationID = unreadNotification.get(0).getRealId();
        getPhotosApi((int) notification_id, realNotificationID, fieldObservationID);

    }

    private void getPhotosApi(int notificationID, String realNotificationID, int fieldObservationID) {
        // Get the data from Field observation (i.e. images) and display them
        Call<FieldObservationResponse> fieldObservation = RetrofitClient.getService(SettingsManager.getDatabaseName()).getFieldObservation(String.valueOf(fieldObservationID));
        fieldObservation.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        // TODO uncomment this at the end!
                        setNotificationAsRead(notificationID, realNotificationID);

                        if (!response.body().getData()[0].getPhotos().isEmpty()) {
                            int photos = response.body().getData()[0].getPhotos().size();
                            for (int i = 0; i < photos; i++) {
                                String url;
                                if (SettingsManager.getDatabaseName().equals("https://biologer.rs")) {
                                    //url = "https://biologer-rs-photos.eu-central-1.linodeobjects.com/" + response.body().getData()[0].getPhotos().get(i).getPath();
                                    url = response.body().getData()[0].getPhotos().get(i).getUrl();
                                } else {
                                    url = SettingsManager.getDatabaseName() + "/storage/" + response.body().getData()[0].getPhotos().get(i).getPath();
                                }
                                Log.d(TAG, "Loading image from: " + url);
                                if (i == 0) {
                                    ImageView imageView1 = findViewById(R.id.notification_view_image1);
                                    updatePhoto(url, imageView1);
                                    findViewById(R.id.notification_view_imageFrame1).setVisibility(View.VISIBLE);
                                }
                                if (i == 1) {
                                    ImageView imageView2 = findViewById(R.id.notification_view_image2);
                                    updatePhoto(url, imageView2);
                                    findViewById(R.id.notification_view_imageFrame2).setVisibility(View.VISIBLE);
                                } if (i == 2) {
                                    ImageView imageView3 = findViewById(R.id.notification_view_image3);
                                    updatePhoto(url, imageView3);
                                    findViewById(R.id.notification_view_imageFrame3).setVisibility(View.VISIBLE);
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

    private Spanned getFormattedMessage(List<UnreadNotificationsDb> unreadNotifications) {
        Spanned text;
        String taxon = unreadNotifications.get(0).getTaxonName();
        String author = getAuthor(unreadNotifications.get(0));

        String action;
        String action1 = null;
        switch (unreadNotifications.get(0).getType()) {
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

        Date date = DateHelper.getDateFromJSON(unreadNotifications.get(0).getUpdatedAt());
        String localized_date = getLocalizedDate(date);
        String localized_time = getLocalizedTime(date);

        if (action1 == null) {
            text = Html.fromHtml("<b>" + author + "</b> " +
                    action + " <i>" + taxon + "</i> " +
                    getString(R.string.on) + " " +
                    localized_date + " (" + localized_time + ").");
        } else {
            text = Html.fromHtml("<b>" + author + "</b> " +
                    action + " <i>" + taxon + "</i> " +
                    " " + action1 + " " +
                    getString(R.string.on) + " " +
                    localized_date + " (" + localized_time + ").");
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

    private String getLocalizedTime(Date date) {
        DateFormat timeFormatLocalized = android.text.format.DateFormat.getTimeFormat(this);
        String time_string;
        if (date != null) {
            time_string = timeFormatLocalized.format(date);
        } else {
            time_string = getString(R.string.unknown_date);
        }
        return time_string;
    }

    private String getLocalizedDate(Date date) {
        DateFormat dateFormatLocalized = android.text.format.DateFormat.getLongDateFormat(this);
        String date_string;
        if (date != null) {
            date_string = dateFormatLocalized.format(date);
        } else {
            date_string = getString(R.string.unknown_date);
        }
        return date_string;
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

        // Display the notification in the new NotificationView activity
        if (!unreadNotification.isEmpty()) {
            Log.d(TAG, "Opening the next notification.");
            long next_notification_id = unreadNotification.get(0).getId();
            Bundle bundle = new Bundle();
            bundle.putInt("id", (int) next_notification_id);
            Intent notificationIntent = new Intent(this, NotificationActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            notificationIntent.putExtras(bundle);
            startActivity(notificationIntent);
        } else {
            Log.d(TAG, "This is the last notification, you could only start from the first one!");
        }
    }

    @SuppressLint("MissingPermission")
    private void getFirstUnreadNotification() {
        // Get the next unread notification from the ObjectBox
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore().boxFor(UnreadNotificationsDb.class);
        if (!unreadNotificationsDbBox.isEmpty()) {
            UnreadNotificationsDb unreadNotification = unreadNotificationsDbBox.getAll().get(0);
            // Display the notification in the new NotificationView activity
            Log.d(TAG, "Opening the first notification.");
            long first_notification_id = unreadNotification.getId();
            Bundle bundle = new Bundle();
            bundle.putInt("id", (int) first_notification_id);
            Intent notificationIntent = new Intent(this, NotificationActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            notificationIntent.putExtras(bundle);
            startActivity(notificationIntent);
        }
    }

    private void setAllNotificationsAsRead() {
        Call<ResponseBody> notificationRead = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .setAllNotificationAsRead(true);

        notificationRead.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "All notifications should be set to read now.");
                    App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).removeAll();
                    NotificationManagerCompat.from(NotificationActivity.this).cancelAll();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(TAG, "Setting notification as read failed!");
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
        notificationRead.enqueue(new Callback<>() {
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

    private void updateNotificationDatabase(long notification_id) {
        // Remove old notification from the ObjectBox database
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = ObjectBox
                .get().boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.equal(notification_id))
                .build();
        query.remove();
        query.close();
        Log.d(TAG, "Notification " + notification_id + " removed from local database, " + App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).count() + " notifications remain.");

        // Remove notification from the Android notification area
        Log.d(TAG, "Trying to remove notification " + notification_id + " from notification area.");
        NotificationManagerCompat.from(NotificationActivity.this).cancel((int) notification_id);

        // Get new observation from the veb
        Call<UnreadNotificationsResponse> unreadNotificationsResponseCall = RetrofitClient.getService(SettingsManager.getDatabaseName()).getUnreadNotifications();
        unreadNotificationsResponseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UnreadNotificationsResponse> call, @NonNull Response<UnreadNotificationsResponse> response) {
                if (response.isSuccessful()) {

                    if (response.body() != null) {
                        int size = response.body().getMeta().getTotal();
                        Log.d(TAG, "Number of unread notifications: " + size);
                        if (size >= 15) {
                            UnreadNotification unreadNotification = response.body().getData().get(14);
                            UnreadNotificationsDb notificationForSQL = new UnreadNotificationsDb(
                                    0, unreadNotification.getId(),
                                    unreadNotification.getType(),
                                    unreadNotification.getNotifiable_type(),
                                    unreadNotification.getData().getField_observation_id(),
                                    unreadNotification.getData().getCauser_name(),
                                    unreadNotification.getData().getCurator_name(),
                                    unreadNotification.getData().getTaxon_name(),
                                    unreadNotification.getUpdated_at());
                            App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notificationForSQL);

                            // Display new notification in Android notification area
                            //List<UnreadNotificationsDb> unreadNotifications = ObjectBox
                            //        .get().boxFor(UnreadNotificationsDb.class).getAll();
                            //long new_notification_id = unreadNotifications.get(unreadNotifications.size() - 1).getId();
                            //Log.d(TAG, "New notification saved in ObjectBOx as ID " + new_notification_id);

                            // Update the notifications
                            final Intent update_notifications = new Intent(NotificationActivity.this, UpdateUnreadNotifications.class);
                            update_notifications.putExtra("download", false);
                            //update_notifications.putExtra("notification_id", new_notification_id);
                            startService(update_notifications);
                        } else {
                            Log.d(TAG, "There are no more notifications, hurray!.");
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UnreadNotificationsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Application could not get data from a server: " + t.getLocalizedMessage());
            }
        });

    }

    private void updatePhoto(String url, ImageView imageView) {

        Call<ResponseBody> photoResponse = RetrofitClient.getService(SettingsManager.getDatabaseName()).getPhoto(url);
        photoResponse.enqueue(new Callback<>() {
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

}
