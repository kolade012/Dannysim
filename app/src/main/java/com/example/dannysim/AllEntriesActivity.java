package com.example.dannysim;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dannysim.adapters.EntriesAdapter;
import com.example.dannysim.models.Entry;
import com.example.dannysim.models.Product;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AllEntriesActivity extends AppCompatActivity implements EntriesAdapter.OnEntryClickListener {
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private EntriesAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvNoEntries;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_entries);

        // Initialize Firestore and date format
        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("All Entries");
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewEntries);
        progressBar = findViewById(R.id.progressBar);
        tvNoEntries = findViewById(R.id.tvNoEntries);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EntriesAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Load entries
        loadEntries();
    }

    private void loadEntries() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        db.collection("entries")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    List<Entry> entries = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            if (!doc.exists()) {
                                continue;
                            }

                            String entryId = doc.getId();

                            // Handle createdAt with null check
                            String entryDate = "N/A";
                            Long createdAtLong = doc.getLong("createdAt");
                            if (createdAtLong != null) {
                                try {
                                    Date date = new Date(createdAtLong);
                                    entryDate = dateFormat.format(date);
                                } catch (Exception e) {
                                    Log.e("LoadEntries", "Error formatting date: " + e.getMessage());
                                }
                            }

                            // Get control number with default
                            int controlNum = doc.getLong("controlNumber").intValue();

                            // Get strings with defaults
                            String entryType = doc.getString("entryType");
                            if (entryType == null) entryType = "N/A";

                            String driver = doc.getString("driver");
                            if (driver == null) driver = "N/A";

                            // Process products safely
                            List<Product> productList = new ArrayList<>();
                            Object productsObj = doc.get("products");
                            if (productsObj instanceof List<?>) {
                                List<?> productsList = (List<?>) productsObj;
                                for (Object item : productsList) {
                                    if (item instanceof Map<?, ?>) {
                                        try {
                                            Map<String, Object> productMap = (Map<String, Object>) item;
                                            String productName = (String) productMap.getOrDefault("product", "N/A");
                                            Long soldLong = (Long) productMap.getOrDefault("sold", 0L);
                                            int soldQuantity = soldLong != null ? soldLong.intValue() : 0;
                                            productList.add(new Product(productName, soldQuantity));
                                        } catch (Exception e) {
                                            Log.e("LoadEntries", "Error parsing product: " + e.getMessage());
                                        }
                                    }
                                }
                            }

                            // Create entry with null-safe constructor
                            Entry entry = new Entry(
                                    entryId,
                                    entryDate,
                                    controlNum,
                                    entryType,
                                    driver,
                                    createdAtLong != null ? createdAtLong : 0L,
                                    productList
                            );
                            entries.add(entry);

                        } catch (Exception e) {
                            Log.e("LoadEntries", "Error parsing entry: " + e.getMessage());
                        }
                    }

                    if (adapter != null) {
                        adapter.setEntries(entries);
                    }
                    updateVisibility(entries.isEmpty());
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Log.e("LoadEntries", "Error loading entries: " + e.getMessage());
                    Toast.makeText(this, "Error loading entries: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateVisibility(true);
                });
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
            Log.e("EntryClick", "Error launching details: " + e.getMessage());
            Toast.makeText(this, "Error opening entry details", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_all_entries, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}