package com.example.studentconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudyGroupDetailsActivity extends AppCompatActivity {

    private String groupId;
    private String uid;
    private StudyGroup currentGroup;
    private StudyGroupMemberAdapter memberAdapter;
    private final List<Map<String, Object>> membersList = new ArrayList<>();

    private TextView tvGroupName, tvCourseCode, tvDescription;
    private TextView tvMode, tvLocation, tvSchedule, tvCreatorName, tvMembersCount;
    private LinearProgressIndicator pbMembers;
    private Button btnJoinGroup, btnLeaveGroup, btnChatGroup;
    private ImageButton btnDeleteGroup;
    private View rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_group_details);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        groupId = getIntent().getStringExtra("groupId");
        if (user == null || groupId == null) {
            finish();
            return;
        }
        uid = user.getUid();

        rootLayout = findViewById(android.R.id.content);
        View btnBack = findViewById(R.id.btn_back);
        btnDeleteGroup = findViewById(R.id.btn_delete_group);

        tvGroupName = findViewById(R.id.tv_group_name);
        tvCourseCode = findViewById(R.id.tv_course_code);
        tvDescription = findViewById(R.id.tv_description);
        tvMode = findViewById(R.id.tv_mode);
        tvLocation = findViewById(R.id.tv_location);
        tvSchedule = findViewById(R.id.tv_schedule);
        tvCreatorName = findViewById(R.id.tv_creator_name);
        tvMembersCount = findViewById(R.id.tv_members_count);
        pbMembers = findViewById(R.id.pb_members);

        btnJoinGroup = findViewById(R.id.btn_join_group);
        btnLeaveGroup = findViewById(R.id.btn_leave_group);
        btnChatGroup = findViewById(R.id.btn_chat_group);

        RecyclerView rvMembers = findViewById(R.id.rv_members);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnDeleteGroup != null) {
            btnDeleteGroup.setOnClickListener(v -> confirmDeleteGroup());
        }

        if (btnJoinGroup != null) {
            btnJoinGroup.setOnClickListener(v -> joinGroup());
        }

        if (btnLeaveGroup != null) {
            btnLeaveGroup.setOnClickListener(v -> confirmLeaveGroup());
        }

        if (btnChatGroup != null) {
            btnChatGroup.setOnClickListener(v -> {
                Intent i = new Intent(StudyGroupDetailsActivity.this, StudyGroupChatActivity.class);
                i.putExtra("groupId", groupId);
                startActivity(i);
            });
        }

        loadGroupDetails();
    }

    private void loadGroupDetails() {
        StudyGroupRepository.getInstance().getStudyGroupDetails(
                groupId,
                group -> {
                    currentGroup = group;
                    renderUI();
                    loadMembers();
                },
                e -> showSnackbar("Failed to load study group: " + e.getMessage())
        );
    }

    private void renderUI() {
        if (currentGroup == null) return;

        tvGroupName.setText(currentGroup.getName());
        tvCourseCode.setText(currentGroup.getCourseCode());
        tvDescription.setText(currentGroup.getDescription());

        String modeText = "Mode: " + currentGroup.getMode();
        tvMode.setText(modeText);

        String locationText = "Online".equals(currentGroup.getMode())
                ? "Meeting Link: " + currentGroup.getLocation()
                : "Location: " + currentGroup.getLocation();
        tvLocation.setText(locationText);

        String scheduleText = "Schedule: " + currentGroup.getSchedule();
        tvSchedule.setText(scheduleText);

        String creatorText = "Created by: " + (currentGroup.getCreatorName() != null ? currentGroup.getCreatorName() : "Organizer");
        tvCreatorName.setText(creatorText);

        String countText = currentGroup.getMemberCount() + " / " + currentGroup.getMaxMembers() + " Members";
        tvMembersCount.setText(countText);

        int progress = (int) (((double) currentGroup.getMemberCount() / currentGroup.getMaxMembers()) * 100);
        pbMembers.setProgress(progress);

        boolean isMember = currentGroup.isMember(uid);
        boolean isCreator = currentGroup.isCreator(uid);
        boolean isFull = currentGroup.isFull();

        // Organizer Delete Option
        btnDeleteGroup.setVisibility(isCreator ? View.VISIBLE : View.GONE);

        // Reset Bottom Bar Buttons
        btnJoinGroup.setVisibility(View.GONE);
        btnLeaveGroup.setVisibility(View.GONE);
        btnChatGroup.setVisibility(View.GONE);

        if (!isMember) {
            btnJoinGroup.setVisibility(View.VISIBLE);
            if (isFull) {
                btnJoinGroup.setText("Group Full");
                btnJoinGroup.setEnabled(false);
            } else {
                btnJoinGroup.setText("Join Group");
                btnJoinGroup.setEnabled(true);
            }
        } else {
            btnChatGroup.setVisibility(View.VISIBLE);
            btnLeaveGroup.setVisibility(View.VISIBLE);
        }
    }

    private void loadMembers() {
        if (currentGroup == null || currentGroup.getMemberIds() == null) return;

        StudyGroupRepository.getInstance().getMembersForGroup(
                currentGroup.getMemberIds(),
                members -> {
                    membersList.clear();
                    membersList.addAll(members);
                    memberAdapter = new StudyGroupMemberAdapter(membersList, currentGroup.getCreatorId());
                    RecyclerView rvMembers = findViewById(R.id.rv_members);
                    if (rvMembers != null) {
                        rvMembers.setAdapter(memberAdapter);
                    }
                },
                e -> showSnackbar("Could not load member details")
        );
    }

    private void joinGroup() {
        btnJoinGroup.setEnabled(false);
        StudyGroupRepository.getInstance().joinStudyGroup(
                groupId,
                uid,
                unused -> {
                    showSnackbar("Joined study group!");
                    loadGroupDetails();
                },
                e -> {
                    btnJoinGroup.setEnabled(true);
                    showSnackbar("Could not join: " + e.getMessage());
                }
        );
    }

    private void confirmLeaveGroup() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Study Group")
                .setMessage("Are you sure you want to leave " + (currentGroup != null ? currentGroup.getName() : "this group") + "?")
                .setPositiveButton("Leave", (dialog, which) -> leaveGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveGroup() {
        StudyGroupRepository.getInstance().leaveStudyGroup(
                groupId,
                uid,
                unused -> {
                    showSnackbar("Left study group");
                    finish();
                },
                e -> showSnackbar("Could not leave group: " + e.getMessage())
        );
    }

    private void confirmDeleteGroup() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Study Group")
                .setMessage("Delete " + (currentGroup != null ? currentGroup.getName() : "this group") + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteGroup() {
        StudyGroupRepository.getInstance().deleteStudyGroup(
                groupId,
                unused -> {
                    showSnackbar("Study group deleted");
                    finish();
                },
                e -> showSnackbar("Could not delete group: " + e.getMessage())
        );
    }

    private void showSnackbar(String message) {
        if (rootLayout != null) {
            Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT).show();
        }
    }
}