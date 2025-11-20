package com.example.outpick.database.supabase;

import com.google.gson.JsonObject;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface SupabaseService {

    // ================= STORAGE UPLOAD/DOWNLOAD =================
    @Multipart
    @POST("storage/v1/object/{bucketName}/{filePath}")
    Call<JsonObject> uploadFile(
            @Path("bucketName") String bucketName,
            @Path("filePath") String filePath,
            @Part MultipartBody.Part file
    );

    @GET("storage/v1/object/public/{bucketName}/{filePath}")
    Call<ResponseBody> downloadFile(
            @Path("bucketName") String bucketName,
            @Path("filePath") String filePath
    );

    @DELETE("storage/v1/object/{bucketName}/{filePath}")
    Call<Void> deleteFile(
            @Path("bucketName") String bucketName,
            @Path("filePath") String filePath
    );

    @GET("storage/v1/object/list/{bucketName}")
    Call<JsonObject> listFiles(
            @Path("bucketName") String bucketName
    );

    // ================= USERS =================
    @Headers("Content-Type: application/json")
    @POST("rpc/get_user_by_id_with_password")
    Call<List<JsonObject>> getUserByIdWithPasswordRpc(@Body JsonObject params);

    @GET("users")
    Call<List<JsonObject>> getUsers();

    @GET("users")
    Call<List<JsonObject>> getUserById(@Query("id") String userId);

    @GET("users")
    Call<List<JsonObject>> getUserByUsername(@Query("username") String username);

    @GET("users")
    Call<List<JsonObject>> getUserByEmail(@Query("email") String email);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("users")
    Call<List<JsonObject>> insertUser(@Body JsonObject user);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH
    Call<List<JsonObject>> updateUserById(@Url String url, @Body JsonObject updates);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("users")
    Call<List<JsonObject>> updateUser(@Query("username") String username, @Body JsonObject updates);

    // ✅ FIXED: Delete user using query parameter
    @Headers("Prefer: return=minimal")
    @DELETE("users")
    Call<Void> deleteUser(@Query("id") String userId);

    @Headers("Content-Type: application/json")
    @POST("rpc/get_user_with_password")
    Call<JsonObject> getUserWithPassword(@Body JsonObject params);

    // ================= OUTFITS =================
    @GET("outfits")
    Call<List<JsonObject>> getOutfits();

    // ✅ ADDED: Get outfit by ID
    @GET("outfits")
    Call<List<JsonObject>> getOutfitById(@Query("id") String outfitId);

    @GET("outfits")
    Call<List<JsonObject>> getOutfitsByCategory(@Query("category") String category);

    @GET("outfits")
    Call<List<JsonObject>> getOutfitsByGender(@Query("gender") String gender);

    @GET("outfits")
    Call<List<JsonObject>> getOutfitsByOccasion(@Query("occasion") String occasion);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("outfits")
    Call<List<JsonObject>> insertOutfit(@Body JsonObject outfit);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH
    Call<List<JsonObject>> updateOutfit(@Url String url, @Body JsonObject updates);

    // ✅ FIXED: Delete outfit using query parameter
    @Headers("Prefer: return=minimal")
    @DELETE("outfits")
    Call<Void> deleteOutfit(@Query("id") String outfitId);

    // ================= USER OUTFITS =================
    @GET("user_outfits")
    Call<List<JsonObject>> getUserOutfits(@Query("user_id") String userId);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("user_outfits")
    Call<List<JsonObject>> assignOutfitToUser(@Body JsonObject assignment);

    // ✅ FIXED: Remove outfit from user using query parameters
    @Headers("Prefer: return=minimal")
    @DELETE("user_outfits")
    Call<Void> removeOutfitFromUser(@Query("user_id") String userId, @Query("outfit_id") String outfitId);

    // ================= USER FAVORITES =================
    @GET("user_favorites")
    Call<List<JsonObject>> getUserFavorites(@Query("user_id") String userId);

    @GET("user_favorites")
    Call<List<JsonObject>> checkFavorite(@Query("user_id") String userId, @Query("outfit_id") String outfitId);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("user_favorites")
    Call<List<JsonObject>> addFavorite(@Body JsonObject favorite);

    // ✅ FIXED: Remove favorite using query parameters
    @Headers("Prefer: return=minimal")
    @DELETE("user_favorites")
    Call<Void> removeFavorite(@Query("user_id") String userId, @Query("outfit_id") String outfitId);

    // ================= CLOTHING =================
    @GET("clothing")
    Call<List<JsonObject>> getClothing();

    @GET("clothing")
    Call<List<JsonObject>> getClothingByUserId(@Query("user_id") String userId);

    @GET("clothing")
    Call<List<JsonObject>> getClothingByCloset(@Query("closet_name") String closetName);

    @GET("clothing")
    Call<List<JsonObject>> getClothingByCategory(@Query("category") String category);

    @GET("clothing")
    Call<List<JsonObject>> getClothingById(@Query("id") String clothingId);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("clothing")
    Call<List<JsonObject>> insertClothing(@Body JsonObject clothing);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH
    Call<List<JsonObject>> updateClothing(@Url String url, @Body JsonObject updates);

    // ✅ FIXED: Delete clothing using query parameter
    @Headers("Prefer: return=minimal")
    @DELETE("clothing")
    Call<Void> deleteClothing(@Query("id") String clothingId);

    // ================= CLOSETS =================
    @GET("closets")
    Call<List<JsonObject>> getClosets();

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("closets")
    Call<List<JsonObject>> insertCloset(@Body JsonObject closet);

    // ✅ FIXED: Delete closet using query parameter
    @Headers("Prefer: return=minimal")
    @DELETE("closets")
    Call<Void> deleteCloset(@Query("id") String closetId);

    // ================= OUTFIT HISTORY =================
    @GET("outfit_history")
    Call<List<JsonObject>> getOutfitHistory();

    @GET("outfit_history")
    Call<List<JsonObject>> getOutfitHistoryByUser(@Query("user_id") String userId);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("outfit_history")
    Call<List<JsonObject>> insertOutfitHistory(@Body JsonObject history);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH
    Call<List<JsonObject>> updateOutfitHistory(@Url String url, @Body JsonObject updates);

    // ✅ FIXED: Delete outfit history using query parameter
    @Headers("Prefer: return=minimal")
    @DELETE("outfit_history")
    Call<Void> deleteOutfitHistory(@Query("id") String historyId);

    @GET("outfit_history")
    Call<List<JsonObject>> checkOutfitInHistory(@Query("outfit_ref_id") String outfitRefId, @Query("action_taken") String actionTaken);

    // ================= CLOSET SNAPSHOTS =================
    @GET("closet_snapshots")
    Call<List<JsonObject>> getClosetSnapshots(@Query("closet_id") String closetId);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("closet_snapshots")
    Call<List<JsonObject>> addSnapshotToCloset(@Body JsonObject snapshot);

    // ✅ FIXED: Remove snapshot using query parameter
    @Headers("Prefer: return=minimal")
    @DELETE("closet_snapshots")
    Call<Void> removeSnapshotFromCloset(@Query("id") String snapshotId);

    // ================= ADVANCED FILTERING =================
    @GET("outfits")
    Call<List<JsonObject>> getFilteredOutfits(
            @Query("category") String category,
            @Query("gender") String gender,
            @Query("event") String event,
            @Query("season") String season,
            @Query("style") String style
    );

    @GET("clothing")
    Call<List<JsonObject>> getFilteredClothing(
            @Query("category") String category,
            @Query("season") String season,
            @Query("occasion") String occasion
    );

    // ================= CUSTOM RPC FUNCTIONS =================
    @Headers("Content-Type: application/json")
    @POST("rpc/check_username_exists")
    Call<JsonObject> checkUsernameExists(@Body JsonObject params);

    @Headers("Content-Type: application/json")
    @POST("rpc/check_email_exists")
    Call<JsonObject> checkEmailExists(@Body JsonObject params);

    @GET("clothing")
    Call<List<JsonObject>> getClothingWithFilters(
            @Query("category") String category,
            @Query("season") String season,
            @Query("occasion") String occasion,
            @Query("closet_name") String closetName
    );

    @GET("clothing")
    Call<List<JsonObject>> searchClothingByName(@Query("name") String name);

    @GET("clothing")
    Call<List<JsonObject>> getFavoriteClothing(@Query("is_favorite") Boolean isFavorite);

    @Headers("Content-Type: application/json")
    @POST("rpc/get_outfits_for_user")
    Call<List<JsonObject>> getOutfitsForUser(@Body JsonObject params);
    @HTTP(method = "DELETE", hasBody = false)
    Call<Void> executeDelete(@Url String url);
    // ✅ ADDED: Custom method for Supabase filters
    @GET
    Call<List<JsonObject>> executeGet(@Url String url);
}