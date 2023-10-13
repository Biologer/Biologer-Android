package org.biologer.biologer.network;

import android.util.Log;

import androidx.annotation.NonNull;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.json.ObservationTypesResponse;
import org.biologer.biologer.network.json.ObservationTypesTranslations;
import org.biologer.biologer.sql.ObservationTypesDb;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateObservationTypes {

    private static final String TAG = "Biologer.ObsTypes";

    public static void updateObservationTypes() {
        String system_time = String.valueOf(System.currentTimeMillis()/1000);
        String updated_at = SettingsManager.getObservationTypesUpdated();

        Call<ObservationTypesResponse> call = RetrofitClient.getService(
                SettingsManager.getDatabaseName()).getObservationTypes(Integer.parseInt(updated_at));
        call.enqueue(new Callback<ObservationTypesResponse>() {

            @Override
            public void onResponse(@NonNull Call<ObservationTypesResponse> call, @NonNull Response<ObservationTypesResponse> response) {
                ObservationTypesResponse observationsResponse = response.body();
                if (observationsResponse != null) {
                    if (observationsResponse.getData().length == 0) {
                        Log.d(TAG, "Recent observation types are already downloaded from server.");
                    } else {
                        org.biologer.biologer.network.json.ObservationTypes[] obs = observationsResponse.getData();
                        for (org.biologer.biologer.network.json.ObservationTypes ob : obs) {
                            Log.d(TAG, "Observation type ID: " + ob.getId() + "; Slug: " + ob.getSlug());

                            // Save translations in a separate table...
                            List<ObservationTypesTranslations> observation_translations = ob.getTranslations();
                            ObservationTypesDb[] localizations = new ObservationTypesDb[observation_translations.size()];
                            for (int j = 0; j < observation_translations.size(); j++) {
                                ObservationTypesDb localization = new ObservationTypesDb(
                                        0,
                                        observation_translations.get(j).getId(),
                                        ob.getId().longValue(),
                                        ob.getSlug(),
                                        observation_translations.get(j).getLocale(),
                                        observation_translations.get(j).getName());
                                localizations[j] = localization;
                            }
                            App.get().getBoxStore().boxFor(ObservationTypesDb.class).put(localizations);

                        }
                        Log.d(TAG, "Observation types locales written to the database, there are " + App.get().getBoxStore().boxFor(ObservationTypesDb.class).count() + " records");
                        SettingsManager.setObservationTypesUpdated(system_time);
                        Log.d(TAG, "Timestamp for observation time update is set to " + system_time);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ObservationTypesResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Observation types could not be retrieved from server: " + t.getLocalizedMessage());
            }
        });
    }
}
