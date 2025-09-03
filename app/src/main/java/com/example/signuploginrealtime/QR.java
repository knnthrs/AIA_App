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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
    private DatabaseReference userRef;
    private ValueEventListener userDataListener;

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
            userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid());

            // Set up real-time listener for user data
            userDataListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Get user name
                        userName = snapshot.child("name").getValue(String.class);
                        if (userName == null || userName.isEmpty()) {
                            userName = "Unknown User";
                        }

                        // Get member ID from database
                        String dbMemberId = snapshot.child("memberId").getValue(String.class);
                        if (dbMemberId != null && !dbMemberId.isEmpty()) {
                            userMemberId = dbMemberId;
                        } else {
                            userMemberId = generateMemberId();
                            userRef.child("memberId").setValue(userMemberId);
                        }

                        // Get membership info
                        String membershipPlanCode = snapshot.child("membershipPlanCode").getValue(String.class);
                        String membershipPlanLabel = snapshot.child("membershipPlanLabel").getValue(String.class);

                        if (membershipPlanCode != null && !membershipPlanCode.isEmpty()) {
                            membershipType = getMembershipTypeFromCode(membershipPlanCode);
                            isActive = true;

                            if (membershipPlanLabel != null && !membershipPlanLabel.isEmpty()) {
                                membershipStatusText = formatMembershipDisplay(membershipPlanLabel);
                            } else {
                                membershipStatusText = membershipType.toUpperCase() + " MEMBER";
                            }
                        } else {
                            membershipType = snapshot.child("membershipType").getValue(String.class);
                            if (membershipType == null) membershipType = "No Plan Selected";

                            Boolean activeStatus = snapshot.child("membershipActive").getValue(Boolean.class);
                            isActive = activeStatus != null ? activeStatus : false;

                            String dbMembershipStatus = snapshot.child("membershipStatus").getValue(String.class);
                            if (dbMembershipStatus != null && !dbMembershipStatus.isEmpty()) {
                                membershipStatusText = dbMembershipStatus.toUpperCase();
                            } else {
                                membershipStatusText = "NO PLAN SELECTED";
                                isActive = false;
                            }
                        }

                        updateUserInfoRealtime();

                        // Generate permanent QR (or load if exists)
                        ensurePermanentQRCode();
                    } else {
                        createDefaultUserData(currentUser);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(QR.this, "Failed to load user data: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateUserInfoRealtime();
                    ensurePermanentQRCode();
                }
            };

            userRef.addValueEventListener(userDataListener);
        } else {
            updateUserInfoRealtime();
        }
    }

    private void createDefaultUserData(FirebaseUser currentUser) {
        if (currentUser == null) return;

        String email = currentUser.getEmail();
        String defaultName = getDefaultName(email);
        String memberId = generateMemberId();

        userName = defaultName;
        userMemberId = memberId;
        membershipStatusText = "NO PLAN SELECTED";
        membershipType = "No Plan Selected";
        isActive = false;

        userRef.child("name").setValue(defaultName);
        userRef.child("email").setValue(email);
        userRef.child("memberId").setValue(memberId);
        userRef.child("membershipStatus").setValue("No Plan Selected");

        updateUserInfoRealtime();
        ensurePermanentQRCode();
    }

    private String getDefaultName(String email) {
        if (email != null && email.contains("@")) {
            return email.split("@")[0];
        }
        return "Gym Member";
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

        userRef.child("qrCode").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String savedQr = task.getResult().getValue(String.class);
                if (savedQr != null && !savedQr.isEmpty()) {
                    generateQRCode(savedQr);
                } else {
                    String qrData = String.format("%s_%s_%s_%s",
                            userMemberId,
                            userName.replaceAll("[\\s\\W]", ""),
                            membershipType.replaceAll("[\\s\\W]", ""),
                            currentUser.getUid());

                    userRef.child("qrCode").setValue(qrData);
                    generateQRCode(qrData);
                }
            } else {
                Toast.makeText(this, "Failed to load QR", Toast.LENGTH_SHORT).show();
            }
        });
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
        if (userRef != null && userDataListener != null) {
            userRef.removeEventListener(userDataListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (userRef != null && userDataListener != null) {
            userRef.removeEventListener(userDataListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userRef != null && userDataListener != null) {
            userRef.addValueEventListener(userDataListener);
        }
    }
}
