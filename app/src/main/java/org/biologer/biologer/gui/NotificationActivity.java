package org.biologer.biologer.gui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.button.MaterialButton;
import com.ortiz.touchview.TouchImageView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.DateHelper;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.FieldObservationResponse;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {
    private static final String TAG = "Biologer.NotifActivity";
    String downloaded;
    ImageView imageView1, imageView2, imageView3;
    boolean image1, image2, image3;
    FrameLayout frameLayout1,frameLayout2, frameLayout3;
    LinearLayout linearLayoutZoom;
    TouchImageView touchImageView;
    MaterialButton buttonReadAll, buttonReadNext;
    TextView textView, textViewAllRead, textViewDate, textViewLocation, textViewID, textViewProject;
    int indexId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Add a toolbar to the Activity
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(R.string.notification);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }

        // If opening from Activity use index of taped list, else use bundle received from other Fragment
        long notificationId;
        Bundle bundle = getIntent().getExtras();
        notificationId = Objects.requireNonNull(bundle).getLong("notification_id");
        indexId = Objects.requireNonNull(bundle).getInt("index_id");
        Log.d(TAG, "Displaying notification with ID: " + notificationId + ".");

        // Get the notification from its ID
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore()
                .boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> queryNotification = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.equal(notificationId))
                .build();
        UnreadNotificationsDb notification = queryNotification.find().get(0);
        queryNotification.close();

        textView = findViewById(R.id.notification_text);
        textView.setText(Html.fromHtml(getFormattedMessage(notification)));
        textViewID = findViewById(R.id.notification_text_id);
        textViewDate = findViewById(R.id.notification_text_date);
        textViewLocation = findViewById(R.id.notification_text_location);
        textViewProject = findViewById(R.id.notification_text_project);
        textViewAllRead = findViewById(R.id.notification_all_read_text);
        downloaded = "no";

        frameLayout1 = findViewById(R.id.notification_view_imageFrame1);
        imageView1 = findViewById(R.id.notification_view_image1);
        frameLayout2 = findViewById(R.id.notification_view_imageFrame2);
        imageView2 = findViewById(R.id.notification_view_image2);
        frameLayout3 = findViewById(R.id.notification_view_imageFrame3);
        imageView3 = findViewById(R.id.notification_view_image3);
        touchImageView = findViewById(R.id.imageViewNotificationZoom);
        linearLayoutZoom = findViewById(R.id.notification_view_zoomed_image);

        buttonReadAll = findViewById(R.id.notification_view_read_all_button);
        buttonReadAll.setEnabled(true);
        buttonReadAll.setOnClickListener(v -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
        });

        // Check if there is notification with larger ID
        Query<UnreadNotificationsDb> queryLargerId = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.greater(notificationId))
                .build();
        boolean is_last = queryLargerId.find().isEmpty();
        Log.d(TAG, "There are " + queryLargerId.find().size() + " IDs that are larger than the selected one! Reporting " + is_last);
        queryLargerId.close();

        buttonReadNext = findViewById(R.id.notification_view_read_next_button);
        buttonReadNext.setOnClickListener(v -> {
            buttonReadNext.setEnabled(false);
            openNextNotification();
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

        getObservationApi((int) notification.getId(), notification.getRealId(), notification.getFieldObservationId());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.i(TAG, "Back button is pressed in Notification Activity.");
                if (linearLayoutZoom.getVisibility() == View.VISIBLE) {
                    Log.i(TAG, "Image is currently showing in the touch image view.");
                    showUiElements();
                } else {
                    Intent intent = new Intent();
                    intent.putExtra("downloaded", downloaded);
                    intent.putExtra("index_id", indexId);
                    setResult(1, intent);
                    finish();
                }
            }
        });
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
        String localized_date = DateHelper.getLocalizedDate(date, this);
        String localized_time = DateHelper.getLocalizedTime(date, this);

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

    private void getObservationApi(int notificationID, String realNotificationID, int fieldObservationID) {
        // Get the data from Field observation (i.e. images) and display them
        Call<FieldObservationResponse> fieldObservation = RetrofitClient.getService(SettingsManager.getDatabaseName()).getFieldObservation(String.valueOf(fieldObservationID));
        fieldObservation.enqueue(new Callback<FieldObservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        // TODO: Maybe we need to set notification as read only
                        //  1. if there are images and all of them are downloaded or
                        //  2. if there are no images
                        setNotificationAsRead(notificationID, realNotificationID);

                        // Show field observation ID
                        String idText = getString(R.string.observation_id) + " " + fieldObservationID;
                        textViewID.setText(idText);

                        // Add observation date
                        String date = response.body().getData()[0].getDay() + "-" +
                                response.body().getData()[0].getMonth() + "-" +
                                response.body().getData()[0].getYear();
                        Date dateReal = DateHelper.getDate(date);
                        String dateText = getString(R.string.observation_date) + " " +
                                DateHelper.getLocalizedDate(dateReal, NotificationActivity.this);
                        textViewDate.setText(dateText);

                         // Add the place of observation
                         String location = response.body().getData()[0].getLocation();
                         if (location != null) {
                             if (!location.equals("")) {
                                 String locationText = getString(R.string.notification_location) + " " + location;
                                 textViewLocation.setText(locationText);
                             }
                         } else {
                             DecimalFormat f = new DecimalFormat("##.0000");
                             String coordinates = f.format(response.body().getData()[0].getLongitude()) + "° E; " +
                                     f.format(response.body().getData()[0].getLatitude()) + "° N";
                             String coordinatesText = getString(R.string.notification_location) + " " + coordinates;
                             textViewLocation.setText(coordinatesText);
                         }

                         // Get the name of the project if it exist
                        String project = response.body().getData()[0].getProject();
                        if (project != null) {
                            if (!project.equals("")) {
                                String projectText = getString(R.string.notification_project_name) + " " +  project;
                                textViewProject.setText(projectText);
                            }
                        }

                        // Finally get the photos
                        if (!response.body().getData()[0].getPhotos().isEmpty()) {
                            int number_of_photos = response.body().getData()[0].getPhotos().size();
                            for (int i = 0; i < number_of_photos; i++) {
                                String url = response.body().getData()[0].getPhotos().get(i).getUrl();
                                Log.d(TAG, "Loading image " + i + " from: " + url);
                                updatePhoto(url, i);
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
                    downloaded = "yes";
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

    private void updatePhoto(String url, int position) {

        Call<ResponseBody> photoResponse = RetrofitClient.getService(SettingsManager.getDatabaseName()).getPhoto(url);
        photoResponse.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            Log.d(TAG, "Image response obtained.");
                            InputStream inputStream = responseBody.byteStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            Log.d(TAG, bitmap.toString() + "; " + bitmap.getHeight() + "x" + bitmap.getWidth());

                            if (position == 0) {
                                imageView1.setImageBitmap(bitmap);
                                frameLayout1.setVisibility(View.VISIBLE);
                                image1 = true;
                                imageView1.setOnClickListener(view -> {
                                    touchImageView.setImageBitmap(bitmap);
                                    hideUiElements();
                                });
                            }

                            if (position == 1) {
                                imageView2.setImageBitmap(bitmap);
                                frameLayout2.setVisibility(View.VISIBLE);
                                image2 = true;
                                imageView2.setOnClickListener(view -> {
                                    touchImageView.setImageBitmap(bitmap);
                                    hideUiElements();
                                });
                            }

                            if (position == 2) {
                                imageView3.setImageBitmap(bitmap);
                                frameLayout3.setVisibility(View.VISIBLE);
                                image3 = true;
                                imageView3.setOnClickListener(view -> {
                                    touchImageView.setImageBitmap(bitmap);
                                    hideUiElements();
                                });
                            }
                        }
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

    private void hideUiElements() {
        linearLayoutZoom.setVisibility(View.VISIBLE);
        frameLayout1.setVisibility(View.GONE);
        frameLayout2.setVisibility(View.GONE);
        frameLayout3.setVisibility(View.GONE);
        buttonReadAll.setVisibility(View.GONE);
        buttonReadNext.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
    }

    private void showUiElements() {
        linearLayoutZoom.setVisibility(View.GONE);
        if (image1) {frameLayout1.setVisibility(View.VISIBLE);}
        if (image2) {frameLayout2.setVisibility(View.VISIBLE);}
        if (image3) {frameLayout3.setVisibility(View.VISIBLE);}
        buttonReadAll.setVisibility(View.VISIBLE);
        buttonReadNext.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
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
        Log.d(TAG, "Notification " + notification_id + " removed from local database, " + App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).count() + " notifications remain.");
    }

    private void openNextNotification() {
        Intent intent = new Intent();
        intent.putExtra("open_next", true);
        intent.putExtra("downloaded", downloaded);
        intent.putExtra("index_id", indexId);
        setResult(2, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Intent intent = new Intent();
            intent.putExtra("downloaded", downloaded);
            intent.putExtra("index_id", indexId);
            setResult(1, intent);
            this.getOnBackPressedDispatcher().onBackPressed();
        }
        return true;
    }

}
