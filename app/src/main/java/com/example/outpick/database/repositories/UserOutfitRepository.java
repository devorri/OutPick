package com.example.outpick.database.repositories;

import com.example.outpick.database.models.Outfit;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class UserOutfitRepository {
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
            // Get the outfit assignments for this user
            Response<List<JsonObject>> assignmentsResponse = supabase.getUserOutfits(userId).execute();
            if (assignmentsResponse.isSuccessful() && assignmentsResponse.body() != null) {
                for (JsonObject assignment : assignmentsResponse.body()) {
                    if (assignment.has("outfit_id") && !assignment.get("outfit_id").isJsonNull()) {
                        String outfitId = assignment.get("outfit_id").getAsString();
                        // Get the full outfit details
                        Outfit outfit = outfitRepository.getOutfitById(outfitId);
                        if (outfit != null) {
                            outfits.add(outfit);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outfits;
    }

    // Assign an outfit to a user (admin function)
    public boolean assignOutfitToUser(String outfitId, String userId, String assignedBy) {
        try {
            JsonObject assignment = new JsonObject();
            assignment.addProperty("user_id", userId);
            assignment.addProperty("outfit_id", outfitId);
            assignment.addProperty("created_by", assignedBy);
            assignment.addProperty("is_suggestion", true);

            Response<JsonObject> response = supabase.assignOutfitToUser(assignment).execute();
            return response.isSuccessful();
        } catch (Exception e) {
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