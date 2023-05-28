package org.biologer.biologer.gui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.Localisation;
import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.adapters.AnnouncementsAdapter;
import org.biologer.biologer.adapters.RecyclerOnClickListener;
import org.biologer.biologer.sql.AnnouncementTranslationsDb;
import org.biologer.biologer.sql.AnnouncementTranslationsDb_;
import org.biologer.biologer.sql.AnnouncementsDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class AnnouncementsActivity extends AppCompatActivity {

    private static final String TAG = "Biologer.Announcements";
    RecyclerView recyclerView;
    AnnouncementsAdapter announcementsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        List<AnnouncementsDb> announcements = ObjectBox.get().boxFor(AnnouncementsDb.class).getAll();
        if (announcements == null) {
            announcements = new ArrayList<>();
        }
        Collections.reverse(announcements);

        String locale_script = Localisation.getLocaleScript();
        for (int i = 0; i < announcements.size(); i++) {
            Box<AnnouncementTranslationsDb> box = ObjectBox.get().boxFor(AnnouncementTranslationsDb.class);
            Query<AnnouncementTranslationsDb> query = box
                    .query(AnnouncementTranslationsDb_.locale.equal(locale_script)
                            .and(AnnouncementTranslationsDb_.id.equal(announcements.get(i).getId())))
                    .build();
            AnnouncementTranslationsDb announcementTranslation = query.findFirst();
            if (announcementTranslation != null) {
                announcements.get(i).setTitle(announcementTranslation.getTitle());
                announcements.get(i).setMessage(announcementTranslation.getMessage());
            }
        }

        recyclerView = findViewById(R.id.recycled_view_announcements);
        announcementsAdapter = new AnnouncementsAdapter(announcements);
        recyclerView.setAdapter(announcementsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnItemTouchListener(
                new RecyclerOnClickListener(this, recyclerView, new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Log.d(TAG, "Announcements item " + position + " clicked");
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Log.d(TAG, "Announcements item " + position + " long pressed");
                    }
                }));

    }

}
