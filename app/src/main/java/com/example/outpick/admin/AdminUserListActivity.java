package com.example.outpick.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.R;
import com.example.outpick.common.adapters.AdminUserAdapter;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminUserAdapter adapter;
    private SupabaseService supabaseService;
    private List<JsonObject> userList = new ArrayList<>();
    private ImageButton btnBack; // ✅ BACK BUTTON VARIABLE

    private static final int EDIT_USER_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_list);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        // ✅ INITIALIZE BACK BUTTON
        btnBack = findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // ✅ BACK BUTTON CLICK LISTENER
        btnBack.setOnClickListener(v -> {
            onBackPressed(); // This will work now!
        });

        loadUsers();
    }

    private void loadUsers() {
        Call<List<JsonObject>> call = supabaseService.getUsers();
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userList.clear();
                    userList.addAll(response.body());

                    if (adapter == null) {
                        adapter = new AdminUserAdapter(AdminUserListActivity.this, userList, supabaseService, AdminUserListActivity.this::refreshList);
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.updateUsers(userList);
                    }
                } else {
                    Toast.makeText(AdminUserListActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(AdminUserListActivity.this, "Error loading users: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Refresh list after edit/delete/suspend
    private void refreshList() {
        loadUsers(); // Reload from Supabase
    }

    // Handle result from UserEditActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_USER_REQUEST && resultCode == RESULT_OK) {
            // Refresh user list after editing
            refreshList();
        }
    }
}