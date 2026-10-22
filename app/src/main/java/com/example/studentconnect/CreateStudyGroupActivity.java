package com.example.studentconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateStudyGroupActivity extends AppCompatActivity {

    private TextInputEditText etGroupName, etGroupDescription, etLocation, etSchedule;
    private AutoCompleteTextView actvCourseCode;
    private TextInputLayout tilLocation;
    private TextView tvMaxMembersCount;
    private ProgressBar progressBar;
    private Button btnCreateGroup;
    private View rootLayout;

    private int maxMembers = 8;
    private String selectedMode = "Online";
    private String uid;
    private String myName = "Student";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_study_group);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        rootLayout = findViewById(android.R.id.content);
        View btnBack = findViewById(R.id.btn_back);
        etGroupName = findViewById(R.id.et_group_name);
        actvCourseCode = findViewById(R.id.actv_course_code);
        etGroupDescription = findViewById(R.id.et_group_description);
        tilLocation = findViewById(R.id.til_location);
        etLocation = findViewById(R.id.et_location);
        etSchedule = findViewById(R.id.et_schedule);
        tvMaxMembersCount = findViewById(R.id.tv_max_members_count);
        progressBar = findViewById(R.id.progress_bar);
        btnCreateGroup = findViewById(R.id.btn_create_group);

        ImageButton btnMinusMax = findViewById(R.id.btn_minus_max);
        ImageButton btnPlusMax = findViewById(R.id.btn_plus_max);
        MaterialButtonToggleGroup toggleMode = findViewById(R.id.toggle_mode_segment);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnMinusMax != null) {
            btnMinusMax.setOnClickListener(v -> {
                if (maxMembers > 2) {
                    maxMembers--;
                    tvMaxMembersCount.setText(String.valueOf(maxMembers));
                }
            });
        }

        if (btnPlusMax != null) {
            btnPlusMax.setOnClickListener(v -> {
                if (maxMembers < 20) {
                    maxMembers++;
                    tvMaxMembersCount.setText(String.valueOf(maxMembers));
                }
            });
        }

        if (toggleMode != null) {
            toggleMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                if (checkedId == R.id.btn_mode_in_person) {
                    selectedMode = "In person";
                    if (tilLocation != null) {
                        tilLocation.setHint("Location / Venue (e.g. Science Complex Lab 3)");
                    }
                } else {
                    selectedMode = "Online";
                    if (tilLocation != null) {
                        tilLocation.setHint("Meeting Link / URL (e.g. Google Meet link)");
                    }
                }
            });
        }

        if (btnCreateGroup != null) {
            btnCreateGroup.setOnClickListener(v -> submitForm());
        }

        loadUserCourses();
        loadMyName();
    }

    @SuppressWarnings("unchecked")
    private void loadUserCourses() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    List<String> needed = (List<String>) doc.get("coursesNeeded");
                    List<String> offered = (List<String>) doc.get("coursesOffered");

                    Set<String> coursesSet = new HashSet<>();
                    if (needed != null) coursesSet.addAll(needed);
                    if (offered != null) coursesSet.addAll(offered);

                    if (!coursesSet.isEmpty() && actvCourseCode != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(coursesSet));
                        actvCourseCode.setAdapter(adapter);
                    }
                });
    }

    private void loadMyName() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            myName = name;
                        }
                    }
                });
    }

    private void submitForm() {
        String groupName = etGroupName.getText() != null ? etGroupName.getText().toString().trim() : "";
        String courseCode = actvCourseCode.getText() != null ? actvCourseCode.getText().toString().trim().toUpperCase() : "";
        String description = etGroupDescription.getText() != null ? etGroupDescription.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String schedule = etSchedule.getText() != null ? etSchedule.getText().toString().trim() : "";

        boolean hasError = false;

        if (TextUtils.isEmpty(groupName)) {
            etGroupName.setError("Group name is required");
            hasError = true;
        }

        if (TextUtils.isEmpty(courseCode)) {
            actvCourseCode.setError("Course code is required");
            hasError = true;
        }

        if (TextUtils.isEmpty(description)) {
            etGroupDescription.setError("Description is required");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        if (TextUtils.isEmpty(schedule)) {
            schedule = "Flexible schedule";
        }

        if (TextUtils.isEmpty(location)) {
            location = selectedMode.equals("Online") ? "Online Meeting Link TBD" : "Campus Location TBD";
        }

        setLoading(true);

        List<String> initialMembers = new ArrayList<>();
        initialMembers.add(uid);

        StudyGroup newGroup = new StudyGroup(
                null,
                groupName,
                courseCode,
                description,
                uid,
                myName,
                initialMembers,
                1,
                maxMembers,
                selectedMode,
                location,
                schedule,
                System.currentTimeMillis(),
                "",
                0
        );

        StudyGroupRepository.getInstance().createStudyGroup(
                newGroup,
                groupId -> {
                    setLoading(false);
                    Intent intent = new Intent(CreateStudyGroupActivity.this, StudyGroupDetailsActivity.class);
                    intent.putExtra("groupId", groupId);
                    startActivity(intent);
                    finish();
                },
                e -> {
                    setLoading(false);
                    if (rootLayout != null) {
                        Snackbar.make(rootLayout, "Failed to create group: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnCreateGroup != null) {
            btnCreateGroup.setEnabled(!loading);
        }
    }
}