package org.biologer.biologer.services;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.biologer.biologer.BuildConfig;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.InternetConnection;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.RefreshTokenResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthHelper {

    private static final String TAG = "Biologer.AuthHelper";

    private final Context context;

    public interface RefreshCallbacks {
        void onSuccess();   // token refreshed
        void onExpired();   // 401 or no refresh token -> must re-login
        void onError();     // network error or non-401 server error
    }

    public AuthHelper(Context context) {
        this.context = context;
    }

    /**
     * Refreshes access and refresh tokens.
     * @param databaseUrl Current database URL (e.g. biologer.rs, .hr, .ba, .me, or dev.biologer.org)
     * @param warnUser Whether to notify the user on failure
     * @param cb Tri-state callbacks
     */
    public void refreshToken(String databaseUrl, boolean warnUser, RefreshCallbacks cb) {
        String refreshToken = SettingsManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            Log.w(TAG, "No refresh token available.");
            cb.onExpired();
            return;
        }

        String apiKey;
        String clientId;
        switch (databaseUrl) {
            case "https://biologer.rs":
                apiKey = BuildConfig.BiologerRS_Key;
                clientId = "2";
                break;
            case "https://biologer.hr":
                apiKey = BuildConfig.BiologerHR_Key;
                clientId = "2";
                break;
            case "https://biologer.ba":
                apiKey = BuildConfig.BiologerBA_Key;
                clientId = "2";
                break;
            case "https://biologer.me":
                apiKey = BuildConfig.BiologerME_Key;
                clientId = "2";
                break;
            case "https://dev.biologer.org":
                apiKey = BuildConfig.BiologerDEV_Key;
                clientId = "6";
                break;
            default:
                Log.e(TAG, "Unknown database URL: " + databaseUrl);
                if (warnUser) {
                    Toast.makeText(context, R.string.database_url_empty, Toast.LENGTH_LONG).show();
                }
                cb.onError();
                return;
        }

        if (!InternetConnection.isConnected(context)) {
            if (warnUser) {
                Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
            }
            cb.onError();
            return;
        }

        Log.d(TAG, "Refreshing token for " + databaseUrl + " (client_id=" + clientId + ")");

        Call<RefreshTokenResponse> call = RetrofitClient
                .getService(databaseUrl)
                .refresh("refresh_token", clientId, apiKey, refreshToken, "*");

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<RefreshTokenResponse> call,
                                   @NonNull Response<RefreshTokenResponse> response) {
                if (response.code() == 401) {
                    Log.w(TAG, "Refresh token expired/invalid (401).");
                    if (warnUser) {
                        Toast.makeText(context, R.string.both_login_and_refresh_tokens_expired, Toast.LENGTH_LONG).show();
                    }
                    cb.onExpired();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    RefreshTokenResponse body = response.body();
                    SettingsManager.setAccessToken(body.getAccessToken());
                    SettingsManager.setRefreshToken(body.getRefreshToken());
                    long expireAt = (System.currentTimeMillis() / 1000) + body.getExpiresIn();
                    SettingsManager.setTokenExpire(String.valueOf(expireAt));
                    Log.d(TAG, "Token refreshed. Expires at: " + expireAt);
                    cb.onSuccess();
                } else {
                    Log.e(TAG, "Failed to refresh token. Code: " + response.code());
                    if (warnUser) {
                        Toast.makeText(context, R.string.refresh_token_failed, Toast.LENGTH_LONG).show();
                    }
                    cb.onError();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RefreshTokenResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Token refresh failed: " + t.getMessage());
                if (warnUser) {
                    Toast.makeText(context, R.string.refresh_token_failed, Toast.LENGTH_LONG).show();
                }
                cb.onError();
            }
        });
    }
}
