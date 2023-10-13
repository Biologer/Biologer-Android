package org.biologer.biologer.gui;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

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

import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnnouncementsFragment extends Fragment {
    private static final String TAG = "Biologer.AnnouncementsR";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_announcements, container, false);

        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.announcement));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> {
            Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        });

        int index = 0;
        if (((AnnouncementsActivity)getActivity()) != null) {
            index = ((AnnouncementsActivity)getActivity()).index;
        }

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

        TextView textTitle = rootView.findViewById(R.id.announcement_reading_title);
        String title = announcements.get(index).getTitle();
        if (translation != null) {
            title = translation.getTitle();
        }
        textTitle.setText(title);

        TextView textText = rootView.findViewById(R.id.announcement_reading_text);
        textText.setMovementMethod(new ScrollingMovementMethod());
        String text = announcements.get(index).getMessage();
        if (translation != null) {
            text = translation.getMessage();
        }
        textText.setText(text);

        TextView textAuthor = rootView.findViewById(R.id.announcement_reading_author);
        String author = announcements.get(index).getCreatorName();
        textAuthor.setText(author);

        TextView textDate = rootView.findViewById(R.id.announcement_reading_date);
        Date date = DateHelper.getDateFromJSON(announcements.get(index).getCreatedAt());
        String date_string = getString(R.string.created_on) + " " + DateHelper.getLocalizedDate(date, getContext());
        textDate.setText(date_string);

        markAnnouncementAsRead(id);

        return rootView;

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

}
