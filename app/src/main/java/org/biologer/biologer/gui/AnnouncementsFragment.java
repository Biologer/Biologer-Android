package org.biologer.biologer.gui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.biologer.biologer.Localisation;
import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.sql.AnnouncementTranslationsDb;
import org.biologer.biologer.sql.AnnouncementTranslationsDb_;
import org.biologer.biologer.sql.AnnouncementsDb;

import java.util.Collections;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;


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

        List<AnnouncementsDb> announcements = ObjectBox.get().boxFor(AnnouncementsDb.class).getAll();
        Collections.reverse(announcements);
        long id = announcements.get(index).getId();
        String locale = Localisation.getLocaleScript();
        Log.d(TAG, "Opening announcement with index: " + index + "; ID: " + id + "; locale: " + locale);

        Box<AnnouncementTranslationsDb> box = ObjectBox.get().boxFor(AnnouncementTranslationsDb.class);
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
        String text = announcements.get(index).getMessage();
        if (translation != null) {
            text = translation.getMessage();
        }
        textText.setText(text);

        return rootView;

    }

}
