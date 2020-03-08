package org.biologer.biologer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.biologer.biologer.RetrofitService;
import org.biologer.biologer.SettingsManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;

    private static Retrofit getClient(String base_url) {
        if (retrofit == null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(15, TimeUnit.SECONDS)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.MINUTES)
                    .addInterceptor(
                            new Interceptor() {
                                @Override
                                public Response intercept(Chain chain) throws IOException {
                                    Request request = chain.request();
                                    Request.Builder builder = request.newBuilder()
                                            .header("Authorization", "Bearer " + SettingsManager.getToken());
                                    request = builder.build();

                                    return chain.proceed(request);
                                }
                            }
                    )
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(base_url)
                    .client(client)
                    .addConverterFactory(JacksonConverterFactory.create(mapper))
                    .build();
        }
        return retrofit;
    }

    public static RetrofitService getService(String base_url) {
        retrofit = getClient(base_url);
        RetrofitService service;
        service = retrofit.create(RetrofitService.class);
        return service;
    }
}