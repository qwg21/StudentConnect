package com.example.studentconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RateHelperActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private TextInputEditText etComment;
    private Button btnSubmit;
    private FirebaseFirestore db;

    private String uid;
    private String tutorId;
    private String tutorName = "Tutor";
    private String course = "Course";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_helper);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        tutorId = getIntent().getStringExtra("tutorId");
        String nameExtra = getIntent().getStringExtra("tutorName");
        String courseExtra = getIntent().getStringExtra("course");

        if (user == null || tutorId == null) {
            finish();
            return;
        }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        if (nameExtra != null && !nameExtra.isEmpty()) tutorName = nameExtra;
        if (courseExtra != null && !courseExtra.isEmpty()) course = courseExtra;

        View btnBack = findViewById(R.id.btn_back);
        TextView tvAvatar = findViewById(R.id.tv_avatar);
        TextView tvName = findViewById(R.id.tv_tutor_name);
        TextView tvCourse = findViewById(R.id.tv_course);
        ratingBar = findViewById(R.id.rating_bar);
        etComment = findViewById(R.id.et_comment);
        btnSubmit = findViewById(R.id.btn_submit);

        tvAvatar.setText(tutorName.substring(0, 1).toUpperCase());
        tvName.setText(tutorName);
        tvCourse.setText(course);

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitRating());
    }

    private void submitRating() {
        int stars = Math.round(ratingBar.getRating());
        if (stars < 1) {
            Toast.makeText(this, "Please select at least 1 star", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";

        btnSubmit.setEnabled(false);

        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("tutorId", tutorId);
        ratingData.put("studentId", uid);
        ratingData.put("stars", (long) stars);
        ratingData.put("comment", comment);
        ratingData.put("createdAt", System.currentTimeMillis());

        db.collection("ratings").add(ratingData)
                .addOnSuccessListener(ref -> updateTutorStats(stars))
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTutorStats(int newStars) {
        DocumentReference tutorRef = db.collection("users").document(tutorId);

        db.runTransaction(transaction -> {
            DocumentSnapshot doc = transaction.get(tutorRef);
            if (!doc.exists()) {
                return null;
            }

            Double currentAvg = doc.getDouble("avgRating");
            Long currentCount = doc.getLong("ratingCount");
            Long currentHelped = doc.getLong("peopleHelped");

            double avg = currentAvg != null ? currentAvg : 0.0;
            long count = currentCount != null ? currentCount : 0L;
            long helped = currentHelped != null ? currentHelped : 0L;

            long newCount = count + 1;
            double newAvg = ((avg * count) + newStars) / newCount;
            long newHelped = helped + 1;

            transaction.update(tutorRef,
                    "avgRating", newAvg,
                    "ratingCount", newCount,
                    "peopleHelped", newHelped);

            return null;
        }).addOnSuccessListener(result -> {
            Toast.makeText(this, "Rating submitted! Thank you.", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Rating saved, but stats update failed.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}