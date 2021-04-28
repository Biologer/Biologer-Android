package org.biologer.biologer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.biologer.biologer.network.JSON.TaxaGroups;
import org.biologer.biologer.network.JSON.TaxaGroupsResponse;
import org.biologer.biologer.network.JSON.TaxaGroupsTranslations;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.TaxaTranslationData;
import org.biologer.biologer.sql.TaxonGroupsData;
import org.biologer.biologer.sql.TaxonGroupsTranslationData;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetTaxaGroups extends Service {

    private static final String TAG = "Biologer.GetTaxaGroups";
    public static final String ACTION_START = "ACTION_START";

    private static GetTaxaGroups instance = null;
    static final String TASK_COMPLETED = "org.biologer.biologer.GetTaxaGroups.TASK_COMPLETED";

    LocalBroadcastManager broadcaster;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        broadcaster = LocalBroadcastManager.getInstance(this);
        Log.d(TAG, "Running onCreate()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.d(TAG, "Fetching taxa groups started!");

        if (networkAvailable()) {
            if (action != null) {
                Call<TaxaGroupsResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).getTaxaGroupsResponse();
                call.enqueue(new Callback<TaxaGroupsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TaxaGroupsResponse> call, @NonNull Response<TaxaGroupsResponse> response) {
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                Log.d(TAG, "Successful TaxaGroups response.");
                                TaxaGroupsResponse taxaGroupsResponse = response.body();
                                saveTaxaGroups(taxaGroupsResponse);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TaxaGroupsResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Failed to get Taxa Groups from server " + t);
                    }
                });
            }
        } else {
            sendResult("no_network");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private boolean networkAvailable() {
         ConnectivityManager connectivitymanager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
         if (connectivitymanager != null) {
             NetworkInfo activeNetworkInfo = connectivitymanager.getActiveNetworkInfo();
             if (activeNetworkInfo != null) {
                 activeNetworkInfo.isConnected();
                 Log.d(TAG, "You are connected to the network.");
                 return true;
             }
         }
         Log.d(TAG, "You are NOT connected to the network.");
         return false;
    }

    private void saveTaxaGroups(TaxaGroupsResponse taxaGroupsResponse) {
        Log.d(TAG, "Test taxa" + taxaGroupsResponse.getData().get(0).getName());
        List<TaxaGroups> taxaGroups = taxaGroupsResponse.getData();

        TaxonGroupsData[] taxon_groups = new TaxonGroupsData[taxaGroups.size()];
        for (int i = 0; i < taxaGroups.size(); i++) {
            TaxaGroups taxon = taxaGroups.get(i);
            Long taxon_id = taxon.getId();
            Long taxon_parent_id = taxon.getParentId();
            String taxon_name = taxon.getName();
            String taxon_description = taxon.getDescription();
            List<TaxaGroupsTranslations> taxon_translations = taxon.getTranslations();

            taxon_groups[i] = new TaxonGroupsData(
                    taxon_id,
                    taxon_parent_id,
                    taxon_name,
                    taxon_description);

            Log.d(TAG, "Taxon group " + i + ": id = " + taxon_id + ", name = " + taxon_name);
            sendResult("done");

            if (taxon_translations.size() > 0) {
                TaxonGroupsTranslationData[] taxa_groups_translation = new TaxonGroupsTranslationData[taxon_translations.size()];
                for (int k = 0; k < taxon_translations.size(); k++) {
                    TaxaGroupsTranslations taxon_translation = taxon_translations.get(k);
                    if (taxon_translation != null) {
                        Long translation_id = taxon_translation.getId();
                        String locale = taxon_translation.getLocale();
                        String name = taxon_translation.getName();
                        String description = taxon_translation.getDescription();

                        taxa_groups_translation[k] = new TaxonGroupsTranslationData(
                                taxon_id,
                                translation_id,
                                locale,
                                name,
                                description);
                    }
                }
                App.get().getDaoSession().getTaxonGroupsTranslationDataDao().insertOrReplaceInTx(taxa_groups_translation);
            }
        }
        App.get().getDaoSession().getTaxonGroupsDataDao().insertOrReplaceInTx(taxon_groups);

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
