package com.example.signuploginrealtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class coach_clients extends AppCompatActivity {

    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_ROLE = "role";
    private static final String KEY_UID = "uid";
    private static final String KEY_COACH_LOGGED_IN = "isCoachLoggedIn";

    // UI Components
    private DrawerLayout drawerLayout;
    private ImageView coachProfileIcon;
    private TextView assignedUsersCount;
    private EditText searchClientsEditText;
    private Button searchButton, clearFilterButton, refreshClientsButton;
    private Spinner filterSpinner;
    private RecyclerView assignedClientsRecyclerView;
    private LinearLayout loadingLayout, emptyStateLayout;

    // Sidebar menu items
    private LinearLayout menuClients, menuWorkouts, menuLogout;
    private TextView sidebarCoachName, sidebarCoachEmail;

    // Data
    private List<Client> clientsList;
    private List<Client> filteredClientsList;
    private ClientsAdapter clientsAdapter;
    private String[] filterOptions = {"All Clients", "Active", "Inactive", "New"};

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_coach_clients);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupRecyclerView();
        setupSpinner();
        setupListeners();
        loadClients();
        loadCoachInfo();
    }

    private void initializeViews() {
        // Initialize Firebase components first
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Main UI components
        drawerLayout = findViewById(R.id.drawer_layout);
        coachProfileIcon = findViewById(R.id.coach_profile_icon);
        assignedUsersCount = findViewById(R.id.assigned_users_count);
        searchClientsEditText = findViewById(R.id.search_clients_edittext);
        searchButton = findViewById(R.id.search_button);
        clearFilterButton = findViewById(R.id.clear_filter_button);
        refreshClientsButton = findViewById(R.id.refresh_clients_button);
        filterSpinner = findViewById(R.id.filter_spinner);
        assignedClientsRecyclerView = findViewById(R.id.assigned_clients_recycler_view);
        loadingLayout = findViewById(R.id.loading_layout);
        emptyStateLayout = findViewById(R.id.empty_state_layout);

        // Sidebar components
        menuClients = findViewById(R.id.menu_clients);
        menuWorkouts = findViewById(R.id.menu_workouts);
        menuLogout = findViewById(R.id.menu_logout);
        sidebarCoachName = findViewById(R.id.sidebar_coach_name);
        sidebarCoachEmail = findViewById(R.id.sidebar_coach_email);

        // Initialize data lists
        clientsList = new ArrayList<>();
        filteredClientsList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        clientsAdapter = new ClientsAdapter(this, filteredClientsList);
        assignedClientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        assignedClientsRecyclerView.setAdapter(clientsAdapter);
    }

    private void setupSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                filterOptions
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);
    }

    private void setupListeners() {
        // Profile icon click - open drawer
        coachProfileIcon.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        // Search functionality
        searchButton.setOnClickListener(v -> performSearch());

        searchClientsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    applyFilter(filterSpinner.getSelectedItemPosition());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter spinner
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Clear filter button
        clearFilterButton.setOnClickListener(v -> {
            searchClientsEditText.setText("");
            filterSpinner.setSelection(0);
            applyFilter(0);
        });

        // Refresh button
        refreshClientsButton.setOnClickListener(v -> {
            loadClients();
            Toast.makeText(this, "Refreshing clients...", Toast.LENGTH_SHORT).show();
        });

        // Sidebar menu listeners
        setupSidebarListeners();
    }

    private void setupSidebarListeners() {
        findViewById(R.id.menu_clients).setOnClickListener(v -> {
            Toast.makeText(this, "My Clients clicked", Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawer(GravityCompat.END);
        });

        findViewById(R.id.menu_workouts).setOnClickListener(v -> {
            Toast.makeText(this, "Workouts clicked", Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawer(GravityCompat.END);
        });

        findViewById(R.id.menu_logout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> logoutCoach())
                    .setNegativeButton("No", null)
                    .show();
            drawerLayout.closeDrawer(GravityCompat.END);
        });
    }

    private void performSearch() {
        String searchQuery = searchClientsEditText.getText().toString().trim().toLowerCase();

        if (searchQuery.isEmpty()) {
            applyFilter(filterSpinner.getSelectedItemPosition());
            return;
        }

        List<Client> searchResults = new ArrayList<>();
        for (Client client : clientsList) {
            if (client.getName().toLowerCase().contains(searchQuery)) {
                searchResults.add(client);
            }
        }

        filteredClientsList.clear();
        filteredClientsList.addAll(searchResults);
        clientsAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void applyFilter(int filterPosition) {
        filteredClientsList.clear();

        switch (filterPosition) {
            case 0: // All Clients
                filteredClientsList.addAll(clientsList);
                break;
            case 1: // Active
                for (Client client : clientsList) {
                    if ("Active".equals(client.getStatus())) {
                        filteredClientsList.add(client);
                    }
                }
                break;
            case 2: // Inactive
                for (Client client : clientsList) {
                    if ("Inactive".equals(client.getStatus())) {
                        filteredClientsList.add(client);
                    }
                }
                break;
            case 3: // New
                for (Client client : clientsList) {
                    if ("New".equals(client.getStatus())) {
                        filteredClientsList.add(client);
                    }
                }
                break;
        }

        clientsAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void loadClients() {
        showLoading(true);

        if (currentUser == null) {
            Toast.makeText(this, "No authenticated user found", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        // First, find the coach's custom ID by querying with their email
        firestore.collection("coaches")
                .whereEqualTo("email", currentUser.getEmail())
                .get()
                .addOnSuccessListener(coachQuerySnapshot -> {
                    if (coachQuerySnapshot.isEmpty()) {
                        Toast.makeText(this, "Coach profile not found", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                        return;
                    }

                    // Get the coach's custom document ID (e.g., "coach1")
                    String coachId = coachQuerySnapshot.getDocuments().get(0).getId();
                    android.util.Log.d("CoachLoad", "Found coach ID: " + coachId);

                    // Now query users collection where coachId equals the coach's custom ID
                    firestore.collection("users")
                            .whereEqualTo("coachId", coachId)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                clientsList.clear();

                                if (queryDocumentSnapshots.isEmpty()) {
                                    showLoading(false);
                                    updateUI();
                                    android.util.Log.d("CoachLoad", "No clients found for coach: " + coachId);
                                    return;
                                }

                                android.util.Log.d("CoachLoad", "Found " + queryDocumentSnapshots.size() + " clients");

                                for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    try {
                                        // Extract user data from Firestore document
                                        String name = document.getString("fullname");
                                        String email = document.getString("email");
                                        String fitnessGoal = document.getString("fitnessGoal");
                                        String fitnessLevel = document.getString("fitnessLevel");
                                        String gender = document.getString("gender");
                                        String membershipStatus = document.getString("membershipStatus");

                                        // Get numeric fields
                                        Long currentStreak = document.getLong("currentStreak");
                                        Long workoutsCompleted = document.getLong("workoutsCompleted");
                                        Long height = document.getLong("height");
                                        Long weight = document.getLong("weight");
                                        Long age = document.getLong("age");

                                        // Determine status based on membership and activity
                                        String status = determineUserStatus(currentStreak, workoutsCompleted, membershipStatus);

                                        // Format weight and height
                                        String weightStr = weight != null ? weight + " kg" : "N/A";
                                        String heightStr = height != null ? height + " cm" : "N/A";

                                        // Handle null values with defaults
                                        name = name != null ? name : "Unknown User";
                                        email = email != null ? email : "";
                                        fitnessGoal = fitnessGoal != null ? fitnessGoal : "General Fitness";
                                        fitnessLevel = fitnessLevel != null ? fitnessLevel : "Beginner";
                                        status = status != null ? status : "Unknown";

                                        // ✅ Create Client object
                                        Client client = new Client(name, email, status, weightStr, heightStr, fitnessGoal, fitnessLevel);

                                        // ⬇️ THIS is the line you need to add
                                        client.setUid(document.getId());   // Store Firestore doc ID

                                        clientsList.add(client);

                                    } catch (Exception e) {
                                        // Log error for debugging
                                        android.util.Log.e("ClientLoad", "Error parsing client data: " + e.getMessage(), e);
                                    }
                                }

                                // Apply current filter and update UI
                                applyFilter(filterSpinner.getSelectedItemPosition());
                                showLoading(false);

                                Toast.makeText(this, "Loaded " + clientsList.size() + " clients", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                android.util.Log.e("ClientLoad", "Error loading clients: " + e.getMessage(), e);
                                Toast.makeText(this, "Failed to load clients: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    android.util.Log.e("CoachLoad", "Error finding coach profile: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to find coach profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Helper method to determine user status based on activity and membership
    private String determineUserStatus(Long currentStreak, Long workoutsCompleted, String membershipStatus) {
        try {
            // Check membership status first
            if (membershipStatus != null && !"active".equals(membershipStatus)) {
                return "Inactive";
            }

            // Default values if null
            if (currentStreak == null) currentStreak = 0L;
            if (workoutsCompleted == null) workoutsCompleted = 0L;

            // Determine status based on activity
            if (workoutsCompleted == 0 && currentStreak == 0) {
                return "New";
            } else if (currentStreak >= 3 || workoutsCompleted >= 5) {
                return "Active";
            } else if (currentStreak >= 1 || workoutsCompleted >= 1) {
                return "Active";
            } else {
                return "Inactive";
            }

        } catch (Exception e) {
            android.util.Log.e("StatusDetermination", "Error determining user status: " + e.getMessage(), e);
            return "Unknown";
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            loadingLayout.setVisibility(View.VISIBLE);
            assignedClientsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        } else {
            loadingLayout.setVisibility(View.GONE);
            updateUI();
        }
    }

    private void updateUI() {
        if (filteredClientsList.isEmpty()) {
            assignedClientsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            assignedUsersCount.setText("Managing 0 clients");
        } else {
            assignedClientsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            assignedUsersCount.setText("Managing " + filteredClientsList.size() + " clients");
        }
    }

    private void loadCoachInfo() {
        if (currentUser != null) {
            // Use email-based query instead of UID-based document lookup
            firestore.collection("coaches")
                    .whereEqualTo("email", currentUser.getEmail())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String name = querySnapshot.getDocuments().get(0).getString("name");
                            String email = querySnapshot.getDocuments().get(0).getString("email");
                            sidebarCoachName.setText(name != null ? name : "Coach");
                            sidebarCoachEmail.setText(email != null ? email : "");
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("LoadCoachInfo", "Error loading coach info: " + e.getMessage(), e);
                    });
        }
    }
    private void logoutCoach() {
        FirebaseAuth.getInstance().signOut();

        // Clear session data using consistent SharedPreferences name and keys
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_ROLE)
                .remove(KEY_UID)
                .apply();

        Intent intent = new Intent(coach_clients.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App?")
                .setMessage("Do you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
                .setNegativeButton("No", null)
                .show();
    }

    // Client data model
    public static class Client {
        private String uid;
        private String name;
        private String email;
        private String status;
        private String weight;
        private String height;
        private String goal;
        private String activityLevel;

        // Empty constructor (required if you use Firebase)
        public Client() {}

        // Constructor with all fields
        public Client(String name, String email, String status, String weight, String height, String goal, String activityLevel) {
            this.name = name;
            this.email = email;
            this.status = status;
            this.weight = weight;
            this.height = height;
            this.goal = goal;
            this.activityLevel = activityLevel;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        // Getters
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public String getWeight() { return weight; }
        public String getHeight() { return height; }
        public String getGoal() { return goal; }
        public String getActivityLevel() { return activityLevel; }
    }
}