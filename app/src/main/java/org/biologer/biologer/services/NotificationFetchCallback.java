package org.biologer.biologer.services;

import org.biologer.biologer.sql.UnreadNotificationsDb;

public interface NotificationFetchCallback {
    // Called when the entire process is complete and the UI should be updated
    void onNotificationUpdated(UnreadNotificationsDb notification);

    // Called if a retry is scheduled (e.g., for HTTP 429/508)
    void onRetryScheduled(UnreadNotificationsDb notification, long delayMillis);

    // Called on complete failure (e.g., network error)
    void onFailure(UnreadNotificationsDb notification, Throwable t);
}