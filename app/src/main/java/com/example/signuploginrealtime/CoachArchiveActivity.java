package com.example.signuploginrealtime;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoachArchiveActivity extends AppCompatActivity {

    private ImageView backButton;
    private TextView archivedClientsCount;
    private RecyclerView archivedClientsRecyclerView;
    private LinearLayout loadingLayout, emptyStateLayout;

    private List<coach_clients.Client> archivedClientsList;
    private ArchivedClientsAdapter adapter;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private String currentCoachId;

    // ✅ Add listener for real-time updates
    private ListenerRegistration archiveListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_archive);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupRecyclerView();
        loadArchivedClients();
    }

    private void initializeViews() {
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        backButton = findViewById(R.id.back_button);
        archivedClientsCount = findViewById(R.id.archived_clients_count);
        archivedClientsRecyclerView = findViewById(R.id.archived_clients_recycler_view);
        loadingLayout = findViewById(R.id.loading_layout);
        emptyStateLayout = findViewById(R.id.empty_state_layout);

        archivedClientsList = new ArrayList<>();

        backButton.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ArchivedClientsAdapter(this, archivedClientsList, new ArchivedClientsAdapter.OnArchivedClientClickListener() {
            @Override
            public void onRestoreClick(coach_clients.Client client) {
                showRestoreDialog(client);
            }

            @Override
            public void onDeleteClick(coach_clients.Client client) {
                showDeleteDialog(client);
            }
        });
        archivedClientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        archivedClientsRecyclerView.setAdapter(adapter);
    }

    private void loadArchivedClients() {
        showLoading(true);

        if (currentUser == null) {
            Toast.makeText(this, "No authenticated user found", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        // ✅ Remove old listener if exists
        if (archiveListener != null) {
            archiveListener.remove();
            archiveListener = null;
        }

        // Find coach ID first
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

                    // ✅ Real-time listener for archived clients
                    // This will show:
                    // 1. Clients archived by coach
                    // 2. Clients auto-archived due to no membership
                    // 3. Clients where admin removed the coach (coachId is null but archivedBy points to this coach)
                    // ✅ Real-time listener for archived clients that belong to this coach
                    archiveListener = firestore.collection("users")
                            .whereEqualTo("archivedBy", currentCoachId)
                            .whereEqualTo("isArchived", true)
                            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                                if (e != null) {
                                    Log.e("ArchiveLoad", "Error listening for archived clients", e);
                                    showLoading(false);
                                    return;
                                }

                                archivedClientsList.clear();

                                if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                                    showLoading(false);
                                    updateUI();
                                    return;
                                }

                                // ✅ Process archived clients (already filtered by query)
                                for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    try {
                                        String archivedBy = document.getString("archivedBy"); {

                                            String name = document.getString("fullname");
                                            String email = document.getString("email");
                                            String fitnessGoal = document.getString("fitnessGoal");
                                            String fitnessLevel = document.getString("fitnessLevel");
                                            String archiveReason = document.getString("archiveReason");

                                            Long height = document.getLong("height");
                                            Long weight = document.getLong("weight");

                                            String weightStr = weight != null ? weight + " kg" : "N/A";
                                            String heightStr = height != null ? height + " cm" : "N/A";

                                            name = name != null ? name : "Unknown User";
                                            email = email != null ? email : "";
                                            fitnessGoal = fitnessGoal != null ? fitnessGoal : "General Fitness";
                                            fitnessLevel = fitnessLevel != null ? fitnessLevel : "Beginner";

                                            // ✅ Show archive reason as status
                                            String statusText = "Archived";
                                            if (archiveReason != null) {
                                                statusText = "Archived: " + archiveReason;
                                            }

                                            coach_clients.Client client = new coach_clients.Client(
                                                    name, email, statusText, weightStr, heightStr, fitnessGoal, fitnessLevel
                                            );
                                            client.setUid(document.getId());

                                            archivedClientsList.add(client);
                                        }

                                    } catch (Exception ex) {
                                        Log.e("ArchiveLoad", "Error parsing archived client: " + ex.getMessage(), ex);
                                    }
                                }

                                adapter.notifyDataSetChanged();
                                showLoading(false);
                                updateUI();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to find coach profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void showRestoreDialog(coach_clients.Client client) {
        new AlertDialog.Builder(this)
                .setTitle("Restore Client")
                .setMessage("Restore " + client.getName() + " to active clients?\n\nThis will reassign you as their coach and unarchive them.")
                .setPositiveButton("Restore", (dialog, which) -> restoreClient(client))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void restoreClient(coach_clients.Client client) {
        // ✅ Restore client: reassign coach + unarchive
        Map<String, Object> restoreData = new HashMap<>();
        restoreData.put("isArchived", false);
        restoreData.put("archivedBy", null);
        restoreData.put("archivedAt", null);
        restoreData.put("archiveReason", null);
        restoreData.put("coachId", currentCoachId); // ✅ Reassign coach

        firestore.collection("users")
                .document(client.getUid())
                .update(restoreData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, client.getName() + " restored successfully", Toast.LENGTH_SHORT).show();
                    // ✅ Listener will handle removal automatically
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to restore client: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("RestoreClient", "Error: " + e.getMessage(), e);
                });
    }

    private void showDeleteDialog(coach_clients.Client client) {
        new AlertDialog.Builder(this)
                .setTitle("Permanently Delete Client")
                .setMessage("Permanently delete " + client.getName() + " from the system?\n\n⚠️ This will:\n- Remove ALL their data\n- Delete workout history\n- Cannot be undone!\n\nAre you absolutely sure?")
                .setPositiveButton("Delete Forever", (dialog, which) -> deleteClient(client))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteClient(coach_clients.Client client) {
        // ✅ Soft delete - mark as deleted but keep data
        Map<String, Object> deleteData = new HashMap<>();
        deleteData.put("isDeleted", true);
        deleteData.put("deletedAt", com.google.firebase.Timestamp.now());
        deleteData.put("deletedBy", currentCoachId);

        firestore.collection("users")
                .document(client.getUid())
                .update(deleteData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, client.getName() + " deleted permanently", Toast.LENGTH_SHORT).show();
                    // ✅ Listener will handle removal automatically
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete client: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("DeleteClient", "Error: " + e.getMessage(), e);
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            loadingLayout.setVisibility(View.VISIBLE);
            archivedClientsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        } else {
            loadingLayout.setVisibility(View.GONE);
            updateUI();
        }
    }

    private void updateUI() {
        if (archivedClientsList.isEmpty()) {
            archivedClientsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            archivedClientsCount.setText("0 Archived Clients");
        } else {
            archivedClientsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            archivedClientsCount.setText(archivedClientsList.size() + " Archived Clients");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ✅ Clean up listener
        if (archiveListener != null) {
            archiveListener.remove();
            archiveListener = null;
        }
    }
}