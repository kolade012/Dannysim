package com.example.dannysim;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UserTypeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_type);

        Button btnAdmin = findViewById(R.id.btnAdmin);
        Button btnStaff = findViewById(R.id.btnStaff);

        btnAdmin.setOnClickListener(v -> {
            startActivity(new Intent(UserTypeActivity.this, AdminLoginActivity.class));
        });

        btnStaff.setOnClickListener(v -> {
            startActivity(new Intent(UserTypeActivity.this, StaffLoginActivity.class));
        });
    }
}