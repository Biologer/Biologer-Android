package org.biologer.biologer.network;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.json.TaxaData;
import org.biologer.biologer.network.json.TaxaResponse;
import org.biologer.biologer.network.json.TaxaStages;
import org.biologer.biologer.network.json.TaxaTranslations;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.TaxaTranslationDb;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonGroupsDb;
import org.biologer.biologer.sql.TaxonGroupsDb_;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.query.Query;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateTaxa extends Service {

    private static final String TAG = "Biologer.UpdateTaxa";
    static final public String TASK_COMPLETED = "org.biologer.biologer.UpdateTaxa.TASK_COMPLETED";
    static final public String EXTRA_TASK_COMPLETED = "org.biologer.biologer.UpdateTaxa.EXTRA_TASK_COMPLETED";
    static final public String TASK_PERCENT = "org.biologer.biologer.UpdateTaxa.TASK_PERCENT";
    static final public String EXTRA_TASK_PERCENT = "org.biologer.biologer.UpdateTaxa.EXTRA_TASK_PERCENT";
    public static final String ACTION_DOWNLOAD_FROM_FIRST = "ACTION_DOWNLOAD_FROM_FIRST";
    public static final String ACTION_DOWNLOAD = "ACTION_DOWNLOAD";
    public static final String ACTION_STOP = "ACTION_STOP";
    LocalBroadcastManager broadcastManager;
    private static UpdateTaxa instance = null;
    ArrayList<String> taxa_groups = new ArrayList<>();
    int[] taxa_groups_int;
    String timestamp;
    String updated_after;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        broadcastManager = LocalBroadcastManager.getInstance(this);
        timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        Log.i(TAG, "UpdateTaxa service started.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            ArrayList<String> groups_list = intent.getStringArrayListExtra("groups");
            if (groups_list == null) {
                // Query to get taxa groups that should be used in a query
                Box<TaxonGroupsDb> taxonGroupsDataBox = App.get().getBoxStore().boxFor(TaxonGroupsDb.class);
                Query<TaxonGroupsDb> query = taxonGroupsDataBox
                        .query(TaxonGroupsDb_.id.notNull())
                        .build();
                List<TaxonGroupsDb> allTaxaGroups = query.find();
                query.close();
                for (int i = 0; i < allTaxaGroups.size(); i++) {
                    int id = (int) allTaxaGroups.get(i).getId();
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    boolean checked = preferences.getBoolean(String.valueOf(allTaxaGroups.get(i).getId()), true);
                    if (checked) {
                        // Log.d(TAG, "Checkbox for taxa group ID " + id + " is checked.");
                        taxa_groups.add(String.valueOf(id));
                    }
                }
            } else {
                taxa_groups = groups_list;
            }

            taxa_groups_int = new int[taxa_groups.size()];
            for (int i = 0; i < taxa_groups.size(); i++) {
                taxa_groups_int[i] = Integer.parseInt(taxa_groups.get(i));
            }

            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_DOWNLOAD_FROM_FIRST:
                        SettingsManager.setTaxaLastPageFetched("1");
                        SettingsManager.setTaxaUpdatedAt("0");
                        updated_after = "0";
                        getTaxa(1);
                        break;
                    case ACTION_DOWNLOAD:
                        updated_after = SettingsManager.getTaxaUpdatedAt();
                        int last_page_fetched = Integer.parseInt(SettingsManager.getTaxaLastPageFetched());
                        getTaxa(last_page_fetched);
                        break;
                    case ACTION_STOP:
                        stopSelf();
                        break;
                }
            }

        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void getTaxa(int page) {

        Call<TaxaResponse> call = RetrofitClient.getService(
                SettingsManager.getDatabaseName()).getTaxa(page, 300,
                Integer.parseInt(updated_after), true, taxa_groups_int, false);

        call.enqueue(new Callback<>() {

            @Override
            public void onResponse(@NonNull Call<TaxaResponse> call, @NonNull Response<TaxaResponse> response) {
                if (response.isSuccessful()) {
                    TaxaResponse taxaResponse = response.body();
                    if (taxaResponse != null) {
                        saveFetchedPage(taxaResponse, page);
                    }
                } else if (response.code() == 429) {
                    String retryAfter = response.headers().get("retry-after");
                    long sec = Long.parseLong(Objects.requireNonNull(retryAfter, "Header did not return number of seconds."));
                    Log.d(TAG, "Server resource limitation reached, retry after " + sec + " seconds.");
                    // Add handler to delay fetching
                    Handler handler = new Handler();
                    Runnable runnable = () -> getTaxa(page);
                    handler.postDelayed(runnable, sec * 1000);
                } else if (response.code() == 508) {
                    Log.d(TAG, "Server detected a loop, retrying in 5 sec.");
                    Handler handler = new Handler();
                    Runnable runnable = () -> getTaxa(page);
                    handler.postDelayed(runnable, 5000);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TaxaResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Application could not get data from a server: " + t.getLocalizedMessage());
                broadcastResult("failed");
                stopSelf();
            }
        });
    }


    private void saveFetchedPage(TaxaResponse taxaResponse, int page) {
        int last_page = taxaResponse.getMeta().getLastPage();
        int percent = (page * 100 ) / last_page;
        broadcastPercent(percent);
        Log.i(TAG, "Page " + page + " downloaded, total " + last_page + " pages (" + percent + "%).");

        List<TaxaData> taxaData = taxaResponse.getData();

        TaxonDb[] final_taxa = new TaxonDb[taxaData.size()];
        for (int i = 0; i < taxaData.size(); i++) {
            TaxaData taxon = taxaData.get(i);
            long taxon_id = taxon.getId();
            String taxon_latin_name = taxon.getName();
            //Log.d(TAG, "Adding taxon " + taxon_latin_name + " with ID: " + taxon_id);

            List<TaxaStages> stages = taxon.getStages();
            StageDb[] final_stages = new StageDb[stages.size()];
            StringBuilder stagesString = new StringBuilder();
            for (int j = 0; j < stages.size(); j++) {
                TaxaStages stage = stages.get(j);
                final_stages[j] = new StageDb(stage.getId(), stage.getName());
                stagesString.append(stage.getId()).append(";");
            }
            App.get().getBoxStore().boxFor(StageDb.class).put(final_stages);

            List<TaxaTranslations> taxaTranslations = taxon.getTaxaTranslations();

            StringBuilder groupString = new StringBuilder();
            for (String string: taxon.getGroups()) {
                groupString.append(string).append(";");
            }

            // Write taxon data in SQL database
            final_taxa[i] = new TaxonDb(
                    taxon_id,
                    taxon.getParentId(),
                    taxon_latin_name,
                    taxon.getRank(),
                    taxon.getRankLevel(),
                    taxon.getAuthor(),
                    taxon.isRestricted(),
                    taxon.isUses_atlas_codes(),
                    taxon.getAncestors_names(),
                    groupString.toString(),
                    stagesString.toString());

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
                            taxaTranslation.getDescription());
                }
                App.get().getBoxStore().boxFor(TaxaTranslationDb.class).put(final_translations);
            }
        }
        App.get().getBoxStore().boxFor(TaxonDb.class).put(final_taxa);

        // If we just finished fetching taxa data for the last page, we can stop showing
        // loader. Otherwise we continue fetching taxa from the API on the next page.
        if (page == last_page) {
            Log.i(TAG, "All taxa were successfully updated from the server!");
            broadcastResult("success");
            // Set the preference to know when the taxonomic data was updates
            SettingsManager.setTaxaUpdatedAt(timestamp);
            SettingsManager.setTaxaLastPageFetched("1");
            stopSelf();
        } else {
            SettingsManager.setTaxaLastPageFetched(String.valueOf(page));
            page++;
            Log.d(TAG, "Downloading taxa from page " + page);
            getTaxa(page);
        }
    }

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    public void broadcastResult(String message) {
        Intent intent = new Intent(TASK_COMPLETED);
        intent.putExtra(EXTRA_TASK_COMPLETED, message);
        broadcastManager.sendBroadcast(intent);
    }

    public void broadcastPercent(int percent) {
        Intent intent = new Intent(TASK_PERCENT);
        intent.putExtra(EXTRA_TASK_PERCENT, percent);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}
