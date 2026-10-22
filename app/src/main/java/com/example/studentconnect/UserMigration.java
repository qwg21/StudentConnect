package com.example.studentconnect;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserMigration {

    @SuppressWarnings("unchecked")
    public static void run(FirebaseFirestore db, String uid, Runnable onDone) {
        DocumentReference ref = db.collection("users").document(uid);

        ref.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                onDone.run();
                return;
            }

            boolean hasNeeded = doc.get("coursesNeeded") != null;
            boolean hasOffered = doc.get("coursesOffered") != null;
            if (hasNeeded && hasOffered && doc.get("peopleHelped") != null) {
                onDone.run();
                return;
            }

            // The legacy role is consulted only to interpret old data during migration.
            // It is not used as an application identity or permission after migration.
            String legacyRole = doc.getString("role");
            List<String> legacyCourses = (List<String>) doc.get("courses");
            List<String> needed = new ArrayList<>();
            List<String> offered = new ArrayList<>();
            if (legacyCourses != null) {
                if ("student".equals(legacyRole)) {
                    needed.addAll(legacyCourses);
                } else if ("tutor".equals(legacyRole)) {
                    offered.addAll(legacyCourses);
                }
            }

            Map<String, Object> data = new HashMap<>();
            if (!hasNeeded) {
                data.put("coursesNeeded", needed);
            }
            if (!hasOffered) {
                data.put("coursesOffered", offered);
            }
            if (doc.get("peopleHelped") == null) {
                Long oldHelped = doc.getLong("studentsHelped");
                data.put("peopleHelped", oldHelped != null ? oldHelped : 0L);
            }

            if (data.isEmpty()) {
                onDone.run();
                return;
            }
            ref.set(data, SetOptions.merge()).addOnCompleteListener(task -> onDone.run());
        }).addOnFailureListener(e -> onDone.run());
    }
}