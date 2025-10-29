package com.example.signuploginrealtime;

import android.content.Intent;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private static final String CACHE_PREFS = "CoachClientsCache";
    private static final String CACHE_CLIENTS_LIST = "cached_clients_list";
    private static final String CACHE_COACH_NAME = "cached_coach_name";
    private static final String CACHE_COACH_EMAIL = "cached_coach_email";

    // UI Components
    private DrawerLayout drawerLayout;
    private ImageView coachProfileIcon;
    private TextView assignedUsersCount;
    private EditText searchClientsEditText;
    private Button searchButton, clearSearchButton;
    private RecyclerView assignedClientsRecyclerView;
    private LinearLayout loadingLayout, emptyStateLayout;

    // Sidebar menu items
    private LinearLayout menuArchive, menuLogout;
    private TextView sidebarCoachName, sidebarCoachEmail;

    // Data
    private List<Client> clientsList;
    private List<Client> filteredClientsList;
    private ClientsAdapter clientsAdapter;

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
        loadCachedData();
        setupListeners();
        loadClients();
        loadCoachInfo();
    }

    private void initializeViews() {
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        drawerLayout = findViewById(R.id.drawer_layout);
        coachProfileIcon = findViewById(R.id.coach_profile_icon);
        assignedUsersCount = findViewById(R.id.assigned_users_count);
        searchClientsEditText = findViewById(R.id.search_clients_edittext);
        searchButton = findViewById(R.id.search_button);
        clearSearchButton = findViewById(R.id.clear_search_button);
        assignedClientsRecyclerView = findViewById(R.id.assigned_clients_recycler_view);
        loadingLayout = findViewById(R.id.loading_layout);
        emptyStateLayout = findViewById(R.id.empty_state_layout);

        menuArchive = findViewById(R.id.menu_archive);
        menuLogout = findViewById(R.id.menu_logout);
        sidebarCoachName = findViewById(R.id.sidebar_coach_name);
        sidebarCoachEmail = findViewById(R.id.sidebar_coach_email);

        clientsList = new ArrayList<>();
        filteredClientsList = new ArrayList<>();
    }

    private void loadCachedData() {
        SharedPreferences cache = getSharedPreferences(CACHE_PREFS, MODE_PRIVATE);

        String cachedName = cache.getString(CACHE_COACH_NAME, null);
        String cachedEmail = cache.getString(CACHE_COACH_EMAIL, null);

        if (cachedName != null) {
            sidebarCoachName.setText(cachedName);
            String firstName = cachedName.split("\\s+")[0];
            TextView welcomeText = findViewById(R.id.welcome_coach_text);
            if (welcomeText != null) {
                welcomeText.setText("Welcome Coach " + firstName + "!");
            }
        }

        if (cachedEmail != null) {
            sidebarCoachEmail.setText(cachedEmail);
        }

        String cachedClientsJson = cache.getString(CACHE_CLIENTS_LIST, null);
        if (cachedClientsJson != null && !cachedClientsJson.isEmpty()) {
            try {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Client>>(){}.getType();
                List<Client> cachedClients = gson.fromJson(cachedClientsJson, listType);

                if (cachedClients != null && !cachedClients.isEmpty()) {
                    clientsList.clear();
                    clientsList.addAll(cachedClients);

                    filteredClientsList.clear();
                    filteredClientsList.addAll(cachedClients);

                    if (clientsAdapter != null) {
                        clientsAdapter.notifyDataSetChanged();
                        updateUI();
                    } else {
                        Log.w("Cache", "⚠️ Adapter is null, skipping cache display");
                    }

                    Log.d("Cache", "✅ Loaded " + cachedClients.size() + " clients from cache");
                }
            } catch (Exception e) {
                Log.e("Cache", "Error loading cached clients", e);
            }
        }
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

    private void setupListeners() {
        coachProfileIcon.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        searchButton.setOnClickListener(v -> performSearch());

        searchClientsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    showAllClients();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearSearchButton.setOnClickListener(v -> {
            searchClientsEditText.setText("");
            showAllClients();
        });

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
            showAllClients();
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

    private void showAllClients() {
        filteredClientsList.clear();
        filteredClientsList.addAll(clientsList);
        clientsAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void loadClients() {
        if (clientsList.isEmpty()) {
            showLoading(true);
        }

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

                    if (usersListener != null) {
                        usersListener.remove();
                        usersListener = null;
                    }

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

                                for (com.google.firebase.firestore.DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                                    QueryDocumentSnapshot document = dc.getDocument();
                                    String userId = document.getId();

                                    switch (dc.getType()) {
                                        case ADDED:
                                        case MODIFIED:
                                            try {
                                                Boolean isArchived = document.getBoolean("isArchived");
                                                if (isArchived != null && isArchived) {
                                                    autoUnarchiveClient(userId);
                                                    break;
                                                }

                                                String assignedCoachId = document.getString("coachId");
                                                if (assignedCoachId == null || !assignedCoachId.equals(currentCoachId)) {
                                                    Log.d("CoachRemoved", "Client no longer assigned: " + userId);
                                                    removeClientFromList(userId);
                                                    break;
                                                }

                                                Client existingClient = findClientById(userId);

                                                if (existingClient != null) {
                                                    Log.d("ClientUpdate", "Updating existing client: " + userId);

                                                    String newProfilePicUrl = document.getString("profilePictureUrl");
                                                    if (newProfilePicUrl != null && !newProfilePicUrl.equals(existingClient.getProfilePictureUrl())) {
                                                        existingClient.setProfilePictureUrl(newProfilePicUrl);
                                                        int index = filteredClientsList.indexOf(existingClient);
                                                        if (index >= 0) {
                                                            clientsAdapter.notifyItemChanged(index);
                                                        }
                                                    }
                                                } else {
                                                    String name = document.getString("fullname");
                                                    String email = document.getString("email");
                                                    String fitnessGoal = document.getString("fitnessGoal");
                                                    String fitnessLevel = document.getString("fitnessLevel");
                                                    String profilePictureUrl = document.getString("profilePictureUrl");

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

                                                    String initialStatus = "Active";
                                                    Client client = new Client(name, email, initialStatus, weightStr, heightStr, fitnessGoal, fitnessLevel);
                                                    client.setUid(userId);
                                                    client.setProfilePictureUrl(profilePictureUrl);

                                                    clientsList.add(client);

                                                    Log.d("ClientAdd", "Added new client: " + name);

                                                    setupMembershipListener(client, currentStreak, workoutsCompleted);
                                                }

                                            } catch (Exception ex) {
                                                Log.e("ClientLoad", "Error processing client: " + ex.getMessage(), ex);
                                            }
                                            break;

                                        case REMOVED:
                                            Log.d("ClientRemove", "Client removed from query: " + userId);
                                            removeClientFromList(userId);
                                            break;
                                    }
                                }

                                saveCachedClients();
                                showAllClients();
                                showLoading(false);
                            });

                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e("CoachLoad", "Error finding coach profile: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to find coach profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveCachedClients() {
        try {
            Gson gson = new Gson();
            String clientsJson = gson.toJson(clientsList);

            SharedPreferences cache = getSharedPreferences(CACHE_PREFS, MODE_PRIVATE);
            cache.edit()
                    .putString(CACHE_CLIENTS_LIST, clientsJson)
                    .apply();

            Log.d("Cache", "✅ Saved " + clientsList.size() + " clients to cache");
        } catch (Exception e) {
            Log.e("Cache", "Error saving clients to cache", e);
        }
    }

    private void setupMembershipListener(Client client, Long currentStreak, Long workoutsCompleted) {
        String userId = client.getUid();

        if (membershipListeners.containsKey(userId)) {
            membershipListeners.get(userId).remove();
        }

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

                    if (!hasActiveMembership) {
                        Log.d("Membership", "❌ No active membership found for: " + client.getName());
                        autoArchiveClient(client);
                        return;
                    }

                    Log.d("Membership", "✅ Active membership confirmed for: " + client.getName());
                    client.setStatus("Active");

                    int index = filteredClientsList.indexOf(client);
                    if (index >= 0) {
                        clientsAdapter.notifyItemChanged(index);
                    }

                    updateUI();
                });

        membershipListeners.put(userId, registration);
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
            firestore.collection("coaches")
                    .whereEqualTo("email", currentUser.getEmail())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String fullName = querySnapshot.getDocuments().get(0).getString("fullname");
                            String email = querySnapshot.getDocuments().get(0).getString("email");

                            SharedPreferences cache = getSharedPreferences(CACHE_PREFS, MODE_PRIVATE);
                            cache.edit()
                                    .putString(CACHE_COACH_NAME, fullName)
                                    .putString(CACHE_COACH_EMAIL, email)
                                    .apply();

                            sidebarCoachName.setText(fullName != null ? fullName : "Coach");
                            sidebarCoachEmail.setText(email != null ? email : "");

                            String firstName = "Coach";
                            if (fullName != null && !fullName.isEmpty()) {
                                String[] nameParts = fullName.trim().split("\\s+");
                                firstName = nameParts[0];
                            }

                            TextView welcomeText = findViewById(R.id.welcome_coach_text);
                            if (welcomeText != null) {
                                welcomeText.setText("Welcome Coach " + firstName + "!");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LoadCoachInfo", "Error loading coach info: " + e.getMessage(), e);
                    });
        }
    }

    private void logoutCoach() {
        FirebaseAuth.getInstance().signOut();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_ROLE)
                .remove(KEY_UID)
                .apply();

        SharedPreferences cache = getSharedPreferences(CACHE_PREFS, MODE_PRIVATE);
        cache.edit().clear().apply();

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
        archiveData.put("coachId", null);
        archiveData.put("archiveReason", "Manually archived by coach");

        firestore.collection("users")
                .document(client.getUid())
                .update(archiveData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, client.getName() + " has been archived", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to archive: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("ArchiveClient", "Error: " + e.getMessage(), e);
                });
    }

    private void autoArchiveClient(Client client) {
        if (client.getUid() == null || client.getUid().isEmpty()) {
            return;
        }

        Log.d("AutoArchive", "🗄️ Auto-archiving client (no active membership): " + client.getName());

        Map<String, Object> archiveData = new HashMap<>();
        archiveData.put("isArchived", true);
        archiveData.put("archivedBy", "system");
        archiveData.put("archivedAt", com.google.firebase.Timestamp.now());
        archiveData.put("coachId", null);
        archiveData.put("archiveReason", "No active membership");

        firestore.collection("users")
                .document(client.getUid())
                .update(archiveData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("AutoArchive", "✅ Client auto-archived successfully: " + client.getName());
                })
                .addOnFailureListener(e -> {
                    Log.e("AutoArchive", "❌ Failed to auto-archive client: " + e.getMessage(), e);
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
        Client toRemove = findClientById(userId);
        if (toRemove != null) {
            clientsList.remove(toRemove);
            Log.d("RemoveClient", "Removed client from list: " + userId);

            if (membershipListeners.containsKey(userId)) {
                membershipListeners.get(userId).remove();
                membershipListeners.remove(userId);
                Log.d("RemoveClient", "Cleaned up membership listener for: " + userId);
            }

            showAllClients();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }

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

    public static class Client {
        private String uid;
        private String name;
        private String email;
        private String status;
        private String weight;
        private String height;
        private String goal;
        private String activityLevel;
        private String profilePictureUrl;

        public Client() {}

        public Client(String name, String email, String status, String weight, String height, String goal, String activityLevel) {
            this.name = name;
            this.email = email;
            this.status = status;
            this.weight = weight;
            this.height = height;
            this.goal = goal;
            this.activityLevel = activityLevel;
        }

        public String getUid() { return uid; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public String getWeight() { return weight; }
        public String getHeight() { return height; }
        public String getGoal() { return goal; }
        public String getActivityLevel() { return activityLevel; }
        public String getProfilePictureUrl() { return profilePictureUrl; }

        public void setUid(String uid) { this.uid = uid; }
        public void setStatus(String status) { this.status = status; }
        public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    }
}