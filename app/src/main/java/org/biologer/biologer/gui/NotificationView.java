package org.biologer.biologer.gui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.JSON.FieldObservationResponse;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        TextView textView = findViewById(R.id.observation_main_text);
        textView.setText(author + " " + action + " " + taxon + ".");

        int fieldObservationID = unreadNotification.get(0).getFieldObservationId();

        Call<FieldObservationResponse> fieldObservation = RetrofitClient.getService(SettingsManager.getDatabaseName()).getFieldObservation(String.valueOf(fieldObservationID));
        fieldObservation.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
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
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "Something is wrong with image response!");
                t.printStackTrace();
            }
        });
    }
}
