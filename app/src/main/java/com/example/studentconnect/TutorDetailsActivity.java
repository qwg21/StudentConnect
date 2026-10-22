package com.example.studentconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TutorDetailsActivity extends AppCompatActivity {

    private TextView tvAvatar, tvName, tvBio, tvNoReviews;
    private TextView tvStatRating, tvStatHelped, tvStatCourses;
    private ChipGroup chipCourses;
    private LinearLayout llReviews;
    private Button btnRequest;
    private FirebaseFirestore db;

    private String uid;
    private String tutorId;
    private String tutorName = "Student";
    private String myName = "Student";
    private List<String> matchingCourses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_details);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        tutorId = getIntent().getStringExtra("tutorId");
        if (user == null || tutorId == null) {
            finish();
            return;
        }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        ArrayList<String> extra = getIntent().getStringArrayListExtra("matchingCourses");
        if (extra != null) {
            matchingCourses = extra;
        }

        tvAvatar = findViewById(R.id.tv_avatar);
        tvName = findViewById(R.id.tv_name);
        tvBio = findViewById(R.id.tv_bio);
        tvNoReviews = findViewById(R.id.tv_no_reviews);
        tvStatRating = findViewById(R.id.tv_stat_rating);
        tvStatHelped = findViewById(R.id.tv_stat_helped);
        tvStatCourses = findViewById(R.id.tv_stat_courses);
        chipCourses = findViewById(R.id.chip_courses);
        llReviews = findViewById(R.id.ll_reviews);
        btnRequest = findViewById(R.id.btn_request);
        View btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
        btnRequest.setOnClickListener(v -> startRequest());

        loadTutor();
        loadMyName();
        loadReviews();
    }

    @SuppressWarnings("unchecked")
    private void loadTutor() {
        db.collection("users").document(tutorId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Student profile not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    String name = doc.getString("name");
                    String bio = doc.getString("bio");
                    List<String> courses = (List<String>) doc.get("coursesOffered");
                    Double avg = doc.getDouble("avgRating");
                    Long count = doc.getLong("ratingCount");
                    Long helped = doc.getLong("peopleHelped");
                    if (helped == null) {
                        helped = doc.getLong("studentsHelped"); // Legacy profile compatibility.
                    }

                    if (name != null && !name.isEmpty()) {
                        tutorName = name;
                    }
                    tvName.setText(tutorName);
                    tvAvatar.setText(tutorName.substring(0, 1).toUpperCase());

                    if (count != null && count > 0 && avg != null) {
                        tvStatRating.setText(String.format(Locale.US, "%.1f ★", avg));
                    } else {
                        tvStatRating.setText("New");
                    }
                    tvStatHelped.setText(String.valueOf(helped != null ? helped : 0L));
                    tvStatCourses.setText(String.valueOf(courses != null ? courses.size() : 0));

                    if (TextUtils.isEmpty(bio)) {
                        tvBio.setText("This student hasn't added a bio yet.");
                    } else {
                        tvBio.setText(bio);
                    }

                    chipCourses.removeAllViews();
                    if (courses != null) {
                        for (String c : courses) {
                            chipCourses.addView(makeChip(c, matchingCourses.contains(c)));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load profile: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private Chip makeChip(String text, boolean highlighted) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setClickable(false);
        chip.setCheckable(false);
        chip.setChipStrokeWidth(0f);
        if (highlighted) {
            chip.setChipBackgroundColorResource(R.color.brand_primary);
            chip.setTextColor(getColor(R.color.white));
        } else {
            chip.setChipBackgroundColorResource(R.color.primary_tint);
            chip.setTextColor(getColor(R.color.brand_dark));
        }
        return chip;
    }

    private void loadMyName() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name != null) {
                        myName = name;
                    }
                });
    }

    private void loadReviews() {
        db.collection("ratings").whereEqualTo("tutorId", tutorId).get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> docs = new ArrayList<>(snap.getDocuments());
                    if (docs.isEmpty()) {
                        return;
                    }
                    Collections.sort(docs, (a, b) -> {
                        Long ta = a.getLong("createdAt");
                        Long tb = b.getLong("createdAt");
                        return Long.compare(tb != null ? tb : 0L, ta != null ? ta : 0L);
                    });

                    tvNoReviews.setVisibility(View.GONE);
                    int pad = (int) (8 * getResources().getDisplayMetrics().density);
                    int shown = Math.min(5, docs.size());

                    for (int i = 0; i < shown; i++) {
                        DocumentSnapshot d = docs.get(i);
                        Long stars = d.getLong("stars");
                        String comment = d.getString("comment");

                        String line = stars(stars != null ? stars : 0L);
                        if (!TextUtils.isEmpty(comment)) {
                            line += "  " + comment;
                        }

                        TextView tv = new TextView(this);
                        tv.setText(line);
                        tv.setTextSize(15);
                        tv.setTextColor(getColor(R.color.text_secondary));
                        tv.setPadding(0, pad, 0, pad);
                        llReviews.addView(tv);
                    }
                });
    }

    private String stars(long n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= n ? "★" : "☆");
        }
        return sb.toString();
    }

    private void startRequest() {
        if (matchingCourses.isEmpty()) {
            Toast.makeText(this, "No matching course to request help for",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (matchingCourses.size() == 1) {
            createRequest(matchingCourses.get(0));
            return;
        }
        String[] items = matchingCourses.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Which course do you need help with?")
                .setItems(items, (d, which) -> createRequest(items[which]))
                .show();
    }

    private void createRequest(String course) {
        btnRequest.setEnabled(false);

        db.collection("requests")
                .whereEqualTo("studentId", uid)
                .whereEqualTo("tutorId", tutorId)
                .whereEqualTo("course", course)
                .get()
                .addOnSuccessListener(snap -> {
                    boolean active = false;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        if (!"declined".equals(d.getString("status"))) {
                            active = true;
                        }
                    }
                    if (active) {
                        Toast.makeText(this, "You already requested help for " + course,
                                Toast.LENGTH_LONG).show();
                        btnRequest.setEnabled(true);
                        return;
                    }

                    Map<String, Object> request = new HashMap<>();
                    request.put("studentId", uid);
                    request.put("studentName", myName);
                    request.put("tutorId", tutorId);
                    request.put("tutorName", tutorName);
                    request.put("course", course);
                    request.put("status", "pending");
                    request.put("createdAt", System.currentTimeMillis());

                    db.collection("requests").add(request)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "Request sent to " + tutorName,
                                        Toast.LENGTH_LONG).show();
                                btnRequest.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Could not send request: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                btnRequest.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not check requests: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    btnRequest.setEnabled(true);
                });
    }
}