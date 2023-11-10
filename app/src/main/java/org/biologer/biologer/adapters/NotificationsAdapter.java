package org.biologer.biologer.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.FieldObservationDataPhotos;
import org.biologer.biologer.network.json.FieldObservationResponse;
import org.biologer.biologer.sql.UnreadNotificationsDb;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsAdapter
        extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
    private final List<UnreadNotificationsDb> myNotifications;
    private static final String TAG = "Biologer.NotifyAdapter";
    Context context;

    public NotificationsAdapter(List<UnreadNotificationsDb> myNotifications) {
        this.myNotifications = myNotifications;
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

        UnreadNotificationsDb notification = myNotifications.get(position);

        TextView textNotifications = viewHolder.textNotification;
        textNotifications.setText(Html.fromHtml(getFormattedMessage(notification)));

        ImageView imageView = viewHolder.observationPhoto;
        setPhoto(notification.getFieldObservationId(), imageView);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        context = recyclerView.getContext();
    }

    @Override
    public int getItemCount() {
        if (myNotifications != null) {
            return myNotifications.size();
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

    private void setPhoto(int fieldObservationID, ImageView imageView) {
        // Get the data from Field observation and display them
        Call<FieldObservationResponse> fieldObservation = RetrofitClient.getService(SettingsManager.getDatabaseName()).getFieldObservation(String.valueOf(fieldObservationID));
        fieldObservation.enqueue(new Callback<FieldObservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        if (!response.body().getData()[0].getPhotos().isEmpty()) {
                            FieldObservationDataPhotos photo = response.body().getData()[0].getPhotos().get(0);
                            String url = photo.getUrl();
                            Log.d(TAG, "Image url is: " + url);

                            Call<ResponseBody> photoResponse = RetrofitClient.getService(SettingsManager.getDatabaseName()).getPhoto(url);
                            photoResponse.enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                                    if (response.isSuccessful()) {
                                        Log.d(TAG, "Photo successfully downloaded.");
                                        try (ResponseBody responseBody = response.body()) {
                                            if (responseBody != null) {
                                                Log.d(TAG, "Image response obtained.");
                                                InputStream inputStream = responseBody.byteStream();
                                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                                // Crop the image to square shape to look a bit better
                                                int w = bitmap.getWidth();
                                                int h = bitmap.getHeight();
                                                if (w < h) {
                                                    int size = w - 200;
                                                    int rest_to_crop = (h - size) / 2;
                                                    imageView.setImageBitmap(Bitmap.createBitmap(bitmap, 100, rest_to_crop, size, size));
                                                } else {
                                                    int size = h - 200;
                                                    int rest_to_crop = (w - size) / 2;
                                                    imageView.setImageBitmap(Bitmap.createBitmap(bitmap, rest_to_crop, 100, size, size));
                                                }
                                            } else {
                                                Log.e(TAG, "Server returned null as the image response.");
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
                    } else {
                        Log.d(TAG, "Response body is null!");
                    }
                } else if (response.code() == 429) {
                    String retryAfter = response.headers().get("retry-after");
                    long sec = Long.parseLong(Objects.requireNonNull(retryAfter, "Header did not return number of seconds."));
                    Log.d(TAG, "Server resource limitation reached, retry after " + sec + " seconds.");
                    // Add handler to delay fetching
                    Handler handler = new Handler();
                    Runnable runnable = () -> setPhoto(fieldObservationID, imageView);
                    handler.postDelayed(runnable, sec * 1000);
                }
                else {
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
}
