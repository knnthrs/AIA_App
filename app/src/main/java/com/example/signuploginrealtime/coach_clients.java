package com.example.signuploginrealtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private LinearLayout menuClients, menuArchive, menuLogout;
    private TextView sidebarCoachName, sidebarCoachEmail;

    // Data
    private List<Client> clientsList;
    private List<Client> filteredClientsList;
    private ClientsAdapter clientsAdapter;
    private String[] filterOptions = {"All Clients", "Active", "Inactive", "New"};

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private String currentCoachId;
    private Map<String, com.google.firebase.firestore.ListenerRegistration> membershipListeners = new HashMap<>();
    private com.google.firebase.firestore.ListenerRegistration usersListener;

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
        menuArchive = findViewById(R.id.menu_archive);
        menuLogout = findViewById(R.id.menu_logout);
        sidebarCoachName = findViewById(R.id.sidebar_coach_name);
        sidebarCoachEmail = findViewById(R.id.sidebar_coach_email);

        // Initialize data lists
        clientsList = new ArrayList<>();
        filteredClientsList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        clientsAdapter = new ClientsAdapter(this, filteredClientsList, new ClientsAdapter.OnClientLongClickListener() {
            @Override
            public void onClientLongClick(Client client) {
                showArchiveDialog(client);
            }
        });
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

        findViewById(R.id.menu_archive).setOnClickListener(v -> {
            Intent intent = new Intent(coach_clients.this, CoachArchiveActivity.class);
            startActivity(intent);
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

        firestore.collection("coaches")
                .whereEqualTo("email", currentUser.getEmail())
                .get()
                .addOnSuccessListener(coachQuerySnapshot -> {
                    if (coachQuerySnapshot.isEmpty()) {
                        Toast.makeText(this, "Coach profile not found", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                        return;
                    }

                    currentCoachId = coachQuerySnapshot.getDocuments().get(0).getId();
                    Log.d("CoachLoad", "Found coach ID: " + currentCoachId);

                    // ✅ Remove old users listener if it exists
                    if (usersListener != null) {
                        usersListener.remove();
                        usersListener = null;
                    }

                    // ✅ Real-time listener for users collection
                    usersListener = firestore.collection("users")
                            .whereEqualTo("coachId", currentCoachId)
                            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                                if (e != null) {
                                    Log.e("ClientLoad", "Error listening for client updates", e);
                                    showLoading(false);
                                    return;
                                }

                                if (queryDocumentSnapshots == null) {
                                    showLoading(false);
                                    updateUI();
                                    return;
                                }

                                // ✅ Use document changes to handle add/modify/remove in real-time
                                for (com.google.firebase.firestore.DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                                    QueryDocumentSnapshot document = dc.getDocument();
                                    String userId = document.getId();

                                    switch (dc.getType()) {
                                        case ADDED:
                                        case MODIFIED:
                                            // ✅ Process new or modified client
                                            try {
                                                // Auto-unarchive if admin reassigned this coach
                                                Boolean isArchived = document.getBoolean("isArchived");
                                                if (isArchived != null && isArchived) {
                                                    autoUnarchiveClient(userId);
                                                    break;
                                                }

                                                // Check if coachId still matches
                                                String assignedCoachId = document.getString("coachId");
                                                if (assignedCoachId == null || !assignedCoachId.equals(currentCoachId)) {
                                                    Log.d("CoachRemoved", "Client no longer assigned: " + userId);
                                                    // Remove from list if it exists
                                                    removeClientFromList(userId);
                                                    break;
                                                }

                                                // ✅ Check if client already exists in list
                                                Client existingClient = findClientById(userId);

                                                if (existingClient != null) {
                                                    // Update existing client
                                                    Log.d("ClientUpdate", "Updating existing client: " + userId);
                                                } else {
                                                    // Add new client
                                                    String name = document.getString("fullname");
                                                    String email = document.getString("email");
                                                    String fitnessGoal = document.getString("fitnessGoal");
                                                    String fitnessLevel = document.getString("fitnessLevel");

                                                    Long currentStreak = document.getLong("currentStreak");
                                                    Long workoutsCompleted = document.getLong("workoutsCompleted");
                                                    Long height = document.getLong("height");
                                                    Long weight = document.getLong("weight");

                                                    String weightStr = weight != null ? weight + " kg" : "N/A";
                                                    String heightStr = height != null ? height + " cm" : "N/A";

                                                    name = name != null ? name : "Unknown User";
                                                    email = email != null ? email : "";
                                                    fitnessGoal = fitnessGoal != null ? fitnessGoal : "General Fitness";
                                                    fitnessLevel = fitnessLevel != null ? fitnessLevel : "Beginner";

                                                    Client client = new Client(name, email, "Checking...", weightStr, heightStr, fitnessGoal, fitnessLevel);
                                                    client.setUid(userId);
                                                    clientsList.add(client);

                                                    Log.d("ClientAdd", "Added new client: " + name);

                                                    // Set up membership listener
                                                    setupMembershipListener(client, currentStreak, workoutsCompleted);
                                                }

                                            } catch (Exception ex) {
                                                Log.e("ClientLoad", "Error processing client: " + ex.getMessage(), ex);
                                            }
                                            break;

                                        case REMOVED:
                                            // ✅ Handle removal in real-time
                                            Log.d("ClientRemove", "Client removed from query: " + userId);
                                            removeClientFromList(userId);
                                            break;
                                    }
                                }

                                // ✅ Update UI after processing all changes
                                applyFilter(filterSpinner.getSelectedItemPosition());
                                showLoading(false);
                            });

                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e("CoachLoad", "Error finding coach profile: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to find coach profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }



    private void setupMembershipListener(Client client, Long currentStreak, Long workoutsCompleted) {
        String userId = client.getUid();

        // Remove old listener if exists
        if (membershipListeners.containsKey(userId)) {
            membershipListeners.get(userId).remove();
        }

        // ✅ Set up new real-time listener
        com.google.firebase.firestore.ListenerRegistration registration = firestore.collection("memberships")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e("Membership", "Error listening for membership updates", error);
                        return;
                    }

                    boolean hasActiveMembership = false;
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            String statusField = document.getString("membershipStatus");
                            if (statusField != null && statusField.equalsIgnoreCase("active")) {
                                Timestamp expirationTimestamp = document.getTimestamp("membershipExpirationDate");
                                if (expirationTimestamp != null && expirationTimestamp.toDate().after(new Date())) {
                                    hasActiveMembership = true;
                                    break;
                                }
                            }
                        }
                    }

                    // ✅ Auto-archive if no active membership
                    if (!hasActiveMembership) {
                        autoArchiveClient(client);
                        return; // Don't update status, client will be removed by snapshot listener
                    }

                    String status = determineUserStatusWithMembership(
                            currentStreak,
                            workoutsCompleted,
                            hasActiveMembership
                    );

                    // ✅ Update client status in real-time
                    client.setStatus(status);

                    // ✅ Find client in filtered list and update
                    int index = filteredClientsList.indexOf(client);
                    if (index >= 0) {
                        clientsAdapter.notifyItemChanged(index);
                    }

                    updateUI();
                });

        // Store the listener so we can remove it later
        membershipListeners.put(userId, registration);
    }



    // ✅ NEW METHOD: Updated status determination with membership check
    private String determineUserStatusWithMembership(Long currentStreak, Long workoutsCompleted, boolean hasActiveMembership) {
        try {
            // This method should only be called for users WITH active membership
            // Users without membership are auto-archived

            // Default values if null
            if (currentStreak == null) currentStreak = 0L;
            if (workoutsCompleted == null) workoutsCompleted = 0L;

            // Determine status based on activity
            if (workoutsCompleted == 0 && currentStreak == 0) {
                return "New";
            } else if (currentStreak >= 1 || workoutsCompleted >= 1) {
                return "Active";
            } else {
                return "New"; // Has membership but hasn't started
            }

        } catch (Exception e) {
            Log.e("StatusDetermination", "Error determining user status: " + e.getMessage(), e);
            return "New";
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

    private void showArchiveDialog(Client client) {
        new AlertDialog.Builder(this)
                .setTitle("Archive Client")
                .setMessage("Are you sure you want to archive " + client.getName() + "?")
                .setPositiveButton("Archive", (dialog, which) -> archiveClient(client))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void archiveClient(Client client) {
        if (client.getUid() == null || client.getUid().isEmpty()) {
            Toast.makeText(this, "Error: Client ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Archiving client...", Toast.LENGTH_SHORT).show();

        Map<String, Object> archiveData = new HashMap<>();
        archiveData.put("isArchived", true);
        archiveData.put("archivedBy", currentCoachId);
        archiveData.put("archivedAt", com.google.firebase.Timestamp.now());
        archiveData.put("coachId", null); // ✅ Remove coach assignment

        firestore.collection("users")
                .document(client.getUid())
                .update(archiveData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, client.getName() + " has been archived and unassigned", Toast.LENGTH_SHORT).show();
                    // Snapshot listener will handle the removal automatically
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to archive client: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("ArchiveClient", "Error: " + e.getMessage(), e);
                });
    }

    private void autoArchiveClient(Client client) {
        if (client.getUid() == null || client.getUid().isEmpty()) {
            return;
        }

        Log.d("AutoArchive", "Auto-archiving client due to no active membership: " + client.getName());

        Map<String, Object> archiveData = new HashMap<>();
        archiveData.put("isArchived", true);
        archiveData.put("archivedBy", "system"); // System archived (no membership)
        archiveData.put("archivedAt", com.google.firebase.Timestamp.now());
        archiveData.put("coachId", null); // ✅ Remove coach assignment
        archiveData.put("archiveReason", "No active membership");

        firestore.collection("users")
                .document(client.getUid())
                .update(archiveData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("AutoArchive", "Client auto-archived successfully: " + client.getName());
                    // Snapshot listener will handle the removal automatically
                })
                .addOnFailureListener(e -> {
                    Log.e("AutoArchive", "Failed to auto-archive client: " + e.getMessage(), e);
                });
    }

    private void autoUnarchiveClient(String userId) {
        Log.d("AutoUnarchive", "Auto-unarchiving client due to admin reassignment: " + userId);

        Map<String, Object> unarchiveData = new HashMap<>();
        unarchiveData.put("isArchived", false);
        unarchiveData.put("archivedBy", null);
        unarchiveData.put("archivedAt", null);
        unarchiveData.put("archiveReason", null);

        firestore.collection("users")
                .document(userId)
                .update(unarchiveData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("AutoUnarchive", "Client auto-unarchived successfully");
                    Toast.makeText(this, "Client restored from archive", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("AutoUnarchive", "Failed to auto-unarchive client: " + e.getMessage(), e);
                });
    }

    private Client findClientById(String userId) {
        for (Client client : clientsList) {
            if (client.getUid() != null && client.getUid().equals(userId)) {
                return client;
            }
        }
        return null;
    }

    private void removeClientFromList(String userId) {
        // Remove from clientsList
        Client toRemove = findClientById(userId);
        if (toRemove != null) {
            clientsList.remove(toRemove);
            Log.d("RemoveClient", "Removed client from list: " + userId);

            // Clean up membership listener
            if (membershipListeners.containsKey(userId)) {
                membershipListeners.get(userId).remove();
                membershipListeners.remove(userId);
                Log.d("RemoveClient", "Cleaned up membership listener for: " + userId);
            }

            // Reapply filter to update UI
            applyFilter(filterSpinner.getSelectedItemPosition());
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ✅ Clean up users listener
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }

        // ✅ Clean up all membership listeners
        for (com.google.firebase.firestore.ListenerRegistration listener : membershipListeners.values()) {
            listener.remove();
        }
        membershipListeners.clear();
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
        private String status;  // Keep this private
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

        // Getters
        public String getUid() { return uid; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public String getWeight() { return weight; }
        public String getHeight() { return height; }
        public String getGoal() { return goal; }
        public String getActivityLevel() { return activityLevel; }

        // ✅ ADD THIS SETTER
        public void setUid(String uid) {
            this.uid = uid;
        }

        // ✅ ADD THIS SETTER - CRITICAL!
        public void setStatus(String status) {
            this.status = status;
        }
    }

}