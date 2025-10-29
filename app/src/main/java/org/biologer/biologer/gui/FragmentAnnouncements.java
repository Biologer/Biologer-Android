package org.biologer.biologer.gui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.biologer.biologer.App;
import org.biologer.biologer.helpers.Localisation;
import org.biologer.biologer.adapters.AnnouncementsAdapter;
import org.biologer.biologer.databinding.FragmentAnnouncementsBinding;
import org.biologer.biologer.services.RecyclerOnClickListener;
import org.biologer.biologer.sql.AnnouncementTranslationsDb;
import org.biologer.biologer.sql.AnnouncementTranslationsDb_;
import org.biologer.biologer.sql.AnnouncementsDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class FragmentAnnouncements extends Fragment {

    private static final String TAG = "Biologer.Announcements";
    private FragmentAnnouncementsBinding binding;
    AnnouncementsAdapter announcementsAdapter;
    int current_size;
    int index;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAnnouncementsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<AnnouncementsDb> announcements = getAnnouncements();

        announcementsAdapter = new AnnouncementsAdapter(announcements);
        binding.recycledViewAnnouncements.setAdapter(announcementsAdapter);
        binding.recycledViewAnnouncements.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.recycledViewAnnouncements.addOnItemTouchListener(
                new RecyclerOnClickListener(getActivity(), binding.recycledViewAnnouncements,
                        new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        index = position;
                        Log.d(TAG, "Announcements item " + position + " clicked");
                        Intent intent = new Intent(getActivity(), ActivityAnnouncement.class);
                        intent.putExtra("index", index);
                        startActivity(intent);
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Log.d(TAG, "Announcements item " + position + " long pressed");
                    }
                }));
    }

    // Function used to get all announcements from SQL database
    private List<AnnouncementsDb> getAnnouncements() {
        List<AnnouncementsDb> announcements = App.get().getBoxStore().boxFor(AnnouncementsDb.class).getAll();
        if (announcements == null) {
            announcements = new ArrayList<>();
            current_size = 0;
        } else {
            Collections.reverse(announcements);
            current_size = announcements.size();
        }
        translateAnnouncements(announcements);
        return announcements;
    }

    // Function used to localize announcements text to locale of the phone
    private void translateAnnouncements(List<AnnouncementsDb> announcements) {
        String locale_script = Localisation.getLocaleScript();
        for (int i = 0; i < announcements.size(); i++) {
            Box<AnnouncementTranslationsDb> box = App.get().getBoxStore().boxFor(AnnouncementTranslationsDb.class);
            Query<AnnouncementTranslationsDb> query = box
                    .query(AnnouncementTranslationsDb_.locale.equal(locale_script)
                            .and(AnnouncementTranslationsDb_.announcementId.equal(announcements.get(i).getId())))
                    .build();
            AnnouncementTranslationsDb announcementTranslation = query.findFirst();
            query.close();

            if (announcementTranslation != null) {
                announcements.get(i).setTitle(announcementTranslation.getTitle());
                announcements.get(i).setMessage(announcementTranslation.getMessage());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "Announcements Fragment resumed.");
        List<AnnouncementsDb> announcements = getAnnouncements();
        announcementsAdapter = new AnnouncementsAdapter(announcements);
        binding.recycledViewAnnouncements.setAdapter(announcementsAdapter);
    }

}
