package org.biologer.biologer.gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.adapters.AnnouncementsAdapter;
import org.biologer.biologer.adapters.RecyclerOnClickListener;
import org.biologer.biologer.network.UpdateAnnouncements;
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
    BroadcastReceiver broadcastReceiver;
    List<AnnouncementsDb> announcements;
    RecyclerView recyclerView;
    AnnouncementsAdapter announcementsAdapter;
    int current_size;
    int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(R.string.announcements);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }

        announcements = App.get().getBoxStore().boxFor(AnnouncementsDb.class).getAll();
        if (announcements == null) {
            announcements = new ArrayList<>();
            current_size = 0;
        } else {
            Collections.reverse(announcements);
            current_size = announcements.size();
        }
        translateAnnouncements(announcements);

        recyclerView = findViewById(R.id.recycled_view_announcements);
        announcementsAdapter = new AnnouncementsAdapter(announcements);
        recyclerView.setAdapter(announcementsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnItemTouchListener(
                new RecyclerOnClickListener(this, recyclerView, new RecyclerOnClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        index = position;
                        Log.d(TAG, "Announcements item " + position + " clicked");
                        Fragment fragment = new AnnouncementsFragment();
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.add(R.id.announcements_frame, fragment);
                        ft.addToBackStack("Announcements fragment");
                        ft.commit();
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Log.d(TAG, "Announcements item " + position + " long pressed");
                    }
                }));

        final Intent getAnnouncements = new Intent(AnnouncementsActivity.this, UpdateAnnouncements.class);
        startService(getAnnouncements);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(UpdateAnnouncements.TASK_COMPLETED);

                // This will be executed after download is completed
                if (s != null) {
                    Log.i(TAG, "Downloading announcements returned the code: " + s);

                    announcements = App.get().getBoxStore().boxFor(AnnouncementsDb.class).getAll();

                    if (announcements != null) {
                        if (current_size < announcements.size()) {
                            Log.d(TAG, "Displaying new announcements.");
                            Collections.reverse(announcements);
                            translateAnnouncements(announcements);

                            /*
                            / This didn't work for some reason
                            for (int i = 0; i < announcements.size(); i++) {
                                if (announcements.size() <= current_size) {
                                    announcementsAdapter.notifyItemChanged(i);
                                    Log.d(TAG, "Updating current announcement");
                                } else {
                                    announcementsAdapter.notifyItemInserted(i);
                                    Log.d(TAG, "Adding new announcement");
                                }
                            }
                             */

                            // So we are doing it the on the bad way...
                            announcementsAdapter = new AnnouncementsAdapter(announcements);
                            recyclerView.setAdapter(announcementsAdapter);

                        } else {
                            Log.d(TAG, "There are no new announcements to display!");
                        }
                    }

                }
            }
        };

    }

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
    public void onPause() {
        super.onPause();
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
            LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver),
                    new IntentFilter(UpdateAnnouncements.TASK_COMPLETED)
            );
    }

}
