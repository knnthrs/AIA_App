package com.example.signuploginrealtime;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class QR extends AppCompatActivity {

    private ImageView qrCodeImage, backButton;
    private TextView qrUserName, membershipStatus;
    private CardView attendanceHistoryCard;
    private LinearLayout attendanceHeader, attendanceContent;
    private ImageView expandIcon;
    private RecyclerView attendanceRecyclerView;
    private TextView noRecordsText, totalVisitsText, thisMonthText;
    private AttendanceAdapter attendanceAdapter;
    private List<AttendanceRecord> attendanceRecords;

    private boolean isAttendanceExpanded = false;

    // User data variables
    private String userName = "Loading...";
    private String membershipStatusText = "Loading...";
    private String membershipType = "Basic";
    private boolean isActive = false;

    // Firebase references
    private FirebaseFirestore firestore;
    private DocumentReference userDocRef;
    private ListenerRegistration userDataListener;
    private ListenerRegistration attendanceListener;
    private LinearLayout noMembershipContainer;
    private android.widget.Button btnGetMembership;
    private ListenerRegistration membershipListener;

    private boolean isDeleteMode = false;
    private List<String> selectedAttendanceIds = new ArrayList<>();
    private LinearLayout deleteToolbar;
    private TextView tvDeleteCount;
    private android.widget.Button btnDeleteSelected, btnCancelDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestore = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        ensureDefaultMembershipRecord();
        loadUserData();
        loadMembershipData();
        loadAttendanceHistory();

        // Migrate back handling to OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isDeleteMode) {
                    exitDeleteMode();
                } else {
                    finish();
                    overridePendingTransition(0, 0);
                }
            }
        });
    }

    private void initializeViews() {
        qrCodeImage = findViewById(R.id.qr_code_image);
        backButton = findViewById(R.id.back_button);
        qrUserName = findViewById(R.id.qr_user_name);
        membershipStatus = findViewById(R.id.membership_status);

        attendanceHistoryCard = findViewById(R.id.attendance_history_card);
        attendanceHeader = findViewById(R.id.attendance_header);
        attendanceContent = findViewById(R.id.attendance_content);
        expandIcon = findViewById(R.id.expand_icon);
        attendanceRecyclerView = findViewById(R.id.attendance_recycler_view);
        noRecordsText = findViewById(R.id.no_records_text);
        totalVisitsText = findViewById(R.id.total_visits_text);
        thisMonthText = findViewById(R.id.this_month_text);
        attendanceRecords = new ArrayList<>();
        noMembershipContainer = findViewById(R.id.no_membership_container);
        btnGetMembership = findViewById(R.id.btn_get_membership);

        deleteToolbar = findViewById(R.id.delete_toolbar);
        tvDeleteCount = findViewById(R.id.tv_delete_count);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);
        btnCancelDelete = findViewById(R.id.btn_cancel_delete);

        btnGetMembership.setOnClickListener(v -> {
            Intent intent = new Intent(QR.this, SelectMembership.class);
            startActivity(intent);
        });

        btnDeleteSelected.setOnClickListener(v -> deleteSelectedAttendance());
        btnCancelDelete.setOnClickListener(v -> exitDeleteMode());
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        attendanceHeader.setOnClickListener(v -> toggleAttendanceHistory());
    }


    private void setupRecyclerView() {
        attendanceAdapter = new AttendanceAdapter(attendanceRecords);
        attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        attendanceRecyclerView.setAdapter(attendanceAdapter);

        attendanceAdapter.setOnLongClickListener(position -> {
            if (!isDeleteMode) {
                enterDeleteMode();
            }
            toggleSelection(position);
            return true;
        });

        attendanceAdapter.setOnClickListener(position -> {
            if (isDeleteMode) {
                toggleSelection(position);
            }
        });
    }

    private void toggleAttendanceHistory() {
        isAttendanceExpanded = !isAttendanceExpanded;

        if (isAttendanceExpanded) {
            attendanceContent.setVisibility(View.VISIBLE);
            expandIcon.setRotation(180);
        } else {
            attendanceContent.setVisibility(View.GONE);
            expandIcon.setRotation(0);
        }
    }

    private void loadAttendanceHistory() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            firestore = FirebaseFirestore.getInstance();

            if (attendanceListener != null) {
                attendanceListener.remove();
            }

            attendanceListener = firestore.collection("users")
                    .document(currentUser.getUid())
                    .collection("attendanceHistory")
                    .orderBy("timeInTimestamp", Query.Direction.DESCENDING)
                    .limit(20)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            Toast.makeText(QR.this, "Failed to load attendance history",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (snapshots != null) {
                            attendanceRecords.clear();
                            int thisMonthCount = 0;

                            for (QueryDocumentSnapshot doc : snapshots) {
                                try {
                                    long timeIn = readMillis(doc, "timeInTimestamp", "timeIn", "timelnTimestamp", "time_in");
                                    long timeOut = readMillis(doc, "timeOutTimestamp", "timeOut", "timeoutTimestamp", "time_out");

                                    if (timeIn <= 0L) {
                                        // Skip malformed document
                                        continue;
                                    }

                                    String status = timeOut > 0 ? "completed" : "active";

                                    AttendanceRecord record = new AttendanceRecord(timeIn, timeOut, status);
                                    record.setDocumentId(doc.getId());
                                    attendanceRecords.add(record);

                                    if (isThisMonth(timeIn)) {
                                        thisMonthCount++;
                                    }
                                } catch (Exception ex) {
                                    Log.w("QR", "Skipping malformed attendance doc: " + doc.getId(), ex);
                                }
                            }

                            attendanceAdapter.notifyDataSetChanged();

                            totalVisitsText.setText(String.valueOf(attendanceRecords.size()));
                            thisMonthText.setText(String.valueOf(thisMonthCount));

                            if (attendanceRecords.isEmpty()) {
                                noRecordsText.setVisibility(View.VISIBLE);
                                attendanceRecyclerView.setVisibility(View.GONE);
                            } else {
                                noRecordsText.setVisibility(View.GONE);
                                attendanceRecyclerView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        }
    }

    // Helper: read a timestamp-like field from Firestore as milliseconds
    private long readMillis(DocumentSnapshot doc, String primaryKey, String... alternateKeys) {
        Object value = doc.get(primaryKey);
        if (value == null) {
            for (String k : alternateKeys) {
                value = doc.get(k);
                if (value != null) break;
            }
        }
        if (value == null) return 0L;

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof Timestamp) {
            Date d = ((Timestamp) value).toDate();
            return d != null ? d.getTime() : 0L;
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return 0L; // Unknown format, skip
            }
        }
        return 0L;
    }

    private void ensureDefaultMembershipRecord() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        firestore.collection("memberships")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Map<String, Object> defaultMembership = new HashMap<>();
                        defaultMembership.put("userId", currentUser.getUid());
                        defaultMembership.put("membershipStatus", "inactive");
                        defaultMembership.put("createdAt", com.google.firebase.Timestamp.now());

                        firestore.collection("memberships")
                                .document(currentUser.getUid())
                                .set(defaultMembership)
                                .addOnSuccessListener(v -> {
                                    Log.d("QR", "Default membership record created");

                                    firestore.collection("users")
                                            .document(currentUser.getUid())
                                            .update("membershipStatus", "inactive")
                                            .addOnSuccessListener(u -> {
                                                Log.d("QR", "User membershipStatus set to inactive");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("QR", "Failed to update user membershipStatus", e);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("QR", "Failed to create default membership", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("QR", "Failed to check membership record", e);
                });
    }

    private boolean isThisMonth(long timestamp) {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
        String recordMonth = monthFormat.format(new Date(timestamp));
        String currentMonth = monthFormat.format(new Date());
        return recordMonth.equals(currentMonth);
    }

    private void loadMembershipData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        if (membershipListener != null) {
            membershipListener.remove();
        }

        membershipListener = firestore.collection("memberships")
                .document(currentUser.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        membershipStatusText = "NO PLAN SELECTED";
                        isActive = false;
                        updateUserInfoRealtime();
                        ensurePermanentQRCode();
                        return;
                    }

                    if (snapshot != null && snapshot.exists() && "active".equals(snapshot.getString("membershipStatus"))) {
                        String planLabel = snapshot.getString("membershipPlanLabel");

                        if (planLabel != null && !planLabel.isEmpty()) {
                            membershipStatusText = formatMembershipDisplay(planLabel);
                            isActive = true;
                        } else {
                            membershipStatusText = "ACTIVE MEMBER";
                            isActive = true;
                        }
                    } else {
                        membershipStatusText = "NO PLAN SELECTED";
                        isActive = false;
                    }

                    updateUserInfoRealtime();
                    ensurePermanentQRCode();
                });
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            firestore = FirebaseFirestore.getInstance();
            userDocRef = firestore.collection("users").document(currentUser.getUid());

            userDataListener = userDocRef.addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Toast.makeText(QR.this, "Failed to load user data: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateUserInfoRealtime();
                    ensurePermanentQRCode();
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    userName = snapshot.getString("fullname");
                    if (userName == null || userName.isEmpty()) userName = "Unknown User";

                    String membershipPlanCode = snapshot.getString("membershipPlanCode");
                    String membershipPlanLabel = snapshot.getString("membershipPlanLabel");

                    if (membershipPlanCode != null && !membershipPlanCode.isEmpty()) {
                        membershipType = getMembershipTypeFromCode(membershipPlanCode);
                        isActive = true;
                        if (membershipPlanLabel != null && !membershipPlanLabel.isEmpty()) {
                            membershipStatusText = formatMembershipDisplay(membershipPlanLabel);
                        } else {
                            membershipStatusText = membershipType.toUpperCase() + " MEMBER";
                        }
                    } else {
                        membershipType = snapshot.getString("membershipType");
                        if (membershipType == null) membershipType = "No Plan Selected";
                        Boolean activeStatus = snapshot.getBoolean("membershipActive");
                        isActive = activeStatus != null ? activeStatus : false;
                        String dbMembershipStatus = snapshot.getString("membershipStatus");
                        if (dbMembershipStatus != null && !dbMembershipStatus.isEmpty()) {
                            membershipStatusText = dbMembershipStatus.toUpperCase();
                        } else {
                            membershipStatusText = "NO PLAN SELECTED";
                            isActive = false;
                        }
                    }

                    updateUserInfoRealtime();
                    ensurePermanentQRCode();
                } else {
                    createDefaultUserData(currentUser);
                }
            });
        } else {
            updateUserInfoRealtime();
        }
    }

    private void createDefaultUserData(FirebaseUser currentUser) {
        if (currentUser == null) return;

        String email = currentUser.getEmail();
        String fullname = currentUser.getDisplayName();

        userName = fullname != null ? fullname : "Gym Member";
        membershipStatusText = "NO PLAN SELECTED";
        membershipType = "No Plan Selected";
        isActive = false;

        if (userDocRef != null) {
            userDocRef.set(new UserProfileFirestore(fullname, email));
        }

        updateUserInfoRealtime();
        ensurePermanentQRCode();
    }

    private String getMembershipTypeFromCode(String planCode) {
        if (planCode == null) return "No Plan Selected";

        if (planCode.startsWith("PT_")) {
            return "Personal Training";
        } else if (planCode.startsWith("STANDARD_")) {
            return "Standard";
        } else {
            return "Standard";
        }
    }

    private String formatMembershipDisplay(String planLabel) {
        if (planLabel == null || planLabel.isEmpty()) {
            return "NO PLAN SELECTED";
        }

        String upperLabel = planLabel.toUpperCase();

        if (upperLabel.contains("PT SESSIONS")) {
            if (upperLabel.contains("1 MONTH")) {
                return "1 MONTH + PT PLAN";
            } else if (upperLabel.contains("3 MONTHS")) {
                return "3 MONTHS + PT PLAN";
            } else {
                return "PERSONAL TRAINING PLAN";
            }
        } else {
            if (upperLabel.contains("1 MONTH")) {
                return "1 MONTH PLAN";
            } else if (upperLabel.contains("3 MONTHS")) {
                return "3 MONTHS PLAN";
            } else if (upperLabel.contains("6 MONTHS")) {
                return "6 MONTHS PLAN";
            } else if (upperLabel.contains("12 MONTHS") || upperLabel.contains("1 YEAR")) {
                return "1 YEAR PLAN";
            } else {
                return "STANDARD PLAN";
            }
        }
    }

    private void updateUserInfoRealtime() {
        qrUserName.setText(userName);
        membershipStatus.setText(membershipStatusText);

        CardView statusCard = (CardView) membershipStatus.getParent();

        if (isActive || membershipStatusText.toUpperCase().contains("ACTIVE")) {
            if (membershipType.equalsIgnoreCase("Premium") || membershipType.equalsIgnoreCase("VIP")) {
                membershipStatus.setTextColor(0xFF9C27B0);
                statusCard.setCardBackgroundColor(0xFFE1BEE7);
            } else {
                membershipStatus.setTextColor(0xFF388E3C);
                statusCard.setCardBackgroundColor(0xFFE8F5E8);
            }
        } else {
            membershipStatus.setTextColor(0xFFD32F2F);
            statusCard.setCardBackgroundColor(0xFFFFEBEE);
        }
    }

    private void ensurePermanentQRCode() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String qrData = currentUser.getUid(); // Only UID

        // Always render immediately so UI never blocks on Firestore
        generateQRCode(qrData);
        showQRCode();

        // Best-effort persist to Firestore
        if (userDocRef != null) {
            userDocRef.update("qrCode", qrData)
                .addOnFailureListener(e -> Log.w("QR", "Failed to persist qrCode", e));
        }
    }

    private String formatMembershipTypeForQR(String planLabel) {
        if (planLabel == null || planLabel.isEmpty()) {
            return "Standard";
        }

        String upper = planLabel.toUpperCase();

        if (upper.contains("PT") || upper.contains("PERSONAL TRAINING")) {
            return "PT";
        } else if (upper.contains("DAILY")) {
            return "Daily";
        } else if (upper.contains("1 MONTH")) {
            return "1Month";
        } else if (upper.contains("3 MONTHS")) {
            return "3Months";
        } else if (upper.contains("6 MONTHS")) {
            return "6Months";
        } else if (upper.contains("12 MONTHS") || upper.contains("1 YEAR")) {
            return "1Year";
        } else {
            return "Standard";
        }
    }

    private void showNoMembershipMessage() {
        CardView qrCodeCard = findViewById(R.id.qr_code_card);
        qrCodeCard.setVisibility(View.GONE);
        noMembershipContainer.setVisibility(View.VISIBLE);
    }

    private void showQRCode() {
        CardView qrCodeCard = findViewById(R.id.qr_code_card);
        qrCodeCard.setVisibility(View.VISIBLE);
        noMembershipContainer.setVisibility(View.GONE);
    }

    private void generateQRCode(String text) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            int size = 512;
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            qrCodeImage.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userDataListener != null) {
            userDataListener.remove();
        }
        if (attendanceListener != null) {
            attendanceListener.remove();
        }
        if (membershipListener != null) {
            membershipListener.remove();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (userDataListener != null) {
            userDataListener.remove();
        }
        if (attendanceListener != null) {
            attendanceListener.remove();
        }
        if (membershipListener != null) {
            membershipListener.remove();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userDocRef != null && userDataListener == null) {
            userDataListener = userDocRef.addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Toast.makeText(QR.this, "Failed to load user data: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateUserInfoRealtime();
                    ensurePermanentQRCode();
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    userName = snapshot.getString("fullname");
                    if (userName == null || userName.isEmpty()) userName = "Unknown User";

                    String membershipPlanCode = snapshot.getString("membershipPlanCode");
                    String membershipPlanLabel = snapshot.getString("membershipPlanLabel");

                    if (membershipPlanCode != null && !membershipPlanCode.isEmpty()) {
                        membershipType = getMembershipTypeFromCode(membershipPlanCode);
                        isActive = true;
                        if (membershipPlanLabel != null && !membershipPlanLabel.isEmpty()) {
                            membershipStatusText = formatMembershipDisplay(membershipPlanLabel);
                        } else {
                            membershipStatusText = membershipType.toUpperCase() + " MEMBER";
                        }
                    } else {
                        membershipType = snapshot.getString("membershipType");
                        if (membershipType == null) membershipType = "No Plan Selected";
                        Boolean activeStatus = snapshot.getBoolean("membershipActive");
                        isActive = activeStatus != null ? activeStatus : false;
                        String dbMembershipStatus = snapshot.getString("membershipStatus");
                        if (dbMembershipStatus != null && !dbMembershipStatus.isEmpty()) {
                            membershipStatusText = dbMembershipStatus.toUpperCase();
                        } else {
                            membershipStatusText = "NO PLAN SELECTED";
                            isActive = false;
                        }
                    }

                    updateUserInfoRealtime();
                    ensurePermanentQRCode();
                } else {
                    createDefaultUserData(FirebaseAuth.getInstance().getCurrentUser());
                }
            });
        }

        loadMembershipData();
        loadAttendanceHistory();
    }

    private static class UserProfileFirestore {
        public String fullname, email, phone, dateOfBirth;

        public UserProfileFirestore(String fullname, String email) {
            this.fullname = fullname;
            this.email = email;
            this.phone = "";
            this.dateOfBirth = "";
        }
    }

    private void enterDeleteMode() {
        isDeleteMode = true;
        selectedAttendanceIds.clear();
        attendanceAdapter.setDeleteMode(true);
        deleteToolbar.setVisibility(View.VISIBLE);
    }

    private void exitDeleteMode() {
        isDeleteMode = false;
        selectedAttendanceIds.clear();
        attendanceAdapter.setDeleteMode(false);
        attendanceAdapter.clearSelections();
        deleteToolbar.setVisibility(View.GONE);
    }

    private void toggleSelection(int position) {
        AttendanceRecord record = attendanceRecords.get(position);
        String docId = record.getDocumentId();

        if (selectedAttendanceIds.contains(docId)) {
            selectedAttendanceIds.remove(docId);
            attendanceAdapter.deselectItem(position);
        } else {
            selectedAttendanceIds.add(docId);
            attendanceAdapter.selectItem(position);
        }

        tvDeleteCount.setText(selectedAttendanceIds.size() + " selected");

        if (selectedAttendanceIds.isEmpty()) {
            exitDeleteMode();
        }
    }

    private void deleteSelectedAttendance() {
        if (selectedAttendanceIds.isEmpty()) {
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Attendance")
                .setMessage("Delete " + selectedAttendanceIds.size() + " selected record(s)?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null) return;

                    int totalItems = selectedAttendanceIds.size();
                    int[] deletedCount = {0};

                    for (String docId : selectedAttendanceIds) {
                        firestore.collection("users")
                                .document(currentUser.getUid())
                                .collection("attendanceHistory")
                                .document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    deletedCount[0]++;
                                    if (deletedCount[0] == totalItems) {
                                        Toast.makeText(this, "Deleted " + totalItems + " record(s)",
                                                Toast.LENGTH_SHORT).show();
                                        exitDeleteMode();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("QR", "Failed to delete: " + docId, e);
                                    Toast.makeText(this, "Some records failed to delete",
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // XML onClick wrapper (if layout uses android:onClick)
    public void deleteSelectedAttendance(View v) {
        deleteSelectedAttendance();
    }
}

