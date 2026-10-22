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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private final List<ChatMessage> messageList = new ArrayList<>();
    private ChatAdapter adapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;

    private String requestId;
    private String uid;
    private String myName = "Student";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        requestId = getIntent().getStringExtra("requestId");
        String peerName = getIntent().getStringExtra("peerName");
        String course = getIntent().getStringExtra("course");

        if (user == null || requestId == null) {
            finish();
            return;
        }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        View btnBack = findViewById(R.id.btn_back);
        TextView tvPeerName = findViewById(R.id.tv_peer_name);
        TextView tvCourse = findViewById(R.id.tv_course_code);
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        ImageButton btnSend = findViewById(R.id.btn_send);

        tvPeerName.setText(peerName != null ? peerName : "Student");
        tvCourse.setText(course != null ? course : "Course");

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);

        adapter = new ChatAdapter(messageList, uid);
        rvMessages.setAdapter(adapter);

        loadMyName();
        listenForMessages();
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

    private void listenForMessages() {
        listenerRegistration = db.collection("chats")
                .document(requestId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, error) -> {
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
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return;
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", uid);
        msg.put("senderName", myName);
        msg.put("text", text);
        msg.put("timestamp", System.currentTimeMillis());

        etMessage.setText("");

        db.collection("chats").document(requestId)
                .collection("messages")
                .add(msg)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}