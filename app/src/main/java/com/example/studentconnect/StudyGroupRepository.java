package com.example.studentconnect;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyGroupRepository {

    private static StudyGroupRepository instance;
    private final FirebaseFirestore db;

    private StudyGroupRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized StudyGroupRepository getInstance() {
        if (instance == null) {
            instance = new StudyGroupRepository();
        }
        return instance;
    }

    // 1. Create a study group
    public void createStudyGroup(StudyGroup group, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        DocumentReference docRef = db.collection("studyGroups").document();
        group.setId(docRef.getId());
        group.setCreatedAt(System.currentTimeMillis());
        if (group.getMemberIds() == null || group.getMemberIds().isEmpty()) {
            List<String> members = new ArrayList<>();
            if (group.getCreatorId() != null) {
                members.add(group.getCreatorId());
            }
            group.setMemberIds(members);
        }
        group.setMemberCount(group.getMemberIds().size());

        docRef.set(group)
                .addOnSuccessListener(unused -> onSuccess.onSuccess(docRef.getId()))
                .addOnFailureListener(onFailure);
    }

    // 2. Fetch all study groups
    public void getStudyGroups(OnSuccessListener<List<StudyGroup>> onSuccess, OnFailureListener onFailure) {
        db.collection("studyGroups")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<StudyGroup> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        StudyGroup g = doc.toObject(StudyGroup.class);
                        if (g != null) {
                            g.setId(doc.getId());
                            list.add(g);
                        }
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    // 3. Fetch study groups for a specific user
    public void getStudyGroupsForUser(String userId, OnSuccessListener<List<StudyGroup>> onSuccess, OnFailureListener onFailure) {
        db.collection("studyGroups")
                .whereArrayContains("memberIds", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<StudyGroup> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        StudyGroup g = doc.toObject(StudyGroup.class);
                        if (g != null) {
                            g.setId(doc.getId());
                            list.add(g);
                        }
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    // 4. Get details of a single study group
    public void getStudyGroupDetails(String groupId, OnSuccessListener<StudyGroup> onSuccess, OnFailureListener onFailure) {
        db.collection("studyGroups").document(groupId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        StudyGroup g = doc.toObject(StudyGroup.class);
                        if (g != null) {
                            g.setId(doc.getId());
                            onSuccess.onSuccess(g);
                            return;
                        }
                    }
                    onFailure.onFailure(new Exception("Study group not found"));
                })
                .addOnFailureListener(onFailure);
    }

    // 5. Join a study group via Firestore transaction
    public void joinStudyGroup(String groupId, String userId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        DocumentReference groupRef = db.collection("studyGroups").document(groupId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(groupRef);
            if (!snapshot.exists()) {
                throw new IllegalStateException("Study group does not exist.");
            }

            StudyGroup group = snapshot.toObject(StudyGroup.class);
            if (group == null) {
                throw new IllegalStateException("Failed to parse study group data.");
            }

            if (group.isMember(userId)) {
                throw new IllegalStateException("You are already a member of this study group.");
            }

            if (group.isFull()) {
                throw new IllegalStateException("This study group is full.");
            }

            List<String> members = group.getMemberIds();
            if (members == null) {
                members = new ArrayList<>();
            }
            members.add(userId);
            long newCount = members.size();

            transaction.update(groupRef,
                    "memberIds", members,
                    "memberCount", newCount);

            return null;
        }).addOnSuccessListener(result -> onSuccess.onSuccess(null))
          .addOnFailureListener(onFailure);
    }

    // 6. Leave a study group via Firestore transaction
    public void leaveStudyGroup(String groupId, String userId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        DocumentReference groupRef = db.collection("studyGroups").document(groupId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(groupRef);
            if (!snapshot.exists()) {
                return null;
            }

            StudyGroup group = snapshot.toObject(StudyGroup.class);
            if (group == null) {
                return null;
            }

            List<String> members = group.getMemberIds();
            if (members != null) {
                members.remove(userId);
            } else {
                members = new ArrayList<>();
            }

            if (members.isEmpty()) {
                transaction.delete(groupRef);
            } else {
                long newCount = members.size();
                Map<String, Object> updates = new HashMap<>();
                updates.put("memberIds", members);
                updates.put("memberCount", newCount);

                // Reassign organizer if leaving user was the creator
                if (userId.equals(group.getCreatorId())) {
                    String newCreatorId = members.get(0);
                    updates.put("creatorId", newCreatorId);
                }

                transaction.update(groupRef, updates);
            }

            return null;
        }).addOnSuccessListener(result -> onSuccess.onSuccess(null))
          .addOnFailureListener(onFailure);
    }

    // 7. Delete a study group
    public void deleteStudyGroup(String groupId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("studyGroups").document(groupId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // 8. Fetch user details for members in chunks of 10
    public void getMembersForGroup(List<String> memberIds, OnSuccessListener<List<Map<String, Object>>> onSuccess, OnFailureListener onFailure) {
        if (memberIds == null || memberIds.isEmpty()) {
            onSuccess.onSuccess(new ArrayList<>());
            return;
        }

        List<String> queryList = memberIds.size() > 10 ? memberIds.subList(0, 10) : memberIds;

        db.collection("users")
                .whereIn(FieldPath.documentId(), queryList)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> members = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("uid", doc.getId());
                            members.add(data);
                        }
                    }
                    onSuccess.onSuccess(members);
                })
                .addOnFailureListener(onFailure);
    }

    // 9. Send a message to study group chat
    public void sendMessage(String groupId, ChatMessage message, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        DocumentReference groupRef = db.collection("studyGroups").document(groupId);

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("senderId", message.senderId);
        msgMap.put("senderName", message.senderName);
        msgMap.put("text", message.text);
        msgMap.put("timestamp", message.timestamp);

        groupRef.collection("messages").add(msgMap)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastMessage", message.text);
                    updates.put("lastMessageAt", message.timestamp);
                    groupRef.set(updates, SetOptions.merge());
                    onSuccess.onSuccess(null);
                })
                .addOnFailureListener(onFailure);
    }

    // 10. Listen for real-time messages in study group chat
    public ListenerRegistration listenForMessages(String groupId, EventListener<QuerySnapshot> listener) {
        return db.collection("studyGroups")
                .document(groupId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }
}