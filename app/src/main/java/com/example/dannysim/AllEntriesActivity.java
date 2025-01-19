package com.example.dannysim;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dannysim.adapters.EntriesAdapter;
import com.example.dannysim.models.Entry;
import com.example.dannysim.models.Product;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AllEntriesActivity extends AppCompatActivity implements EntriesAdapter.OnEntryClickListener {
    private static final String TAG = "AllEntriesActivity";
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private EntriesAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvNoEntries;
    private TextView tvCurrentFilter;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat monthYearFormat;
    private Calendar selectedMonth = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_entries);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewEntries);
        progressBar = findViewById(R.id.progressBar);
        tvNoEntries = findViewById(R.id.tvNoEntries);
        tvCurrentFilter = findViewById(R.id.tvCurrentFilter);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("All Entries");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Find the ScrollViews
        HorizontalScrollView headerScroll = findViewById(R.id.headerScrollView);
        HorizontalScrollView contentScroll = findViewById(R.id.contentScrollView);

        // Sync horizontal scrolling
        headerScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                contentScroll.scrollTo(scrollX, 0));

        contentScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                headerScroll.scrollTo(scrollX, 0));

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EntriesAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Update filter display
        updateFilterDisplay();

        // Load initial entries
        loadEntriesWithRealtimeUpdates();

        // Handle filter button click
        findViewById(R.id.filterButton).setOnClickListener(v -> showMonthYearPicker());
    }

    private void showMonthYearPicker() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_month_year_picker, null);
        NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
        NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);

        // Setup month picker
        String[] months = new DateFormatSymbols().getMonths();
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(months);
        monthPicker.setValue(selectedMonth.get(Calendar.MONTH));

        // Setup year picker
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(currentYear - 5); // 5 years back
        yearPicker.setMaxValue(currentYear);
        yearPicker.setValue(selectedMonth.get(Calendar.YEAR));

        new AlertDialog.Builder(this)
                .setTitle("Select Month and Year")
                .setView(dialogView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    selectedMonth.set(Calendar.YEAR, yearPicker.getValue());
                    selectedMonth.set(Calendar.MONTH, monthPicker.getValue());
                    updateFilterDisplay();
                    loadEntriesWithRealtimeUpdates();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear Filter", (dialog, which) -> {
                    selectedMonth = Calendar.getInstance();
                    updateFilterDisplay();
                    loadEntriesWithRealtimeUpdates();
                })
                .show();
    }

    private void updateFilterDisplay() {
        String filterText = getString(R.string.current_filter, monthYearFormat.format(selectedMonth.getTime()));
        tvCurrentFilter.setText(filterText);
    }

    private void loadEntriesWithRealtimeUpdates() {
        showLoading(true);

        // Calculate start and end of selected month
        Calendar startOfMonth = (Calendar) selectedMonth.clone();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);
        startOfMonth.set(Calendar.SECOND, 0);
        startOfMonth.set(Calendar.MILLISECOND, 0);

        Calendar endOfMonth = (Calendar) selectedMonth.clone();
        endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        endOfMonth.set(Calendar.HOUR_OF_DAY, 23);
        endOfMonth.set(Calendar.MINUTE, 59);
        endOfMonth.set(Calendar.SECOND, 59);
        endOfMonth.set(Calendar.MILLISECOND, 999);

        db.collection("entries")
                .whereGreaterThanOrEqualTo("createdAt", startOfMonth.getTimeInMillis())
                .whereLessThanOrEqualTo("createdAt", endOfMonth.getTimeInMillis())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy("controlNumber", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    showLoading(false);

                    if (error != null) {
                        Log.e(TAG, "Error loading entries: ", error);
                        showError("Error loading entries");
                        updateVisibility(true);
                        return;
                    }

                    List<Entry> entries = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Entry entry = parseEntryFromDocument(doc);
                            if (entry != null) {
                                entries.add(entry);
                            }
                        }
                    }

                    updateUI(entries);
                });
    }

    private Entry parseEntryFromDocument(DocumentSnapshot doc) {
        if (!doc.exists()) return null;

        try {
            String entryId = doc.getId();
            Long createdAtLong = doc.getLong("createdAt");
            String entryDate = formatDate(createdAtLong);
            int controlNum = getIntValue(doc.getLong("controlNumber"));
            String entryType = getStringValue(doc.getString("entryType"));
            String driver = getStringValue(doc.getString("driver"));
            List<Product> products = parseProducts(doc.get("products"));

            return new Entry(
                    entryId,
                    entryDate,
                    controlNum,
                    entryType,
                    driver,
                    createdAtLong,
                    products
            );
        } catch (Exception e) {
            Log.e(TAG, "Error creating entry: " + e.getMessage());
            return null;
        }
    }

    private String formatDate(Long timestamp) {
        if (timestamp == null) return "N/A";
        try {
            return dateFormat.format(new Date(timestamp));
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + e.getMessage());
            return "N/A";
        }
    }

    private int getIntValue(Long value) {
        return value != null ? value.intValue() : 0;
    }

    private String getStringValue(String value) {
        return value != null ? value : "N/A";
    }

    @SuppressWarnings("unchecked")
    private List<Product> parseProducts(Object productsObj) {
        List<Product> productList = new ArrayList<>();
        if (!(productsObj instanceof List<?>)) return productList;

        List<?> productsList = (List<?>) productsObj;
        for (Object item : productsList) {
            if (!(item instanceof Map<?, ?>)) continue;

            try {
                Map<String, Object> productMap = (Map<String, Object>) item;
                String productName = (String) productMap.getOrDefault("product", "N/A");
                Long soldLong = (Long) productMap.getOrDefault("sold", 0L);
                int soldQuantity = soldLong != null ? soldLong.intValue() : 0;
                productList.add(new Product(productName, soldQuantity));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing product: " + e.getMessage());
            }
        }
        return productList;
    }

    private void updateUI(List<Entry> entries) {
        showLoading(false);
        adapter.setEntries(entries);
        updateVisibility(entries.isEmpty());
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateVisibility(boolean isEmpty) {
        if (tvNoEntries != null) {
            tvNoEntries.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onEntryClick(Entry entry) {
        try {
            Intent intent = new Intent(this, EntryDetailsActivity.class);
            intent.putExtra("entry", entry);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching details: " + e.getMessage());
            showError("Error opening entry details");
        }
    }
}