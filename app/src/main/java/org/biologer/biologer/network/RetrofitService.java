package org.biologer.biologer.network;

import org.biologer.biologer.network.JSON.APIEntry;
import org.biologer.biologer.network.JSON.ElevationResponse;
import org.biologer.biologer.network.JSON.ObservationTypesResponse;
import org.biologer.biologer.network.JSON.RefreshTokenResponse;
import org.biologer.biologer.network.JSON.RegisterResponse;
import org.biologer.biologer.network.JSON.TaxaGroupsResponse;
import org.biologer.biologer.network.JSON.TaxaResponse;
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
    @Headers({"Accept: application/json"})
    @POST("oauth/token")
    Call<LoginResponse> login(@Field("grant_type") String grant_type,
                              @Field("client_id") String client_id,
                              @Field("client_secret") String client_secret,
                              @Field("scope") String scope,
                              @Field("username") String username,
                              @Field("password") String password);

    @FormUrlEncoded
    @Headers({"Accept: application/json"})
    @POST("oauth/token")
    Call<RefreshTokenResponse> refresh(@Field("grant_type") String grant_type,
                                     @Field("client_id") String client_id,
                                     @Field("client_secret") String client_secret,
                                     @Field("refresh_token") String refresh_token,
                                     @Field("scope") String scope);

    @FormUrlEncoded
    @Headers({"Accept: application/json"})
    @POST("api/register")
    Call<RegisterResponse> register(@Field("client_id") int client_id,
                                 @Field("client_secret") String client_secret,
                                 @Field("first_name") String first_name,
                                 @Field("last_name") String last_name,
                                 @Field("data_license") int data_license,
                                 @Field("image_license") int image_license,
                                 @Field("institution") String institution,
                                 @Field("email") String email,
                                 @Field("password") String password);

    @Headers({"Accept: application/json"})
    @GET("api/taxa")
    Call<TaxaResponse> getTaxa(@Query("page") int page_number,
                               @Query("per_page") int per_page,
                               @Query("updated_after") int updated_after,
                               @Query("withGroupsIds") boolean withGroupsIds,
                               @Query("groups[]") int[] groups,
                               @Query("ungrouped") boolean fetch_ungrouped);

    @Multipart
    @Headers({"Accept: application/json"})
    @POST("api/uploads/photos")
    Call<UploadFileResponse> uploadFile(@Part MultipartBody.Part file);

    @Headers({"Accept: application/json"
    ,"content-type: application/json"})
    @POST("api/field-observations")
    Call<APIEntryResponse> uploadEntry(@Body APIEntry apiEntry);

    @Headers({"Accept: application/json"})
    @GET("/api/my/profile")
    Call<UserDataResponse> getUserData();

    @Headers({"Accept: application/json"})
    @GET("/api/view-groups")
    Call<TaxaGroupsResponse> getTaxaGroupsResponse();

    @Headers({"Accept: application/json"})
    @GET("/api/observation-types")
    Call<ObservationTypesResponse> getObservationTypes(@Query("updated_after") int updated_after);

    @Headers({"Accept: application/json"})
    @POST("/api/elevation")
    Call<ElevationResponse> getElevation(@Query("latitude") double latitude,
                                         @Query("longitude") double longitude);

}
