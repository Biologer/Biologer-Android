package org.biologer.biologer.gui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.databinding.ActivityAnnouncementBinding;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.services.DateHelper;
import org.biologer.biologer.services.ObjectBoxHelper;
import org.biologer.biologer.sql.AnnouncementTranslationsDb;
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
    public static final String EXTRA_DISPLAY_FRAGMENT = "org.biologer.biologer.gui.ActivityAnnouncement.DISPLAY_FRAGMENT";
    public static final String FRAGMENT_ANNOUNCEMENTS_TAG = "ANNOUNCEMENTS_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnnouncementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Toolbar
        addToolbar();

        // Get index or announcement_id from intent
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            Log.e(TAG, "Intent extras were null.");
            finishAndDisplayAnnouncements();
            return;
        }

        // SCENARIO 1: Launched from FCM Notification with remote ID
        if (bundle.containsKey("announcement_id")) {
            String remoteIdString = bundle.getString("announcement_id");
            try {
                long announcementId = Long.parseLong(Objects.requireNonNull(remoteIdString));
                Log.d(TAG, "Launched from FCM. Attempting to display announcement with remote ID: " + announcementId);
                updateAnnouncementText(announcementId);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid announcement_id from Intent: " + remoteIdString);
                finishAndDisplayAnnouncements();
                return;
            }
            return;
        }

        // SCENARIO 2: Launched from local list with local index (existing logic)
        if (bundle.containsKey("index")) {

            int index = bundle.getInt("index");
            Log.d(TAG, "Launched from list. Displaying announcement with index ID: " + index + ".");

            List<AnnouncementsDb> announcements = App.get().getBoxStore().boxFor(AnnouncementsDb.class).getAll();
            Collections.reverse(announcements);

            if (index >= 0 && index < announcements.size()) {
                AnnouncementsDb announcement = announcements.get(index);
                long announcementId = announcement.getId();
                Log.d(TAG, "Found announcement ID: " + announcementId + ".");
                updateAnnouncementText(announcementId);
            } else {
                Log.e(TAG, "Invalid index: " + index);
                finishAndDisplayAnnouncements();
            }
            return;
        }

        Log.e(TAG, "Intent extras did not contain 'announcement_id' or 'index'.");
        finishAndDisplayAnnouncements();
    }

    private void finishAndDisplayAnnouncements() {
        Intent intent = new Intent(this, ActivityLanding.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_DISPLAY_FRAGMENT, FRAGMENT_ANNOUNCEMENTS_TAG);
        startActivity(intent);
        finish();
    }

    private void updateAnnouncementText(long id) {
        AnnouncementTranslationsDb translation = ObjectBoxHelper.getTranslatedAnnouncementById(id);
        AnnouncementsDb announcement = ObjectBoxHelper.getAnnouncementById(id);

        if (announcement == null) {
            Log.w(TAG, "Announcement with ID " + id + " not found locally. It might still be downloading.");
            Toast.makeText(this, getString(R.string.no_announcement_localy), Toast.LENGTH_LONG).show();
            finishAndDisplayAnnouncements();
            return;
        }

        String title = announcement.getTitle();
        String text = announcement.getMessage();
        String author = announcement.getCreatorName();
        Date date = DateHelper.getDateFromJSON(announcement.getCreatedAt());
        String dateString = getString(R.string.created_on) + " " + DateHelper.getLocalizedDate(date, this);

        if (translation != null) {
            title = translation.getTitle();
            text = translation.getMessage();
        }

        binding.announcementReadingTitle.setText(title);
        binding.announcementReadingText.setMovementMethod(new ScrollingMovementMethod());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.announcementReadingText.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
        } else {
            binding.announcementReadingText.setText(Html.fromHtml(text));
        }
        binding.announcementReadingAuthor.setText(author);
        binding.announcementReadingDate.setText(dateString);

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