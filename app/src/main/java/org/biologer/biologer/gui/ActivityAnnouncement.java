package org.biologer.biologer.gui;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.databinding.ActivityAnnouncementBinding;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.services.DateHelper;
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

public class ActivityAnnouncement extends AppCompatActivity {

    private static final String TAG = "Biologer.AnnouncementsR";
    private ActivityAnnouncementBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnnouncementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Toolbar
        addToolbar();

        // Get index from intent
        Bundle bundle = getIntent().getExtras();
        int index = Objects.requireNonNull(bundle).getInt("index");
        Log.d(TAG, "Displaying announcement with index ID: " + index + ".");

        // Load announcements
        List<AnnouncementsDb> announcements = App.get().getBoxStore().boxFor(AnnouncementsDb.class).getAll();
        Collections.reverse(announcements);
        long id = announcements.get(index).getId();
        String locale = Localisation.getLocaleScript();
        Log.d(TAG, "Opening announcement with index: " + index + "; ID: " + id + "; locale: " + locale);

        // Query translation
        Box<AnnouncementTranslationsDb> box = App.get().getBoxStore().boxFor(AnnouncementTranslationsDb.class);
        Query<AnnouncementTranslationsDb> query = box
                .query(AnnouncementTranslationsDb_.announcementId.equal(id)
                        .and(AnnouncementTranslationsDb_.locale.equal(locale)))
                .build();
        AnnouncementTranslationsDb translation = query.findFirst();
        query.close();

        // Title
        String title = announcements.get(index).getTitle();
        if (translation != null) title = translation.getTitle();
        binding.announcementReadingTitle.setText(title);

        // Message
        String text = announcements.get(index).getMessage();
        if (translation != null) text = translation.getMessage();
        binding.announcementReadingText.setMovementMethod(new ScrollingMovementMethod());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.announcementReadingText.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
        } else {
            binding.announcementReadingText.setText(Html.fromHtml(text));
        }

        // Author
        String author = announcements.get(index).getCreatorName();
        binding.announcementReadingAuthor.setText(author);

        // Date
        Date date = DateHelper.getDateFromJSON(announcements.get(index).getCreatedAt());
        String dateString = getString(R.string.created_on) + " " + DateHelper.getLocalizedDate(date, this);
        binding.announcementReadingDate.setText(dateString);

        // Mark as read
        markAnnouncementAsRead(id);
    }

    private void addToolbar() {
        setSupportActionBar(binding.toolbar.toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(R.string.announcement);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }
    }

    private void markAnnouncementAsRead(long id) {
        // Mark locally
        Box<AnnouncementsDb> announcements = App.get().getBoxStore().boxFor(AnnouncementsDb.class);
        Query<AnnouncementsDb> query = announcements.query(AnnouncementsDb_.id.equal(id)).build();
        AnnouncementsDb announcement = query.findFirst();
        query.close();
        if (announcement != null) {
            announcement.setRead(true);
            App.get().getBoxStore().boxFor(AnnouncementsDb.class).put(announcement);
        }

        // Mark online
        Call<ResponseBody> call = RetrofitClient.getService(SettingsManager.getDatabaseName())
                .setAnnouncementAsRead(id);
        call.enqueue(new Callback<>() {
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
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
