package org.biologer.biologer.network;

import org.biologer.biologer.network.json.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RetrofitWeatherService {
    @GET("weather")
    Call<WeatherResponse> getCurrentWeather(
            @Query("lat") String lat,
            @Query("lon") String lon,
            @Query("appid") String apiKey,
            @Query("units") String units
    );
}