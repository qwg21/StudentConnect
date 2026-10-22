package com.example.studentconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class StudyGroupChatActivity extends AppCompatActivity {

    private final List<ChatMessage> messageList = new ArrayList<>();
    private StudyGroupChatAdapter adapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private TextView tvGroupName, tvCourseCode;
    private ListenerRegistration listenerRegistration;

    private String groupId;
    private String uid;
    private String myName = "Student";
    private FirebaseFirestore db;
    private View rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_group_chat);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        groupId = getIntent().getStringExtra("groupId");

        if (user == null || groupId == null) {
            finish();
            return;
        }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        rootLayout = findViewById(android.R.id.content);
        View btnBack = findViewById(R.id.btn_back);
        tvGroupName = findViewById(R.id.tv_group_name);
        tvCourseCode = findViewById(R.id.tv_course_code);
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        ImageButton btnSend = findViewById(R.id.btn_send);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> sendMessage());
        }

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);

        adapter = new StudyGroupChatAdapter(messageList, uid);
        rvMessages.setAdapter(adapter);

        loadMyName();
        verifyMembershipAndStartChat();
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

    private void verifyMembershipAndStartChat() {
        StudyGroupRepository.getInstance().getStudyGroupDetails(
                groupId,
                group -> {
                    if (!group.isMember(uid)) {
                        Toast.makeText(this, "Only members can access this group chat.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    tvGroupName.setText(group.getName());
                    tvCourseCode.setText(group.getCourseCode());

                    listenForMessages();
                },
                e -> {
                    Toast.makeText(this, "Failed to load group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
        );
    }

    private void listenForMessages() {
        listenerRegistration = StudyGroupRepository.getInstance().listenForMessages(
                groupId,
                (snap, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (snap != null) {
                        messageList.clear();
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Long time = doc.getLong("timestamp");
                            messageList.add(new ChatMessage(
                                    doc.getString("senderId"),
                                    doc.getString("senderName"),
                                    doc.getString("text"),
                                    time != null ? time : 0L
                            ));
                        }
                        adapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            rvMessages.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }
                }
        );
    }

    private void sendMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) {
            return;
        }

        ChatMessage msg = new ChatMessage(
                uid,
                myName,
                text,
                System.currentTimeMillis()
        );

        etMessage.setText("");

        StudyGroupRepository.getInstance().sendMessage(
                groupId,
                msg,
                unused -> {},
                e -> {
                    if (rootLayout != null) {
                        Snackbar.make(rootLayout, "Failed to send message", Snackbar.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}