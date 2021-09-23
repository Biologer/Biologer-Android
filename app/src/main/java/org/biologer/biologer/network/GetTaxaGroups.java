package org.biologer.biologer.network;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.JSON.TaxaGroups;
import org.biologer.biologer.network.JSON.TaxaGroupsResponse;
import org.biologer.biologer.network.JSON.TaxaGroupsTranslations;
import org.biologer.biologer.sql.TaxonGroupsData;
import org.biologer.biologer.sql.TaxonGroupsTranslationData;
import org.biologer.biologer.sql.TaxonGroupsTranslationDataDao;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetTaxaGroups extends Service {

    private static final String TAG = "Biologer.GetTaxaGroups";

    static final String TASK_COMPLETED = "org.biologer.biologer.network.GetTaxaGroups.TASK_COMPLETED";

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

        if (InternetConnection.isConnected(this)) {
            Log.d(TAG, "Network is available.");
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
            Log.d(TAG, "Network is not available.");
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

        // Get the IDs from server adn compare them to SQL. Delete SQL if deleted on server
        ArrayList<Long> server_ids = new ArrayList<>();
        for (int i = 0; i < taxaGroups.size(); i++) {
            server_ids.add(taxaGroups.get(i).getId());
        }
        ArrayList<Long> sql_ids = new ArrayList<>();
        List<TaxonGroupsData> sql_ids_list = App.get().getDaoSession().getTaxonGroupsDataDao().queryBuilder().list();
        for (int i = 0; i < sql_ids_list.size(); i++) {
            sql_ids.add(sql_ids_list.get(i).getId());
        }

        sql_ids.removeAll(server_ids);

        if (!sql_ids.isEmpty()) {
            Log.d(TAG, "Taxon IDs in SQL which are deleted from server" + sql_ids);

            for (int i = 0; i < sql_ids.size(); i++) {
                Long id = sql_ids.get(i);
                List<TaxonGroupsTranslationData> sql_tr = App.get().getDaoSession().getTaxonGroupsTranslationDataDao().
                        queryBuilder().where(TaxonGroupsTranslationDataDao.Properties.ViewGroupId.eq(id)).list();
                ArrayList<Long> ids = new ArrayList<>();
                for (int j = 0; j < sql_tr.size(); j++) {
                    ids.add(sql_tr.get(j).getId());
                }
                App.get().getDaoSession().getTaxonGroupsTranslationDataDao().deleteByKeyInTx(ids);
            }

            App.get().getDaoSession().getTaxonGroupsDataDao().deleteByKeyInTx(sql_ids);
        } else {
            Log.d(TAG, "SQL is up to date!");
        }

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
