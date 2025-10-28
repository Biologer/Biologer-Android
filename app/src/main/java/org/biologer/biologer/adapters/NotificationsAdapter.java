package org.biologer.biologer.adapters;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.services.DateHelper;
import org.biologer.biologer.services.FileManipulation;
import org.biologer.biologer.services.NotificationFetchCallback;
import org.biologer.biologer.services.NotificationsHelper;
import org.biologer.biologer.sql.UnreadNotificationsDb;

import java.util.Date;
import java.util.List;

public class NotificationsAdapter
        extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
    private final List<UnreadNotificationsDb> notifications;
    private static final String TAG = "Biologer.NotifyAdapter";
    private final Context context;

    public NotificationsAdapter(Context context, List<UnreadNotificationsDb> notification) {
        this.context = context;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textNotifications.setText(Html.fromHtml(getFormattedMessage(notification), Html.FROM_HTML_MODE_LEGACY));
        } else {
            textNotifications.setText(Html.fromHtml(getFormattedMessage(notification)));
        }

        ImageView imageView = viewHolder.observationPhoto;
        imageView.setImageDrawable(null); // Clear the previous image
        imageView.setImageResource(R.mipmap.ic_kornjaca); // Set the icon before the real image is loaded
        setPhoto(notification, viewHolder, position); // Download and display image
        if (notification.getMarked() == 1) {
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(viewHolder.itemView.getContext(), R.color.colorPrimaryLight));
        } else {
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(viewHolder.itemView.getContext(), R.color.fragment_background));
        }

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

    private void setPhoto(UnreadNotificationsDb notification, ViewHolder holder, int position) {

        // Get the ImageView once
        final ImageView imageView = holder.observationPhoto;
        String thumbnail = notification.getThumbnail();

        // 1. Check if the thumbnail exists locally
        if (thumbnail != null && !thumbnail.equals("No photo")) {
            Uri uri = Uri.parse(thumbnail);
            if (FileManipulation.uriFileExist(context, uri)) {
                // State 1: Cache Hit - Display local image
                imageView.setImageURI(uri);
                return; // Finished successfully
            } else {
                // State 2: Cache Miss (File deleted) - Reset data to force download
                notification.setThumbnail(null);
                notification.setImage1(null);
                App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notification);
                // Fall through to download logic
            }
        }

        // 2. Download Logic (if thumbnail is null or was just reset)
        if (notification.getThumbnail() == null) {

            // State 3: Needs Download - Show the loading indicator
            imageView.setImageResource(R.drawable.ic_gps_turtle); // TODO

            NotificationsHelper.fetchFieldObservationAndPhotos(
                    context,
                    notification,
                    new NotificationFetchCallback() {
                        @Override
                        public void onNotificationUpdated(UnreadNotificationsDb updatedNotification) {
                            // Check if the ViewHolder is still bound to the correct item
                            if (holder.getBindingAdapterPosition() == position) {
                                String updatedThumbnail = updatedNotification.getThumbnail();
                                if (updatedThumbnail != null && !updatedThumbnail.equals("No photo")) {
                                    // Success: Display downloaded image
                                    imageView.setImageURI(Uri.parse(updatedThumbnail));
                                } else {
                                    // Success: Data fetched, but no photo URL was found
                                    imageView.setImageResource(R.drawable.ic_photo_camera); // TODO
                                }
                                // Note: You may need to call notifyItemChanged(position) if this
                                // fetch updates data used in other TextViews in the adapter.
                            }
                        }

                        @Override
                        public void onRetryScheduled(UnreadNotificationsDb updatedNotification, long delayMillis) {
                            // State: Retry scheduled
                            // You might want to update the loading icon/text here
                        }

                        @Override
                        public void onFailure(UnreadNotificationsDb updatedNotification, Throwable t) {
                            // State: Complete failure (e.g., network error)
                            if (holder.getBindingAdapterPosition() == position) {
                                imageView.setImageResource(R.drawable.ic_photo_camera); // TODO
                            }
                        }
                    });
        } else if (notification.getThumbnail().equals("No photo")) {
            // State 4: Already marked "No photo"
            imageView.setImageResource(R.drawable.ic_photo_camera); // TODO
        }
    }
}
