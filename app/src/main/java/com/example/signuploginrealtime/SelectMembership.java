package com.example.signuploginrealtime;

import android.content.SharedPreferences;
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
    private android.widget.CheckBox termsCheckbox;

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
    private ListenerRegistration packagesListener;
    private ListenerRegistration membershipListener;

    private CardView currentlySelectedCard = null;
    private List<CardView> allCards = new ArrayList<>();
    private boolean isProcessingPayment = false;
    private String selectedCoachId = null;
    private String selectedCoachName = null;
    private String selectedScheduleDate = null;
    private String selectedScheduleTime = null;
    private List<String> loadedPackageIds = new ArrayList<>();
    private boolean isInitialLoad = true;
    private boolean warningBannerShown = false;
    private SharedPreferences packageCache;
    private boolean packagesDisplayedFromCache = false;



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
        termsCheckbox = findViewById(R.id.terms_checkbox);

        dailyContainer = findViewById(R.id.daily_container);
        standardContainer = findViewById(R.id.standard_container);
        ptContainer = findViewById(R.id.pt_container);

        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        confirmButtonCard.setVisibility(View.GONE);
        termsCheckbox.setVisibility(View.GONE);

        // Make "Terms and Conditions" text clickable
        setupTermsCheckbox();

        // Display cached packages immediately
        List<Map<String, Object>> cachedPackages = loadPackageDataFromCache();
        if (!cachedPackages.isEmpty()) {
            dailyContainer.removeAllViews();
            standardContainer.removeAllViews();
            ptContainer.removeAllViews();
            allCards.clear();

            for (Map<String, Object> packageData : cachedPackages) {
                String packageId = (String) packageData.get("id");
                String type = (String) packageData.get("type");
                int months = (int) packageData.get("months");
                int durationDays = (int) packageData.get("durationDays");
                int sessions = (int) packageData.get("sessions");
                double price = (double) packageData.get("price");

                CardView card = createPackageCard(packageId, type, months, durationDays, sessions, price);
                addCardToContainer(card, type, sessions, durationDays);
            }

            packagesDisplayedFromCache = true;
        }

        checkAndHandleExpiredMemberships();
        checkExistingMembershipOnce();  // ✅ TAMA TO
        loadPackagesOnce();             // ✅ TAMA TO

        confirmButtonCard.setOnClickListener(v -> {
            if (selectedPackageId == null) {
                Toast.makeText(this, "Please select a plan first.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate terms checkbox
            if (!termsCheckbox.isChecked()) {
                Toast.makeText(this, "Please agree to the Terms and Conditions", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedSessions > 0) {
                showCoachSelectionDialog();
            } else {
                initiatePayMongoPayment();
            }
        });
    }
    private void setupTermsCheckbox() {
        String checkboxText = "I agree to the Terms and Conditions";
        android.text.SpannableString spannableString = new android.text.SpannableString(checkboxText);

        int start = checkboxText.indexOf("Terms and Conditions");
        int end = start + "Terms and Conditions".length();

        android.text.style.ClickableSpan clickableSpan = new android.text.style.ClickableSpan() {
            @Override
            public void onClick(View widget) {
                showFullTermsAndConditions();
            }

            @Override
            public void updateDrawState(android.text.TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#2196F3")); // Blue
                ds.setUnderlineText(true);
            }
        };

        spannableString.setSpan(clickableSpan, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsCheckbox.setText(spannableString);
        termsCheckbox.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    private void checkExistingMembershipOnce() {
        // ONE-TIME check using .get() instead of listener
        db.collection("memberships")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
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

                            //NOW setup real-time listener AFTER initial UI is done
                            setupMembershipListener();
                        } else {
                            hasActiveMembership = false;
                            currentMembershipPlan = "";
                        }
                    } else {
                        hasActiveMembership = false;
                        currentMembershipPlan = "";
                    }

                    Log.d(TAG, "👤 Initial membership check complete");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking membership", e);
                    hasActiveMembership = false;
                    currentMembershipPlan = "";
                });
    }

    private void setupMembershipListener() {
        //Real-time updates AFTER initial load
        membershipListener = db.collection("memberships")
                .document(currentUserId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to membership", error);
                        return;
                    }

                    // Process membership updates (same logic as before but won't trigger on first load)
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
                        }
                    }

                    Log.d(TAG, "👤 Membership updated in real-time");
                });
    }


    private void savePackageDataToCache(List<Map<String, Object>> packagesData) {
        if (packageCache == null) {
            packageCache = getSharedPreferences("SelectMembership_cache", MODE_PRIVATE);
        }

        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray();
            for (Map<String, Object> packageData : packagesData) {
                org.json.JSONObject jsonObject = new org.json.JSONObject(packageData);
                jsonArray.put(jsonObject);
            }

            packageCache.edit().putString("cached_packages_data", jsonArray.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving packages", e);
        }
    }

    private List<Map<String, Object>> loadPackageDataFromCache() {
        if (packageCache == null) {
            packageCache = getSharedPreferences("SelectMembership_cache", MODE_PRIVATE);
        }

        List<Map<String, Object>> packagesList = new ArrayList<>();
        String cachedData = packageCache.getString("cached_packages_data", "");

        if (cachedData.isEmpty()) {
            return packagesList;
        }

        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray(cachedData);
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject jsonObject = jsonArray.getJSONObject(i);

                Map<String, Object> packageData = new HashMap<>();
                packageData.put("id", jsonObject.getString("id"));
                packageData.put("type", jsonObject.getString("type"));
                packageData.put("months", jsonObject.getInt("months"));
                packageData.put("durationDays", jsonObject.getInt("durationDays"));
                packageData.put("sessions", jsonObject.getInt("sessions"));
                packageData.put("price", jsonObject.getDouble("price"));

                packagesList.add(packageData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading packages", e);
        }

        return packagesList;
    }



    private void loadPackagesOnce() {
        if (loadingProgress != null && !packagesDisplayedFromCache) {
            loadingProgress.setVisibility(View.VISIBLE);
        }

        db.collection("packages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }

                    if (queryDocumentSnapshots == null) return;

                    List<String> newPackageSignatures = new ArrayList<>();
                    List<Map<String, Object>> packagesData = new ArrayList<>();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String packageId = document.getId();
                        String type = document.getString("type");
                        Long months = document.getLong("months");
                        Long durationDays = document.getLong("durationDays");
                        Long sessions = document.getLong("sessions");
                        Double price = document.getDouble("price");

                        if (type == null || price == null) continue;
                        if (months == null) months = 0L;
                        if (durationDays == null) durationDays = 0L;
                        if (sessions == null) sessions = 0L;

                        String signature = packageId + "_" + price + "_" + type + "_" + months + "_" + sessions;
                        newPackageSignatures.add(signature);

                        Map<String, Object> packageData = new HashMap<>();
                        packageData.put("id", packageId);
                        packageData.put("type", type);
                        packageData.put("months", months.intValue());
                        packageData.put("durationDays", durationDays.intValue());
                        packageData.put("sessions", sessions.intValue());
                        packageData.put("price", price);
                        packagesData.add(packageData);
                    }

                    savePackageDataToCache(packagesData);

                    if (packageCache == null) {
                        packageCache = getSharedPreferences("SelectMembership_cache", MODE_PRIVATE);
                    }
                    String signaturesStr = android.text.TextUtils.join(",", newPackageSignatures);
                    packageCache.edit().putString("package_signatures", signaturesStr).apply();

                    loadedPackageIds = new ArrayList<>(newPackageSignatures);

                    if (!packagesDisplayedFromCache) {
                        dailyContainer.removeAllViews();
                        standardContainer.removeAllViews();
                        ptContainer.removeAllViews();
                        allCards.clear();

                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
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
                    }

                    setupPackagesListener();
                })
                .addOnFailureListener(error -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }
                    Log.e(TAG, "Error loading packages", error);
                    Toast.makeText(this, "Failed to load packages: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }



    private void setupPackagesListener() {
        packagesListener = db.collection("packages")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to packages", error);
                        return;
                    }

                    if (queryDocumentSnapshots == null) return;

                    // Build NEW signatures AND package data
                    List<String> newPackageSignatures = new ArrayList<>();
                    List<Map<String, Object>> packagesData = new ArrayList<>();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String packageId = document.getId();
                        String type = document.getString("type");
                        Long months = document.getLong("months");
                        Long durationDays = document.getLong("durationDays");
                        Long sessions = document.getLong("sessions");
                        Double price = document.getDouble("price");

                        if (type == null || price == null) continue;
                        if (months == null) months = 0L;
                        if (durationDays == null) durationDays = 0L;
                        if (sessions == null) sessions = 0L;

                        // Build signature
                        String signature = packageId + "_" + price + "_" + type + "_" + months + "_" + sessions;
                        newPackageSignatures.add(signature);

                        // Build package data for cache
                        Map<String, Object> packageData = new HashMap<>();
                        packageData.put("id", packageId);
                        packageData.put("type", type);
                        packageData.put("months", months.intValue());
                        packageData.put("durationDays", durationDays.intValue());
                        packageData.put("sessions", sessions.intValue());
                        packageData.put("price", price);
                        packagesData.add(packageData);
                    }

                    // Sort both lists before comparing
                    List<String> sortedOld = new ArrayList<>(loadedPackageIds);
                    List<String> sortedNew = new ArrayList<>(newPackageSignatures);
                    java.util.Collections.sort(sortedOld);
                    java.util.Collections.sort(sortedNew);

                    // Check if ACTUALLY changed
                    if (sortedOld.equals(sortedNew)) {
                        Log.d(TAG, "📦 Packages unchanged, skipping UI rebuild");
                        return;
                    }

                    Log.d(TAG, "📦 Packages changed, reloading...");

                    //UPDATE CACHE with new data
                    savePackageDataToCache(packagesData);

                    if (packageCache == null) {
                        packageCache = getSharedPreferences("SelectMembership_cache", MODE_PRIVATE);
                    }
                    String signaturesStr = android.text.TextUtils.join(",", newPackageSignatures);
                    packageCache.edit().putString("package_signatures", signaturesStr).apply();

                    loadedPackageIds = new ArrayList<>(newPackageSignatures);

                    // Rebuild UI
                    dailyContainer.removeAllViews();
                    standardContainer.removeAllViews();
                    ptContainer.removeAllViews();
                    allCards.clear();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
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
                });
    }


    private void showActiveMembershipWarning() {
        // Prevent duplicate banners
        if (warningBannerShown) {
            Log.d(TAG, "⚠️ Warning banner already shown, skipping");
            return;
        }

        Log.d(TAG, "📢 Showing warning banner...");

        // Find the ScrollView
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

        if (scrollView == null) {
            Log.e(TAG, "❌ ScrollView not found!");
            return;
        }

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

        // Insert banner at the top
        scrollContent.addView(warningBanner, 0);

        warningBannerShown = true;
        Log.d(TAG, "✅ Warning banner added");
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

        // Set click listener - Select package and show checkbox
        card.setOnClickListener(v -> {
            selectPackage(card, packageId, type, months, durationDays, sessions, price);
        });

        allCards.add(card);
        return card;
    }
    private void showFullTermsAndConditions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Terms and Conditions");

        // Create ScrollView for long content
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        scrollView.setPadding(padding, padding, padding, padding);

        TextView termsText = new TextView(this);
        termsText.setText("TERMS AND CONDITIONS\n\n" +
                "1. PAYMENT & SUBSCRIPTION\n" +
                "• All payments are processed securely through PayMongo\n" +
                "• Membership becomes active immediately upon successful payment\n" +
                "• Subscription is valid for the purchased duration only\n\n" +
                "2. MEMBERSHIP CHANGES\n" +
                "• Changing or upgrading plans will replace your current membership\n" +
                "• No refunds will be provided for unused time on previous plans\n" +
                "• New membership starts immediately upon confirmation\n\n" +
                "3. CANCELLATION & REFUNDS\n" +
                "• All sales are final and non-refundable\n" +
                "• Memberships cannot be transferred to another person\n" +
                "• You may cancel auto-renewal at any time before expiration\n\n" +
                "4. GYM ACCESS & USAGE\n" +
                "• Membership is non-transferable and for personal use only\n" +
                "• Gym rules and regulations must be followed at all times\n\n" +
                "5. LIABILITY\n" +
                "• The gym is not responsible for lost or stolen items\n" +
                "• Members use facilities at their own risk\n" +
                "• Medical clearance may be required for certain activities\n\n" +
                "6. CONDUCT\n" +
                "• Members must follow all gym rules and staff instructions\n" +
                "• Inappropriate behavior may result in membership termination\n" +
                "• Equipment must be used properly and returned after use\n\n" +
                "By agreeing to these terms, you acknowledge that you have read, understood, and accept all conditions.");

        termsText.setTextColor(Color.parseColor("#333333"));
        termsText.setTextSize(14);
        termsText.setLineSpacing(4, 1.2f);

        scrollView.addView(termsText);
        builder.setView(scrollView);

        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(true);

        AlertDialog dialog = builder.create();

        // Apply rounded background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // Style the button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#2196F3")); // Blue
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
        // Double-click to unselect
        if (currentlySelectedCard == card && packageId.equals(selectedPackageId)) {
            unselectPackage();
            return;
        }

        selectedPackageId = packageId;
        selectedPlanType = type;
        selectedMonths = months;
        selectedDurationDays = durationDays;
        selectedSessions = sessions;
        selectedPrice = price;

        resetAllCards();
        enlargeCard(card);
        blackoutOtherCards(card);
        currentlySelectedCard = card;

        if (confirmButtonCard.getVisibility() != View.VISIBLE) {
            confirmButtonCard.setVisibility(View.VISIBLE);
            termsCheckbox.setVisibility(View.VISIBLE);
        }
    }

    private void unselectPackage() {
        selectedPackageId = null;
        selectedPlanType = null;
        selectedMonths = 0;
        selectedDurationDays = 0;
        selectedSessions = 0;
        selectedPrice = 0;
        currentlySelectedCard = null;

        resetAllCards();
        enableAllCards();

        confirmButtonCard.setVisibility(View.GONE);
        termsCheckbox.setVisibility(View.GONE);
        termsCheckbox.setChecked(false);
    }

    private void resetAllCards() {
        for (CardView card : allCards) {
            card.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
        }
    }

    private void blackoutOtherCards(CardView selectedCard) {
        for (CardView card : allCards) {
            if (card != selectedCard) {
                card.setAlpha(0.4f);
                card.setEnabled(false);
            } else {
                card.setAlpha(1f);
                card.setEnabled(true);
            }
        }
    }

    private void enableAllCards() {
        for (CardView card : allCards) {
            card.setAlpha(1f);
            card.setEnabled(true);
        }
    }

    private void enlargeCard(CardView card) {
        card.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
    }

    private void showCoachSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
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

        AlertDialog dialog = builder.create(); // Create the dialog first

        // Apply rounded background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show(); //Show the dialog

        // Style the buttons after showing
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
                        // Go to schedule selection
                        openScheduleSelection();
                    });

                    builder.setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    });

                    AlertDialog dialog = builder.create();

                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
                    }

                    dialog.show();

                    // Style the Cancel button after showing
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
                .get()  // Remove yung .whereEqualTo("status", "active")
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
                    // Go to schedule selection
                    openScheduleSelection();
                })
                .addOnFailureListener(e -> {
                    loadingProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error assigning coach. Proceeding without coach.", Toast.LENGTH_LONG).show();
                    selectedCoachId = null;
                    selectedCoachName = null;
                    initiatePayMongoPayment();
                });
    }

    private void openScheduleSelection() {
        Intent intent = new Intent(SelectMembership.this, ScheduleSelectionActivity.class);
        intent.putExtra("coachId", selectedCoachId);
        intent.putExtra("coachName", selectedCoachName);
        intent.putExtra("sessions", selectedSessions);
        intent.putExtra("packageId", selectedPackageId);
        intent.putExtra("planType", selectedPlanType);
        intent.putExtra("months", selectedMonths);
        intent.putExtra("durationDays", selectedDurationDays);
        intent.putExtra("price", selectedPrice);
        startActivityForResult(intent, 200);
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
            attributes.put("description", description);
            attributes.put("remarks", "Membership: " + description);

            data.put("data", new JSONObject().put("attributes", attributes));

            Log.d(TAG, "PayMongo Request: " + data.toString());

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
        } else if (requestCode == 200) {
            // Schedule selection result
            if (resultCode == RESULT_OK && data != null) {
                selectedScheduleDate = data.getStringExtra("selectedDate");
                selectedScheduleTime = data.getStringExtra("selectedTime");
                String returnedCoachId = data.getStringExtra("coachId");
                String returnedCoachName = data.getStringExtra("coachName");

                // Update coach info if returned
                if (returnedCoachId != null) {
                    selectedCoachId = returnedCoachId;
                }
                if (returnedCoachName != null) {
                    selectedCoachName = returnedCoachName;
                }

                Log.d(TAG, "Schedule selected: " + selectedScheduleDate + " at " + selectedScheduleTime);
                Toast.makeText(this, "Schedule confirmed! Proceeding to payment...", Toast.LENGTH_SHORT).show();

                // Now proceed to payment
                initiatePayMongoPayment();
            } else {
                Toast.makeText(this, "Schedule selection cancelled", Toast.LENGTH_SHORT).show();
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

                        // Check BOTH planLabel AND planType
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

        // Update the user's membership document
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

        // Save coach info if selected
        if (selectedCoachId != null) {
            membershipData.put("coachId", selectedCoachId);
            membershipData.put("coachName", selectedCoachName);

            // Save schedule if selected
            if (selectedScheduleDate != null && selectedScheduleTime != null) {
                membershipData.put("scheduleDate", selectedScheduleDate);
                membershipData.put("scheduleTime", selectedScheduleTime);
            }
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

                    // Handle coach assignment based on package type
                    if (selectedCoachId != null && selectedSessions > 0) {
                        // PT package - assign coach and unarchive if needed
                        userUpdate.put("coachId", selectedCoachId);
                        userUpdate.put("isArchived", false);
                        userUpdate.put("archivedBy", null);
                        userUpdate.put("archivedAt", null);
                        userUpdate.put("archiveReason", null);
                        Log.d(TAG, "✅ Assigning coach for PT package");

                        // Save user to coach's students subcollection immediately
                        saveToCoachStudentsSubcollection(userId, fullName, currentUser.getEmail(), selectedCoachId);

                    } else if (selectedSessions == 0) {
                        // Non-PT package - need to archive if they had a coach
                        // First get current coachId before updating
                        db.collection("users").document(userId).get()
                            .addOnSuccessListener(userDoc -> {
                                String previousCoachId = userDoc.getString("coachId");

                                Map<String, Object> finalUpdate = new HashMap<>(userUpdate);
                                finalUpdate.put("coachId", null);

                                // If they had a coach, archive them
                                if (previousCoachId != null && !previousCoachId.isEmpty()) {
                                    finalUpdate.put("isArchived", true);
                                    finalUpdate.put("archivedBy", previousCoachId);
                                    finalUpdate.put("archivedAt", Timestamp.now());
                                    finalUpdate.put("archiveReason", "Switched to non-PT package");
                                    Log.d(TAG, "🗄️ User had coach " + previousCoachId + ", archiving them");
                                } else {
                                    Log.d(TAG, "ℹ️ User had no coach, just removing coachId");
                                }

                                // Apply the update
                                db.collection("users").document(userId).update(finalUpdate)
                                    .addOnSuccessListener(v2 -> {
                                        Log.d(TAG, "✅ User updated with archive status");
                                        continueWithHistoryAndPayment(userId, fullName, paymentMethod, startTimestamp, expirationTimestamp);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "❌ Failed to update user with archive", e);
                                    });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Failed to get previous coachId", e);
                                // Fallback: just remove coachId
                                userUpdate.put("coachId", null);
                                db.collection("users").document(userId).update(userUpdate)
                                    .addOnSuccessListener(v2 -> continueWithHistoryAndPayment(userId, fullName, paymentMethod, startTimestamp, expirationTimestamp));
                            });
                        return; // Exit here since we're handling async
                    }


                    Log.d(TAG, "📝 Updating users/" + userId);

                    db.collection("users")
                            .document(userId)
                            .update(userUpdate)
                            .addOnSuccessListener(v -> {
                                Log.d(TAG, "✅ User document updated successfully");

                                // Add to history collection
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

                                            //Add payment record
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

                                                        //Navigate to MainActivity
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


    private void continueWithHistoryAndPayment(String userId, String fullName, String paymentMethod,
                                                Timestamp startTimestamp, Timestamp expirationTimestamp) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Add to history collection
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

                    // Add payment record
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

                    db.collection("users")
                            .document(userId)
                            .collection("paymentHistory")
                            .add(paymentData)
                            .addOnSuccessListener(paymentDocRef -> {
                                Log.d(TAG, "💰 Payment added to paymentHistory");

                                // Save schedule booking if PT package
                                if (selectedScheduleDate != null && selectedScheduleTime != null && selectedCoachId != null) {
                                    saveScheduleBooking(userId, fullName);
                                }

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
                            .addOnFailureListener(e -> navigateToMainAfterError());
                })
                .addOnFailureListener(e -> navigateToMainAfterError());
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

    private void saveScheduleBooking(String userId, String fullName) {
        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("userId", userId);
        scheduleData.put("userName", fullName);
        scheduleData.put("coachId", selectedCoachId);
        scheduleData.put("coachName", selectedCoachName);
        scheduleData.put("date", selectedScheduleDate);
        scheduleData.put("time", selectedScheduleTime);
        scheduleData.put("status", "scheduled");
        scheduleData.put("createdAt", Timestamp.now());

        db.collection("schedules")
                .add(scheduleData)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "📅 Schedule booking saved: " + docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to save schedule booking", e);
                });
    }

    // Save user to coach's students subcollection
    private void saveToCoachStudentsSubcollection(String userId, String fullName, String email, String coachId) {
        // Get user's phone number
        db.collection("users").document(userId).get()
            .addOnSuccessListener(userDoc -> {
                String phone = userDoc.getString("phone");

                // Create student data with only required fields
                Map<String, Object> studentData = new HashMap<>();
                studentData.put("userId", userId);
                studentData.put("name", fullName);
                studentData.put("email", email);
                studentData.put("phone", phone != null ? phone : "N/A");

                // Save to coach's students subcollection
                db.collection("coaches")
                    .document(coachId)
                    .collection("students")
                    .document(userId)
                    .set(studentData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ User saved to coach's students subcollection");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to save to students subcollection: " + e.getMessage(), e);
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to get user phone number: " + e.getMessage(), e);
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

        // Add to history with "expired" status
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

                    //Reset membership to "None"
                    resetMembershipToNone(userId, fullName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to archive expired membership", e);
                    e.printStackTrace();
                    // Still try to reset even if archiving fails
                    resetMembershipToNone(userId, fullName);
                });
    }

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

                    // Update the users collection
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

                                    // checkExistingMembership();
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
        Log.d(TAG, "🧹 Activity destroyed");
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}

