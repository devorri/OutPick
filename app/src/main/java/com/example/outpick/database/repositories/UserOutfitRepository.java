package com.example.outpick.database.repositories;

import android.util.Log;

import com.example.outpick.database.models.Outfit;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class UserOutfitRepository {
    private static final String TAG = "UserOutfitRepo";
    private SupabaseService supabase;
    private OutfitRepository outfitRepository;

    public UserOutfitRepository(SupabaseService supabase, OutfitRepository outfitRepository) {
        this.supabase = supabase;
        this.outfitRepository = outfitRepository;
    }

    // Get outfits assigned to a specific user
    public List<Outfit> getOutfitsForUser(String userId) {
        List<Outfit> outfits = new ArrayList<>();
        try {
            Log.d(TAG, "🔄 STEP 1: Fetching user outfits for user ID: " + userId);

            // ✅ FIXED: Use executeGet with proper Supabase filter syntax
            String filterUrl = "user_outfits?user_id=eq." + userId;
            Call<List<JsonObject>> call = supabase.executeGet(filterUrl);
            Log.d(TAG, "🔍 Custom Request URL: " + call.request().url().toString());

            Response<List<JsonObject>> assignmentsResponse = call.execute();

            Log.d(TAG, "🔍 API Response - Code: " + assignmentsResponse.code() + ", Success: " + assignmentsResponse.isSuccessful());

            if (assignmentsResponse.isSuccessful()) {
                if (assignmentsResponse.body() != null) {
                    Log.d(TAG, "✅ STEP 2: Found " + assignmentsResponse.body().size() + " outfit assignments");

                    if (assignmentsResponse.body().isEmpty()) {
                        Log.e(TAG, "❌ NO assignments found in user_outfits table for user: " + userId);
                    }

                    for (JsonObject assignment : assignmentsResponse.body()) {
                        Log.d(TAG, "🔍 Assignment JSON: " + assignment.toString());

                        if (assignment.has("outfit_id") && !assignment.get("outfit_id").isJsonNull()) {
                            String outfitId = assignment.get("outfit_id").getAsString();
                            Log.d(TAG, "🎯 STEP 3: Processing outfit ID: " + outfitId);

                            // Get the full outfit details
                            Outfit outfit = outfitRepository.getOutfitById(outfitId);
                            if (outfit != null) {
                                // ✅ ADDED: Set the suggestion flag from assignment
                                if (assignment.has("is_suggestion") && !assignment.get("is_suggestion").isJsonNull()) {
                                    outfit.setSuggestion(assignment.get("is_suggestion").getAsBoolean());
                                }

                                Log.d(TAG, "✅ STEP 4: Successfully added outfit: " + outfit.getName() + " (Suggestion: " + outfit.isSuggestion() + ")");
                                outfits.add(outfit);
                            } else {
                                Log.e(TAG, "❌ STEP 4: Outfit not found for ID: " + outfitId);
                            }
                        } else {
                            Log.e(TAG, "❌ Assignment missing outfit_id field");
                        }
                    }
                } else {
                    Log.e(TAG, "❌ Response body is null");
                }
            } else {
                Log.e(TAG, "❌ API call failed. Code: " + assignmentsResponse.code());
                if (assignmentsResponse.errorBody() != null) {
                    try {
                        String error = assignmentsResponse.errorBody().string();
                        Log.e(TAG, "❌ Error: " + error);
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Could not read error body");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in getOutfitsForUser: " + e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG, "📊 FINAL: Returning " + outfits.size() + " outfits for user " + userId);
        return outfits;
    }

    // Assign an outfit to a user (admin function)
    public boolean assignOutfitToUser(String outfitId, String userId, String assignedBy) {
        try {
            Log.d(TAG, "🎯 ASSIGN: Assigning outfit " + outfitId + " to user " + userId + " by " + assignedBy);

            JsonObject assignment = new JsonObject();
            assignment.addProperty("user_id", userId);
            assignment.addProperty("outfit_id", outfitId);
            assignment.addProperty("created_by", assignedBy);

            // ✅ FIX: Set is_suggestion based on who created it
            boolean isSuggestion = !"self".equals(assignedBy); // User's own outfits are NOT suggestions
            assignment.addProperty("is_suggestion", isSuggestion);

            Log.d(TAG, "🔍 Assignment details - is_suggestion: " + isSuggestion + ", created_by: " + assignedBy);

            Response<List<JsonObject>> response = supabase.assignOutfitToUser(assignment).execute();
            boolean success = response.isSuccessful();
            Log.d(TAG, "✅ ASSIGN: Assignment result = " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "❌ ASSIGN: Error assigning outfit: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Remove outfit assignment from user
    public boolean removeOutfitFromUser(String outfitId, String userId) {
        try {
            Response<Void> response = supabase.removeOutfitFromUser(userId, outfitId).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Check if outfit is assigned to user
    public boolean isOutfitAssignedToUser(String outfitId, String userId) {
        try {
            Response<List<JsonObject>> response = supabase.getUserOutfits(userId).execute();
            if (response.isSuccessful() && response.body() != null) {
                for (JsonObject assignment : response.body()) {
                    if (assignment.has("outfit_id") && assignment.get("outfit_id").getAsString().equals(outfitId)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}