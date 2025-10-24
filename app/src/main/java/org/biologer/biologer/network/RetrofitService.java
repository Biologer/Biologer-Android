package org.biologer.biologer.network;

import org.biologer.biologer.network.json.APIEntry;
import org.biologer.biologer.network.json.APIEntryResponse;
import org.biologer.biologer.network.json.APITimedCounts;
import org.biologer.biologer.network.json.APITimedCountsResponse;
import org.biologer.biologer.network.json.AnnouncementsResponse;
import org.biologer.biologer.network.json.ElevationResponse;
import org.biologer.biologer.network.json.FieldObservationResponse;
import org.biologer.biologer.network.json.LoginResponse;
import org.biologer.biologer.network.json.ObservationTypesResponse;
import org.biologer.biologer.network.json.RefreshTokenResponse;
import org.biologer.biologer.network.json.RegisterResponse;
import org.biologer.biologer.network.json.TaxaGroupsResponse;
import org.biologer.biologer.network.json.TaxaResponse;
import org.biologer.biologer.network.json.UnreadNotificationsResponse;
import org.biologer.biologer.network.json.UploadFileResponse;
import org.biologer.biologer.network.json.UserDataResponse;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

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

    @Headers({"Accept: application/json"
            ,"content-type: application/json"})
    @POST("api/timed-count-observations")
    Call<APITimedCountsResponse> uploadTimedCount(@Body APITimedCounts timedCounts);

    @Headers({"Accept: application/json"})
    @GET("api/field-observations/")
    Call<FieldObservationResponse> getFieldObservation(
            @Query(value="id", encoded = true) String id);

    @GET
    Call<ResponseBody> getPhoto(
            @Url String image);

    @Headers({"Accept: application/json"})
    @GET("/api/my/profile")
    Call<UserDataResponse> getUserData();

    @FormUrlEncoded
    @POST("api/user/fcm-token")
    Call<ResponseBody> updateFcmToken(@Field("fcm_token") String token);

    @Headers({"Accept: application/json"})
    @GET("/api/my/unread-notifications")
    Call<UnreadNotificationsResponse> getUnreadNotifications(
            @Query("page") int page,
            @Query("per_page") int perPage,
            @Query("updated_after") long updatedAfter);

    @Headers({"Accept: application/json"})
    @POST("/api/my/read-notifications/batch")
    Call<ResponseBody> setNotificationAsRead(
            @Query("notifications_ids[]") String[] notifications);

    @Headers({"Accept: application/json"})
    @POST("/api/my/read-notifications/batch")
    Call<ResponseBody> setAllNotificationAsRead(
            @Query("all[]") boolean read_all);

    @Headers({"Accept: application/json" })
    @POST("/api/password/forgot")
    Call<ResponseBody> resetForgottenPassword(
            @Query("email") String email);

    @Headers({"Accept: application/json" })
    @DELETE("/api/users/{user_id}")
    Call<ResponseBody> deleteUser(
            @Path("user_id") long userId,
            @Query("delete_observations") int delete_observations);

    @Headers({"Accept: application/json" })
    @PUT("/api/users/{user_id}")
    Call<ResponseBody> editUserEmail(
            @Path("user_id") long userId,
            @Query("email") String email);

    @Headers({"Accept: application/json" })
    @PUT("/api/users/{user_id}")
    Call<ResponseBody> editUserPassword(
            @Path("user_id") long userId,
            @Query("password") String password);

    @Headers({"Accept: application/json"})
    @GET("/api/announcements")
    Call<AnnouncementsResponse> getAnnouncements();

    @Headers({"Accept: application/json"})
    @POST("/api/read-announcements")
    Call<ResponseBody> setAnnouncementAsRead(
            @Query("announcement_id") long announcement_id);

    @Headers({"Accept: application/json"})
    @GET("/api/view-groups")
    Call<TaxaGroupsResponse> getTaxaGroupsResponse();

    @Headers({"Accept: application/json"})
    @GET("/api/observation-types")
    Call<ObservationTypesResponse> getObservationTypes(
            @Query("updated_after") int updated_after);

    @Headers({"Accept: application/json" })
    @POST("/api/elevation")
    Call<ElevationResponse> getElevation(@Query("latitude") double latitude,
                                         @Query("longitude") double longitude);

}
