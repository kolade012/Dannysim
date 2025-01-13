package com.example.dannysim;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class AllEntriesActivity extends AppCompatActivity implements EntriesAdapter.OnEntryClickListener {
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private EntriesAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvNoEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_entries);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

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
        progressBar.setVisibility(View.VISIBLE);

        db.collection("entries")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    List<Entry> entries = new ArrayList<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Entry entry = document.toObject(Entry.class);
                        if (entry != null) {
                            entry.setId(document.getId());
                            entries.add(entry);
                        }
                    }

                    adapter.setEntries(entries);
                    updateVisibility(entries.isEmpty());
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading entries: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateVisibility(boolean isEmpty) {
        tvNoEntries.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onEntryClick(Entry entry) {
        // Handle entry click - perhaps show details
        Intent intent = new Intent(this, EntryDetailsActivity.class);
        intent.putExtra("entry", entry);
        startActivity(intent);
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
