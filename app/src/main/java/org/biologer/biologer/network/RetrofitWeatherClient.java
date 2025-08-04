package org.biologer.biologer.network;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.biologer.biologer.SettingsManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class RetrofitWeatherClient {

    private static final String TAG = "Biologer.RWeatherClient";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static RetrofitWeatherService weatherService = null;

    public static RetrofitWeatherService getClient() {
        if (weatherService == null) {
            try {
                Log.d(TAG, "Attempting to build Retrofit client...");

                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                OkHttpClient client = new OkHttpClient.Builder()
                        .readTimeout(15, TimeUnit.SECONDS)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .addInterceptor(
                                chain -> {
                                    Request request = chain.request();
                                    Request.Builder builder = request.newBuilder()
                                            .header("Authorization", "Bearer " + SettingsManager.getAccessToken());
                                    request = builder.build();

                                    return chain.proceed(request);
                                }
                        )
                        .addInterceptor(logging)
                        .build();

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(client)
                        .addConverterFactory(JacksonConverterFactory.create())
                        .build();

                weatherService = retrofit.create(RetrofitWeatherService.class);
                Log.d(TAG, "Retrofit client successfully built.");

            } catch (Exception e) {
                // This catch block is critical. Any error here is why the call fails.
                Log.e(TAG, "Failed to build Retrofit client. Exception: " + e.getMessage());
                // You can also print the stack trace for more details
                e.printStackTrace();
            }
        }
        return weatherService;
    }
}