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

        if (archiveListener != null) {
            archiveListener.remove();
            archiveListener = null;
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

                    archiveListener = firestore.collection("users")
                            .whereEqualTo("isArchived", true)
                            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                                if (e != null) {
                                    Log.e("ArchiveLoad", "Error listening for archived clients", e);
                                    showLoading(false);
                                    return;
                                }

                                if (queryDocumentSnapshots == null) {
                                    showLoading(false);
                                    updateUI();
                                    return;
                                }

                                archivedClientsList.clear();

                                for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    String userId = document.getId();
                                    try {
                                        String archivedBy = document.getString("archivedBy");
                                        String assignedCoachId = document.getString("coachId");
                                        Boolean isArchived = document.getBoolean("isArchived");
                                        Boolean isDeleted = document.getBoolean("isDeleted");
                                        com.google.firebase.Timestamp archivedAt = document.getTimestamp("archivedAt");
                                        com.google.firebase.Timestamp deletedAt = document.getTimestamp("deletedAt");

                                        Log.d("ArchiveDebug", "=== Checking client: " + userId + " ===");
                                        Log.d("ArchiveDebug", "currentCoachId: " + currentCoachId);
                                        Log.d("ArchiveDebug", "archivedBy: " + archivedBy);
                                        Log.d("ArchiveDebug", "assignedCoachId: " + assignedCoachId);
                                        Log.d("ArchiveDebug", "isArchived: " + isArchived);
                                        Log.d("ArchiveDebug", "isDeleted: " + isDeleted);

                                        boolean deletedAfterArchived = false;
                                        if (isDeleted != null && isDeleted && deletedAt != null && archivedAt != null) {
                                            deletedAfterArchived = deletedAt.toDate().after(archivedAt.toDate());
                                            Log.d("ArchiveDebug", "deletedAfterArchived: " + deletedAfterArchived);
                                        }

                                        if (deletedAfterArchived) {
                                            Log.d("ArchiveDebug", "❌ SKIPPING - Deleted after archived");
                                            continue;
                                        }

                                        if (currentCoachId.equals(archivedBy) &&
                                                (isArchived != null && isArchived) &&
                                                (assignedCoachId == null || !assignedCoachId.equals(currentCoachId))) {

                                            Log.d("ArchiveDebug", "✅ ADDING TO ARCHIVE LIST");

                                            String name = document.getString("fullname");
                                            String email = document.getString("email");
                                            String fitnessGoal = document.getString("fitnessGoal");
                                            String fitnessLevel = document.getString("fitnessLevel");
                                            String archiveReason = document.getString("archiveReason");
                                            String profilePictureUrl = document.getString("profilePictureUrl");  // ✅ ADDED

                                            Long height = document.getLong("height");
                                            Long weight = document.getLong("weight");

                                            String weightStr = weight != null ? weight + " kg" : "N/A";
                                            String heightStr = height != null ? height + " cm" : "N/A";

                                            name = name != null ? name : "Unknown User";
                                            email = email != null ? email : "";
                                            fitnessGoal = fitnessGoal != null ? fitnessGoal : "General Fitness";
                                            fitnessLevel = fitnessLevel != null ? fitnessLevel : "Beginner";

                                            String statusText = "Archived";
                                            if (archiveReason != null) {
                                                statusText = "Archived: " + archiveReason;
                                            }

                                            coach_clients.Client client = new coach_clients.Client(
                                                    name, email, statusText, weightStr, heightStr, fitnessGoal, fitnessLevel
                                            );
                                            client.setUid(userId);
                                            client.setProfilePictureUrl(profilePictureUrl);  // ✅ ADDED
                                            archivedClientsList.add(client);
                                        }
                                    } catch (Exception ex) {
                                        Log.e("ArchiveLoad", "Error: " + ex.getMessage(), ex);
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

    private coach_clients.Client findArchivedClientById(String userId) {
        for (coach_clients.Client client : archivedClientsList) {
            if (client.getUid() != null && client.getUid().equals(userId)) {
                return client;
            }
        }
        return null;
    }

    private void removeArchivedClientFromList(String userId) {
        coach_clients.Client toRemove = findArchivedClientById(userId);
        if (toRemove != null) {
            archivedClientsList.remove(toRemove);
            Log.d("RemoveArchived", "Removed client from archive list: " + userId);
            adapter.notifyDataSetChanged();
            updateUI();
        }
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
        removeArchivedClientFromList(client.getUid());

        Map<String, Object> restoreData = new HashMap<>();
        restoreData.put("isArchived", false);
        restoreData.put("archivedBy", null);
        restoreData.put("archivedAt", null);
        restoreData.put("archiveReason", null);
        restoreData.put("coachId", currentCoachId);

        firestore.collection("users")
                .document(client.getUid())
                .update(restoreData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, client.getName() + " restored successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to restore client: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("RestoreClient", "Error: " + e.getMessage(), e);
                    loadArchivedClients();
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
        removeArchivedClientFromList(client.getUid());

        Map<String, Object> deleteData = new HashMap<>();
        deleteData.put("isDeleted", true);
        deleteData.put("deletedAt", com.google.firebase.Timestamp.now());
        deleteData.put("deletedBy", currentCoachId);
        deleteData.put("isArchived", false);
        deleteData.put("archivedBy", null);
        deleteData.put("archivedAt", null);
        deleteData.put("archiveReason", null);

        firestore.collection("users")
                .document(client.getUid())
                .update(deleteData)
                .addOnSuccessListener(aVoid -> {
                    // Also delete from coach's students subcollection
                    deleteFromStudentsSubcollection(client.getUid(), client.getName());
                    Toast.makeText(this, client.getName() + " deleted permanently", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete client: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("DeleteClient", "Error: " + e.getMessage(), e);
                    loadArchivedClients();
                });
    }

    private void deleteFromStudentsSubcollection(String userId, String clientName) {
        if (currentCoachId == null || currentCoachId.isEmpty()) {
            Log.e("DeleteStudent", "Coach ID is null, cannot delete from subcollection");
            return;
        }

        firestore.collection("coaches")
                .document(currentCoachId)
                .collection("students")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("DeleteStudent", "✅ Client deleted from students subcollection: " + clientName);
                })
                .addOnFailureListener(e -> {
                    Log.e("DeleteStudent", "❌ Failed to delete from subcollection: " + e.getMessage(), e);
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

        if (archiveListener != null) {
            archiveListener.remove();
            archiveListener = null;
        }
    }
}