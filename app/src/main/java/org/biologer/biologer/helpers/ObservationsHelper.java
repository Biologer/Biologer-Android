package org.biologer.biologer.helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.FieldObservationData;
import org.biologer.biologer.network.json.FieldObservationDataTypes;
import org.biologer.biologer.network.json.FieldObservationResponse;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.EntryDb_;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.objectbox.Box;
import io.objectbox.query.Query;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ObservationsHelper {
    private static final String TAG = "Biologer.ObsHelper";

    public static void fetchMyObservations(int page, String timestamp, ObservationPageCallback callback) {
        RetrofitClient.getService(SettingsManager.getDatabaseName())
                .getMyFieldObservations(page, 50, timestamp, "id", "desc")
                .enqueue(new Callback<FieldObservationResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            FieldObservationResponse body = response.body();
                            List<FieldObservationData> data = Arrays.asList(body.getData());

                            if (!data.isEmpty()) {
                                saveToLocalDatabase(data);

                                // If this is Page 1, we should capture the most recent
                                // timestamp to use for the NEXT sync session.
                                if (page == 1) {
                                    String timestamp = data.get(0).getActivity().get(0).getCreatedAt();
                                    SettingsManager.setFieldObservationsUpdatedAt(
                                            String.valueOf(
                                                    DateHelper.getMillisTimestampFromIsoString(timestamp)
                                            )
                                    );
                                }
                            }

                            boolean hasNext = body.getMeta().getCurrentPage() < body.getMeta().getLastPage();
                            callback.onSuccess(data, hasNext, body.getMeta().getLastPage());

                        } else {
                            callback.onError("HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<FieldObservationResponse> call, @NonNull Throwable t) {
                        callback.onError(t.getLocalizedMessage());
                    }
                });
    }

    public static void downloadAllMyObservations(int currentPage, SyncCallback syncCallback) {
        fetchMyObservations(currentPage, "0", new ObservationPageCallback() {
            @Override
            public void onSuccess(List<FieldObservationData> data, boolean hasNextPage, int totalPages) {
                syncCallback.onPageDownloaded(currentPage, totalPages);

                if (hasNextPage) {
                    downloadAllMyObservations(currentPage + 1, syncCallback);
                } else {
                    syncCallback.onFinished();
                }
            }

            @Override
            public void onError(String error) {
                syncCallback.onError(error);
            }
        });
    }

    public interface ObservationPageCallback {
        void onSuccess(List<FieldObservationData> data, boolean hasNextPage, int totalPages);
        void onError(String error);
    }

    public interface SyncCallback {
        void onPageDownloaded(int current, int total);
        void onFinished();
        void onError(String message);
    }

    public interface ObservationFetchCallback {
        void onSuccess(FieldObservationData data);
        void onError(String error);
    }

    public static void fetchObservation(long observationId, ObservationFetchCallback callback) {
        Call<FieldObservationResponse> call = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .getFieldObservation(String.valueOf(observationId));

        call.enqueue(new Callback<FieldObservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FieldObservationData[] dataArray = response.body().getData();
                    if (dataArray != null && dataArray.length > 0) {
                        callback.onSuccess(dataArray[0]);
                    } else {
                        callback.onError("No data found for ID: " + observationId);
                    }
                } else {
                    callback.onError("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<FieldObservationResponse> call, @NonNull Throwable t) {
                callback.onError(t.getLocalizedMessage());
            }
        });
    }

    private static void saveToLocalDatabase(List<FieldObservationData> dataList) {
        if (dataList == null || dataList.isEmpty()) return;

        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);

        App.get().getBoxStore().runInTx(() -> {
            for (FieldObservationData data : dataList) {
                EntryDb existing;

                try (Query<EntryDb> query = box.query(EntryDb_.serverId.equal(data.getId())).build()) {
                    existing = query.findFirst();
                } catch (Exception e) {
                    Log.e(TAG, "Error in ObjectBox query for serverId: " + data.getId(), e);
                    continue;
                }

                if (existing == null) {
                    try {
                        Log.d(TAG, "Saving field observation id " + data.getId() + " to ObjectBox!");
                        Set<Long> observationTypeIds = new HashSet<>();
                        if (data.getTypes() != null && !data.getTypes().isEmpty()) {
                            for (FieldObservationDataTypes type : data.getTypes()) {
                                observationTypeIds.add(type.getId());
                            }
                        }

                        EntryDb newEntry = new EntryDb(
                                0,
                                data.getId(),
                                true,
                                false,
                                data.getTaxonId() != null ? data.getTaxonId() : 0,
                                null,
                                data.getTaxonSuggestion(),
                                String.valueOf(data.getYear()),
                                String.valueOf(data.getMonth() - 1),
                                String.valueOf(data.getDay()),
                                data.getNote(),
                                data.getNumber(),
                                data.getSex(),
                                data.getStageId(),
                                data.getAtlasCode(),
                                String.valueOf(data.isFoundDead()),
                                data.getFoundDeadNote(),
                                data.getLatitude(),
                                data.getLongitude(),
                                data.getAccuracy(),
                                data.getElevation(),
                                data.getLocation(),
                                null,
                                null,
                                null,
                                data.getProject(),
                                data.getFoundOn(),
                                String.valueOf(data.getDataLicense()),
                                0,
                                data.getTime(),
                                data.getHabitat(),
                                observationTypeIds.toString()
                        );

                        box.put(newEntry);
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving to ObjectBox: " + data.getId(), e);
                    }
                } else {
                    Log.d(TAG, "Not saving id " + data.getId() + " to ObjectBox, already there!");
                }
            }
        });
    }

    private static String findLatestTimestamp(List<FieldObservationData> dataList) {
        String latest = "0";
        for (FieldObservationData data : dataList) {
            if (data.getActivity() != null && !data.getActivity().isEmpty()) {
                String current = data.getActivity().get(0).getUpdatedAt();
                if (isNewer(current, latest)) {
                    latest = current;
                }
            }
        }
        return latest;
    }

    private static boolean isNewer(String newTs, String oldTs) {
        if (newTs == null || newTs.equals("0")) return false;
        if (oldTs == null || oldTs.equals("0")) return true;
        return newTs.compareTo(oldTs) > 0; // ISO 8601
    }

}
