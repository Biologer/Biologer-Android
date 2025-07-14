package org.biologer.biologer.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.FieldObservationData;
import org.biologer.biologer.network.json.FieldObservationDataPhotos;
import org.biologer.biologer.network.json.FieldObservationResponse;
import org.biologer.biologer.services.DateHelper;
import org.biologer.biologer.services.FileManipulation;
import org.biologer.biologer.services.PreparePhotos;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsAdapter
        extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
    private final List<UnreadNotificationsDb> notifications;
    private static final String TAG = "Biologer.NotifyAdapter";
    Context context;

    public NotificationsAdapter(List<UnreadNotificationsDb> notification) {
        this.notifications = notification;
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        public TextView textNotification;
        public ImageView observationPhoto;

        public ViewHolder(View view) {
            super(view);

            // Define click listener for the ViewHolder's View
            textNotification = view.findViewById(R.id.notification_list_text);
            observationPhoto = view.findViewById(R.id.image_view_notification);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.notifications_list, viewGroup, false);

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Log.d(TAG, "Binding to the notifications adapter now!");

        UnreadNotificationsDb notification = notifications.get(position);

        TextView textNotifications = viewHolder.textNotification;
        textNotifications.setText(Html.fromHtml(getFormattedMessage(notification)));

        ImageView imageView = viewHolder.observationPhoto;
        imageView.setImageDrawable(null); // Clear the previous image
        imageView.setImageResource(R.mipmap.ic_kornjaca); // Set the icon before the real image is loaded
        setPhoto(notification, imageView); // Download and display image
        if (notification.getMarked() == 1) {
            viewHolder.itemView.setBackgroundColor(viewHolder.itemView.getResources().getColor(R.color.colorPrimaryLight));
        } else {
            viewHolder.itemView.setBackgroundColor(viewHolder.itemView.getResources().getColor(R.color.fragment_background));
        }

    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        context = recyclerView.getContext();
    }

    @Override
    public int getItemCount() {
        if (notifications != null) {
            return notifications.size();
        } else {
            return 0;
        }
    }

    private String getFormattedMessage(UnreadNotificationsDb unreadNotification) {
        String text;
        String taxon = unreadNotification.getTaxonName();
        String author = getAuthor(unreadNotification);

        String action;
        String action1 = null;
        switch (unreadNotification.getType()) {
            case "App\\Notifications\\FieldObservationApproved":
                action = context.getString(R.string.approved_observation);
                break;
            case "App\\Notifications\\FieldObservationEdited":
                action = context.getString(R.string.changed_observation);
                break;
            case "App\\Notifications\\FieldObservationMarkedUnidentifiable":
                action = context.getString(R.string.marked_unidentifiable);
                action1 = context.getString(R.string.marked_unidentifiable2);
                break;
            default:
                action = context.getString(R.string.did_something_with_observation);
                break;
        }

        Date date = DateHelper.getDateFromJSON(unreadNotification.getUpdatedAt());
        String localized_date = DateHelper.getLocalizedDate(date, context);
        String localized_time = DateHelper.getLocalizedTime(date, context);

        if (action1 == null) {
            text = "<b>" + author + "</b> " +
                    action + " <i>" + taxon + "</i> " +
                    context.getString(R.string.on) + " " +
                    localized_date + " (" + localized_time + ").";
        } else {
            text = "<b>" + author + "</b> " +
                    action + " <i>" + taxon + "</i> " +
                    " " + action1 + " " +
                    context.getString(R.string.on) + " " +
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

    private void setPhoto(UnreadNotificationsDb unreadNotificationsDb, ImageView imageView) {

        // First check if there is a photo stored in the internal memory and ObjectBox
        Box<UnreadNotificationsDb> notificationsDbBox = App
                .get().getBoxStore().boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> notificationsDbQuery = notificationsDbBox
                .query(UnreadNotificationsDb_.fieldObservationId.equal(unreadNotificationsDb.getFieldObservationId()))
                .build();
        List<UnreadNotificationsDb> notifications = notificationsDbQuery.find();
        notificationsDbQuery.close();
        if (!notifications.isEmpty()) {
            Log.i(TAG, "There are " + notifications.size() + " notifications for field observation ID " + unreadNotificationsDb.getFieldObservationId());
            for (int i = 0; i < notifications.size(); i++) {
                String thumbnail = notifications.get(i).getThumbnail();
                // If there is no thumbnail, we should download it from the internet
                // Note that the photo will also be saved in a separate file
                Log.d(TAG, "Thumbnail is set to " + thumbnail);
                if (thumbnail == null && i == 0) {
                    Call<FieldObservationResponse> fieldObservation = RetrofitClient
                            .getService(SettingsManager.getDatabaseName())
                            .getFieldObservation(String.valueOf(unreadNotificationsDb.getFieldObservationId()));
                    fieldObservation.enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                            if (response.isSuccessful()) {
                                if (response.body() != null) {
                                    FieldObservationData[] fieldObservationDataArray = response.body().getData();
                                    if (fieldObservationDataArray != null && fieldObservationDataArray.length > 0) {
                                        String date = fieldObservationDataArray[0].getDay() + "-" +
                                                fieldObservationDataArray[0].getMonth() + "-" +
                                                fieldObservationDataArray[0].getYear();
                                        String location = fieldObservationDataArray[0].getLocation();
                                        if (location == null || location.isEmpty()) {
                                            DecimalFormat f = new DecimalFormat("##.0000");
                                            location = f.format(fieldObservationDataArray[0].getLongitude()) + "° E; " +
                                                    f.format(fieldObservationDataArray[0].getLatitude()) + "° N";
                                        }
                                        String project = fieldObservationDataArray[0].getProject();
                                        String finalTaxon = fieldObservationDataArray[0].getTaxonSuggestion();

                                        List<FieldObservationDataPhotos> photos = fieldObservationDataArray[0].getPhotos();
                                        if (photos != null && !photos.isEmpty()) {
                                            String url = photos.get(0).getUrl();
                                            Log.d(TAG, "Image 1 url is: " + url);

                                            // Save the URL from online images without downloading.
                                            // It will save some online traffic latter on.
                                            if (photos.size() > 1) {
                                                Log.d(TAG, "Image 2 url is: " + photos.get(1).getUrl());
                                                for (int j = 0; j < notifications.size(); j++) {
                                                    notifications.get(j).setImage2(photos.get(1).getUrl());
                                                }
                                            } else {
                                                Log.d(TAG, "Image 2 does not exist, setting to no photo.");
                                                for (int j = 0; j < notifications.size(); j++) {
                                                    notifications.get(j).setImage2("No photo");
                                                }
                                            }

                                            if (photos.size() > 2) {
                                                Log.d(TAG, "Image 3 url is: " + photos.get(2).getUrl());
                                                for (int j = 0; j < notifications.size(); j++) {
                                                    notifications.get(j).setImage3(photos.get(2).getUrl());
                                                }
                                            } else {
                                                Log.d(TAG, "Image 3 does not exist, setting to no photo.");
                                                for (int j = 0; j < notifications.size(); j++) {
                                                    notifications.get(j).setImage3("No photo");
                                                }
                                            }

                                            Call<ResponseBody> photoResponse = RetrofitClient.getService(SettingsManager.getDatabaseName()).getPhoto(url);
                                            photoResponse.enqueue(new Callback<>() {
                                                @Override
                                                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                                                    if (response.isSuccessful()) {
                                                        Log.d(TAG, "Photo successfully downloaded.");
                                                        try (ResponseBody responseBody = response.body()) {
                                                            if (responseBody != null) {
                                                                Log.d(TAG, "Image response obtained.");
                                                                InputStream inputStream = responseBody.byteStream();
                                                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                                                                // Store the original image 1 first
                                                                Uri uri = PreparePhotos.saveBitmap(context, bitmap);
                                                                for (int j = 0; j < notifications.size(); j++) {
                                                                    notifications.get(j).setImage1(uri != null ? uri.toString() : null);
                                                                }

                                                                // Crop the thumbnail to square shape to look a bit better
                                                                int w = bitmap.getWidth();
                                                                int h = bitmap.getHeight();
                                                                if (w < h) {
                                                                    int size = w - 200;
                                                                    int rest_to_crop = (h - size) / 2;
                                                                    Bitmap thumbnail = Bitmap.createBitmap(bitmap, 100, rest_to_crop, size, size);
                                                                    imageView.setImageBitmap(thumbnail);
                                                                    Uri th_uri = PreparePhotos.saveBitmap(context, thumbnail);
                                                                    for (int j = 0; j < notifications.size(); j++) {
                                                                        notifications.get(j).setThumbnail(th_uri != null ? th_uri.toString() : null);
                                                                        App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notifications.get(j));
                                                                    }
                                                                } else {
                                                                    int size = h - 200;
                                                                    int rest_to_crop = (w - size) / 2;
                                                                    Bitmap thumbnail = Bitmap.createBitmap(bitmap, rest_to_crop, 100, size, size);
                                                                    imageView.setImageBitmap(thumbnail);
                                                                    Uri th_uri = PreparePhotos.saveBitmap(context, thumbnail);
                                                                    for (int j = 0; j < notifications.size(); j++) {
                                                                        notifications.get(j).setThumbnail(th_uri != null ? th_uri.toString() : null);
                                                                        App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notifications.get(j));
                                                                    }
                                                                }
                                                            } else {
                                                                Log.e(TAG, "Server returned null as the image response.");
                                                            }
                                                        }
                                                    } else {
                                                        Log.e(TAG, "Photo download unsuccessful. Code: " + response.code() + ", Message: " + response.message());
                                                    }
                                                }

                                                @Override
                                                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                                                    Log.e(TAG, "Something is wrong with image response! Error: " + t.getLocalizedMessage(), t);
                                                }
                                            });

                                        } else {
                                            Log.i(TAG, "This observation does not have a photo.");
                                            for (int k = 0; k < notifications.size(); k++) {
                                                notifications.get(k).setThumbnail("No photo");
                                                notifications.get(k).setImage1("No photo");
                                                notifications.get(k).setImage2("No photo");
                                                notifications.get(k).setImage3("No photo");
                                            }
                                        }

                                        // Save everything to the ObjectBox
                                        for (int j = 0; j < notifications.size(); j++) {
                                            notifications.get(j).setDate(date);
                                            notifications.get(j).setLocation(location);
                                            notifications.get(j).setProject(project);
                                            notifications.get(j).setFinalTaxonName(finalTaxon);
                                            Log.i(TAG, "Writing important field observation data to the ObjectBox.");
                                            App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notifications.get(j));
                                        }
                                    } else {
                                        Log.d(TAG, "Field observation data array is null or empty for ID: " + unreadNotificationsDb.getFieldObservationId());
                                        // Handle the case where there's no data for the field observation.
                                        // This might mean there's no such observation or an issue with the API response.
                                        // You might want to update the UI or notifications to reflect this.
                                    }
                                } else {
                                    Log.d(TAG, "Response body for field observation is null!");
                                }
                            } else if (response.code() == 429) {
                                String retryAfter = response.headers().get("retry-after");
                                long sec = Long.parseLong(Objects.requireNonNull(retryAfter, "Header did not return number of seconds."));
                                Log.d(TAG, "Server resource limitation reached, retry after " + sec + " seconds.");
                                // Add handler to delay fetching
                                Handler handler = new Handler();
                                Runnable runnable = () -> setPhoto(unreadNotificationsDb, imageView);
                                handler.postDelayed(runnable, sec * 1000);
                            } else if (response.code() == 508) {
                                Log.d(TAG, "Server detected a loop, retrying in 5 sec.");
                                Handler handler = new Handler();
                                Runnable runnable = () -> setPhoto(unreadNotificationsDb, imageView);
                                handler.postDelayed(runnable, 5000);
                            } else {
                                Log.d(TAG, "The response for field observation is not successful. Code: " + response.code() + ", Message: " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<FieldObservationResponse> call, @NonNull Throwable t) {
                            Log.d(TAG, "Something is wrong!", t);
                        }
                    });

                } else if (thumbnail != null && thumbnail.equals("No photo")) {
                    Log.i(TAG, "There is no photo, ignoring...");
                }

                // If the thumbnail exist, we should just display it in tne notifications.
                else {
                    if (i == 0) {
                        Log.i(TAG, "There is a thumbnail already, displaying that one: " + notifications.get(0).getThumbnail());
                        Uri uri = Uri.parse(notifications.get(0).getThumbnail());
                        if (FileManipulation.uriFileExist(context, uri)) {
                            imageView.setImageURI(uri);
                        } else {
                            // If the image file is deleted, download it again.
                            for (int j = 0; j < notifications.size(); j++) {
                                notifications.get(j).setThumbnail(null);
                                notifications.get(j).setImage1(null);
                                App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notifications.get(j));
                            }
                            setPhoto(notifications.get(0), imageView);
                        }
                    }
                }
            }
        } else {
            Log.e(TAG, "Image URI not saved to ObjectBox.");
        }
    }
}
