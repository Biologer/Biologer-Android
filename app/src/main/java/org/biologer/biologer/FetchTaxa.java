package org.biologer.biologer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.biologer.biologer.gui.SplashActivity;
import org.biologer.biologer.network.JSON.TaxaStages;
import org.biologer.biologer.network.JSON.TaxaResponse;
import org.biologer.biologer.network.JSON.Taxa;
import org.biologer.biologer.network.JSON.TaxaTranslations;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.Stage;
import org.biologer.biologer.sql.TaxonData;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FetchTaxa extends Service {

    private static final String TAG = "Biologer.FetchTaxa";

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_START_NEW = "ACTION_START_NEW";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_CANCEL = "ACTION_CANCEL";
    public static final String ACTION_CANCEL_PAUSED = "ACTION_CANCEL_PAUSED";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    private String stop_fetching;
    private static FetchTaxa instance = null;
    static final String TASK_COMPLETED = "org.biologer.biologer.FetchTaxa.TASK_COMPLETED";
    int retry_number = 1;

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
                        stopForeground(true);
                        notificationUpdateText(getString(R.string.notify_title_taxa_canceled), getString(R.string.notify_desc_taxa_canceled));
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

    private void notificationInitiate() {
        // Start the fetching and display notification
        Log.i(TAG, "Service for fetching taxa started.");

        // Create initial notification to be set to Foreground
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

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

    public void fetchTaxa() {
        // If user selected pause or cancel we will stop the script
        switch (stop_fetching) {
            case "pause":
                Log.d(TAG, "Fetching of taxa data is paused by the user!");
                sendResult("paused");
                stopForeground(true);
                notificationResumeFetchButton(progressStatus, getString(R.string.notify_title_taxa), getString(R.string.notify_desc_taxa), getString(R.string.resume_action));
                stopSelf();
                break;
            case "cancel":
                Log.d(TAG, "Fetching of taxa data is canceled by the user!");
                sendResult("canceled");
                stopForeground(true);
                notificationUpdateText(getString(R.string.notify_title_taxa_canceled), getString(R.string.notify_desc_taxa_canceled));
                stopSelf();
                break;
            case "keep_going":

                Call<TaxaResponse> call = RetrofitClient.getService(
                        SettingsManager.getDatabaseName()).getTaxa(current_page, 200, updated_at);
                call.enqueue(new Callback<TaxaResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<TaxaResponse> call, @NonNull Response<TaxaResponse> response) {
                        if (response.isSuccessful()) {
                            TaxaResponse taxaResponse = response.body();
                            assert taxaResponse != null;
                            saveFetchedPage(taxaResponse);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TaxaResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Application could not get data from a server: " + t.getLocalizedMessage());

                        if (retry_number == 4) {
                            sendResult("failed");
                            stopForeground(true);
                            notificationResumeFetchButton(progressStatus, getString(R.string.notify_title_taxa_failed), getString(R.string.notify_desc_taxa_failed), getString(R.string.retry));
                            stopSelf();
                            Log.d(TAG, "Fetching taxa failed!");
                        }
                        else {
                                Log.d(TAG, "Starting retry loop No. " + retry_number + ".");
                                fetchTaxa();
                                retry_number++;
                        }
                        }
                });
        }
    }

    private void saveFetchedPage(TaxaResponse taxaResponse) {

        if (totalPages == 0) {
            totalPages = taxaResponse.getMeta().getLastPage();
        }
        List<Taxa> taxa = taxaResponse.getData();

        // Variables used to update the Progress Bar status
        progressStatus = (current_page * 100 / totalPages);
        notificationUpdateProgress(progressStatus);

        Log.i(TAG, "Page " + current_page + " downloaded, total " + totalPages + " pages");

        for (Taxa taxon : taxa) {
            long taxon_id = taxon.getId();
            String taxon_latin_name = taxon.getName();
            // Log.d(TAG, "Adding taxon " + taxon_name + " with ID: " + taxon_id);
            boolean use_atlas_code = taxon.isUses_atlas_codes();
            String ancestor_names = taxon.getAncestors_names();

            List<TaxaStages> stages = taxon.getStages();
            Stage[] final_stages = new Stage[stages.size()];
            for (int i = 0; i < stages.size(); i++) {
                TaxaStages stage = stages.get(i);
                final_stages[i] = new Stage(null, stage.getName(), stage.getId(), taxon_id);
            }
            App.get().getDaoSession().getStageDao().insertOrReplaceInTx(final_stages);

            List<TaxaTranslations> taxaTranslations = taxon.getTaxaTranslations();
            TaxonData[] final_translations = new TaxonData[taxaTranslations.size()];
            for (int i = 0; i < taxaTranslations.size(); i++) {
                TaxaTranslations taxaTranslation = taxaTranslations.get(i);
                final_translations[i] = new TaxonData(
                        null,
                        taxon_id,
                        taxon_latin_name,
                        use_atlas_code,
                        ancestor_names,
                        taxaTranslation.getLocale(),
                        taxaTranslation.getNativeName());
                //Log.d(TAG, "Taxon translation_id: " + taxaTranslation.getId() + ", id: "+ taxon_id + ", name: " + taxon_latin_name + ", locale: " +taxaTranslation.getLocale() + ", native name: " + taxaTranslation.getNativeName());
            }
            App.get().getDaoSession().getTaxonDataDao().insertOrReplaceInTx(final_translations);
        }

        // If we just finished fetching taxa data for the last page, we can stop showing
        // loader. Otherwise we continue fetching taxa from the API on the next page.
        if (isLastPage(current_page)) {
            // Inform the user of success
            Log.i(TAG, "All taxa were successfully updated from the server!");
            sendResult("fetched");
            // Stop the foreground service and update notification
            stopForeground(true);
            notificationUpdateText(getString(R.string.notify_title_taxa_updated), getString(R.string.notify_desc_taxa_updated));
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

    private void notificationUpdateProgress(int progressStatus) {
        // To do something if notification is taped, we must set up an intent
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Add Pause button intent in notification.
        Intent pauseIntent = new Intent(this, FetchTaxa.class);
        pauseIntent.setAction(ACTION_PAUSE);
        PendingIntent pendingPauseIntent = PendingIntent.getService(this, 0, pauseIntent, 0);
        NotificationCompat.Action pauseAction = new NotificationCompat.Action(android.R.drawable.ic_media_pause, getString(R.string.pause_action), pendingPauseIntent);

        // Add Cancel button intent in notification.
        Intent cancelIntent = new Intent(this, FetchTaxa.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, 0);
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

    private void notificationResumeFetchButton(int progressStatus, String title, String description, String resume) {
        // To do something if notification is taped, we must set up an intent
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Add Resume button intent in notification.
        Intent resumeIntent = new Intent(this, FetchTaxa.class);
        resumeIntent.setAction(ACTION_RESUME);
        PendingIntent pendingResumeIntent = PendingIntent.getService(this, 0, resumeIntent, 0);
        NotificationCompat.Action resumeAction = new NotificationCompat.Action(android.R.drawable.ic_media_play, resume, pendingResumeIntent);

        // Add Cancel button intent in notification.
        Intent cancelIntent = new Intent(this, FetchTaxa.class);
        cancelIntent.setAction(ACTION_CANCEL_PAUSED);
        PendingIntent pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, 0);
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

    private void notificationUpdateText(String title, String description) {
        // To do something if notification is taped, we must set up an intent
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

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
        App.get().getDaoSession().getTaxonDataDao().deleteAll();
        App.get().getDaoSession().getStageDao().deleteAll();
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