package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;
import android.content.Intent;
import android.widget.ProgressBar;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;
import android.app.AlertDialog;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;

public class SelectMembership extends AppCompatActivity {

    private static final String TAG = "SelectMembership";
    private static final String PAYMONGO_SECRET_KEY = "sk_test_7AjfDjSecFKtHZX6ee8Sa95B";

    private View backButton;
    private CardView confirmButtonCard;
    private ProgressBar loadingProgress;

    // Containers for dynamic cards
    private LinearLayout dailyContainer;
    private LinearLayout standardContainer;
    private LinearLayout ptContainer;

    // Active membership tracking
    private boolean hasActiveMembership = false;
    private String currentMembershipPlan = "";
    private Date currentExpirationDate = null;

    private String selectedPackageId = null;
    private String selectedPlanType = null;
    private int selectedMonths = 0;
    private int selectedDurationDays = 0;
    private int selectedSessions = 0;
    private double selectedPrice = 0;

    private FirebaseFirestore db;
    private String currentUserId;
    private Executor executor = Executors.newSingleThreadExecutor();
    private ListenerRegistration packagesListener;  // ✅ ADD THIS
    private ListenerRegistration membershipListener;  // ✅ ADD THIS

    private CardView currentlySelectedCard = null;
    private List<CardView> allCards = new ArrayList<>();
    private boolean isProcessingPayment = false;
    private String selectedCoachId = null;
    private String selectedCoachName = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_membership);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in to select a plan.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        backButton = findViewById(R.id.back_button);
        confirmButtonCard = findViewById(R.id.confirm_membership_button);
        loadingProgress = findViewById(R.id.loading_progress);

        // Get the containers from layout
        dailyContainer = findViewById(R.id.daily_container);
        standardContainer = findViewById(R.id.standard_container);
        ptContainer = findViewById(R.id.pt_container);

        backButton.setOnClickListener(v -> finish());
        confirmButtonCard.setVisibility(View.GONE);

        checkAndHandleExpiredMemberships();  // Check and handle expired first
        checkExistingMembership();            // ✅ ADD THIS LINE - Check for active membership
        loadPackagesFromFirestore();

        confirmButtonCard.setOnClickListener(v -> {
            if (selectedPackageId == null) {
                Toast.makeText(this, "Please select a plan first.", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Check if selected plan has PT sessions
            if (selectedSessions > 0) {
                showCoachSelectionDialog();
            } else {
                initiatePayMongoPayment();
            }
        });
    }

    private void checkExistingMembership() {
        // ✅ Use addSnapshotListener instead of get()
        membershipListener = db.collection("memberships")
                .document(currentUserId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to membership", error);
                        hasActiveMembership = false;
                        currentMembershipPlan = "";
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists() &&
                            "active".equals(documentSnapshot.getString("membershipStatus"))) {

                        String planType = documentSnapshot.getString("membershipPlanType");

                        if (planType != null && !planType.isEmpty() && !planType.equals("None")) {
                            hasActiveMembership = true;

                            Long months = documentSnapshot.getLong("months");
                            Long sessions = documentSnapshot.getLong("sessions");

                            String displayName = generateTitleText(
                                    planType,
                                    months != null ? months.intValue() : 0,
                                    0,
                                    sessions != null ? sessions.intValue() : 0
                            );

                            currentMembershipPlan = displayName;

                            Timestamp expTimestamp = documentSnapshot.getTimestamp("membershipExpirationDate");
                            if (expTimestamp != null) {
                                currentExpirationDate = expTimestamp.toDate();
                            }

                            showActiveMembershipWarning();
                        } else {
                            hasActiveMembership = false;
                            currentMembershipPlan = "";
                            Log.d(TAG, "Membership exists but plan is 'None' - treating as inactive");
                        }
                    } else {
                        hasActiveMembership = false;
                        currentMembershipPlan = "";
                    }

                    Log.d(TAG, "👤 Membership status updated in real-time");
                });
    }

    private void showActiveMembershipWarning() {
        // Find the ScrollView - it's a direct child of the CoordinatorLayout
        View rootView = findViewById(R.id.main);
        ScrollView scrollView = null;

        if (rootView instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) rootView;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof ScrollView) {
                    scrollView = (ScrollView) child;
                    break;
                }
            }
        }

        if (scrollView == null) return;

        LinearLayout scrollContent = (LinearLayout) scrollView.getChildAt(0);

        // Create warning banner
        CardView warningBanner = new CardView(this);
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bannerParams.setMargins(
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density)
        );
        warningBanner.setLayoutParams(bannerParams);
        warningBanner.setCardBackgroundColor(Color.parseColor("#FFF3CD"));
        warningBanner.setRadius(12 * getResources().getDisplayMetrics().density);
        warningBanner.setCardElevation(4 * getResources().getDisplayMetrics().density);

        // Simple TextView with icon in the text
        TextView warningText = new TextView(this);
        String warningMessage = "⚠️ You have an active membership: " + currentMembershipPlan +
                ". Selecting a new plan will replace your current membership.";
        warningText.setText(warningMessage);
        warningText.setTextColor(Color.parseColor("#856404"));
        warningText.setTextSize(13);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        warningText.setLayoutParams(textParams);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        warningText.setPadding(padding, padding, padding, padding);

        warningBanner.addView(warningText);

        // Insert banner at the top of scroll content (before Daily Package header)
        scrollContent.addView(warningBanner, 0);
    }


    private void loadPackagesFromFirestore() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }

        // ✅ Use addSnapshotListener instead of get()
        packagesListener = db.collection("packages")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }

                    if (error != null) {
                        Log.e(TAG, "Error listening to packages", error);
                        Toast.makeText(this, "Failed to load packages: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (queryDocumentSnapshots == null) return;

                    // Clear existing cards
                    dailyContainer.removeAllViews();
                    standardContainer.removeAllViews();
                    ptContainer.removeAllViews();
                    allCards.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String packageId = document.getId();
                        String type = document.getString("type");
                        Long months = document.getLong("months");
                        Long durationDays = document.getLong("durationDays");
                        Long sessions = document.getLong("sessions");
                        Double price = document.getDouble("price");

                        if (type == null || price == null) continue;
                        if (months == null) months = 0L;
                        if (durationDays == null) durationDays = 0L;

                        CardView card = createPackageCard(
                                packageId,
                                type,
                                months.intValue(),
                                durationDays.intValue(),
                                sessions != null ? sessions.intValue() : 0,
                                price
                        );

                        addCardToContainer(card, type, sessions != null ? sessions.intValue() : 0, durationDays.intValue());
                    }

                    Log.d(TAG, "📦 Packages updated in real-time");
                });
    }


    private void addCardToContainer(CardView card, String type, int sessions, int durationDays) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (300 * getResources().getDisplayMetrics().density),
                (int) (170 * getResources().getDisplayMetrics().density)
        );
        params.setMargins(0, 0, (int) (16 * getResources().getDisplayMetrics().density), 0);
        card.setLayoutParams(params);

        // Determine which container based on type and sessions
        if ("Daily".equals(type) || durationDays == 1) {
            dailyContainer.addView(card);
        } else if (sessions > 0) {
            ptContainer.addView(card);
        } else {
            standardContainer.addView(card);
        }
    }

    private CardView createPackageCard(String packageId, String type, int months, int durationDays, int sessions, double price) {
        CardView card = new CardView(this);
        card.setCardElevation(6 * getResources().getDisplayMetrics().density);
        card.setRadius(32 * getResources().getDisplayMetrics().density);
        card.setCardBackgroundColor(Color.WHITE);
        card.setClickable(true);
        card.setFocusable(true);
        card.setForeground(getDrawable(android.R.drawable.list_selector_background));

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        mainLayout.setPadding(padding, padding, padding, padding);

        // Header with title and badge
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        headerParams.setMargins(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
        headerLayout.setLayoutParams(headerParams);

        // Title section
        LinearLayout titleSection = new LinearLayout(this);
        titleSection.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        titleSection.setLayoutParams(titleParams);

        TextView titleText = new TextView(this);
        titleText.setText(generateTitleText(type, months, durationDays, sessions));
        titleText.setTextColor(Color.parseColor("#333333"));
        titleText.setTextSize(20);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleSection.addView(titleText);

        TextView subtitleText = new TextView(this);
        subtitleText.setText(generateSubtitleText(type, months, sessions));
        subtitleText.setTextColor(Color.parseColor("#666666"));
        subtitleText.setTextSize(14);
        titleSection.addView(subtitleText);

        headerLayout.addView(titleSection);

        // Badge (optional)
        String badgeText = getBadgeText(months, sessions);
        if (badgeText != null) {
            CardView badge = new CardView(this);
            badge.setCardBackgroundColor(getBadgeColor(months, sessions));
            badge.setRadius(24 * getResources().getDisplayMetrics().density);
            badge.setCardElevation(0);

            TextView badgeTextView = new TextView(this);
            badgeTextView.setText(badgeText);
            badgeTextView.setTextColor(Color.WHITE);
            badgeTextView.setTextSize(12);
            badgeTextView.setTypeface(null, android.graphics.Typeface.BOLD);
            int badgePadding = (int) (6 * getResources().getDisplayMetrics().density);
            int badgePaddingH = (int) (12 * getResources().getDisplayMetrics().density);
            badgeTextView.setPadding(badgePaddingH, badgePadding, badgePaddingH, badgePadding);

            badge.addView(badgeTextView);
            headerLayout.addView(badge);
        }

        mainLayout.addView(headerLayout);

        // Price
        TextView priceText = new TextView(this);
        priceText.setText("₱" + String.format("%.0f", price));
        priceText.setTextColor(getPriceColor(sessions));
        priceText.setTextSize(28);
        priceText.setTypeface(null, android.graphics.Typeface.BOLD);
        mainLayout.addView(priceText);

        // Features
        TextView featuresText = new TextView(this);
        featuresText.setText(generateFeaturesText(sessions));
        featuresText.setTextColor(Color.parseColor("#666666"));
        featuresText.setTextSize(12);
        LinearLayout.LayoutParams featuresParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        featuresParams.setMargins(0, (int) (4 * getResources().getDisplayMetrics().density), 0, 0);
        featuresText.setLayoutParams(featuresParams);
        mainLayout.addView(featuresText);

        // Highlight current membership
        if (isCurrentMembership(type, months, sessions)) {
            card.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // Light green background

            // Add "Current Plan" badge
            TextView currentBadge = new TextView(this);
            currentBadge.setText("✓ Current Plan");
            currentBadge.setTextColor(Color.parseColor("#2E7D32"));
            currentBadge.setTextSize(11);
            currentBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            currentBadge.setBackgroundColor(Color.parseColor("#C8E6C9"));
            int badgePadding = (int) (4 * getResources().getDisplayMetrics().density);
            int badgePaddingH = (int) (8 * getResources().getDisplayMetrics().density);
            currentBadge.setPadding(badgePaddingH, badgePadding, badgePaddingH, badgePadding);

            LinearLayout.LayoutParams currentBadgeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            currentBadgeParams.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, 0);
            currentBadge.setLayoutParams(currentBadgeParams);

            mainLayout.addView(currentBadge);
        }

        card.addView(mainLayout);

        // Set click listener
        card.setOnClickListener(v -> {
            if (hasActiveMembership) {
                showMembershipChangeConfirmation(card, packageId, type, months, durationDays, sessions, price);
            } else {
                selectPackage(card, packageId, type, months, durationDays, sessions, price);
            }
        });

        allCards.add(card);
        return card;
    }

    private void showMembershipChangeConfirmation(CardView card, String packageId,
                                                  String type, int months, int durationDays,
                                                  int sessions, double price) {
        // ✅ Don't show confirmation if current plan is "None"
        if (currentMembershipPlan == null || currentMembershipPlan.equals("None") || currentMembershipPlan.isEmpty()) {
            selectPackage(card, packageId, type, months, durationDays, sessions, price);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("⚠️ Change Membership?");

        String expirationInfo = "";
        if (currentExpirationDate != null) {
            expirationInfo = "\n\nYour current plan expires on: " +
                    android.text.format.DateFormat.format("MMM dd, yyyy", currentExpirationDate);
        }

        String newPlanDisplay = generateTitleText(type, months, durationDays, sessions);

        builder.setMessage("You currently have an active membership:\n\n" +
                "Current Plan: " + currentMembershipPlan + expirationInfo +
                "\n\nNew Plan: " + newPlanDisplay +
                "\n\n⚠️ WARNING: If you proceed with this change, you will:\n" +
                "• Lose access to your current membership\n" +
                "• Forfeit any remaining time on your current plan\n" +
                "• Not receive a refund for the previous payment\n\n" +
                "Are you sure you want to continue?");

        builder.setPositiveButton("Yes, Change Membership", (dialog, which) -> {
            selectPackage(card, packageId, type, months, durationDays, sessions, price);
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setCancelable(true);

        AlertDialog dialog = builder.create();

        // Apply rounded background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // Style the buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#D32F2F"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#666666"));
    }


    private String generateTitleText(String type, int months, int durationDays, int sessions) {
        StringBuilder title = new StringBuilder();

        // For Daily Pass
        if (durationDays == 1 || "Daily".equals(type)) {
            return "Daily";
        }

        // For Standard Monthly (no PT sessions)
        if (sessions == 0) {
            if (months == 1) {
                return "Standard Monthly";
            } else if (months == 3) {
                return "Standard 3 Months";
            } else if (months == 6) {
                return "Standard 6 Months";
            } else if (months == 12) {
                return "Standard Annual";
            }
        }

        // For Monthly with PT
        if (sessions > 0) {
            if (months == 1) {
                return "Monthly with " + sessions + " PT";
            } else if (months == 3) {
                return "3 Months with " + sessions + " PT";
            } else if (months == 6) {
                return "6 Months with " + sessions + " PT";
            } else if (months == 12) {
                return "Annual with " + sessions + " PT";
            }
        }

        // Fallback
        return type;
    }

    private boolean isCurrentMembership(String type, int months, int sessions) {
        if (!hasActiveMembership) {
            return false;
        }

        // For Daily packages, check if both are "Daily"
        if ("Daily".equals(type) || months == 0) {
            return currentMembershipPlan.equals("Daily");
        }

        // Generate display name and compare
        String displayName = generateTitleText(type, months, 0, sessions);
        return currentMembershipPlan.equals(displayName);
    }



    private String generateSubtitleText(String type, int months, int sessions) {
        if (sessions > 0) {
            return "Membership + personal training";
        } else if (months == 1) {
            return "Basic monthly membership";
        } else if (months == 3) {
            return "Save ₱900 vs monthly";
        } else if (months == 6) {
            return "Save ₱3,000 vs monthly";
        } else if (months == 12) {
            return "Save ₱9,000 vs monthly";
        } else {
            return "Perfect for single workout sessions";
        }
    }

    private String getBadgeText(int months, int sessions) {
        if (sessions >= 24) return "Ultimate";
        if (sessions > 0) return "Premium";
        if (months == 12) return "Max Save";
        if (months == 6) return "Best Value";
        if (months == 3) return "Save";
        if (months == 1) return "Popular";
        if (months == 0) return "Try Now";
        return null;
    }

    private int getBadgeColor(int months, int sessions) {
        if (sessions >= 24) return Color.parseColor("#FF5722");
        if (sessions > 0) return Color.parseColor("#9C27B0");
        if (months == 12) return Color.parseColor("#FF5722");
        if (months == 6) return Color.parseColor("#2196F3");
        if (months == 3) return Color.parseColor("#2196F3");
        if (months == 1) return Color.parseColor("#4CAF50");
        return Color.parseColor("#FFC107");
    }

    private int getPriceColor(int sessions) {
        if (sessions >= 24) return Color.parseColor("#FF5722");
        if (sessions > 0) return Color.parseColor("#9C27B0");
        return Color.parseColor("#4CAF50");
    }

    private String generateFeaturesText(int sessions) {
        if (sessions > 0) {
            return "Gym access + " + sessions + " personal training sessions";
        }
        return "Full gym access • All equipment • Locker room";
    }

    private void selectPackage(CardView card, String packageId, String type,
                               int months, int durationDays, int sessions, double price) {
        selectedPackageId = packageId;
        selectedPlanType = type;
        selectedMonths = months;
        selectedDurationDays = durationDays;
        selectedSessions = sessions;
        selectedPrice = price;

        resetAllCards();
        enlargeCard(card);
        currentlySelectedCard = card;

        if (confirmButtonCard.getVisibility() != View.VISIBLE) {
            confirmButtonCard.setVisibility(View.VISIBLE);
        }
    }

    private void resetAllCards() {
        for (CardView card : allCards) {
            card.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
        }
    }

    private void enlargeCard(CardView card) {
        card.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
    }

    private void showCoachSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle); // ✅ Add style
        builder.setTitle("Personal Training Coach");
        builder.setMessage("This membership includes " + selectedSessions + " PT sessions.\n\nHow would you like to choose your coach?");

        builder.setPositiveButton("Choose My Coach", (dialog, which) -> {
            dialog.dismiss();
            showAvailableCoachesList();
        });

        builder.setNegativeButton("Auto-Assign", (dialog, which) -> {
            dialog.dismiss();
            autoAssignCoach();
        });

        builder.setCancelable(true);

        AlertDialog dialog = builder.create(); // ✅ Create the dialog first

        // ✅ Apply rounded background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show(); // ✅ Show the dialog

        // ✅ Style the buttons after showing
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#4CAF50")); // Green
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#2196F3")); // Blue
    }
    private void showAvailableCoachesList() {
        loadingProgress.setVisibility(View.VISIBLE);

        db.collection("coaches")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    loadingProgress.setVisibility(View.GONE);

                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No available coaches at the moment. Auto-assigning...", Toast.LENGTH_SHORT).show();
                        autoAssignCoach();
                        return;
                    }

                    List<String> coachNames = new ArrayList<>();
                    List<String> coachIds = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("fullname");
                        if (name != null) {
                            coachNames.add(name);
                            coachIds.add(doc.getId());
                        }
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
                    builder.setTitle("Select Your Coach");
                    builder.setItems(coachNames.toArray(new String[0]), (dialog, which) -> {
                        selectedCoachId = coachIds.get(which);
                        selectedCoachName = coachNames.get(which);

                        Toast.makeText(this, "Coach selected: " + selectedCoachName, Toast.LENGTH_SHORT).show();
                        initiatePayMongoPayment();
                    });

                    builder.setNegativeButton("Cancel", (dialog, which) -> {  // ✅ Add action
                        dialog.dismiss();
                    });

                    AlertDialog dialog = builder.create();

                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
                    }

                    dialog.show();

                    // ✅ Style the Cancel button after showing
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#D32F2F")); // Red
                })
                .addOnFailureListener(e -> {
                    loadingProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading coaches. Auto-assigning...", Toast.LENGTH_SHORT).show();
                    autoAssignCoach();
                });
    }

    private void autoAssignCoach() {
        loadingProgress.setVisibility(View.VISIBLE);

        db.collection("coaches")
                .get()  // ✅ Remove yung .whereEqualTo("status", "active")
                .addOnSuccessListener(querySnapshot -> {
                    loadingProgress.setVisibility(View.GONE);

                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No coaches available. Membership will be activated without coach assignment.", Toast.LENGTH_LONG).show();
                        selectedCoachId = null;
                        selectedCoachName = null;
                        initiatePayMongoPayment();
                        return;
                    }

                    // Get random coach or first available
                    QueryDocumentSnapshot firstCoach = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                    selectedCoachId = firstCoach.getId();
                    selectedCoachName = firstCoach.getString("fullname");

                    Toast.makeText(this, "Coach assigned: " + selectedCoachName, Toast.LENGTH_SHORT).show();
                    initiatePayMongoPayment();
                })
                .addOnFailureListener(e -> {
                    loadingProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error assigning coach. Proceeding without coach.", Toast.LENGTH_LONG).show();
                    selectedCoachId = null;
                    selectedCoachName = null;
                    initiatePayMongoPayment();
                });
    }


    private void initiatePayMongoPayment() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }
        confirmButtonCard.setEnabled(false);

        int amountInCents = (int) (selectedPrice * 100);

        executor.execute(() -> {
            try {
                String paymentLinkUrl = createPayMongoPaymentLink(amountInCents);

                runOnUiThread(() -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }
                    confirmButtonCard.setEnabled(true);

                    if (paymentLinkUrl != null) {
                        Intent intent = new Intent(SelectMembership.this, PayMongoPaymentActivity.class);
                        intent.putExtra("paymentUrl", paymentLinkUrl);
                        intent.putExtra("packageId", selectedPackageId);
                        intent.putExtra("membershipPlanType", selectedPlanType);
                        intent.putExtra("months", selectedMonths);
                        intent.putExtra("durationDays", selectedDurationDays);
                        intent.putExtra("sessions", selectedSessions);
                        intent.putExtra("price", selectedPrice);
                        startActivityForResult(intent, 100);
                    } else {
                        Toast.makeText(SelectMembership.this,
                                "Failed to create payment link. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error creating payment", e);
                runOnUiThread(() -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }
                    confirmButtonCard.setEnabled(true);
                    Toast.makeText(SelectMembership.this,
                            "Payment error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String createPayMongoPaymentLink(int amountInCents) {
        try {
            URL url = new URL("https://api.paymongo.com/v1/links");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Basic " +
                    android.util.Base64.encodeToString(
                            (PAYMONGO_SECRET_KEY + ":").getBytes(),
                            android.util.Base64.NO_WRAP
                    ));
            conn.setDoOutput(true);

            JSONObject data = new JSONObject();
            JSONObject attributes = new JSONObject();

            String description = generateTitleText(selectedPlanType, selectedMonths, selectedDurationDays, selectedSessions);
            attributes.put("amount", amountInCents);
            attributes.put("description", description);  // ← UPDATED NA TO
            attributes.put("remarks", "Membership: " + description);  // ← UPDATED NA TO

            data.put("data", new JSONObject().put("attributes", attributes));  // ← CORRECT YANG FORMAT NA YAN

            OutputStream os = conn.getOutputStream();
            os.write(data.toString().getBytes());
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "PayMongo Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                String checkoutUrl = jsonResponse.getJSONObject("data")
                        .getJSONObject("attributes")
                        .getString("checkout_url");

                Log.d(TAG, "Payment URL created: " + checkoutUrl);
                return checkoutUrl;
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Log.e(TAG, "PayMongo Error: " + response.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error creating PayMongo link", e);
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            if (resultCode == RESULT_OK && data != null) {
                boolean paymentSuccess = data.getBooleanExtra("paymentSuccess", false);

                if (paymentSuccess) {
                    String paymentMethod = data.getStringExtra("paymentMethod");

                    Log.d(TAG, "✅ Payment successful, proceeding to save membership");

                    // Set flag to prevent user interaction
                    isProcessingPayment = true;

                    // Hide all UI elements to show blank screen
                    View mainView = findViewById(R.id.main);
                    if (mainView != null) {
                        mainView.setVisibility(View.GONE);
                    }

                    // Show loading indicator
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.VISIBLE);
                    }

                    // Small delay to ensure UI updates, then save membership
                    new android.os.Handler().postDelayed(() -> {
                        saveMembership(paymentMethod);
                    }, 300);
                } else {
                    Toast.makeText(this, "Payment was not completed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Payment was cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveMembership(String paymentMethod) {
        createNewMembership(paymentMethod);
    }

    private void createNewMembership(String paymentMethod) {
        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String fullName = "Unknown User";
                    if (userDoc.exists()) {
                        fullName = userDoc.getString("fullname");
                        if (fullName == null || fullName.isEmpty()) {
                            fullName = "Unknown User";
                        }
                    }
                    archiveOldMembershipIfExists(fullName, paymentMethod);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user name", e);
                    archiveOldMembershipIfExists("Unknown User", paymentMethod);
                });
    }


    private void archiveOldMembershipIfExists(String fullName, String paymentMethod) {
        db.collection("memberships")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(existingDoc -> {
                    if (existingDoc.exists() && "active".equals(existingDoc.getString("membershipStatus"))) {
                        String existingPlanType = existingDoc.getString("membershipPlanType");  // ✅ ADD THIS

                        // ✅ Check BOTH planLabel AND planType
                        if (existingPlanType != null && !existingPlanType.isEmpty() && !existingPlanType.equals("None")) {

                            Log.d(TAG, "Replacing existing membership: " + existingPlanType);
                            saveNewMembershipData(fullName, paymentMethod);
                        } else {
                            // Plan is "None" or empty - just create new membership
                            Log.d(TAG, "No real active membership found (planType=" + existingPlanType + "), creating new one");
                            saveNewMembershipData(fullName, paymentMethod);
                        }
                    } else {
                        // No existing active membership
                        Log.d(TAG, "No existing active membership, creating first membership");
                        saveNewMembershipData(fullName, paymentMethod);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing membership", e);
                    saveNewMembershipData(fullName, paymentMethod);
                });
    }

    private void saveNewMembershipData(String fullName, String paymentMethod) {
        Log.d(TAG, "🔵 Starting saveNewMembershipData...");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "❌ Current user is NULL!");
            runOnUiThread(() -> {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                if (loadingProgress != null) loadingProgress.setVisibility(View.GONE);
            });
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "Selected Plan Type: " + selectedPlanType);
        Log.d(TAG, "Price: " + selectedPrice);

        // Generate start and expiration dates
        Timestamp startTimestamp = Timestamp.now();
        Timestamp expirationTimestamp = getExpirationTimestamp(startTimestamp);

        Log.d(TAG, "Start Date: " + startTimestamp.toDate());
        Log.d(TAG, "Expiration Date: " + expirationTimestamp.toDate());

        // ✅ STEP 1: Update the user's membership document
        Map<String, Object> membershipData = new HashMap<>();
        membershipData.put("fullname", fullName);
        membershipData.put("userId", userId);
        membershipData.put("email", currentUser.getEmail());
        membershipData.put("membershipPlanType", selectedPlanType);
        membershipData.put("months", selectedMonths);
        membershipData.put("sessions", selectedSessions);
        membershipData.put("price", selectedPrice);
        membershipData.put("membershipStatus", "active");
        membershipData.put("membershipStartDate", startTimestamp);
        membershipData.put("membershipExpirationDate", expirationTimestamp);
        membershipData.put("lastUpdated", Timestamp.now());

        // ✅ Save coach info if selected
        if (selectedCoachId != null) {
            membershipData.put("coachId", selectedCoachId);
            membershipData.put("coachName", selectedCoachName);
        } else {
            membershipData.put("coachName", "No coach assigned");
        }

        Log.d(TAG, "📝 Writing to memberships/" + userId);

        // Use .set() instead of .add() to update the user's membership document
        db.collection("memberships")
                .document(userId)  // Use userId as document ID
                .set(membershipData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Membership document updated for user: " + userId);

                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("membershipPlanType", selectedPlanType);
                    userUpdate.put("membershipActive", true);
                    userUpdate.put("membershipStatus", "active");
                    userUpdate.put("membershipExpirationDate", expirationTimestamp);
                    userUpdate.put("months", selectedMonths);
                    userUpdate.put("sessions", selectedSessions);

                    // ✅ Save coach assignment if PT package
                    if (selectedCoachId != null) {
                        userUpdate.put("coachId", selectedCoachId);
                    }


                    Log.d(TAG, "📝 Updating users/" + userId);

                    db.collection("users")
                            .document(userId)
                            .update(userUpdate)
                            .addOnSuccessListener(v -> {
                                Log.d(TAG, "✅ User document updated successfully");

                                // ✅ STEP 3: Add to history collection
                                Map<String, Object> historyData = new HashMap<>();
                                historyData.put("fullname", fullName);
                                historyData.put("userId", userId);
                                historyData.put("email", currentUser.getEmail());
                                historyData.put("membershipPlanType", selectedPlanType);
                                historyData.put("months", selectedMonths);
                                historyData.put("sessions", selectedSessions);
                                historyData.put("price", selectedPrice);
                                historyData.put("paymentMethod", paymentMethod);
                                historyData.put("status", "active");
                                historyData.put("timestamp", Timestamp.now());
                                historyData.put("startDate", startTimestamp);
                                historyData.put("expirationDate", expirationTimestamp);

                                Log.d(TAG, "📝 Adding to history collection");

                                db.collection("history")
                                        .add(historyData)
                                        .addOnSuccessListener(historyDocRef -> {
                                            Log.d(TAG, "📜 History record added: " + historyDocRef.getId());

                                            // ✅ STEP 4: Add payment record
                                            Map<String, Object> paymentData = new HashMap<>();
                                            paymentData.put("userId", userId);
                                            paymentData.put("fullname", fullName);
                                            paymentData.put("email", currentUser.getEmail());
                                            paymentData.put("membershipPlanType", selectedPlanType);
                                            paymentData.put("months", selectedMonths);
                                            paymentData.put("sessions", selectedSessions);
                                            paymentData.put("price", selectedPrice);
                                            paymentData.put("paymentMethod", paymentMethod);
                                            paymentData.put("paymentStatus", "paid");
                                            paymentData.put("timestamp", Timestamp.now());
                                            paymentData.put("startDate", startTimestamp);
                                            paymentData.put("expirationDate", expirationTimestamp);

                                            Log.d(TAG, "📝 Adding to users/" + userId + "/paymentHistory");

                                            db.collection("users")
                                                    .document(userId)
                                                    .collection("paymentHistory")
                                                    .add(paymentData)
                                                    .addOnSuccessListener(paymentDocRef -> {
                                                        Log.d(TAG, "💰 Payment added to paymentHistory: " + paymentDocRef.getId());
                                                        Log.d(TAG, "🎉 ALL STEPS COMPLETED! Navigating to MainActivity...");

                                                        // ✅ STEP 5: Navigate to MainActivity
                                                        runOnUiThread(() -> {
                                                            if (loadingProgress != null) {
                                                                loadingProgress.setVisibility(View.GONE);
                                                            }

                                                            Toast.makeText(SelectMembership.this,
                                                                    "Membership activated successfully!",
                                                                    Toast.LENGTH_SHORT).show();

                                                            Intent mainIntent = new Intent(SelectMembership.this, MainActivity.class);
                                                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            mainIntent.putExtra("membershipActivated", true);
                                                            startActivity(mainIntent);
                                                            finish();
                                                        });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "❌ Failed to add payment history: " + e.getMessage(), e);
                                                        // Still navigate even if payment history fails
                                                        navigateToMainAfterError();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ Failed to add history record: " + e.getMessage(), e);
                                            // Still navigate even if history fails
                                            navigateToMainAfterError();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Failed to update user document: " + e.getMessage(), e);
                                runOnUiThread(() -> {
                                    if (loadingProgress != null)
                                        loadingProgress.setVisibility(View.GONE);
                                    Toast.makeText(SelectMembership.this,
                                            "Error updating membership. Please contact support.",
                                            Toast.LENGTH_LONG).show();
                                });
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update membership document: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        if (loadingProgress != null) loadingProgress.setVisibility(View.GONE);
                        Toast.makeText(SelectMembership.this,
                                "Failed to activate membership. Please try again.",
                                Toast.LENGTH_LONG).show();
                    });
                });
    }


    private void navigateToMainAfterError() {
        runOnUiThread(() -> {
            if (loadingProgress != null) {
                loadingProgress.setVisibility(View.GONE);
            }

            Toast.makeText(this,
                    "Membership activated! (Some details may be incomplete)",
                    Toast.LENGTH_SHORT).show();

            Intent mainIntent = new Intent(SelectMembership.this, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            mainIntent.putExtra("membershipActivated", true);
            startActivity(mainIntent);
            finish();
        });
    }


    // Helper to calculate expiration date based on plan duration
    private Timestamp getExpirationTimestamp(Timestamp startTimestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTimestamp.toDate());

        if (selectedMonths > 0) {
            calendar.add(Calendar.MONTH, selectedMonths);
        } else {
            int days = (selectedDurationDays > 0) ? selectedDurationDays : 1;
            calendar.add(Calendar.DAY_OF_MONTH, days);
        }

        Date expirationDate = calendar.getTime();
        return new Timestamp(expirationDate);
    }

    private void checkAndHandleExpiredMemberships() {
        db.collection("memberships")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.d(TAG, "No membership document found");
                        return;
                    }

                    String status = doc.getString("membershipStatus");
                    Timestamp expirationTimestamp = doc.getTimestamp("membershipExpirationDate");

                    // Check if membership is active and has an expiration date
                    if ("active".equals(status) && expirationTimestamp != null) {
                        Date expirationDate = expirationTimestamp.toDate();
                        Date currentDate = new Date();

                        // Check if expired
                        if (currentDate.after(expirationDate)) {
                            Log.d(TAG, "⏰ Membership has expired! Archiving and resetting...");
                            archiveExpiredMembership(doc);
                        } else {
                            Log.d(TAG, "✅ Membership is still active");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking expiration", e);
                });
    }

    /**
     * Archive expired membership to history and reset to "None"
     */
    private void archiveExpiredMembership(com.google.firebase.firestore.DocumentSnapshot membershipDoc) {
        String userId = membershipDoc.getString("userId");
        String planType = membershipDoc.getString("membershipPlanType");
        Double price = membershipDoc.getDouble("price");
        Timestamp startDate = membershipDoc.getTimestamp("membershipStartDate");
        Timestamp expirationDate = membershipDoc.getTimestamp("membershipExpirationDate");

        // Get fullName with default
        String fullNameTemp = membershipDoc.getString("fullname");
        final String fullName = (fullNameTemp == null || fullNameTemp.isEmpty()) ? "Unknown User" : fullNameTemp;

        Log.d(TAG, "🔄 Archiving expired membership for: " + fullName);
        Log.d(TAG, "📊 Plan: " + planType);

        // ✅ STEP 1: Add to history with "expired" status
        Map<String, Object> historyData = new HashMap<>();
        historyData.put("fullname", fullName);
        historyData.put("userId", userId);
        historyData.put("email", membershipDoc.getString("email"));
        historyData.put("membershipPlanType", planType);
        historyData.put("months", membershipDoc.getLong("months"));
        historyData.put("sessions", membershipDoc.getLong("sessions"));
        historyData.put("price", price);
        historyData.put("status", "expired");
        historyData.put("timestamp", Timestamp.now());
        historyData.put("startDate", startDate);
        historyData.put("expirationDate", expirationDate);

        Log.d(TAG, "📝 Writing expired membership to history...");

        db.collection("history")
                .add(historyData)
                .addOnSuccessListener(historyDocRef -> {
                    Log.d(TAG, "✅ Expired membership archived to history: " + historyDocRef.getId());

                    // ✅ STEP 2: Reset membership to "None"
                    resetMembershipToNone(userId, fullName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to archive expired membership", e);
                    e.printStackTrace();
                    // Still try to reset even if archiving fails
                    resetMembershipToNone(userId, fullName);
                });
    }

    /**
     * Reset membership document to "None" status
     */
    private void resetMembershipToNone(String userId, String fullName) {
        Log.d(TAG, "🔄 Resetting membership to 'None' for: " + fullName);

        Map<String, Object> resetData = new HashMap<>();
        resetData.put("fullname", fullName);
        resetData.put("userId", userId);
        resetData.put("email", null);
        resetData.put("membershipPlanType", "None");
        resetData.put("membershipStatus", "expired");
        resetData.put("months", 0);
        resetData.put("sessions", 0);
        resetData.put("price", 0);
        resetData.put("lastUpdated", Timestamp.now());
        resetData.put("membershipStartDate", null);
        resetData.put("membershipExpirationDate", null);

        Log.d(TAG, "📝 Writing to memberships/" + userId);

        db.collection("memberships")
                .document(userId)
                .set(resetData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Membership document reset to 'None'");

                    // ✅ STEP 2: Update the users collection
                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("membershipPlanType", "None");
                    userUpdate.put("membershipActive", false);
                    userUpdate.put("membershipStatus", "expired");
                    userUpdate.put("membershipExpirationDate", null);
                    userUpdate.put("months", 0);
                    userUpdate.put("sessions", 0);


                    Log.d(TAG, "📝 Updating users/" + userId);

                    db.collection("users")
                            .document(userId)
                            .update(userUpdate)
                            .addOnSuccessListener(v -> {
                                Log.d(TAG, "✅ User document updated to 'None'");

                                runOnUiThread(() -> {
                                    hasActiveMembership = false;
                                    currentMembershipPlan = "";
                                    Toast.makeText(SelectMembership.this,
                                            "Your membership has expired",
                                            Toast.LENGTH_SHORT).show();

                                    checkExistingMembership();
                                });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Failed to update user document", e);
                                e.printStackTrace();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to reset membership document", e);
                    e.printStackTrace();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ Remove listeners when activity is destroyed
        if (packagesListener != null) {
            packagesListener.remove();
        }
        if (membershipListener != null) {
            membershipListener.remove();
        }
        Log.d(TAG, "🧹 Listeners removed");
    }
}