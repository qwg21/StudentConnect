package com.example.studentconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome, tvRating, tvPeopleHelped, tvNeededEmpty, tvOfferedEmpty;
    private ChipGroup chipsNeeded, chipsOffered;
    private RecyclerView rvMyStudyGroups;
    private View cardJoinOrCreateStudyGroup;
    private HomeStudyGroupAdapter myStudyGroupAdapter;
    private final List<StudyGroup> myStudyGroupsList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;
    private boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }
        uid = user.getUid();

        tvWelcome = findViewById(R.id.tv_welcome);
        tvRating = findViewById(R.id.tv_avg_rating);
        tvPeopleHelped = findViewById(R.id.tv_people_helped);
        tvNeededEmpty = findViewById(R.id.tv_needed_empty);
        tvOfferedEmpty = findViewById(R.id.tv_offered_empty);
        chipsNeeded = findViewById(R.id.chips_needed);
        chipsOffered = findViewById(R.id.chips_offered);

        rvMyStudyGroups = findViewById(R.id.rv_my_study_groups);
        cardJoinOrCreateStudyGroup = findViewById(R.id.card_join_or_create_study_group);
        View tvSeeAllStudyGroups = findViewById(R.id.tv_see_all_study_groups);

        Button btnFindTutors = findViewById(R.id.btnFindTutors);
        if (btnFindTutors != null) {
            btnFindTutors.setOnClickListener(v ->
                    startActivity(new Intent(this, TutorMatchesActivity.class)));
        }

        if (rvMyStudyGroups != null) {
            rvMyStudyGroups.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            myStudyGroupAdapter = new HomeStudyGroupAdapter(myStudyGroupsList, group -> {
                Intent i = new Intent(HomeActivity.this, StudyGroupDetailsActivity.class);
                i.putExtra("groupId", group.getId());
                startActivity(i);
            });
            rvMyStudyGroups.setAdapter(myStudyGroupAdapter);
        }

        View.OnClickListener openStudyGroupsListener = v -> {
            Intent intent = new Intent(HomeActivity.this, TutorMatchesActivity.class);
            startActivity(intent);
        };

        if (tvSeeAllStudyGroups != null) {
            tvSeeAllStudyGroups.setOnClickListener(openStudyGroupsListener);
        }
        if (cardJoinOrCreateStudyGroup != null) {
            cardJoinOrCreateStudyGroup.setOnClickListener(openStudyGroupsListener);
        }

        // Bento Department Category Cards click listeners
        View cardProg = findViewById(R.id.card_cat_programming);
        View cardMath = findViewById(R.id.card_cat_math);
        View cardSystems = findViewById(R.id.card_cat_systems);
        View cardSci = findViewById(R.id.card_cat_sciences);

        if (cardProg != null) {
            cardProg.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, TutorMatchesActivity.class);
                intent.putExtra("CATEGORY_FILTER", "Software Dev");
                startActivity(intent);
            });
        }
        if (cardMath != null) {
            cardMath.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, TutorMatchesActivity.class);
                intent.putExtra("CATEGORY_FILTER", "Mathematics");
                startActivity(intent);
            });
        }
        if (cardSystems != null) {
            cardSystems.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, TutorMatchesActivity.class);
                intent.putExtra("CATEGORY_FILTER", "Database & Systems");
                startActivity(intent);
            });
        }
        if (cardSci != null) {
            cardSci.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, TutorMatchesActivity.class);
                intent.putExtra("CATEGORY_FILTER", "Computer Networks");
                startActivity(intent);
            });
        }

        // Edit buttons for courses deck
        View tvEditNeeded = findViewById(R.id.tv_edit_needed);
        View tvEditOffered = findViewById(R.id.tv_edit_offered);
        if (tvEditNeeded != null) {
            tvEditNeeded.setOnClickListener(v ->
                    startActivity(new Intent(this, MyCoursesActivity.class)));
        }
        if (tvEditOffered != null) {
            tvEditOffered.setOnClickListener(v ->
                    startActivity(new Intent(this, MyCoursesActivity.class)));
        }

        // Bottom Navigation bar listeners
        View navDiscover = findViewById(R.id.nav_item_discover);
        View navRequests = findViewById(R.id.nav_item_requests);
        View navProfile = findViewById(R.id.nav_item_profile);
        View avatarHeader = findViewById(R.id.cv_avatar_header);

        if (navDiscover != null) {
            navDiscover.setOnClickListener(v ->
                    startActivity(new Intent(this, TutorMatchesActivity.class)));
        }
        if (navRequests != null) {
            navRequests.setOnClickListener(v ->
                    startActivity(new Intent(this, RequestsActivity.class)));
        }
        if (navProfile != null) {
            navProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));
        }
        if (avatarHeader != null) {
            avatarHeader.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));
        }

        UserMigration.run(db, uid, () -> loadProfile());
        loadMyStudyGroups();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (started && uid != null) {
            loadProfile();
            loadMyStudyGroups();
        }
        started = true;
    }

    private void loadMyStudyGroups() {
        if (uid == null) return;

        StudyGroupRepository.getInstance().getStudyGroupsForUser(
                uid,
                groups -> {
                    myStudyGroupsList.clear();
                    myStudyGroupsList.addAll(groups);
                    if (myStudyGroupAdapter != null) {
                        myStudyGroupAdapter.notifyDataSetChanged();
                    }

                    if (myStudyGroupsList.isEmpty()) {
                        if (rvMyStudyGroups != null) rvMyStudyGroups.setVisibility(View.GONE);
                        if (cardJoinOrCreateStudyGroup != null) cardJoinOrCreateStudyGroup.setVisibility(View.VISIBLE);
                    } else {
                        if (rvMyStudyGroups != null) rvMyStudyGroups.setVisibility(View.VISIBLE);
                        if (cardJoinOrCreateStudyGroup != null) cardJoinOrCreateStudyGroup.setVisibility(View.GONE);
                    }
                },
                e -> {}
        );
    }

    @SuppressWarnings("unchecked")
    private void loadProfile() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        tvWelcome.setText(greeting() + "!");
                        return;
                    }
                    String name = doc.getString("name");
                    Double avg = doc.getDouble("avgRating");
                    Long count = doc.getLong("ratingCount");
                    Long helped = doc.getLong("peopleHelped");
                    List<String> needed = (List<String>) doc.get("coursesNeeded");
                    List<String> offered = (List<String>) doc.get("coursesOffered");

                    tvWelcome.setText(greeting() + (name != null && !name.isEmpty()
                            ? ", " + name : "") + "!");

                    if (count != null && count > 0 && avg != null) {
                        tvRating.setText(String.format(Locale.US, "%.1f ★", avg));
                    } else {
                        tvRating.setText("-");
                    }
                    tvPeopleHelped.setText(String.valueOf(helped != null ? helped : 0L));

                    fillChips(chipsNeeded, tvNeededEmpty, needed, false);
                    fillChips(chipsOffered, tvOfferedEmpty, offered, true);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load profile", Toast.LENGTH_SHORT).show());
    }

    private void fillChips(ChipGroup group, TextView emptyView, List<String> courses,
                           boolean highlighted) {
        group.removeAllViews();
        if (courses == null || courses.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        emptyView.setVisibility(View.GONE);
        for (String c : courses) {
            group.addView(makeChip(c, highlighted));
        }
    }

    private Chip makeChip(String text, boolean highlighted) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setClickable(false);
        chip.setCheckable(false);
        chip.setChipStrokeWidth(0f);
        if (highlighted) {
            chip.setChipBackgroundColorResource(R.color.card_tint_peach);
            chip.setTextColor(getColor(R.color.text_primary));
        } else {
            chip.setChipBackgroundColorResource(R.color.card_tint_lavender);
            chip.setTextColor(getColor(R.color.text_primary));
        }
        return chip;
    }

    private String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return "Good morning";
        } else if (hour < 17) {
            return "Good afternoon";
        }
        return "Good evening";
    }

    private void comingSoon(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}