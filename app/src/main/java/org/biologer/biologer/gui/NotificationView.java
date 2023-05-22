package org.biologer.biologer.gui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.button.MaterialButton;

import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.JSON.FieldObservationResponse;
import org.biologer.biologer.network.JSON.UnreadNotification;
import org.biologer.biologer.network.JSON.UnreadNotificationsResponse;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.UpdateUnreadNotifications;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;

public class NotificationView extends AppCompatActivity {

    private static final String TAG = "Biologer.Observation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_view);

        Intent intent = getIntent();

        long notification_id = intent.getIntExtra("id", 0);

        Log.d(TAG, "Taped notification ID: " + notification_id);

        Box<UnreadNotificationsDb> unreadNotificationsDbBox = ObjectBox.get().boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.equal(notification_id))
                .build();
        List<UnreadNotificationsDb> unreadNotification = query.find();
        query.close();

        String taxon = unreadNotification.get(0).getTaxonName();
        String author;
        if (unreadNotification.get(0).getCuratorName() != null) {
            author = unreadNotification.get(0).getCuratorName();
        } else {
            author = unreadNotification.get(0).getCauserName();
        }

        String action;
        if (unreadNotification.get(0).getType().equals("App\\Notifications\\FieldObservationApproved")) {
            action = getString(R.string.approved_observation);
        } else if (unreadNotification.get(0).getType().equals("App\\Notifications\\FieldObservationEdited")) {
            action = getString(R.string.changed_observation);
        } else {
            action = getString(R.string.did_something_with_observation);
        }


        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
        String originalDate = unreadNotification.get(0).getUpdatedAt();
        Date date = null;
        try {
            date = dateFormat.parse(originalDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        DateFormat dateFormatLocalized = android.text.format.DateFormat.getLongDateFormat(this);
        DateFormat timeFormatLocalized = android.text.format.DateFormat.getTimeFormat(this);
        String date_string = null;
        if (date != null) {
            date_string = dateFormatLocalized.format(date);
        } else {
            date_string = getString(R.string.unknown_date);
        }
        String time_string = null;
        if (date != null) {
            time_string = timeFormatLocalized.format(date);
        } else {
            time_string = getString(R.string.unknown_date);
        }

        TextView textView = findViewById(R.id.observation_main_text);
        textView.setText(Html.fromHtml("<b>" + author + "</b> " + action + "<i>" + taxon + "</i> " + getString(R.string.on) + " " + date_string + " (" + time_string + ")."));

        MaterialButton buttonReadAll = findViewById(R.id.notification_view_read_all_button);
        buttonReadAll.setOnClickListener(v -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("This action will mark all the notifications as read including the ones on the web. Do you want to continue anyway?")
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.yes), (dialog, id) -> {
                        buttonReadAll.setEnabled(false);
                        setAllNotificationsAsRead();
                        dialog.dismiss();
                    })
                    .setNegativeButton(getString(R.string.no), (dialog, id) -> {
                        dialog.dismiss();
                    }
        );
            final AlertDialog alert = builder.create();
            alert.show();
        });

        int fieldObservationID = unreadNotification.get(0).getFieldObservationId();
        String realNotificationID = unreadNotification.get(0).getRealId();

        Call<FieldObservationResponse> fieldObservation = RetrofitClient.getService(SettingsManager.getDatabaseName()).getFieldObservation(String.valueOf(fieldObservationID));
        fieldObservation.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        setNotificationAsRead(realNotificationID, (int) notification_id);

                        if (!response.body().getData()[0].getPhotos().isEmpty()) {
                            int photos = response.body().getData()[0].getPhotos().size();
                            for (int i = 0; i < photos; i++) {
                                String url;
                                if (SettingsManager.getDatabaseName().equals("https://biologer.rs")) {
                                    url = "https://biologer-rs-photos.eu-central-1.linodeobjects.com/" + response.body().getData()[0].getPhotos().get(i).getPath();
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

                    // TODO Update notifications online!

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

    private void setAllNotificationsAsRead() {
        Call<ResponseBody> notificationRead = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .setAllNotificationAsRead(true);

        notificationRead.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "All notifications should be set to read now.");
                    ObjectBox.get().boxFor(UnreadNotificationsDb.class).removeAll();

                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(TAG, "Setting notification as read failed!");
                t.printStackTrace();
            }
        });

    }

    private void setNotificationAsRead(String notification_id, int system_notification_id) {
        String[] notification = new String[1];
        notification[0] = notification_id;

        Call<ResponseBody> notificationRead = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .setNotificationAsRead(notification);
        notificationRead.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Notification " + notification_id + " should be set to read now.");
                    updateNotificationDatabase(system_notification_id);
                    // TODO This didn’t work :(
                    //NotificationManagerCompat.from(NotificationView.this).cancelAll();
                    // TODO Try with this...
                    final Intent update_notifications = new Intent(NotificationView.this, UpdateUnreadNotifications.class);
                    update_notifications.putExtra("download", false);
                    startService(update_notifications);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(TAG, "Setting notification as read failed!");
                t.printStackTrace();
            }
        });

    }

    private void updateNotificationDatabase(long notificationId) {
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = ObjectBox.get().boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.equal(notificationId))
                .build();
        query.remove();
        query.close();

        Log.d(TAG, "Notification " + notificationId + " removed from local database, a total of " + ObjectBox.get().boxFor(UnreadNotificationsDb.class).count() + " notifications remains.");

        getNewUnreadNotification();

    }

    private void getNewUnreadNotification() {
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
                            ObjectBox.get().boxFor(UnreadNotificationsDb.class).put(notificationForSQL);

                            // Update the notifications
                            final Intent update_notifications = new Intent(NotificationView.this, UpdateUnreadNotifications.class);
                            update_notifications.putExtra("download", false);
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
