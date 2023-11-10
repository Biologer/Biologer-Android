package org.biologer.biologer.gui;

import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.DateHelper;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.AnnouncementTranslationsDb;
import org.biologer.biologer.sql.AnnouncementTranslationsDb_;
import org.biologer.biologer.sql.AnnouncementsDb;
import org.biologer.biologer.sql.AnnouncementsDb_;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnnouncementActivity extends AppCompatActivity {
    private static final String TAG = "Biologer.AnnouncementsR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcement);

        // Add a toolbar to the Activity
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(R.string.announcement);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }

        // If opening from Activity use index of taped list, else use bundle received from other Fragment
        int index;
        Bundle bundle = getIntent().getExtras();
        index = Objects.requireNonNull(bundle).getInt("index");
        Log.d(TAG, "Displaying announcement with index ID: " + index + ".");

        List<AnnouncementsDb> announcements = App.get().getBoxStore().boxFor(AnnouncementsDb.class).getAll();
        Collections.reverse(announcements);
        long id = announcements.get(index).getId();
        String locale = Localisation.getLocaleScript();
        Log.d(TAG, "Opening announcement with index: " + index + "; ID: " + id + "; locale: " + locale);

        Box<AnnouncementTranslationsDb> box = App.get().getBoxStore().boxFor(AnnouncementTranslationsDb.class);
        Query<AnnouncementTranslationsDb> query = box
                .query(AnnouncementTranslationsDb_.announcementId.equal(id)
                        .and(AnnouncementTranslationsDb_.locale.equal(locale)))
                .build();
        AnnouncementTranslationsDb translation = query.findFirst();
        query.close();

        TextView textTitle = findViewById(R.id.announcement_reading_title);
        String title = announcements.get(index).getTitle();
        if (translation != null) {
            title = translation.getTitle();
        }
        textTitle.setText(title);

        TextView textText = findViewById(R.id.announcement_reading_text);
        textText.setMovementMethod(new ScrollingMovementMethod());
        String text = announcements.get(index).getMessage();
        if (translation != null) {
            text = translation.getMessage();
        }
        textText.setText(Html.fromHtml(text));

        TextView textAuthor = findViewById(R.id.announcement_reading_author);
        String author = announcements.get(index).getCreatorName();
        textAuthor.setText(author);

        TextView textDate = findViewById(R.id.announcement_reading_date);
        Date date = DateHelper.getDateFromJSON(announcements.get(index).getCreatedAt());
        String date_string = getString(R.string.created_on) + " " + DateHelper.getLocalizedDate(date, this);
        textDate.setText(date_string);

        markAnnouncementAsRead(id);

    }

    private void markAnnouncementAsRead(long id) {

        // Mark Announcement as read locally
        Box<AnnouncementsDb> announcements = App.get().getBoxStore().boxFor(AnnouncementsDb.class);
        Query<AnnouncementsDb> query = announcements
                .query(AnnouncementsDb_.id.equal(id))
                .build();
        AnnouncementsDb announcement = query.findFirst();
        query.close();
        if (announcement != null) {
            announcement.setRead(true);
            App.get().getBoxStore().boxFor(AnnouncementsDb.class).put(announcement);
        }

        // Mark announcement as read online
        Call<ResponseBody> call = RetrofitClient.getService(
                SettingsManager.getDatabaseName()).setAnnouncementAsRead(id);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Announcement should be marked as read now.");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                Log.d(TAG, "Announcement could not be marked as read " + t.getLocalizedMessage());
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.getOnBackPressedDispatcher().onBackPressed();
        }
        return true;
    }

}
