package org.biologer.biologer.gui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.ortiz.touchview.TouchImageView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.services.DateHelper;
import org.biologer.biologer.services.FileManipulation;
import org.biologer.biologer.services.PreparePhotos;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {
    private static final String TAG = "Biologer.NotyActivity";
    String downloaded;
    ImageView imageView1, imageView2, imageView3;
    boolean image1, image2, image3, image1_ok, image2_ok, image3_ok;
    FrameLayout frameLayout1,frameLayout2, frameLayout3;
    LinearLayout linearLayoutZoom;
    TouchImageView touchImageView;
    MaterialButton buttonReadNext;
    TextView textView, textViewAllRead, textViewDate, textViewLocation, textViewID, textViewProject, textViewFinalTaxon;
    UnreadNotificationsDb notification;
    int indexId;
    long notificationId;

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
        notification = queryNotification.find().get(0);
        queryNotification.close();

        textView = findViewById(R.id.notification_text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView.setText(Html.fromHtml(getFormattedMessage(), Html.FROM_HTML_MODE_LEGACY));
        } else {
            textView.setText(Html.fromHtml(getFormattedMessage()));
        }
        textViewID = findViewById(R.id.notification_text_id);
        textViewFinalTaxon = findViewById(R.id.notification_text_final_taxon);
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
            textViewAllRead.setVisibility(View.VISIBLE);
        }

        getFieldObservationData();
        displayPhotos();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.i(TAG, "Back button is pressed in Notification Activity.");
                if (linearLayoutZoom.getVisibility() == View.VISIBLE) {
                    Log.i(TAG, "Image is currently showing in the touch image view.");
                    showUiElements();
                } else {
                    sendResult(1);
                }
            }
        });
    }

    private String getFormattedMessage() {
        String text;
        String taxon = notification.getTaxonName();
        String author = getAuthor(notification);

        String action;
        String action1 = null;
        switch (notification.getType()) {
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

        Date date = DateHelper.getDateFromJSON(notification.getUpdatedAt());
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

    private void getFieldObservationData() {
        // Show current identification of the taxon online
        String finalTaxon = getString(R.string.observation_taxon) + " <i>" + notification.getFinalTaxonName() + "</i>";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textViewFinalTaxon.setText(Html.fromHtml(finalTaxon, Html.FROM_HTML_MODE_LEGACY));
        } else {
            textViewFinalTaxon.setText(Html.fromHtml(finalTaxon));
        }

        // Show field observation ID
        String idText = getString(R.string.observation_id) + " " + notification.getFieldObservationId();
        textViewID.setText(idText);

        // Add observation date
        String date = notification.getDate();
        Date dateReal = DateHelper.getDate(date);
        String dateText = getString(R.string.observation_date) + " " +
                DateHelper.getLocalizedDate(dateReal, NotificationActivity.this);
        textViewDate.setText(dateText);

        // Add the place of observation
        String location = notification.getLocation();
        String locationText = getString(R.string.notification_location) + " " + location;
        textViewLocation.setText(locationText);

        // Get the name of the project if it exist
        String project = notification.getProject();
        if (project != null) {
            if (!project.isEmpty()) {
                String projectText = getString(R.string.notification_project_name) + " " +  project;
                textViewProject.setText(projectText);
            }
        }
    }

    private void displayPhotos() {

        // Image 1
        if (notification.getImage1() != null) {
            if (notification.getImage1().equals("No photo")) {
                Log.i(TAG, "No photo 1 for this notification.");
                image1_ok = true;
            } else {
                String type = FileManipulation.uriType(notification.getImage1());
                Log.i(TAG, "Image 1 exist, loading " + notification.getImage1() + "; Type: " + type);
                if (type != null) {
                    if (type.equals("file")) {
                        // There is a file already downloaded with an uri to the storage
                        Uri uri = Uri.parse(notification.getImage1());
                        imageView1.setImageURI(uri);
                        frameLayout1.setVisibility(View.VISIBLE);
                        image1 = true;
                        imageView1.setOnClickListener(view -> {
                            File file = FileManipulation.getInternalFileFromUri(this, uri);
                            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                            touchImageView.setImageBitmap(bitmap);
                            hideUiElements();
                        });
                        image1_ok = true;
                    } else {
                        // There is https uri written to the Image 1, download it...
                        downloadPhoto(notification.getImage1(), 0, notification.getRealId());
                    }
                }
            }
        }

        // Image 2
        if (notification.getImage2() != null) {
            if (notification.getImage2().equals("No photo")) {
                Log.i(TAG, "No photo 2 for this notification.");
                image2_ok = true;
            } else {
                String type = FileManipulation.uriType(notification.getImage2());
                Log.i(TAG, "Image 2 exist, loading " + notification.getImage2() + "; Type: " + type);
                if (type != null) {
                    if (type.equals("file")) {
                        // There is a file already downloaded with an uri to the storage
                        Uri uri = Uri.parse(notification.getImage2());
                        imageView2.setImageURI(uri);
                        frameLayout2.setVisibility(View.VISIBLE);
                        image2 = true;
                        imageView2.setOnClickListener(view -> {
                            File file = FileManipulation.getInternalFileFromUri(this, uri);
                            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                            touchImageView.setImageBitmap(bitmap);
                            hideUiElements();
                        });
                        image2_ok = true;
                    } else {
                        // There is https uri written to the Image 1, download it...
                        downloadPhoto(notification.getImage2(), 1, notification.getRealId());
                    }
                }
            }
        }

        // Image 3
        if (notification.getImage3() != null) {
            if (notification.getImage3().equals("No photo")) {
                Log.i(TAG, "No photo 3 for this notification.");
                image3_ok = true;
            } else {
                String type = FileManipulation.uriType(notification.getImage3());
                Log.i(TAG, "Image 3 exist, loading " + notification.getImage3() + "; Type: " + type);
                if (type != null) {
                    if (type.equals("file")) {
                        // There is a file already downloaded with an uri to the storage
                        Uri uri = Uri.parse(notification.getImage3());
                        imageView3.setImageURI(uri);
                        frameLayout3.setVisibility(View.VISIBLE);
                        image3 = true;
                        imageView3.setOnClickListener(view -> {
                            File file = FileManipulation.getInternalFileFromUri(this, uri);
                            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                            touchImageView.setImageBitmap(bitmap);
                            hideUiElements();
                        });
                        image3_ok = true;
                    } else {
                        // There is https uri written to the Image 1, download it...
                        downloadPhoto(notification.getImage3(), 2, notification.getRealId());
                    }
                }
            }
        }
    }

    private void downloadPhoto(String url, int position, String realNotificationID) {

        Call<ResponseBody> photoResponse = RetrofitClient.getService(SettingsManager.getDatabaseName()).getPhoto(url);
        photoResponse.enqueue(new Callback<>() {
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
                                Uri image_uri = PreparePhotos.saveBitmap(NotificationActivity.this, bitmap);
                                updateObjectBox(position, image_uri != null ? image_uri.toString() : null, realNotificationID);
                                imageView1.setOnClickListener(view -> {
                                    touchImageView.setImageBitmap(bitmap);
                                    hideUiElements();
                                });
                                image1_ok = true;
                            }

                            if (position == 1) {
                                imageView2.setImageBitmap(bitmap);
                                frameLayout2.setVisibility(View.VISIBLE);
                                image2 = true;
                                Uri image_uri = PreparePhotos.saveBitmap(NotificationActivity.this, bitmap);
                                updateObjectBox(position, image_uri != null ? image_uri.toString() : null, realNotificationID);
                                imageView2.setOnClickListener(view -> {
                                    touchImageView.setImageBitmap(bitmap);
                                    hideUiElements();
                                });
                                image2_ok = true;
                            }

                            if (position == 2) {
                                imageView3.setImageBitmap(bitmap);
                                frameLayout3.setVisibility(View.VISIBLE);
                                image3 = true;
                                Uri image_uri = PreparePhotos.saveBitmap(NotificationActivity.this, bitmap);
                                updateObjectBox(position, image_uri != null ? image_uri.toString() : null, realNotificationID);
                                imageView3.setOnClickListener(view -> {
                                    touchImageView.setImageBitmap(bitmap);
                                    hideUiElements();
                                });
                                image3_ok = true;
                            }
                        }
                    }
                }
            }

            private void updateObjectBox(int position, String imageUri, String realNotificationID) {
                Box<UnreadNotificationsDb> notificationsDbBox = App.get().getBoxStore().boxFor(UnreadNotificationsDb.class);
                Query<UnreadNotificationsDb> notificationsDbQuery = notificationsDbBox
                        .query(UnreadNotificationsDb_.realId.equal(realNotificationID))
                        .build();
                List<UnreadNotificationsDb> notifications = notificationsDbQuery.find();
                notificationsDbQuery.close();
                if (!notifications.isEmpty()) {
                    for (int i = 0; i < notifications.size() - 1; i++) {
                        UnreadNotificationsDb notification = notifications.get(i);
                        if (position == 0) {
                            notification.setImage1(imageUri);
                        }
                        if (position == 1) {
                            notification.setImage2(imageUri);
                        }
                        if (position == 2) {
                            notification.setImage3(imageUri);
                        }

                        App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notification);
                    }
                } else {
                    Log.e(TAG, "Image URI not saved to ObjectBox.");
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
        buttonReadNext.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
    }

    private void showUiElements() {
        linearLayoutZoom.setVisibility(View.GONE);
        if (image1) {frameLayout1.setVisibility(View.VISIBLE);}
        if (image2) {frameLayout2.setVisibility(View.VISIBLE);}
        if (image3) {frameLayout3.setVisibility(View.VISIBLE);}
        buttonReadNext.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
    }

    private void openNextNotification() {
        sendResult(2);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            sendResult(1);
            this.getOnBackPressedDispatcher().onBackPressed();
        }
        return true;
    }

    private void sendResult(int resultCode) {
        if (image1_ok && image2_ok && image3_ok) {
            downloaded = "yes";
        }
        Intent intent = new Intent();
        intent.putExtra("downloaded", downloaded);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("real_notification_id", notification.getRealId());
        intent.putExtra("index_id", indexId);
        setResult(resultCode, intent);
        finish();
    }

}
