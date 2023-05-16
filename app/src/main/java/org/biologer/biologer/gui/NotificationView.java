package org.biologer.biologer.gui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;

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

    }

}
