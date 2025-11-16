package com.example.outpick.database.supabase;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface SupabaseService {

    // ================= USERS =================

    @Headers("Content-Type: application/json")
    @POST("rpc/get_user_by_id_with_password")
    Call<List<JsonObject>> getUserByIdWithPasswordRpc(@Body JsonObject params);
    @GET("users")
    Call<List<JsonObject>> getUsers();
    @GET("users")
    Call<List<JsonObject>> getUserByIdWithPassword(@Query("id") String userId, @Query("select") String select);
    @GET("users")
    Call<List<JsonObject>> getUserById(@Query("id") String userId);

    @GET("users")
    Call<List<JsonObject>> getUserByUsername(@Query("username") String username);

    @GET("users")
    Call<List<JsonObject>> getUserByEmail(@Query("email") String email);

    // NEW: Method to get user with specific columns including password


    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("users")
    Call<JsonObject> insertUser(@Body JsonObject user); // KEEP AS JsonObject

    @PATCH("users")
    Call<JsonObject> updateUser(@Query("username") String username, @Body JsonObject updates);

    @PATCH("users")
    Call<JsonObject> updateUserById(@Query("id") String userId, @Body JsonObject updates);

    @DELETE("users")
    Call<Void> deleteUser(@Query("id") String userId);

    // RPC function for getting user with password (optional)
    @Headers("Content-Type: application/json")
    @POST("rpc/get_user_with_password")
    Call<JsonObject> getUserWithPassword(@Body JsonObject params);

    // ================= OUTFITS =================
    @GET("outfits")
    Call<List<JsonObject>> getOutfits();

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
    Call<JsonObject> insertOutfit(@Body JsonObject outfit); // KEEP AS JsonObject

    @PATCH("outfits")
    Call<JsonObject> updateOutfit(@Query("id") String outfitId, @Body JsonObject updates);

    @DELETE("outfits")
    Call<Void> deleteOutfit(@Query("id") String outfitId);

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
    Call<JsonObject> addFavorite(@Body JsonObject favorite); // KEEP AS JsonObject

    @DELETE("user_favorites")
    Call<Void> removeFavorite(@Query("user_id") String userId, @Query("outfit_id") String outfitId);

    // ================= CLOTHING =================
    @GET("clothing")
    Call<List<JsonObject>> getClothingByCloset(@Query("closet_name") String closetName);

    @GET("clothing")
    Call<List<JsonObject>> getClothing();

    @GET("clothing")
    Call<List<JsonObject>> getClothingByCategory(@Query("category") String category);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("clothing")
    Call<JsonObject> insertClothing(@Body JsonObject clothing); // KEEP AS JsonObject

    @PATCH("clothing")
    Call<JsonObject> updateClothing(@Query("id") String clothingId, @Body JsonObject updates);

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
    Call<JsonObject> insertCloset(@Body JsonObject closet); // KEEP AS JsonObject

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
    Call<JsonObject> insertOutfitHistory(@Body JsonObject history); // KEEP AS JsonObject

    @PATCH("outfit_history")
    Call<JsonObject> updateOutfitHistory(@Query("id") String historyId, @Body JsonObject updates);

    @DELETE("outfit_history")
    Call<Void> deleteOutfitHistory(@Query("id") String historyId);

    @GET("outfit_history")
    Call<List<JsonObject>> checkOutfitInHistory(@Query("outfit_ref_id") String outfitRefId, @Query("action_taken") String actionTaken);

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
}