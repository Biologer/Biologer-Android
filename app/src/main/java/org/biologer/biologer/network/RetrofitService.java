package org.biologer.biologer.network;

import org.biologer.biologer.network.JSON.APIEntry;
import org.biologer.biologer.network.JSON.ElevationResponse;
import org.biologer.biologer.network.JSON.ObservationTypesResponse;
import org.biologer.biologer.network.JSON.TaksoniResponse;
import org.biologer.biologer.network.JSON.UserDataResponse;
import org.biologer.biologer.network.JSON.LoginResponse;
import org.biologer.biologer.network.JSON.UploadFileResponse;
import org.biologer.biologer.network.JSON.APIEntryResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

/**
 * Created by brjovanovic on 12/24/2017.
 */

public interface RetrofitService {

    @FormUrlEncoded
    @POST("oauth/token")
    Call<LoginResponse> login(@Field("grant_type") String grant_type,
                              @Field("client_id") String client_id,
                              @Field("client_secret") String client_secret,
                              @Field("scope") String scope,
                              @Field("username") String username,
                              @Field("password") String password);

    @FormUrlEncoded
    @POST("v1/login")
    Call<LoginResponse> login2(@Field("email") String username,
                              @Field("password") String password);

    @GET("api/taxa")
    Call<TaksoniResponse> getTaxa(@Query("page") int page_number,
                                  @Query("per_page") int per_page,
                                  @Query("updated_after") int updated_after);

    @Multipart
    @POST("api/uploads/photos")
    Call<UploadFileResponse> uploadFile( @Part MultipartBody.Part file);

    @Headers({"Accept: application/json"
    ,"content-type: application/json"})
    @POST("api/field-observations")
    Call<APIEntryResponse> uploadEntry(@Body APIEntry apiEntry);

    @GET("/api/my/profile")
    Call<UserDataResponse> getUserData();

    @GET("/api/observation-types")
    Call<ObservationTypesResponse> getObservationTypes(@Query("updated_after") int updated_after);

    @POST("/api/elevation")
    Call<ElevationResponse> getElevation(@Query("latitude") double latitude,
                                         @Query("longitude") double longitude);

}