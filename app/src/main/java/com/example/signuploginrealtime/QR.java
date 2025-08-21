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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class QR extends AppCompatActivity {

    private ImageView qrCodeImage, backButton;
    private TextView qrGeneratedTime, qrUserName, memberId, membershipStatus;
    private CardView generateNewQrButton;

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
        qrGeneratedTime = findViewById(R.id.qr_generated_time);
        qrUserName = findViewById(R.id.qr_user_name);
        memberId = findViewById(R.id.member_id);
        membershipStatus = findViewById(R.id.membership_status);
        generateNewQrButton = findViewById(R.id.generate_new_qr_button);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        generateNewQrButton.setOnClickListener(v -> {
            generateNewQRCode();
            Toast.makeText(this, "New QR code generated!", Toast.LENGTH_SHORT).show();
        });
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

                        // Get email for member ID generation
                        String email = snapshot.child("email").getValue(String.class);
                        userMemberId = generateMemberId(currentUser.getUid(), email);

                        // Get membership information
                        membershipType = snapshot.child("membershipType").getValue(String.class);
                        if (membershipType == null) membershipType = "Basic";

                        Boolean activeStatus = snapshot.child("membershipActive").getValue(Boolean.class);
                        isActive = activeStatus != null ? activeStatus : false;

                        // Get membership expiry date if available
                        String expiryDate = snapshot.child("membershipExpiry").getValue(String.class);

                        // Format membership status
                        membershipStatusText = formatMembershipStatus(membershipType, isActive, expiryDate);

                        // Update UI with real-time data
                        updateUserInfoRealtime();

                        // Generate QR code if this is the first load
                        if (qrGeneratedTime.getText().toString().equals("Generated: --:--:--")) {
                            generateNewQRCode();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(QR.this, "Failed to load user data: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Use default values and generate QR
                    updateUserInfoRealtime();
                    if (qrGeneratedTime.getText().toString().equals("Generated: --:--:--")) {
                        generateNewQRCode();
                    }
                }
            };

            // Attach the listener for real-time updates
            userRef.addValueEventListener(userDataListener);
        } else {
            // User not authenticated, use defaults
            updateUserInfoRealtime();
            generateNewQRCode();
        }
    }

    private void updateUserInfoRealtime() {
        // Update name
        qrUserName.setText(userName);

        // Update member ID
        memberId.setText("Member ID: #" + userMemberId);

        // Update membership status with color coding
        membershipStatus.setText(membershipStatusText);

        // Set status colors based on membership state
        CardView statusCard = (CardView) membershipStatus.getParent();

        if (isActive) {
            if (membershipType.equalsIgnoreCase("Premium") || membershipType.equalsIgnoreCase("VIP")) {
                // Premium/VIP members - Purple theme
                membershipStatus.setTextColor(0xFF9C27B0); // Purple
                statusCard.setCardBackgroundColor(0xFFE1BEE7); // Light purple
            } else {
                // Basic active members - Green theme
                membershipStatus.setTextColor(0xFF388E3C); // Green
                statusCard.setCardBackgroundColor(0xFFE8F5E8); // Light green
            }
        } else {
            // Inactive members - Red theme
            membershipStatus.setTextColor(0xFFD32F2F); // Red
            statusCard.setCardBackgroundColor(0xFFFFEBEE); // Light red
        }
    }

    private String formatMembershipStatus(String type, boolean active, String expiryDate) {
        String status = active ? "Active" : "Inactive";
        String formattedType = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

        if (expiryDate != null && !expiryDate.isEmpty() && active) {
            return formattedType + " Member (" + status + ")";
        } else {
            return formattedType + " Member (" + status + ")";
        }
    }

    private String generateMemberId(String uid, String email) {
        // Create a more sophisticated member ID
        String baseId = String.valueOf(Math.abs(uid.hashCode())).substring(0, 4);

        if (email != null && !email.isEmpty()) {
            // Add first letter of email for uniqueness
            String emailPrefix = email.substring(0, 1).toUpperCase();
            return emailPrefix + baseId;
        }

        return "U" + baseId;
    }

    private void generateNewQRCode() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : "anonymous";

        // Create unique QR data with comprehensive user info
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        long timestamp = System.currentTimeMillis();
        String activeStatus = isActive ? "ACTIVE" : "INACTIVE";

        // Enhanced Format: MEMBERID_USERNAME_MEMBERSHIPTYPE_STATUS_USERID_UNIQUEID_TIMESTAMP
        String qrData = String.format("%s_%s_%s_%s_%s_%s_%d",
                userMemberId,
                userName.replaceAll("[\\s\\W]", ""),
                membershipType.toUpperCase(),
                activeStatus,
                userId.substring(0, Math.min(8, userId.length())),
                uniqueId,
                timestamp);

        generateQRCode(qrData);
        updateGeneratedTime();

        // Show success message with member info
        String message = String.format("QR Generated for %s\nMember ID: %s\nStatus: %s",
                userName, userMemberId, activeStatus);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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

    private void updateGeneratedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        qrGeneratedTime.setText("Generated: " + currentTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firebase listener to prevent memory leaks
        if (userRef != null && userDataListener != null) {
            userRef.removeEventListener(userDataListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Optional: Remove listener when app goes to background to save resources
        if (userRef != null && userDataListener != null) {
            userRef.removeEventListener(userDataListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-attach listener when app comes back to foreground
        if (userRef != null && userDataListener != null) {
            userRef.addValueEventListener(userDataListener);
        }
    }
}