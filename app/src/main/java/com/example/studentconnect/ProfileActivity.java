package com.example.studentconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatar, tvEmail, tvRating, tvPeopleHelped;
    private TextInputEditText etName, etBio;
    private Button btnSave;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        View btnBack = findViewById(R.id.btn_back);
        tvAvatar = findViewById(R.id.tv_avatar);
        tvEmail = findViewById(R.id.tv_email);
        tvRating = findViewById(R.id.tv_avg_rating);
        tvPeopleHelped = findViewById(R.id.tv_people_helped);
        etName = findViewById(R.id.et_name);
        etBio = findViewById(R.id.et_bio);
        btnSave = findViewById(R.id.btn_save);
        Button btnCourses = findViewById(R.id.btn_courses);
        Button btnLogout = findViewById(R.id.btn_logout);

        tvEmail.setText(user.getEmail());

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfile());
        btnCourses.setOnClickListener(v -> startActivity(new Intent(this, MyCoursesActivity.class)));
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            goToLogin();
        });

        loadProfile();
    }

    private void loadProfile() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String bio = doc.getString("bio");
                        Double avg = doc.getDouble("avgRating");
                        Long count = doc.getLong("ratingCount");
                        Long helped = doc.getLong("peopleHelped");

                        if (name != null) {
                            etName.setText(name);
                            if (!name.isEmpty()) {
                                tvAvatar.setText(name.substring(0, 1).toUpperCase());
                            }
                        }
                        if (bio != null) {
                            etBio.setText(bio);
                        }

                        if (count != null && count > 0 && avg != null) {
                            tvRating.setText(String.format(Locale.US, "%.1f ★", avg));
                        } else {
                            tvRating.setText("-");
                        }
                        tvPeopleHelped.setText(String.valueOf(helped != null ? helped : 0L));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveProfile() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String bio = etBio.getText() != null ? etBio.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name cannot be empty");
            return;
        }

        btnSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("bio", bio);

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Could not update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
