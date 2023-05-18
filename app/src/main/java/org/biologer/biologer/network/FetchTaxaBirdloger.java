package org.biologer.biologer.network;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.gui.LandingActivity;
import org.biologer.biologer.network.JSON.TaxaDataBirdloger;
import org.biologer.biologer.network.JSON.TaxaResponseBirdloger;
import org.biologer.biologer.network.JSON.TaxaStages;
import org.biologer.biologer.network.JSON.TaxaTranslations;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.TaxaTranslationDb;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonGroupsDb;
import org.biologer.biologer.sql.TaxonGroupsDb_;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FetchTaxaBirdloger extends Service {

    private static final String TAG = "Biologer.FetchTaxa";

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_START_NEW = "ACTION_START_NEW";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_CANCEL = "ACTION_CANCEL";
    public static final String ACTION_CANCEL_PAUSED = "ACTION_CANCEL_PAUSED";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    private String stop_fetching;
    private static FetchTaxaBirdloger instance = null;
    static final String TASK_COMPLETED = "org.biologer.biologer.network.FetchTaxa.TASK_COMPLETED";
    int retry_number = 1;
    ArrayList<String> taxa_groups = new ArrayList<>();


    LocalBroadcastManager broadcaster;

    private int totalPages = 0;
    private static int updated_at = Integer.parseInt(SettingsManager.getTaxaUpdatedAt());
    private static int current_page = Integer.parseInt(SettingsManager.getTaxaLastPageFetched());
    private static int progressStatus = 0;

    String system_time;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        broadcaster = LocalBroadcastManager.getInstance(this);
        system_time = String.valueOf(System.currentTimeMillis()/1000);
        Log.d(TAG, "Running onCreate()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            ArrayList<String> groups_list = intent.getStringArrayListExtra("groups");
            if (groups_list == null) {
                // Query to get taxa groups that should be used in a query
                Box<TaxonGroupsDb> taxonGroupsDataBox = ObjectBox.get().boxFor(TaxonGroupsDb.class);
                Query<TaxonGroupsDb> query = taxonGroupsDataBox
                        .query(TaxonGroupsDb_.id.notNull())
                        .build();
                List<TaxonGroupsDb> allTaxaGroups = query.find();
                query.close();
                for (int i = 0; i < allTaxaGroups.size(); i++) {
                    int id = (int)allTaxaGroups.get(i).getId();
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    boolean checked = preferences.getBoolean(String.valueOf(allTaxaGroups.get(i).getId()), true);
                    if (checked) {
                        // Log.d(TAG, "Checkbox for taxa group ID " + id + " is checked.");
                        taxa_groups.add(String.valueOf(id));
                    }
                }
            } else {
                taxa_groups = groups_list;
                current_page = 1;
                updated_at = 0;
            }

            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_START:
                        Log.i(TAG, "Action start selected, starting foreground service.");
                        stop_fetching = "keep_going";
                        // Start the service
                        notificationInitiate();
                        break;
                    case ACTION_START_NEW:
                        Log.i(TAG, "Action start from first page selected, starting foreground service.");
                        // Clean previous data just in case
                        cleanDatabase();
                        stop_fetching = "keep_going";
                        // Start the service
                        notificationInitiate();
                        break;
                    case ACTION_PAUSE:
                        Log.i(TAG, "Action pause selected, pausing foreground service.");
                        stop_fetching = "pause";
                        break;
                    case ACTION_CANCEL:
                        Log.i(TAG, "Action cancel selected while download was running.");
                        stop_fetching = "cancel";
                        break;
                    case ACTION_CANCEL_PAUSED:
                        Log.i(TAG, "Action cancel selected while download was paused.");
                        sendResult("canceled");
                        updateNotification(getString(R.string.notify_title_taxa_canceled), getString(R.string.notify_desc_taxa_canceled), null, null);
                        stopSelf();
                        break;
                    case ACTION_RESUME:
                        Log.i(TAG, "Action resume selected, continuing the foreground service.");
                        stop_fetching = "keep_going";
                        notificationInitiate();
                        break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void fetchTaxa() {
        // If user selected pause or cancel we will stop the script
        switch (stop_fetching) {
            case "pause":
                Log.d(TAG, "Fetching of taxa data is paused by the user!");
                sendResult("paused");
                updateNotification(getString(R.string.notify_title_taxa), getString(R.string.notify_desc_taxa), getString(R.string.resume_action), progressStatus);
                stopSelf();
                break;
            case "cancel":
                Log.d(TAG, "Fetching of taxa data is canceled by the user!");
                sendResult("canceled");
                updateNotification(getString(R.string.notify_title_taxa_canceled), getString(R.string.notify_desc_taxa_canceled), null, null);
                stopSelf();
                break;
            case "keep_going":

                Call<TaxaResponseBirdloger> call = RetrofitClient.getService(
                        SettingsManager.getDatabaseName()).getBirdlogerTaxa(current_page, 100, updated_at);

                call.enqueue(new Callback<>() {

                    @Override
                    public void onResponse(@NonNull Call<TaxaResponseBirdloger> call, @NonNull Response<TaxaResponseBirdloger> response) {
                        if (response.isSuccessful()) {
                            TaxaResponseBirdloger taxaResponseBirdloger = response.body();
                            if (taxaResponseBirdloger != null) {
                                saveFetchedPage(taxaResponseBirdloger);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TaxaResponseBirdloger> call, @NonNull Throwable t) {
                        Log.e(TAG, "Application could not get data from a server: " + t.getLocalizedMessage());

                        if (retry_number == 4) {
                            sendResult("failed");
                            updateNotification(getString(R.string.notify_title_taxa_failed), getString(R.string.notify_desc_taxa_failed), getString(R.string.retry), progressStatus);
                            stopSelf();
                            Log.d(TAG, "Fetching taxa failed!");
                        } else {
                            Log.d(TAG, "Starting retry loop No. " + retry_number + ".");
                            fetchTaxa();
                            retry_number++;
                        }
                    }
                });
        }
    }

    private void saveFetchedPage(TaxaResponseBirdloger taxaResponseBirdloger) {

        if (totalPages == 0) {
            totalPages = taxaResponseBirdloger.getMeta().getLastPage();
            Log.d(TAG, "Last page: " + taxaResponseBirdloger.getMeta().getLastPage() +
                    "; to: " + taxaResponseBirdloger.getMeta().getTo() + "; total: " + taxaResponseBirdloger.getMeta().getTotal());
        }
        List<TaxaDataBirdloger> taxaData = taxaResponseBirdloger.getData();

        // Variables used to update the Progress Bar status
        progressStatus = (current_page * 100 / totalPages);
        notificationUpdateProgress(progressStatus);

        Log.i(TAG, "Page " + current_page + " downloaded, total " + totalPages + " pages");

        TaxonDb[] final_taxa = new TaxonDb[taxaData.size()];
        for (int i = 0; i < taxaData.size(); i++) {
            TaxaDataBirdloger taxon = taxaData.get(i);
            Long taxon_id = taxon.getId();
            String taxon_latin_name = taxon.getName();
            // Log.d(TAG, "Adding taxon " + taxon_latin_name + " with ID: " + taxon_id);

            List<TaxaStages> stages = taxon.getStages();
            StageDb[] final_stages = new StageDb[stages.size()];
            for (int j = 0; j < stages.size(); j++) {
                TaxaStages stage = stages.get(j);
                final_stages[j] = new StageDb(0, stage.getName(), stage.getId(), taxon_id);
            }
            ObjectBox.get().boxFor(StageDb.class).put(final_stages);
            //App.get().getDaoSession().getStageDao().insertOrReplaceInTx(final_stages);

            List<TaxaTranslations> taxaTranslations = taxon.getTaxaTranslations();

            // Write taxon data in SQL database
            final_taxa[i] = new TaxonDb(
                    taxon_id,
                    taxon.getParentId(),
                    taxon_latin_name,
                    taxon.getRank(),
                    taxon.getRankLevel(),
                    taxon.getAuthor(),
                    !taxon.getRestricted().equals("0"),
                    taxon.isUses_atlas_codes(),
                    taxon.getAncestors_names(),
                    "Birds");
            Log.d(TAG, "Saving taxon " + taxon_id + ": " + taxon_latin_name + "(group " + taxon.getGroups() + ")");

            // If there are translations save them in different table
            if (!taxaTranslations.isEmpty()) {
                TaxaTranslationDb[] final_translations = new TaxaTranslationDb[taxaTranslations.size()];
                for (int k = 0; k < taxaTranslations.size(); k++) {
                    TaxaTranslations taxaTranslation = taxaTranslations.get(k);
                    final_translations[k] = new TaxaTranslationDb(
                            taxaTranslation.getId(),
                            taxon_id,
                            taxaTranslation.getLocale(),
                            taxaTranslation.getNativeName(),
                            taxon_latin_name,
                            taxon.isUses_atlas_codes(),
                            taxaTranslation.getDescription());
                    Log.d(TAG, "Saving taxon translation " + taxaTranslation.getId() + ": " + taxon_latin_name +
                            " (" + taxaTranslation.getLocale() + ": " + taxaTranslation.getNativeName() + taxaTranslation.getDescription() + ")");
                }
                ObjectBox.get().boxFor(TaxaTranslationDb.class).put(final_translations);
                //App.get().getDaoSession().getTaxaTranslationDataDao().insertOrReplaceInTx(final_translations);
            }
        }
        ObjectBox.get().boxFor(TaxonDb.class).put(final_taxa);
        //App.get().getDaoSession().getTaxonDataDao().insertOrReplaceInTx(final_taxa);

        // If we just finished fetching taxa data for the last page, we can stop showing
        // loader. Otherwise we continue fetching taxa from the API on the next page.
        if (isLastPage(current_page)) {
            updateNotification(getString(R.string.notify_title_taxa_updated), getString(R.string.notify_desc_taxa_updated), null, null);
            // Inform the user of success
            Log.i(TAG, "All taxa were successfully updated from the server!");
            sendResult("fetched");
            // Set the preference to know when the taxonomic data was updates
            SettingsManager.setTaxaUpdatedAt(system_time);
            SettingsManager.setTaxaLastPageFetched("1");
            SettingsManager.setSkipTaxaDatabaseUpdate("0");
            stopSelf();
        } else {
            current_page++;
            Log.d(TAG, "Incrementing the current page to " + current_page);
            SettingsManager.setTaxaLastPageFetched(String.valueOf(current_page));
            fetchTaxa();
        }

    }

    // Stop the foreground service and update the notification
    private void updateNotification(String title, String description, String buttonTitle, Integer progressStatus) {
        if (progressStatus != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationResumeFetchButton(progressStatus, title, description, buttonTitle);
                stopForeground(STOP_FOREGROUND_DETACH);
            } else {
                stopForeground(true);
                notificationResumeFetchButton(progressStatus, title, description, buttonTitle);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationUpdateText(title, description);
                stopForeground(STOP_FOREGROUND_DETACH);
            } else {
                stopForeground(true);
                notificationUpdateText(title, description);
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationInitiate() {
        // Start the fetching and display notification
        Log.i(TAG, "Service for fetching taxa started.");

        // Create initial notification to be set to Foreground
        Intent intent = new Intent(this, LandingActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "biologer_taxa")
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(getString(R.string.notify_title_taxa))
                .setContentText(getString(R.string.notify_desc_taxa))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false);

        Notification notification = mBuilder.build();
        startForeground(1, notification);

        fetchTaxa();

    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationUpdateProgress(int progressStatus) {
        // To do something if notification is taped, we must set up an intent
        Intent intent = new Intent(this, LandingActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        // Add Pause button intent in notification.
        Intent pauseIntent = new Intent(this, FetchTaxa.class);
        pauseIntent.setAction(ACTION_PAUSE);
        PendingIntent pendingPauseIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingPauseIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingPauseIntent = PendingIntent.getService(this, 0, pauseIntent, 0);
        }
        NotificationCompat.Action pauseAction = new NotificationCompat.Action(android.R.drawable.ic_media_pause, getString(R.string.pause_action), pendingPauseIntent);

        // Add Cancel button intent in notification.
        Intent cancelIntent = new Intent(this, FetchTaxa.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent pendingCancelIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, 0);
        }
        NotificationCompat.Action cancelAction = new NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.cancel), pendingCancelIntent);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "biologer_taxa")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(getString(R.string.notify_title_taxa))
                .setContentText(getString(R.string.notify_desc_taxa))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setProgress(100, progressStatus, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .addAction(pauseAction)
                .addAction(cancelAction);

        Notification notification = mBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        mNotificationManager.notify(1, notification);
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationResumeFetchButton(int progressStatus, String title, String description, String resume) {
        // To do something if notification is taped, we must set up an intent
        Intent intent = new Intent(this, LandingActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        // Add Resume button intent in notification.
        Intent resumeIntent = new Intent(this, FetchTaxa.class);
        resumeIntent.setAction(ACTION_RESUME);
        PendingIntent pendingResumeIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingResumeIntent = PendingIntent.getService(this, 0, resumeIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingResumeIntent = PendingIntent.getService(this, 0, resumeIntent, 0);
        }
        NotificationCompat.Action resumeAction = new NotificationCompat.Action(android.R.drawable.ic_media_play, resume, pendingResumeIntent);

        // Add Cancel button intent in notification.
        Intent cancelIntent = new Intent(this, FetchTaxa.class);
        cancelIntent.setAction(ACTION_CANCEL_PAUSED);
        PendingIntent pendingCancelIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, 0);
        }
        NotificationCompat.Action cancelAction = new NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.cancel), pendingCancelIntent);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "biologer_taxa")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setProgress(100, progressStatus, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .addAction(resumeAction)
                .addAction(cancelAction);

        Notification notification = mBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        mNotificationManager.notify(1, notification);
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationUpdateText(String title, String description) {
        // To do something if notification is taped, we must set up an intent
        Intent intent = new Intent(this, LandingActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "biologer_taxa")
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true);

        Notification notification = mBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        mNotificationManager.notify(1, notification);
    }

    private boolean isLastPage(int page) {
        return page == totalPages;
    }

    // to check if the service is still running
    public static boolean isInstanceCreated() {
        return instance != null;
    }

    private void cleanDatabase() {
        SettingsManager.setTaxaUpdatedAt("0");
        updated_at = 0;
        current_page = 1;
        SettingsManager.setTaxaLastPageFetched("1");
        ObjectBox.get().boxFor(TaxonDb.class).removeAll();
        ObjectBox.get().boxFor(StageDb.class).removeAll();
    }

    public void sendResult(String message) {
        Log.d(TAG, "Sending the result to broadcaster! Message: " + message + ".");
        Intent intent = new Intent(TASK_COMPLETED);
        if(message != null)
            intent.putExtra(TASK_COMPLETED, message);
        broadcaster.sendBroadcast(intent);
    }

    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "Running onDestroy().");
    }
}