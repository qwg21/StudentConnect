package com.example.studentconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyCoursesActivity extends AppCompatActivity {

    private static final String NEEDED = "coursesNeeded";
    private static final String OFFERED = "coursesOffered";

    private final List<String> needed = new ArrayList<>();
    private final List<String> offered = new ArrayList<>();
    private final List<String> shown = new ArrayList<>();
    private String currentField = NEEDED;

    private CourseAdapter adapter;
    private TextInputEditText etCourse;
    private TextView tvEmpty, tvSubtitle;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_courses);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        etCourse = findViewById(R.id.et_course);
        tvEmpty = findViewById(R.id.tv_empty);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        Button btnAdd = findViewById(R.id.btn_add);
        View btnBack = findViewById(R.id.btn_back);
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggle_group);
        RecyclerView rv = findViewById(R.id.rv_courses);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter(shown, this::confirmRemove);
        rv.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> addCourse());

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            currentField = checkedId == R.id.btn_tab_offered ? OFFERED : NEEDED;
            showCurrent();
        });

        showCurrent();
        UserMigration.run(db, uid, this::loadCourses);
    }

    private List<String> currentList() {
        return NEEDED.equals(currentField) ? needed : offered;
    }

    @SuppressWarnings("unchecked")
    private void loadCourses() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    needed.clear();
                    offered.clear();
                    List<String> n = (List<String>) doc.get(NEEDED);
                    List<String> o = (List<String>) doc.get(OFFERED);
                    if (n != null) {
                        needed.addAll(n);
                    }
                    if (o != null) {
                        offered.addAll(o);
                    }
                    showCurrent();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load courses", Toast.LENGTH_SHORT).show());
    }

    private void showCurrent() {
        shown.clear();
        shown.addAll(currentList());
        adapter.notifyDataSetChanged();

        boolean isNeeded = NEEDED.equals(currentField);
        tvSubtitle.setText(isNeeded
                ? "Courses you want help with. We use these to find people who can help you."
                : "Courses you can help others with. People who need them will find you.");
        tvEmpty.setText(isNeeded
                ? "No courses yet. Add the courses you need help with."
                : "No courses yet. Add the courses you can help others with.");
        tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addCourse() {
        String raw = etCourse.getText() != null ? etCourse.getText().toString() : "";
        String code = raw.trim().replaceAll("\\s+", " ").toUpperCase();

        if (TextUtils.isEmpty(code)) {
            etCourse.setError("Enter a course code");
            return;
        }
        if (currentList().contains(code)) {
            etCourse.setError("Already in this list");
            return;
        }

        final String field = currentField;
        final List<String> target = currentList();

        Map<String, Object> data = new HashMap<>();
        data.put(field, FieldValue.arrayUnion(code));

        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    target.add(code);
                    etCourse.setText("");
                    showCurrent();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not add course: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void confirmRemove(String course) {
        new AlertDialog.Builder(this)
                .setTitle("Remove course")
                .setMessage("Remove " + course + " from this list?")
                .setPositiveButton("Remove", (d, w) -> removeCourse(course))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeCourse(String course) {
        final String field = currentField;
        final List<String> target = currentList();

        Map<String, Object> data = new HashMap<>();
        data.put(field, FieldValue.arrayRemove(course));

        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    target.remove(course);
                    showCurrent();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not remove course: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}