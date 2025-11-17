package com.example.outpick.database.repositories;

import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ClosetSnapshotRepository {
    private final SupabaseService supabaseService;

    public ClosetSnapshotRepository(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    public boolean addOutfitToCloset(String closetId, String snapshotPath) {
        try {
            JsonObject snapshot = new JsonObject();
            snapshot.addProperty("closet_id", closetId);
            snapshot.addProperty("snapshot_path", snapshotPath);

            Call<List<JsonObject>> call = supabaseService.addSnapshotToCloset(snapshot);
            Response<List<JsonObject>> response = call.execute();

            return response.isSuccessful() && response.body() != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<JsonObject> getSnapshotsInCloset(String closetId) {
        try {
            Call<List<JsonObject>> call = supabaseService.getClosetSnapshots(closetId);
            Response<List<JsonObject>> response = call.execute();
            return response.isSuccessful() ? response.body() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}