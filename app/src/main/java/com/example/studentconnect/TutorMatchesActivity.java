package com.example.studentconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TutorMatchesActivity extends AppCompatActivity {

    // Helpers
    private final List<Tutor> tutors = new ArrayList<>();
    private TutorAdapter tutorAdapter;
    private TextView tvEmpty, tvSubtitle;
    private Button btnAddCoursesEmpty;
    private View llHelpersContainer, llEmptyState;
    private ChipGroup chipGroupCourses;

    // Study Groups
    private final List<StudyGroup> studyGroups = new ArrayList<>();
    private StudyGroupAdapter studyGroupAdapter;
    private View llStudyGroupsContainer, llStudyGroupsEmptyState, btnCreateGroup;
    private Button btnCreateFirstGroup;
    private ProgressBar pbStudyGroups;
    private ChipGroup chipGroupStudyGroupCourses;

    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_matches);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        // Bind Views - Common / Header
        View btnBack = findViewById(R.id.btn_back);
        MaterialButtonToggleGroup toggleDiscover = findViewById(R.id.toggle_discover_segment);

        // Bind Views - Helpers Segment
        llHelpersContainer = findViewById(R.id.ll_helpers_container);
        tvEmpty = findViewById(R.id.tv_empty);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        llEmptyState = findViewById(R.id.ll_empty_state);
        btnAddCoursesEmpty = findViewById(R.id.btnAddCoursesEmpty);
        chipGroupCourses = findViewById(R.id.chipGroupCourses);
        RecyclerView rvTutors = findViewById(R.id.rv_tutors);

        // Bind Views - Study Groups Segment
        llStudyGroupsContainer = findViewById(R.id.ll_study_groups_container);
        llStudyGroupsEmptyState = findViewById(R.id.ll_study_groups_empty_state);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        btnCreateFirstGroup = findViewById(R.id.btnCreateFirstGroup);
        pbStudyGroups = findViewById(R.id.pb_study_groups);
        EditText etSearchStudyGroups = findViewById(R.id.etSearchStudyGroups);
        chipGroupStudyGroupCourses = findViewById(R.id.chipGroupStudyGroupCourses);
        RecyclerView rvStudyGroups = findViewById(R.id.rv_study_groups);

        // Setup Helpers RecyclerView
        rvTutors.setLayoutManager(new LinearLayoutManager(this));
        tutorAdapter = new TutorAdapter(tutors, tutor -> {
            Intent i = new Intent(this, TutorDetailsActivity.class);
            i.putExtra("tutorId", tutor.uid);
            i.putStringArrayListExtra("matchingCourses", new ArrayList<>(tutor.matchingCourses));
            startActivity(i);
        });
        rvTutors.setAdapter(tutorAdapter);

        // Setup Study Groups RecyclerView
        rvStudyGroups.setLayoutManager(new LinearLayoutManager(this));
        studyGroupAdapter = new StudyGroupAdapter(studyGroups, uid, group -> {
            Intent i = new Intent(this, StudyGroupDetailsActivity.class);
            i.putExtra("groupId", group.getId());
            startActivity(i);
        });
        rvStudyGroups.setAdapter(studyGroupAdapter);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnAddCoursesEmpty != null) {
            btnAddCoursesEmpty.setOnClickListener(v ->
                    startActivity(new Intent(this, MyCoursesActivity.class)));
        }

        if (btnCreateGroup != null) {
            btnCreateGroup.setOnClickListener(v ->
                    startActivity(new Intent(this, CreateStudyGroupActivity.class)));
        }

        if (btnCreateFirstGroup != null) {
            btnCreateFirstGroup.setOnClickListener(v ->
                    startActivity(new Intent(this, CreateStudyGroupActivity.class)));
        }

        // Segment Toggle Listener
        if (toggleDiscover != null) {
            toggleDiscover.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                if (checkedId == R.id.btn_segment_groups) {
                    showStudyGroupsSegment();
                } else {
                    showHelpersSegment();
                }
            });
        }

        // Helpers Chip Group Listener
        if (chipGroupCourses != null) {
            chipGroupCourses.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                Chip chip = findViewById(id);
                if (chip != null && tutorAdapter != null) {
                    String selectedTag = chip.getText().toString();
                    if ("All".equalsIgnoreCase(selectedTag)) {
                        tutorAdapter.getFilter().filter("");
                    } else {
                        tutorAdapter.getFilter().filter(selectedTag);
                    }
                }
            });
        }

        // Study Groups Search Listener
        if (etSearchStudyGroups != null) {
            etSearchStudyGroups.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (studyGroupAdapter != null) {
                        studyGroupAdapter.getFilter().filter(s.toString());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Study Groups Chip Filter Listener
        if (chipGroupStudyGroupCourses != null) {
            chipGroupStudyGroupCourses.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                Chip chip = findViewById(id);
                if (chip != null && studyGroupAdapter != null) {
                    String selectedTag = chip.getText().toString();
                    if ("All".equalsIgnoreCase(selectedTag)) {
                        studyGroupAdapter.getFilter().filter("");
                    } else {
                        studyGroupAdapter.getFilter().filter(selectedTag);
                    }
                }
            });
        }

        loadMatches();
        loadUserCoursesForStudyGroupFilter();
    }

    private void showHelpersSegment() {
        if (llHelpersContainer != null) llHelpersContainer.setVisibility(View.VISIBLE);
        if (llStudyGroupsContainer != null) llStudyGroupsContainer.setVisibility(View.GONE);
    }

    private void showStudyGroupsSegment() {
        if (llHelpersContainer != null) llHelpersContainer.setVisibility(View.GONE);
        if (llStudyGroupsContainer != null) llStudyGroupsContainer.setVisibility(View.VISIBLE);
        loadStudyGroups();
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    private void loadUserCoursesForStudyGroupFilter() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || chipGroupStudyGroupCourses == null) return;

                    List<String> needed = (List<String>) doc.get("coursesNeeded");
                    List<String> offered = (List<String>) doc.get("coursesOffered");

                    Set<String> uniqueCourses = new HashSet<>();
                    if (needed != null) uniqueCourses.addAll(needed);
                    if (offered != null) uniqueCourses.addAll(offered);

                    if (!uniqueCourses.isEmpty()) {
                        for (String course : uniqueCourses) {
                            Chip chip = new Chip(this);
                            chip.setText(course);
                            chip.setCheckable(true);
                            chip.setChipBackgroundColorResource(R.color.selector_tab_bg);
                            chip.setTextColor(getColorStateList(R.color.selector_tab_text));
                            chip.setChipCornerRadius(getResources().getDimension(R.dimen.radius_small));
                            chipGroupStudyGroupCourses.addView(chip);
                        }
                    }
                });
    }

    private void loadStudyGroups() {
        if (pbStudyGroups != null) pbStudyGroups.setVisibility(View.VISIBLE);

        StudyGroupRepository.getInstance().getStudyGroups(
                groups -> {
                    if (pbStudyGroups != null) pbStudyGroups.setVisibility(View.GONE);
                    studyGroups.clear();
                    studyGroups.addAll(groups);
                    if (studyGroupAdapter != null) {
                        studyGroupAdapter.updateData(studyGroups);
                    }

                    boolean isEmpty = studyGroups.isEmpty();
                    if (llStudyGroupsEmptyState != null) {
                        llStudyGroupsEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    }
                },
                e -> {
                    if (pbStudyGroups != null) pbStudyGroups.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load study groups: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    @SuppressWarnings("unchecked")
    private void loadMatches() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        showMessage("Your profile could not be found. Please sign in again.", false);
                        return;
                    }
                    List<String> myCourses = (List<String>) doc.get("coursesNeeded");
                    if (myCourses == null || myCourses.isEmpty()) {
                        showMessage("Add courses you need help with first so we can find students who can help.", true);
                        return;
                    }
                    findHelpers(myCourses);
                })
                .addOnFailureListener(e ->
                        showMessage("Could not load your profile: " + e.getMessage(), false));
    }

    @SuppressWarnings("unchecked")
    private void findHelpers(List<String> myCourses) {
        List<String> queryList = myCourses.size() > 30 ? myCourses.subList(0, 30) : myCourses;

        db.collection("users")
                .whereArrayContainsAny("coursesOffered", queryList)
                .get()
                .addOnSuccessListener(snap -> {
                    tutors.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        if (d.getId().equals(uid)) {
                            continue;
                        }

                        List<String> theirs = (List<String>) d.get("coursesOffered");
                        List<String> matching = new ArrayList<>();
                        if (theirs != null) {
                            for (String c : theirs) {
                                if (myCourses.contains(c)) {
                                    matching.add(c);
                                }
                            }
                        }
                        if (matching.isEmpty()) {
                            continue;
                        }

                        String name = d.getString("name");
                        Double avg = d.getDouble("avgRating");
                        Long count = d.getLong("ratingCount");
                        Long helped = d.getLong("peopleHelped");
                        if (helped == null) {
                            helped = d.getLong("studentsHelped");
                        }

                        tutors.add(new Tutor(
                                d.getId(),
                                name != null ? name : "Student",
                                d.getString("bio"),
                                matching,
                                avg != null ? avg : 0.0,
                                count != null ? count : 0L,
                                helped != null ? helped : 0L));
                    }

                    Collections.sort(tutors, (a, b) -> {
                        int byMatches = Integer.compare(b.matchingCourses.size(),
                                a.matchingCourses.size());
                        if (byMatches != 0) {
                            return byMatches;
                        }
                        return Double.compare(b.avgRating, a.avgRating);
                    });

                    tutorAdapter.updateTutors(tutors);

                    if (tutors.isEmpty()) {
                        showMessage("No students can help with these courses yet. Check back soon.", true);
                    } else {
                        if (llEmptyState != null) {
                            llEmptyState.setVisibility(View.GONE);
                        }
                        if (tvSubtitle != null) {
                            tvSubtitle.setText(tutors.size() == 1
                                    ? "1 student can help with your courses"
                                    : tutors.size() + " students can help with your courses");
                        }
                    }
                })
                .addOnFailureListener(e ->
                        showMessage("Could not load matches: " + e.getMessage(), false));
    }

    private void showMessage(String message, boolean showAddCoursesButton) {
        if (tvEmpty != null) {
            tvEmpty.setText(message);
        }
        if (llEmptyState != null) {
            llEmptyState.setVisibility(View.VISIBLE);
        }
        if (btnAddCoursesEmpty != null) {
            btnAddCoursesEmpty.setVisibility(showAddCoursesButton ? View.VISIBLE : View.GONE);
        }
    }
}