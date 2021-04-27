package org.biologer.biologer.adapters;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.JSON.TaxaGroupsResponse;
import org.biologer.biologer.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetTaxaGroups extends Service {

    private static final String TAG = "Biologer.GetTaxaGroups";

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
        if (action != null) {
            Call<TaxaGroupsResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).getTaxaGroupsResponse();
            call.enqueue(new Callback<TaxaGroupsResponse>() {
                @Override
                public void onResponse(@NonNull Call<TaxaGroupsResponse> call, @NonNull Response<TaxaGroupsResponse> response) {
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            Log.d(TAG, "Successful TaxaGroups response.");
                            TaxaGroupsResponse taxaGroupsResponse = response.body();
                            Log.d(TAG, "Test taxa" + taxaGroupsResponse.getData().get(0).getName());
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<TaxaGroupsResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Failed to get Taxa Groups from server " + t);
                }
            });
        }
        return super.onStartCommand(intent, flags, startId);
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
