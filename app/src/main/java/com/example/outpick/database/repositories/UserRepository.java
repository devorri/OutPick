package com.example.outpick.database.repositories;

import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserRepository {
    private SupabaseService supabase;

    public UserRepository(SupabaseService supabase) {
        this.supabase = supabase;
    }

    public boolean registerUser(String firstName, String lastName, String email,
                                String phone, String username, String password, String gender) {
        try {
            JsonObject user = new JsonObject();
            user.addProperty("first_name", firstName);
            user.addProperty("last_name", lastName);
            user.addProperty("email", email);
            user.addProperty("phone", phone != null ? phone : "");
            user.addProperty("username", username);
            user.addProperty("password", password);
            user.addProperty("gender", gender);
            user.addProperty("role", "User");
            user.addProperty("status", "Offline");
            user.addProperty("display_name", firstName + " " + lastName);
            user.addProperty("signup_date", getCurrentDate());
            user.addProperty("last_login", "Never");
            user.addProperty("last_logout", "N/A");
            user.addProperty("suspended", false);

            // ✅ FIXED: Use Response<List<JsonObject>> for insert operations
            Response<List<JsonObject>> response = supabase.insertUser(user).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public JsonObject registerUserAndGetData(String firstName, String lastName, String email,
                                             String phone, String username, String password, String gender) {
        try {
            JsonObject user = new JsonObject();
            user.addProperty("first_name", firstName);
            user.addProperty("last_name", lastName);
            user.addProperty("email", email);
            user.addProperty("phone", phone != null ? phone : "");
            user.addProperty("username", username);
            user.addProperty("password", password);
            user.addProperty("gender", gender);
            user.addProperty("role", "User");
            user.addProperty("status", "Offline");
            user.addProperty("display_name", firstName + " " + lastName);
            user.addProperty("signup_date", getCurrentDate());
            user.addProperty("last_login", "Never");
            user.addProperty("last_logout", "N/A");
            user.addProperty("suspended", false);

            // ✅ FIXED: Use Response<List<JsonObject>> for insert operations
            Response<List<JsonObject>> response = supabase.insertUser(user).execute();
            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                return response.body().get(0); // Return the first created user
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean checkUsernameExists(String username) {
        try {
            // ✅ FIXED: Use Response<List<JsonObject>> for GET operations
            Response<List<JsonObject>> response = supabase.getUserByUsername(username).execute();
            return response.isSuccessful() && response.body() != null && !response.body().isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkEmailExists(String email) {
        try {
            // ✅ FIXED: Use Response<List<JsonObject>> for GET operations
            Response<List<JsonObject>> response = supabase.getUserByEmail(email).execute();
            return response.isSuccessful() && response.body() != null && !response.body().isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public JsonObject loginUser(String username, String password) {
        try {
            // ✅ FIXED: Use Response<List<JsonObject>> for GET operations
            Response<List<JsonObject>> response = supabase.getUserByUsername(username).execute();
            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                JsonObject user = response.body().get(0);
                String dbPassword = user.get("password").getAsString();
                if (dbPassword.equals(password)) {
                    return user; // Return user data on successful login
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateUserStatus(String username, String status) {
        try {
            JsonObject updates = new JsonObject();
            updates.addProperty("status", status);
            if ("Active".equals(status)) {
                updates.addProperty("last_login", getCurrentDateTime());
            } else {
                updates.addProperty("last_logout", getCurrentDateTime());
            }

            // ✅ FIXED: Use Response<List<JsonObject>> for UPDATE operations
            Response<List<JsonObject>> response = supabase.updateUser(username, updates).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // NEW: Method to get user by ID with password for UserEditActivity
    public JsonObject getUserWithPassword(String userId) {
        try {
            // ✅ FIXED: Use the RPC method that includes password
            JsonObject params = new JsonObject();
            params.addProperty("user_id", userId);

            Response<List<JsonObject>> response = supabase.getUserByIdWithPasswordRpc(params).execute();
            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                return response.body().get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}