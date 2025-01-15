package com.example.dannysim;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.util.Log;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dannysim.adapters.EntriesAdapter;
import com.example.dannysim.models.Entry;
import com.example.dannysim.models.Product;
import com.example.dannysim.models.User;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StaffDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerViewEntries;
    private EntriesAdapter entriesAdapter;
    private ProgressBar progressBar;
    private TextView tvNoEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        tvNoEntries = findViewById(R.id.tvNoEntries);
        recyclerViewEntries = findViewById(R.id.recyclerViewEntries);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Setup cards
        MaterialCardView cardCreateEntry = findViewById(R.id.cardCreateEntry);
        MaterialCardView cardViewStock = findViewById(R.id.cardViewStock);
        MaterialCardView cardViewMyEntries = findViewById(R.id.cardViewMyEntries);

        cardCreateEntry.setOnClickListener(v -> startActivity(new Intent(this, CreateEntryActivity.class)));
        cardViewStock.setOnClickListener(v -> startActivity(new Intent(this, ViewStockActivity.class)));

        // Setup RecyclerView
        recyclerViewEntries = findViewById(R.id.recyclerViewEntries);
        recyclerViewEntries.setLayoutManager(new LinearLayoutManager(this));
        loadRecentEntries();
    }

    private void loadRecentEntries() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        db.collection("entries")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy("controlNumber", Query.Direction.ASCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    if (error != null) {
                        Toast.makeText(this, "Error loading entries: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        List<Entry> entries = new ArrayList<>();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                String entryId = doc.getId();

                                // Handle createdAt as Long instead of Timestamp
                                String entryDate = "N/A";
                                Long createdAtLong = doc.getLong("createdAt");
                                if (createdAtLong != null) {
                                    Date date = new Date(createdAtLong);
                                    entryDate = dateFormat.format(date);
                                }

                                // Get control number
                                Long controlNumberLong = doc.getLong("controlNumber");
                                int controlNum = controlNumberLong != null ? controlNumberLong.intValue() : 0;

                                String entryType = doc.getString("entryType") != null ?
                                        doc.getString("entryType") : "N/A";

                                String driver = doc.getString("driver") != null ?
                                        doc.getString("driver") : "N/A";

                                // Get products map
                                List<Map<String, Object>> productsList = (List<Map<String, Object>>) doc.get("products");
                                List<Product> productList = new ArrayList<>();

                                if (productsList != null && !productsList.isEmpty()) {
                                    for (Map<String, Object> productMap : productsList) {
                                        String productName = (String) productMap.get("product");
                                        Long sold = (Long) productMap.get("sold");
                                        int soldQuantity = sold != null ? sold.intValue() : 0;
                                        productList.add(new Product(productName, soldQuantity));
                                    }
                                }

                                // Use the correct constructor with 'createdAtLong'
                                entries.add(new Entry(
                                        entryId,
                                        entryDate,
                                        controlNum,
                                        entryType,
                                        driver,
                                        createdAtLong, // Use createdAtLong directly
                                        productList
                                ));
                            } catch (Exception e) {
                                Log.e("LoadEntries", "Error parsing entry: " + e.getMessage());
                            }
                        }

                        if (entriesAdapter == null) {
                            entriesAdapter = new EntriesAdapter(entries, entry -> {
                                // Handle entry click - navigate to details
                                Intent intent = new Intent(StaffDashboardActivity.this, EntryDetailsActivity.class);
                                intent.putExtra("entry", entry);
                                startActivity(intent);
                            });
                            recyclerViewEntries.setAdapter(entriesAdapter);
                        } else {
                            entriesAdapter.setEntries(entries);
                        }
                        updateVisibility(entries.isEmpty());
                    } else {
                        updateVisibility(true);
                    }
                });
    }

    private void updateVisibility(boolean isEmpty) {
        if (tvNoEntries != null && recyclerViewEntries != null) {
            tvNoEntries.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerViewEntries.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_all_entries) {
            startActivity(new Intent(this, AllEntriesActivity.class));
        } else if (id == R.id.nav_change_password) {
            showChangePasswordDialog();
        } else if (id == R.id.nav_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showChangePasswordDialog() {
        // Create dialog using Material Design
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        // Initialize the TextInputEditText fields
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmNewPassword);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.setTitle("Change Password");

        // Add buttons
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Change", (DialogInterface.OnClickListener) null); // Set null click listener initially
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialogInterface, i) -> dialog.dismiss());

        dialog.show();

        // Set the positive button click listener after showing the dialog
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Validate inputs
            if (currentPassword.isEmpty()) {
                etCurrentPassword.setError("Current password is required");
                return;
            }
            if (newPassword.isEmpty()) {
                etNewPassword.setError("New password is required");
                return;
            }
            if (confirmPassword.isEmpty()) {
                etConfirmPassword.setError("Please confirm new password");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }
            if (newPassword.length() < 6) {
                etNewPassword.setError("Password must be at least 6 characters");
                return;
            }

            // Show progress and disable buttons
            progressBar.setVisibility(View.VISIBLE);
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

            // Get current user
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Re-authenticate user before changing password
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

                user.reauthenticate(credential)
                        .addOnSuccessListener(aVoid -> {
                            // Authentication successful, proceed with password change
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(StaffDashboardActivity.this,
                                                    "Password updated successfully", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                        } else {
                                            showError(task.getException(), etNewPassword);
                                            enableDialogButtons(dialog);
                                        }
                                        progressBar.setVisibility(View.GONE);
                                    });
                        })
                        .addOnFailureListener(e -> {
                            showError(e, etCurrentPassword);
                            enableDialogButtons(dialog);
                            progressBar.setVisibility(View.GONE);
                        });
            }
        });
    }

    // Helper method to show errors
    private void showError(Exception e, TextInputEditText editText) {
        String message = e != null ? e.getMessage() : "An error occurred";
        if (message.contains("password")) {
            editText.setError(message);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    // Helper method to re-enable dialog buttons
    private void enableDialogButtons(AlertDialog dialog) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, UserTypeActivity.class));
        finish();
    }
}