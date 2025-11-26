package org.biologer.biologer.network;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import org.biologer.biologer.network.json.TaxaSynonym;
import org.biologer.biologer.network.json.TaxaTranslations;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.SynonymsDb;
import org.biologer.biologer.sql.SynonymsDb_;
import org.biologer.biologer.sql.TaxaTranslationDb;
import org.biologer.biologer.sql.TaxaTranslationDb_;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonGroupsDb;
import org.biologer.biologer.sql.TaxonGroupsDb_;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private int totalPagesExpected = -1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // Use a single thread to serialize DB updates.

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
                }
            }

        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void getTaxa(int page) {

        Call<TaxaResponse> call = RetrofitClient.getService(
                SettingsManager.getDatabaseName()).getTaxa(
                page,
                300,
                Integer.parseInt(updated_after),
                true,
                taxa_groups_int,
                false
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<TaxaResponse> call, @NonNull Response<TaxaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveFetchedPage(response.body(), page);
                } else if (response.code() == 429) {
                    // Handle rate limiting with exponential backoff
                    long retryAfter = 5000; // default 5s
                    String retryAfterHeader = response.headers().get("retry-after");
                    if (retryAfterHeader != null) {
                        try {
                            retryAfter = Math.min(Long.parseLong(retryAfterHeader) * 1000, 60000);
                        } catch (NumberFormatException ignored) {}
                    }
                    Log.w(TAG, "Rate limit hit, retrying in " + retryAfter / 1000 + "s");

                    new Handler(Looper.getMainLooper()).postDelayed(() -> getTaxa(page), retryAfter);
                } else if (response.code() == 508) {
                    Log.w(TAG, "Server detected a loop, retrying in 5s.");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> getTaxa(page), 5000);
                } else {
                    Log.e(TAG, "Unexpected response code: " + response.code());
                    broadcastResult("failed");
                    stopSelf();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TaxaResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error while fetching taxa: " + t.getLocalizedMessage());
                broadcastResult("failed");
                stopSelf();
            }
        });
    }

    private void saveFetchedPage(@NonNull TaxaResponse taxaResponse, int page) {
        // Get data from server response
        int total = taxaResponse.getMeta().getTotal();
        int perPage = taxaResponse.getMeta().getPerPage();

        if (totalPagesExpected < 0 && total > 0 && perPage > 0) {
            totalPagesExpected = (int) Math.ceil((double) total / perPage);
            Log.d(TAG, "Expected total pages: " + totalPagesExpected);
        }

        List<TaxaData> taxaData = taxaResponse.getData();

        // Save if there is some data
        if (taxaData != null && !taxaData.isEmpty()) {
            // Offload the database work to a background thread.
            executor.execute(() -> {
                saveTaxaToDatabase(taxaData);

                // Continue when the work is done.
                new Handler(Looper.getMainLooper()).post(() -> continueTaxaDownload(taxaResponse, page));
            });

        } else {
            continueTaxaDownload(taxaResponse, page);
        }
    }

    private void continueTaxaDownload(@NonNull TaxaResponse taxaResponse, int page) {
        int lastPage = taxaResponse.getMeta().getLastPage();

        // Compute and report progress
        int denominator = totalPagesExpected > 0 ? totalPagesExpected : lastPage;
        if (denominator <= 0) denominator = Math.max(page, 1);
        int percent = Math.min((page * 100) / denominator, 100);
        broadcastPercent(percent);
        Log.i(TAG, "Page " + page + " downloaded, progress " + percent + "%");

        broadcastPercent(percent);
        Log.i(TAG, "Page " + page + " downloaded, progress " + percent + "%");

        boolean noMoreData = (taxaResponse.getData() == null || taxaResponse.getData().isEmpty());
        boolean lastPageReached = (page >= lastPage && lastPage > 0);

        if (noMoreData || lastPageReached) {
            Log.i(TAG, "All taxa successfully updated from server.");
            broadcastResult("success");
            SettingsManager.setTaxaUpdatedAt(timestamp);
            SettingsManager.setTaxaLastPageFetched("1");
            stopSelf();
        } else {
            int nextPage = page + 1;
            SettingsManager.setTaxaLastPageFetched(String.valueOf(nextPage));
            Log.d(TAG, "Downloading taxa from page " + nextPage);
            getTaxa(nextPage);
        }
    }

    private void saveTaxaToDatabase(@NonNull List<TaxaData> taxaData) {
        Box<TaxonDb> taxonBox = App.get().getBoxStore().boxFor(TaxonDb.class);
        Box<StageDb> stageBox = App.get().getBoxStore().boxFor(StageDb.class);
        Box<TaxaTranslationDb> translationBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
        Box<SynonymsDb> synonymBox = App.get().getBoxStore().boxFor(SynonymsDb.class);

        for (TaxaData taxon : taxaData) {
            List<TaxaStages> stages = taxon.getStages();
            if (stages != null && !stages.isEmpty()) {
                StageDb[] stageArray = new StageDb[stages.size()];
                StringBuilder stageString = new StringBuilder();
                for (int i = 0; i < stages.size(); i++) {
                    TaxaStages stage = stages.get(i);
                    stageArray[i] = new StageDb(stage.getId(), stage.getName());
                    stageString.append(stage.getId()).append(";");
                }
                stageBox.put(stageArray);
            }

            TaxonDb taxonDb = getTaxonDb(taxon);
            taxonBox.put(taxonDb);

            List<TaxaTranslations> translations = taxon.getTaxaTranslations();
            if (translations != null && !translations.isEmpty()) {
                // Remove old translation for this ID if already exists
                try (Query<TaxaTranslationDb> query = translationBox.query()
                        .equal(TaxaTranslationDb_.taxonId, taxon.getId())
                        .build()) {
                    query.remove();
                }

                // Add the translation to the ObjectBox
                TaxaTranslationDb[] translationArray = new TaxaTranslationDb[translations.size()];
                for (int i = 0; i < translations.size(); i++) {
                    TaxaTranslations tr = translations.get(i);
                    translationArray[i] = new TaxaTranslationDb(
                            tr.getId(),
                            taxon.getId(),
                            tr.getLocale(),
                            tr.getNativeName(),
                            taxon.getName(),
                            tr.getDescription()
                    );
                }
                translationBox.put(translationArray);
            }

            List<TaxaSynonym> synonyms = taxon.getSynonyms();
            if (synonyms != null && !synonyms.isEmpty()) {
                // Remove old synonym for this ID if already exists
                try (Query<SynonymsDb> query = synonymBox.query()
                        .equal(SynonymsDb_.taxonId, taxon.getId())
                        .build()) {
                    query.remove();
                }

                // Add synonym to ObjectBox
                List<SynonymsDb> synonymDbList = new ArrayList<>(synonyms.size());
                for (TaxaSynonym syn : synonyms) {
                    synonymDbList.add(new SynonymsDb(0, taxon.getId(), syn.getName()));
                }
                synonymBox.put(synonymDbList);
            }

        }
    }

    @NonNull
    private static TaxonDb getTaxonDb(TaxaData taxon) {
        StringBuilder groupString = new StringBuilder();
        for (String group : taxon.getGroups()) {
            groupString.append(group).append(";");
        }

        return new TaxonDb(
                taxon.getId(),
                taxon.getParentId(),
                taxon.getName(),
                taxon.getRank(),
                taxon.getRankLevel(),
                taxon.getAuthor(),
                taxon.isRestricted(),
                taxon.isUses_atlas_codes(),
                taxon.getAncestors_names(),
                groupString.toString(),
                ""
        );
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
