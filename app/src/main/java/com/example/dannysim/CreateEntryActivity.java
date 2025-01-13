package com.example.dannysim;

import android.app.DatePickerDialog;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateEntryActivity extends AppCompatActivity {
    private static final String TAG = "CreateEntryActivity";

    // Firebase instance
    private FirebaseFirestore db;

    // UI Elements
    private TableLayout productsTable;
    private TextView controlNumberView;
    private EditText dateEdit;
    private EditText driverEdit;
    private Spinner entryTypeSpinner;
    private Button addRowButton;
    private Button saveButton;

    // Tracking variables
    private int rowCount = 0;
    private int currentControlNumber = 1;
    private Calendar selectedDate;
    private boolean isLoading = false;
    private boolean hasInitializedInventory = false; // Flag to track inventory initialization

    // Constants
    private static final String FIELD_CONTROL_NUMBER = "controlNumber"; // Match exact field name in Firestore
    private static final String COLLECTION_CONTROL_NUMBERS = "control_numbers";
    private static final String TAG_CONTROL_NUMBER = "ControlNumberManager";
    private static final String COLLECTION_ENTRIES = "entries";
    private static final String COLLECTION_INVENTORY = "inventory";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_entry);

        Log.d(TAG, "Activity onCreate started");

        try {
            initializeFirebase();
            initializeViews();
            setupEntryTypeSpinner();
            setupDatePicker();
            setupButtons();
            getLatestControlNumber();
            addNewRow();

            // Check and initialize inventory documents (one-time process)
            if (!hasInitializedInventory) {
                initializeInventory();
                hasInitializedInventory = true;
            }

            Log.d(TAG, "Activity initialization completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during activity initialization", e);
            showErrorDialog("Initialization Error", "Failed to initialize the application. Please try again.");
        }
    }

    private void initializeFirebase() {
        Log.d(TAG, "Initializing Firebase");
        db = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views");

        try {
            productsTable = findViewById(R.id.productsTable);
            controlNumberView = findViewById(R.id.controlNumber);
            dateEdit = findViewById(R.id.date);
            driverEdit = findViewById(R.id.driver);
            entryTypeSpinner = findViewById(R.id.entryTypeSpinner);
            addRowButton = findViewById(R.id.addRowButton);
            saveButton = findViewById(R.id.saveButton);

            selectedDate = Calendar.getInstance();
            updateDateDisplay();

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void setupEntryTypeSpinner() {
        Log.d(TAG, "Setting up entry type spinner");

        try {
            List<String> entryTypes = Arrays.asList("Sales", "Stock Received");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, entryTypes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            entryTypeSpinner.setAdapter(adapter);

            Log.d(TAG, "Entry type spinner setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up entry type spinner", e);
            showErrorDialog("Setup Error", "Failed to setup entry types. Please restart the application.");
        }
    }

    private void setupDatePicker() {
        Log.d(TAG, "Setting up date picker");

        dateEdit.setOnClickListener(v -> {
            try {
                showDatePickerDialog();
            } catch (Exception e) {
                Log.e(TAG, "Error showing date picker", e);
                showErrorDialog("Date Selection Error", "Failed to show date picker. Please try again.");
            }
        });
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Log.d(TAG, String.format("Date selected: %d/%d/%d", dayOfMonth, month + 1, year));

                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    updateDateDisplay();
                    checkAndUpdateControlNumber(year, month);
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void setupButtons() {
        Log.d(TAG, "Setting up buttons");

        addRowButton.setOnClickListener(v -> {
            if (!isLoading) {
                addNewRow();
            }
        });

        saveButton.setOnClickListener(v -> {
            if (!isLoading) {
                validateAndShowPreview();
            }
        });
    }

    private void updateDateDisplay() {
        String date = String.format(Locale.US, "%02d/%02d/%d",
                selectedDate.get(Calendar.DAY_OF_MONTH),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.YEAR));
        dateEdit.setText(date);

        Log.d(TAG, "Date display updated: " + date);
    }

    private void showError(String message) {
        Log.w(TAG, "Showing error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void getLatestControlNumber() {
        Log.d(TAG_CONTROL_NUMBER, "Fetching latest control number");
        setLoading(true);

        Timestamp currentTimestamp = Timestamp.now();
        Date currentDate = currentTimestamp.toDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);

        db.collection(COLLECTION_ENTRIES)
                .whereEqualTo("year", currentYear)
                .whereEqualTo("month", currentMonth)
                .orderBy(FIELD_CONTROL_NUMBER, Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                // Get the current control number and increment by 1
                                currentControlNumber = document.getLong(FIELD_CONTROL_NUMBER).intValue() + 1;
                            } else {
                                // If no documents exist, start from 1
                                currentControlNumber = 1;
                            }
                            controlNumberView.setText(String.valueOf(currentControlNumber));
                            Log.d(TAG_CONTROL_NUMBER, "New control number set: " + currentControlNumber);
                        } else {
                            Log.e(TAG_CONTROL_NUMBER, "Error fetching control number", task.getException());
                            showErrorDialog("Database Error",
                                    "Failed to get control number. Please try again.");
                        }
                        setLoading(false);
                    }
                });
    }

    private void checkAndUpdateControlNumber(int year, int month) {
        Log.d(TAG_CONTROL_NUMBER, String.format("Checking control number for year: %d, month: %d",
                year, month));

        Calendar currentCal = Calendar.getInstance();
        if (year != currentCal.get(Calendar.YEAR) ||
                month != currentCal.get(Calendar.MONTH)) {

            setLoading(true);

            db.collection(COLLECTION_ENTRIES)
                    .whereEqualTo("year", year)
                    .whereEqualTo("month", month)
                    .orderBy(FIELD_CONTROL_NUMBER, Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (!task.getResult().isEmpty()) {
                                    DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                    // Get the current control number and increment by 1
                                    currentControlNumber = document.getLong(FIELD_CONTROL_NUMBER).intValue() + 1;
                                } else {
                                    // If no documents exist, start from 1
                                    currentControlNumber = 1;
                                }
                                controlNumberView.setText(String.valueOf(currentControlNumber));
                                Log.d(TAG_CONTROL_NUMBER, "Control number updated for new month: " +
                                        currentControlNumber);
                            } else {
                                Log.e(TAG_CONTROL_NUMBER, "Error updating control number for new month",
                                        task.getException());
                                showErrorDialog("Database Error",
                                        "Failed to update control number. Please try again.");
                            }
                            setLoading(false);
                        }
                    });
        }
    }

    private void addNewRow() {
        Log.d(TAG, "Adding new table row");

        try {
            TableRow row = new TableRow(this);
            rowCount++;

            // Serial Number
            TextView serialNo = new TextView(this);
            serialNo.setText(String.valueOf(rowCount));
            serialNo.setPadding(3, 3, 3, 3);

            // Product Spinner
            Spinner productSpinner = new Spinner(this);
            ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item,
                    getProductsList());
            productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            productSpinner.setAdapter(productAdapter);

            // Out EditText
            EditText outEdit = new EditText(this);
            outEdit.setInputType(InputType.TYPE_CLASS_NUMBER);

            // In EditText
            EditText inEdit = new EditText(this);
            inEdit.setInputType(InputType.TYPE_CLASS_NUMBER);

            // Sold TextView (calculated)
            TextView soldText = new TextView(this);
            soldText.setPadding(3, 3, 3, 3);

            setupRowCalculations(outEdit, inEdit, soldText);

            // Add views to row
            row.addView(serialNo);
            row.addView(productSpinner);
            row.addView(outEdit);
            row.addView(inEdit);
            row.addView(soldText);

            productsTable.addView(row);

            Log.d(TAG, "New row added successfully: " + rowCount);
        } catch (Exception e) {
            Log.e(TAG, "Error adding new row", e);
            showErrorDialog("Error", "Failed to add new row. Please try again.");
        }
    }

    private void showErrorDialog(String title, String message) {
        Log.e(TAG, "Showing error dialog: " + title + " - " + message);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void setupRowCalculations(EditText outEdit, EditText inEdit, TextView soldText) {
        TextWatcher calculateSold = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                calculateSoldValue(outEdit, inEdit, soldText);
            }
        };

        outEdit.addTextChangedListener(calculateSold);
        inEdit.addTextChangedListener(calculateSold);
    }

    private void calculateSoldValue(EditText outEdit, EditText inEdit, TextView soldText) {
        try {
            int out = outEdit.getText().toString().isEmpty() ? 0 :
                    Integer.parseInt(outEdit.getText().toString());
            int in = inEdit.getText().toString().isEmpty() ? 0 :
                    Integer.parseInt(inEdit.getText().toString());

            String entryType = entryTypeSpinner.getSelectedItem().toString();
            int soldValue;

            if (entryType.equals("Sales")) {
                soldValue = out - in;
            } else {
                soldValue = Math.abs(out - in);
            }

            soldText.setText(String.valueOf(soldValue));
            Log.d(TAG, String.format("Calculated sold value: %d (out: %d, in: %d, type: %s)",
                    soldValue, out, in, entryType));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error calculating sold value", e);
            soldText.setText("0");
        }
    }

    private List<String> getProductsList() {
        return Arrays.asList(
                "30CL", "35CL 7UP", "35CL M.D", "50CL",
                "PEPSI", "TBL", "G.APPLE", "PINEAPPLE", "75CL AQUAFINA",
                "RED APPLE", "7UP", "ORANGE", "SODA",
                "S.ORANGE", "S.7UP", "SK 50CL", "SK 30CL"
        );
    }

    private void validateAndShowPreview() {
        Log.d(TAG, "Validating inputs before showing preview");

        if (validateInputs()) {
            showPreviewDialog();
        }
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(driverEdit.getText())) {
            showError("Please enter driver name");
            return false;
        }

        if (TextUtils.isEmpty(dateEdit.getText())) {
            showError("Please select a date");
            return false;
        }

        for (int i = 1; i < productsTable.getChildCount(); i++) {
            TableRow row = (TableRow) productsTable.getChildAt(i);
            EditText outEdit = (EditText) row.getChildAt(2);
            EditText inEdit = (EditText) row.getChildAt(3);

            if (TextUtils.isEmpty(outEdit.getText()) && TextUtils.isEmpty(inEdit.getText())) {
                showError("Please fill in OUT or IN values for row " + i);
                return false;
            }
        }

        Log.d(TAG, "Input validation passed");
        return true;
    }

    private void showPreviewDialog() {
        Log.d(TAG, "Showing preview dialog");

        try {
            StringBuilder preview = new StringBuilder();
            preview.append("Control No: ").append(currentControlNumber).append("\n");
            preview.append("Driver: ").append(driverEdit.getText()).append("\n");
            preview.append("Entry Type: ").append(entryTypeSpinner.getSelectedItem().toString()).append("\n");
            preview.append("Date: ").append(dateEdit.getText()).append("\n\n");
            preview.append("Products:\n");

            for (int i = 1; i < productsTable.getChildCount(); i++) {
                TableRow row = (TableRow) productsTable.getChildAt(i);
                String sn = ((TextView) row.getChildAt(0)).getText().toString();
                String product = ((Spinner) row.getChildAt(1)).getSelectedItem().toString();
                String sold = ((TextView) row.getChildAt(4)).getText().toString();

                preview.append(sn).append(". ")
                        .append(product).append(" - ")
                        .append(sold).append("\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle("Preview")
                    .setMessage(preview.toString())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        saveToDatabase();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Edit", (dialog, which) -> dialog.dismiss())
                    .show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        runOnUiThread(() -> {
            addRowButton.setEnabled(!loading);
            saveButton.setEnabled(!loading);

            // Disable all input fields while loading
            dateEdit.setEnabled(!loading);
            driverEdit.setEnabled(!loading);
            entryTypeSpinner.setEnabled(!loading);

            for (int i = 1; i < productsTable.getChildCount(); i++) {
                TableRow row = (TableRow) productsTable.getChildAt(i);
                row.setEnabled(!loading);
            }

            if (loading) {
                // Show Snackbar with indefinite duration and prevent further interactions
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Processing...", Snackbar.LENGTH_INDEFINITE);
                snackbar.show();

                // Disable touch interactions on the Snackbar
                View snackbarView = snackbar.getView();
                snackbarView.setClickable(false);
            } else {
                // Dismiss any existing Snackbar
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT);
                snackbar.show(); // Show a very short Snackbar to dismiss the previous one
            }
        });
    }

    private void saveToDatabase() {
        Log.d(TAG, "Starting database save operation");
        setLoading(true);

        try {
            WriteBatch batch = db.batch();
            String entryType = entryTypeSpinner.getSelectedItem().toString();

            // Create main entry document
            Map<String, Object> entryData = new HashMap<>();
            entryData.put("controlNumber", currentControlNumber);
            entryData.put("driver", driverEdit.getText().toString());
            entryData.put("entryType", entryType);
            entryData.put("date", selectedDate.getTimeInMillis());
            entryData.put("year", selectedDate.get(Calendar.YEAR));
            entryData.put("month", selectedDate.get(Calendar.MONTH));
            entryData.put("createdAt", Calendar.getInstance().getTimeInMillis());

            // Create products list
            List<Map<String, Object>> products = new ArrayList<>();
            for (int i = 1; i < productsTable.getChildCount(); i++) {
                TableRow row = (TableRow) productsTable.getChildAt(i);
                String product = ((Spinner) row.getChildAt(1)).getSelectedItem().toString();
                String outStr = ((EditText) row.getChildAt(2)).getText().toString();
                String inStr = ((EditText) row.getChildAt(3)).getText().toString();
                String soldStr = ((TextView) row.getChildAt(4)).getText().toString();

                int out = TextUtils.isEmpty(outStr) ? 0 : Integer.parseInt(outStr);
                int in = TextUtils.isEmpty(inStr) ? 0 : Integer.parseInt(inStr);
                int sold = Integer.parseInt(soldStr);

                Map<String, Object> productData = new HashMap<>();
                productData.put("product", product);
                productData.put("out", out);
                productData.put("in", in);
                productData.put("sold", sold);
                products.add(productData);

                // Update inventory
                updateInventory(batch, product, out, in, entryType);
            }

            entryData.put("products", products);

            // Add entry document to batch
            DocumentReference entryRef = db.collection(COLLECTION_ENTRIES).document();
            batch.set(entryRef, entryData);

            // Commit the batch
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Database save successful");
                        setLoading(false);
                        showSuccessAndFinish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Database save failed", e);
                        setLoading(false);
                        showErrorDialog("Save Error", "Failed to save entry. Please try again.");
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing database save", e);
            setLoading(false);
            showErrorDialog("Save Error", "Failed to prepare entry data. Please try again.");
        }
    }

    private void showSuccessAndFinish() {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage("Entry saved successfully!")
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void updateInventory(WriteBatch batch, String product, int out, int in, String entryType) {
        DocumentReference inventoryRef = db.collection(COLLECTION_INVENTORY).document(product);

        if (entryType.equals("Sales")) {
            // For sales, decrease inventory by sold amount (out - in)
            batch.update(inventoryRef, "quantity", FieldValue.increment(-(out - in)));
        } else {
            // For stock received, increase inventory by received amount
            batch.update(inventoryRef, "quantity", FieldValue.increment(in - out));
        }
    }

    private void initializeInventory() {
        Log.d(TAG, "Initializing inventory");

        List<String> products = Arrays.asList(
                "30CL", "35CL 7UP", "35CL M.D", "50CL",
                "PEPSI", "TBL", "G.APPLE", "PINEAPPLE", "75CL AQUAFINA",
                "RED APPLE", "7UP", "ORANGE", "SODA",
                "S.ORANGE", "S.7UP", "SK 50CL", "SK 30CL"
        );

        for (String product : products) {
            DocumentReference docRef = db.collection(COLLECTION_INVENTORY).document(product);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (!document.exists()) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("quantity", 0); // Initialize quantity to 0
                            docRef.set(data)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Inventory document created for: " + product);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error creating inventory document for: " + product, e);
                                    });
                        }
                    } else {
                        Log.e(TAG, "Error getting document:", task.getException());
                    }
                }
            });
        }
    }
}