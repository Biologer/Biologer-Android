package org.biologer.biologer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;import android.util.Log;

import org.biologer.biologer.model.RetrofitClient;
import org.biologer.biologer.model.greendao.Stage;
import org.biologer.biologer.model.greendao.TaxonData;
import org.biologer.biologer.model.network.Stage6;
import org.biologer.biologer.model.network.TaksoniResponse;
import org.biologer.biologer.model.network.Taxa;
import org.biologer.biologer.model.network.TaxaTranslations;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class FetchTaxa extends Service {

    private static final String TAG = "Biologer.FetchTaxa";

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_START_NEW = "ACTION_START_NEW";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_CANCEL = "ACTION_CANCEL";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    private String stop_fetching = "no";
    private static FetchTaxa instance = null;

    private int totalPages = 0;
    private static int updated_after = Integer.parseInt(SettingsManager.getTaxaDatabaseUpdated());
    private static int last_page = Integer.parseInt(SettingsManager.getTaxaLastPageFetched());
    private static int progressStatus = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
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
                        updated_after = Integer.parseInt(SettingsManager.getTaxaDatabaseUpdated());
                        stop_fetching = "no";
                        // Start the service
                        notificationInitiate();
                        break;
                    case ACTION_START_NEW:
                        Log.i(TAG, "Action start from first page selected, starting foreground service.");
                        // Clean previous data just in case
                        cleanDatabase();
                        stop_fetching = "no";
                        // Start the service
                        notificationInitiate();
                        break;
                    case ACTION_PAUSE:
                        Log.i(TAG, "Action pause selected, pausing foreground service.");
                        SettingsManager.setTaxaLastPageFetched(String.valueOf(last_page));
                        stop_fetching = "pause";
                        stopForeground(true);
                        break;
                    case ACTION_CANCEL:
                        // If paused we have to kill the Service, else we continue with the loop and the
                        // service will be killed after fetching the current page...
                        if(stop_fetching.equals("no")) {
                            Log.i(TAG, "Action cancel selected, killing the paused foreground service.");
                            stop_fetching = "cancel";
                            stopForeground(true);
                            notificationUpdateText(getString(R.string.notify_title_taxa_canceled), getString(R.string.notify_desc_taxa_canceled));
                            SettingsManager.setTaxaLastPageFetched(String.valueOf(last_page));
                            stopSelf();
                        } else {
                            Log.i(TAG, "Action cancel selected, killing the running foreground service.");
                            stop_fetching = "cancel";
                            stopForeground(true);
                        }
                        break;
                    case ACTION_RESUME:
                        Log.i(TAG, "Action resume selected, continuing the foreground service.");
                        stop_fetching = "no";
                        notificationInitiate();
                        break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "Running onDestroy(). Last page fetched was " + last_page + " out of " + totalPages + " total pages.");
    }

    private void cleanDatabase() {
        SettingsManager.setTaxaDatabaseUpdated("0");
        updated_after = 0;
        last_page = 1;
        SettingsManager.setTaxaLastPageFetched("1");
        App.get().getDaoSession().getTaxonDataDao().deleteAll();
        App.get().getDaoSession().getStageDao().deleteAll();
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

        if (updated_after != 0) {
            last_page = 1;
        }
        if (last_page == 1) {
            fetchTaxa(last_page);
            Log.d(TAG, "Fetching taxa from the page 1.");
        } else {
            fetchTaxa(last_page + 1);
            Log.d(TAG, "Fetching taxa from the page " + (last_page + 1));
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

    private void notificationResumeFetchButton(int progressStatus) {
        // To do something if notification is taped, we must set up an intent
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Add Resume button intent in notification.
        Intent resumeIntent = new Intent(this, FetchTaxa.class);
        resumeIntent.setAction(ACTION_RESUME);
        PendingIntent pendingResumeIntent = PendingIntent.getService(this, 0, resumeIntent, 0);
        NotificationCompat.Action resumeAction = new NotificationCompat.Action(android.R.drawable.ic_media_play, getString(R.string.resume_action), pendingResumeIntent);

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

    public void fetchTaxa(final int page) {
            Call<TaksoniResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).getTaxa(page, 100, updated_after);
            call.enqueue(new CallbackWithRetry<TaksoniResponse>(call) {
                @Override
                public void onResponse(@NonNull Call<TaksoniResponse> call, @NonNull Response<TaksoniResponse> response) {
                    if (response.isSuccessful()) {
                        TaksoniResponse taksoniResponse = response.body();
                        // Fetch the next page of data
                        if (stop_fetching.equals("no")) {
                            assert taksoniResponse != null;
                            if (totalPages == 0) {
                                totalPages = taksoniResponse.getMeta().getLastPage();
                            }
                            List<Taxa> taxa = taksoniResponse.getData();

                            // Variables used to update the Progress Bar status
                            progressStatus = (page * 100 / totalPages);
                            last_page = page;
                            notificationUpdateProgress(progressStatus);

                            Log.i(TAG, "Fetching page " + page + " of " + totalPages + " total pages");

                            for (Taxa taxon : taxa) {
                                Long taxon_id = taxon.getId();
                                String taxon_latin_name = taxon.getName();
                                // Log.d(TAG, "Adding taxon " + taxon_name + " with ID: " + taxon_id);

                                List<Stage6> stages = taxon.getStages();
                                Stage[] final_stages = new Stage[stages.size()];
                                for (int i = 0; i < stages.size(); i++) {
                                    Stage6 stage = stages.get(i);
                                    final_stages[i] = new Stage(null, stage.getName(), stage.getId(), taxon_id);
                                }
                                App.get().getDaoSession().getStageDao().insertInTx(final_stages);

                                List<TaxaTranslations> taxaTranslations = taxon.getTaxaTranslations();
                                TaxonData[] final_translations = new TaxonData[taxaTranslations.size()];
                                for (int i = 0; i < taxaTranslations.size(); i++) {
                                    TaxaTranslations taxaTranslation = taxaTranslations.get(i);
                                    final_translations[i] = new TaxonData(
                                            null,
                                            taxon_id,
                                            taxon_latin_name,
                                            taxaTranslation.getLocale(),
                                            taxaTranslation.getNativeName());
                                    Log.d(TAG, "Taxon translation_id: " + taxaTranslation.getId() + ", id: "+ taxon_id + ", name: " + taxon_latin_name + ", locale: " +taxaTranslation.getLocale() + ", native name: " + taxaTranslation.getNativeName());
                                }
                                App.get().getDaoSession().getTaxonDataDao().insertInTx(final_translations);
                            }

                            // If we just finished fetching taxa data for the last page, we can stop showing
                            // loader. Otherwise we continue fetching taxa from the API on the next page.
                            if (isLastPage(page)) {
                                // Inform the user of success
                                Log.i(TAG, "All taxa were successfully updated from the server!");
                                // Stop the foreground service and update notification
                                stopForeground(true);
                                notificationUpdateText(getString(R.string.notify_title_taxa_updated), getString(R.string.notify_desc_taxa_updated));
                                // Set the preference to know when the taxonomic data was updates
                                SettingsManager.setTaxaDatabaseUpdated(Long.toString(taksoniResponse.getMeta().getLastUpdatedAt()));
                                SettingsManager.setTaxaLastPageFetched(String.valueOf(last_page));
                                stopSelf();
                            } else {
                                fetchTaxa(last_page + 1);
                            }
                        }

                        // If user selected pause or cancel we will stop the script
                        if (stop_fetching.equals("pause")) {
                            Log.d(TAG, "Fetching of taxa data is paused by the user!");
                            notificationResumeFetchButton(progressStatus);
                            stopSelf();
                        }

                        if (stop_fetching.equals("cancel")) {
                            Log.d(TAG, "Fetching of taxa data is canceled by the user!");
                            notificationUpdateText(getString(R.string.notify_title_taxa_canceled), getString(R.string.notify_desc_taxa_canceled));
                            stopSelf();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<TaksoniResponse> call, @NonNull Throwable t) {
                    // Remove partially retrieved data from the database
                    // cleanDatabase();
                    // Inform the user on failure and write log message
                    Log.e(TAG, "Application could not get data from a server!");
                    notificationUpdateText(getString(R.string.notify_title_taxa_failed), getString(R.string.notify_desc_taxa_failed));
                    stopSelf();
                }
            });
        }

    private boolean isLastPage(int page) {
        return page == totalPages;
    }

    public static int getProgressStatus() {
        return progressStatus;
    }

    // to check if the service is still running
    public static boolean isInstanceCreated() {
        return instance != null;
    }
}