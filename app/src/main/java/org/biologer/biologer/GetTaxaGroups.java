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
import org.biologer.biologer.sql.TaxonGroupsData;
import org.biologer.biologer.sql.TaxonGroupsTranslationData;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetTaxaGroups extends Service {

    private static final String TAG = "Biologer.GetTaxaGroups";

    static final String TASK_COMPLETED = "org.biologer.biologer.GetTaxaGroups.TASK_COMPLETED";

    LocalBroadcastManager broadcaster;

    @Override
    public void onCreate() {
        super.onCreate();
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
        Log.d(TAG, "Fetching taxa groups started!");

        ConnectivityManager connectivitymanager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivitymanager.getActiveNetworkInfo().isConnected()) {
            Log.d(TAG, "Network available.");
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
        } else {
            sendResult("no_network");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void saveTaxaGroups(TaxaGroupsResponse taxaGroupsResponse) {
        Log.d(TAG, "Saving taxa groups to SQL tables");
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

            // Log.d(TAG, "Taxon group " + i + ": id = " + taxon_id + ", name = " + taxon_name);

            TaxonGroupsTranslationData[] translationData = new TaxonGroupsTranslationData[taxon_translations.size()];
            for (int k = 0; k < taxon_translations.size(); k++) {
                TaxaGroupsTranslations taxon_translation = taxon_translations.get(k);
                Long translation_id = taxon_translation.getId();
                String locale = taxon_translation.getLocale();
                String name = taxon_translation.getName();
                String description = taxon_translation.getDescription();
                //Log.d(TAG, "Group ID: " + taxon_id + "; from translation: " + taxon_translation.getViewGroupId() + "; group name: " + taxon_name + "; locale ID: " + translation_id + "; locale: " + locale + "; translation: " + name);

                translationData[k] = new TaxonGroupsTranslationData(
                        translation_id,
                        taxon_id,
                        locale,
                        name,
                        description);
            }
            App.get().getDaoSession().getTaxonGroupsTranslationDataDao().insertOrReplaceInTx(translationData);
        }
        App.get().getDaoSession().getTaxonGroupsDataDao().insertOrReplaceInTx(taxon_groups);

        sendResult("done");

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
        Log.d(TAG, "Running onDestroy().");
    }
}
