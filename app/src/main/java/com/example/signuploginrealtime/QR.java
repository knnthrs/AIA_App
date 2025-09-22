package com.example.signuploginrealtime;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

public class QR extends AppCompatActivity {

    private ImageView qrCodeImage, backButton;
    private TextView qrUserName, memberId, membershipStatus;

    // User data variables
    private String userName = "Loading...";
    private String userMemberId = "-----";
    private String membershipStatusText = "Loading...";
    private String membershipType = "Basic";
    private boolean isActive = false;

    // Firebase references
    private FirebaseFirestore firestore;
    private DocumentReference userDocRef;
    private ListenerRegistration userDataListener;

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

        initializeViews();
        setupClickListeners();
        loadUserData();
    }

    private void initializeViews() {
        qrCodeImage = findViewById(R.id.qr_code_image);
        backButton = findViewById(R.id.back_button);
        qrUserName = findViewById(R.id.qr_user_name);
        memberId = findViewById(R.id.member_id);
        membershipStatus = findViewById(R.id.membership_status);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
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

                    String dbMemberId = snapshot.getString("memberId");
                    if (dbMemberId != null && !dbMemberId.isEmpty()) {
                        userMemberId = dbMemberId;
                    } else {
                        userMemberId = generateMemberId();
                        userDocRef.update("memberId", userMemberId);
                    }

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
        String fullname = currentUser.getDisplayName(); // ✅ Firebase displayName
        String memberId = generateMemberId();

        userName = fullname != null ? fullname : "Gym Member";
        userMemberId = memberId;
        membershipStatusText = "NO PLAN SELECTED";
        membershipType = "No Plan Selected";
        isActive = false;

        if (userDocRef != null) {
            userDocRef.set(new UserProfileFirestore(fullname, email, memberId));
        }

        updateUserInfoRealtime();
        ensurePermanentQRCode();
    }


    private String generateMemberId() {
        long timestamp = System.currentTimeMillis();
        return "GYM" + String.valueOf(timestamp).substring(7);
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
        memberId.setText("Member ID: #" + userMemberId);
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

        if (userDocRef != null) {
            userDocRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot snapshot = task.getResult();
                    String savedQr = snapshot != null ? snapshot.getString("qrCode") : null;
                    if (savedQr != null && !savedQr.isEmpty()) {
                        generateQRCode(savedQr);
                    } else {
                        String qrData = String.format("%s_%s_%s_%s",
                                userMemberId,
                                userName.replaceAll("[\\s\\W]", ""),
                                membershipType.replaceAll("[\\s\\W]", ""),
                                currentUser.getUid());
                        userDocRef.update("qrCode", qrData);
                        generateQRCode(qrData);
                    }
                } else {
                    Toast.makeText(this, "Failed to load QR", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (userDataListener != null) {
            userDataListener.remove();
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

                    String dbMemberId = snapshot.getString("memberId");
                    if (dbMemberId != null && !dbMemberId.isEmpty()) {
                        userMemberId = dbMemberId;
                    } else {
                        userMemberId = generateMemberId();
                        userDocRef.update("memberId", userMemberId);
                    }

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
    }

    // Helper Firestore user profile class
    private static class UserProfileFirestore {
        public String fullname, email, memberId, phone, dateOfBirth, membershipStatus;

        public UserProfileFirestore(String fullname, String email, String memberId) {
            this.fullname = fullname;
            this.email = email;
            this.memberId = memberId;
            this.phone = "";
            this.dateOfBirth = "";
            this.membershipStatus = "Active Member";
        }
    }
}
