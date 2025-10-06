package com.example.signuploginrealtime;

import android.os.Bundle;
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
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_archive);

        // Handle window insets properly
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

                    // Load archived users
                    firestore.collection("users")
                            .whereEqualTo("coachId", currentCoachId)
                            .whereEqualTo("isArchived", true)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                archivedClientsList.clear();

                                if (queryDocumentSnapshots.isEmpty()) {
                                    showLoading(false);
                                    updateUI();
                                    return;
                                }

                                for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    try {
                                        String name = document.getString("fullname");
                                        String email = document.getString("email");
                                        String fitnessGoal = document.getString("fitnessGoal");
                                        String fitnessLevel = document.getString("fitnessLevel");
                                        String membershipStatus = document.getString("membershipStatus");

                                        Long height = document.getLong("height");
                                        Long weight = document.getLong("weight");

                                        String weightStr = weight != null ? weight + " kg" : "N/A";
                                        String heightStr = height != null ? height + " cm" : "N/A";

                                        name = name != null ? name : "Unknown User";
                                        email = email != null ? email : "";
                                        fitnessGoal = fitnessGoal != null ? fitnessGoal : "General Fitness";
                                        fitnessLevel = fitnessLevel != null ? fitnessLevel : "Beginner";

                                        coach_clients.Client client = new coach_clients.Client(
                                                name, email, "Archived", weightStr, heightStr, fitnessGoal, fitnessLevel
                                        );
                                        client.setUid(document.getId());

                                        archivedClientsList.add(client);

                                    } catch (Exception e) {
                                        android.util.Log.e("ArchiveLoad", "Error parsing archived client: " + e.getMessage(), e);
                                    }
                                }

                                adapter.notifyDataSetChanged();
                                showLoading(false);
                                updateUI();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Failed to load archived clients", Toast.LENGTH_SHORT).show();
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
                .setMessage("Restore " + client.getName() + " to active clients?")
                .setPositiveButton("Restore", (dialog, which) -> restoreClient(client))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void restoreClient(coach_clients.Client client) {
        firestore.collection("users")
                .document(client.getUid())
                .update(
                        "isArchived", FieldValue.delete(),
                        "archivedBy", FieldValue.delete(),
                        "archivedAt", FieldValue.delete(),
                        "archivedReason", FieldValue.delete()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, client.getName() + " restored successfully", Toast.LENGTH_SHORT).show();
                    archivedClientsList.remove(client);
                    adapter.notifyDataSetChanged();
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to restore client", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteDialog(coach_clients.Client client) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Client")
                .setMessage("Permanently delete " + client.getName() + " from archive?\n\nThis cannot be undone!")
                .setPositiveButton("Delete", (dialog, which) -> deleteClient(client))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteClient(coach_clients.Client client) {
        // Note: You might want to keep the data and just mark it as deleted
        // instead of actually deleting the document
        firestore.collection("users")
                .document(client.getUid())
                .update("isDeleted", true, "deletedAt", com.google.firebase.Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, client.getName() + " deleted from archive", Toast.LENGTH_SHORT).show();
                    archivedClientsList.remove(client);
                    adapter.notifyDataSetChanged();
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete client", Toast.LENGTH_SHORT).show();
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
}