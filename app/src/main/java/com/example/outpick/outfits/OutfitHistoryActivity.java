package com.example.outpick.outfits;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.outpick.database.models.OutfitHistoryItem;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.dialogs.FilterHistoryBottomSheet;
import com.example.outpick.R;
import com.example.outpick.common.SuggestionPreviewImageActivity;
import com.example.outpick.common.BaseDrawerActivity;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutfitHistoryActivity extends BaseDrawerActivity
        implements FilterHistoryBottomSheet.OnFilterAppliedListener {

    private static final String TAG = "OutfitHistoryActivity";

    private ImageView backArrow;
    private TableLayout tableLayoutHistory;
    private SupabaseService supabaseService;
    private String currentUserId = "";

    private String lastQuery = null;
    private Set<String> lastSelectedStyles = null;
    private Set<String> lastSelectedEvents = null;
    private Set<String> lastSelectedSeasons = null;
    private String lastSelectedGender = null;

    // Define the number of columns being DISPLAYED in the TableLayout
    private static final int DISPLAY_COLUMN_COUNT = 4; // Outfit Name, Event, Date, Action (Category is hidden)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outfit_history);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();

        // Get current user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // --- Setup drawer ---
        setupDrawer(R.id.drawer_layout, R.id.nav_view);

        // --- Set username in drawer ---
        String username = getIntent().getStringExtra("username");
        if (username != null) {
            NavigationView navView = findViewById(R.id.nav_view);
            View headerView = navView.getHeaderView(0);
            TextView usernameText = headerView.findViewById(R.id.nav_header_username);
            if (usernameText != null) usernameText.setText(username);
        }

        // --- Back arrow ---
        backArrow = findViewById(R.id.backArrow);
        if (backArrow != null) backArrow.setOnClickListener(v -> finish());

        // --- Filter button ---
        findViewById(R.id.btnfilter).setOnClickListener(v -> {
            FilterHistoryBottomSheet bottomSheet = FilterHistoryBottomSheet.newInstance(
                    lastQuery,
                    lastSelectedStyles,
                    lastSelectedEvents,
                    lastSelectedSeasons
            );
            bottomSheet.show(getSupportFragmentManager(), "FilterHistoryBottomSheet");
        });

        tableLayoutHistory = findViewById(R.id.tableLayoutHistory);

        if (tableLayoutHistory == null) {
            Log.e(TAG, "tableLayoutHistory not found (R.id.tableLayoutHistory). Check XML.");
            Toast.makeText(this, "Layout Error: Cannot display history table.", Toast.LENGTH_LONG).show();
            return;
        }

        // **FIX**: Adjust the initial header row's columns to match DISPLAY_COLUMN_COUNT
        adjustHeaderColumns();

        // --- Initial load ---
        applyFilters(null, null, null, null, null);
    }

    /**
     * Helper to adjust the header row dynamically.
     */
    private void adjustHeaderColumns() {
        TableRow headerRow = (TableRow) tableLayoutHistory.getChildAt(0);
        if (headerRow != null) {
            if (headerRow.getChildCount() != DISPLAY_COLUMN_COUNT) {
                Log.w(TAG, "Header column count does not match the expected display count (" + DISPLAY_COLUMN_COUNT + "). Please verify XML.");
            }
        }
    }

    @Override
    public void onFilterApplied(String query,
                                Set<String> selectedStyles,
                                Set<String> selectedEvents,
                                Set<String> selectedSeasons) {

        lastQuery = query;
        lastSelectedEvents = selectedEvents != null ? Set.copyOf(selectedEvents) : null;
        lastSelectedSeasons = selectedSeasons != null ? Set.copyOf(selectedSeasons) : null;
        lastSelectedStyles = selectedStyles != null ? Set.copyOf(selectedStyles) : null;

        applyFilters(lastQuery, lastSelectedStyles, lastSelectedEvents, lastSelectedSeasons, lastSelectedGender);
    }

    public void setGenderFilter(String gender) {
        lastSelectedGender = gender;
        applyFilters(lastQuery, lastSelectedStyles, lastSelectedEvents, lastSelectedSeasons, lastSelectedGender);
    }

    private void applyFilters(String searchQuery,
                              Set<String> selectedStyles,
                              Set<String> selectedEvents,
                              Set<String> selectedSeasons,
                              String selectedGender) {

        if (tableLayoutHistory == null) return;

        // Clear previous rows (keeping the header row at index 0)
        if (tableLayoutHistory.getChildCount() > 1) {
            tableLayoutHistory.removeViews(1, tableLayoutHistory.getChildCount() - 1);
        }

        // Show loading
        addLoadingRow();

        // Load data from Supabase
        loadHistoryFromSupabase(searchQuery, selectedStyles, selectedEvents, selectedSeasons, selectedGender);
    }

    private void loadHistoryFromSupabase(String searchQuery,
                                         Set<String> selectedStyles,
                                         Set<String> selectedEvents,
                                         Set<String> selectedSeasons,
                                         String selectedGender) {

        // Build the Supabase query - get history for current user
        Call<List<JsonObject>> call = supabaseService.getOutfitHistory();

        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<OutfitHistoryItem> historyItems = parseHistoryItems(response.body());
                    // Apply local filtering since Supabase query is simple
                    List<OutfitHistoryItem> filteredItems = applyLocalFilters(
                            historyItems, searchQuery, selectedStyles, selectedEvents, selectedSeasons, selectedGender
                    );
                    displayHistoryItems(filteredItems);
                } else {
                    Log.e(TAG, "Supabase query failed: " + response.message());
                    Toast.makeText(OutfitHistoryActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
                    displayEmptyState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(OutfitHistoryActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                displayEmptyState();
            }
        });
    }

    private List<OutfitHistoryItem> parseHistoryItems(List<JsonObject> jsonObjects) {
        List<OutfitHistoryItem> historyItems = new ArrayList<>();
        Gson gson = new Gson();

        for (JsonObject jsonObject : jsonObjects) {
            try {
                OutfitHistoryItem history = gson.fromJson(jsonObject, OutfitHistoryItem.class);
                // Only include items for current user
                historyItems.add(history);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing history item: " + e.getMessage());
            }
        }

        return historyItems;
    }

    private List<OutfitHistoryItem> applyLocalFilters(List<OutfitHistoryItem> allItems,
                                                      String searchQuery,
                                                      Set<String> selectedStyles,
                                                      Set<String> selectedEvents,
                                                      Set<String> selectedSeasons,
                                                      String selectedGender) {

        List<OutfitHistoryItem> filtered = new ArrayList<>();

        for (OutfitHistoryItem item : allItems) {
            boolean matches = true;

            // Text search
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String query = searchQuery.toLowerCase().trim();
                String outfitName = item.getOutfitName() != null ? item.getOutfitName().toLowerCase() : "";
                if (!outfitName.contains(query)) {
                    matches = false;
                    continue;
                }
            }

            // Style filter
            if (selectedStyles != null && !selectedStyles.isEmpty()) {
                String itemStyle = item.getCategory() != null ? item.getCategory() : "";
                if (!selectedStyles.contains(itemStyle)) {
                    matches = false;
                    continue;
                }
            }

            // Event filter
            if (selectedEvents != null && !selectedEvents.isEmpty()) {
                String itemEvent = item.getEvent() != null ? item.getEvent() : "";
                if (!selectedEvents.contains(itemEvent)) {
                    matches = false;
                    continue;
                }
            }

            // Season filter
            if (selectedSeasons != null && !selectedSeasons.isEmpty()) {
                String itemSeason = item.getSeason() != null ? item.getSeason() : "";
                if (!selectedSeasons.contains(itemSeason)) {
                    matches = false;
                    continue;
                }
            }

            // Gender filter
            if (selectedGender != null && !selectedGender.isEmpty()) {
                String itemGender = item.getGender() != null ? item.getGender() : "";
                if (!selectedGender.equalsIgnoreCase(itemGender)) {
                    matches = false;
                    continue;
                }
            }

            if (matches) {
                filtered.add(item);
            }
        }

        return filtered;
    }

    private void displayHistoryItems(List<OutfitHistoryItem> historyItems) {
        // Clear any loading/empty rows
        if (tableLayoutHistory.getChildCount() > 1) {
            tableLayoutHistory.removeViews(1, tableLayoutHistory.getChildCount() - 1);
        }

        if (historyItems.isEmpty()) {
            displayEmptyState();
            return;
        }

        for (OutfitHistoryItem history : historyItems) {
            TableRow row = new TableRow(this);
            row.setPadding(4, 8, 4, 8);

            // Format action text
            String action = history.getActionTaken();
            if ("Use".equalsIgnoreCase(action)) action = "Used";
            else if ("Recreate".equalsIgnoreCase(action)) action = "Recreated";

            // Format date
            String formattedDate = formatDate(history.getDateUsed());

            // 1. Outfit Name (Used for image/details click)
            row.addView(createCell(
                    history.getOutfitName(),
                    history.getCategory(),
                    history.getImagePath(),
                    history.getEvent(),
                    formattedDate,
                    action
            ));

            // 2. Event
            row.addView(createCell(history.getEvent(), null, null, null, null, null));

            // 3. Date
            row.addView(createCell(formattedDate, null, null, null, null, null));

            // 4. Action
            row.addView(createCell(action, null, null, null, null, null));

            tableLayoutHistory.addView(row);
        }
    }

    private void addLoadingRow() {
        TableRow row = new TableRow(this);
        TextView loadingText = new TextView(this);
        loadingText.setText("Loading...");
        loadingText.setGravity(Gravity.CENTER);
        loadingText.setPadding(16, 16, 16, 16);

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        params.span = DISPLAY_COLUMN_COUNT;
        loadingText.setLayoutParams(params);

        row.addView(loadingText);
        tableLayoutHistory.addView(row);
    }

    private void displayEmptyState() {
        // Clear any existing rows except header
        if (tableLayoutHistory.getChildCount() > 1) {
            tableLayoutHistory.removeViews(1, tableLayoutHistory.getChildCount() - 1);
        }

        TableRow row = new TableRow(this);
        TextView emptyText = new TextView(this);
        emptyText.setText("No outfit history found");
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setPadding(16, 16, 16, 16);

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        params.span = DISPLAY_COLUMN_COUNT;
        emptyText.setLayoutParams(params);

        row.addView(emptyText);
        tableLayoutHistory.addView(row);
    }

    private TextView createCell(String text, String category, String imagePath,
                                String event, String date, String action) {
        TextView tv = new TextView(this);
        tv.setText(text != null ? text : "");
        tv.setPadding(8, 8, 8, 8);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(false);
        tv.setMaxLines(2);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);

        // Make outfit name clickable to view details
        if (imagePath != null && category != null) {
            tv.setTextColor(getResources().getColor(R.color.teal_700));
            tv.setOnClickListener(v -> {
                Intent intent = new Intent(OutfitHistoryActivity.this, SuggestionPreviewImageActivity.class);
                intent.putExtra("imageUri", imagePath);
                intent.putExtra("name", text);
                intent.putExtra("category", category);
                intent.putExtra("event", event);
                intent.putExtra("date", date);
                intent.putExtra("action", action);
                startActivity(intent);
            });
        }
        return tv;
    }

    private String formatDate(String inputDate) {
        if (inputDate == null || inputDate.trim().isEmpty()) {
            return "Unknown date";
        }

        try {
            // Try multiple date formats to handle different Supabase timestamp formats
            SimpleDateFormat[] inputFormats = {
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX", Locale.getDefault()),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            };

            Date date = null;
            for (SimpleDateFormat format : inputFormats) {
                try {
                    date = format.parse(inputDate);
                    break;
                } catch (Exception e) {
                    // Try next format
                }
            }

            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd yyyy\nhh:mma", Locale.getDefault());
                return outputFormat.format(date);
            } else {
                return inputDate; // Return original if parsing fails
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + inputDate, e);
            return inputDate;
        }
    }
}