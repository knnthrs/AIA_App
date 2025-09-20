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

    private static final String PREFS_NAME = "LoginPrefs";
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
    private LinearLayout  menuClients, menuWorkouts, menuLogout;
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

        // Simulate loading delay
        new android.os.Handler().postDelayed(() -> {
            // Sample data - replace with your actual data loading logic
            clientsList.clear();
            clientsList.add(new Client("John Doe", "john@email.com", "Active", "75 kg", "180 cm", "Weight Loss", "Moderate"));
            clientsList.add(new Client("Jane Smith", "jane@email.com", "Active", "65 kg", "165 cm", "Muscle Gain", "High"));
            clientsList.add(new Client("Mike Johnson", "mike@email.com", "Inactive", "80 kg", "175 cm", "Maintain Weight", "Low"));
            clientsList.add(new Client("Sarah Wilson", "sarah@email.com", "New", "55 kg", "160 cm", "Weight Loss", "Low"));
            clientsList.add(new Client("Tom Brown", "tom@email.com", "Active", "90 kg", "185 cm", "Strength Training", "High"));


            applyFilter(filterSpinner.getSelectedItemPosition());
            showLoading(false);
        }, 1500);
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
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Load coach info from Firestore
        if (currentUser != null) {
            firestore.collection("coaches").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            String name = snapshot.getString("name");
                            String email = snapshot.getString("email");
                            sidebarCoachName.setText(name != null ? name : "Coach");
                            sidebarCoachEmail.setText(email != null ? email : "");
                        }
                    });
        }
    }

    private void logoutCoach() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

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
