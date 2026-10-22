package com.example.studentconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestsActivity extends AppCompatActivity {

    private final List<Request> allReceivedRequests = new ArrayList<>();
    private final List<Request> allSentRequests = new ArrayList<>();
    private final List<Request> filteredRequests = new ArrayList<>();

    private View llEmptyState;
    private TextView tvEmpty, tvEmptySubtitle;
    private Button btnFindHelper, btnTabReceived, btnTabSent;
    private ProgressBar progressBar;
    private View rootLayout;

    private FirebaseFirestore db;
    private String uid;
    private boolean isReceivedTab = true;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        rootLayout = findViewById(android.R.id.content);

        View btnBack = findViewById(R.id.btn_back);
        llEmptyState = findViewById(R.id.ll_empty_state);
        tvEmpty = findViewById(R.id.tv_empty);
        tvEmptySubtitle = findViewById(R.id.tv_empty_subtitle);
        btnFindHelper = findViewById(R.id.btn_find_helper);
        btnTabReceived = findViewById(R.id.btn_tab_received);
        btnTabSent = findViewById(R.id.btn_tab_sent);
        progressBar = findViewById(R.id.progress_bar);
        ChipGroup chipGroupFilters = findViewById(R.id.chip_group_filters);

        MaterialButtonToggleGroup toggle = findViewById(R.id.toggle_group);
        RecyclerView rv = findViewById(R.id.rv_requests);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnFindHelper != null) {
            btnFindHelper.setOnClickListener(v -> {
                Intent i = new Intent(this, TutorMatchesActivity.class);
                startActivity(i);
            });
        }

        rv.setLayoutManager(new LinearLayoutManager(this));
        setupAdapter(rv);
        setupBottomNav();

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            isReceivedTab = (checkedId == R.id.btn_tab_received);
            applyFilterAndRender();
        });

        if (chipGroupFilters != null) {
            chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                if (id == R.id.chip_filter_pending) {
                    currentFilter = "pending";
                } else if (id == R.id.chip_filter_accepted) {
                    currentFilter = "accepted";
                } else if (id == R.id.chip_filter_declined) {
                    currentFilter = "declined";
                } else if (id == R.id.chip_filter_completed) {
                    currentFilter = "completed";
                } else {
                    currentFilter = "all";
                }
                applyFilterAndRender();
            });
        }

        loadAllRequests();
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.nav_item_home);
        View navDiscover = findViewById(R.id.nav_item_discover);
        View navProfile = findViewById(R.id.nav_item_profile);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent i = new Intent(this, HomeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            });
        }
        if (navDiscover != null) {
            navDiscover.setOnClickListener(v -> {
                Intent i = new Intent(this, TutorMatchesActivity.class);
                startActivity(i);
            });
        }
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent i = new Intent(this, ProfileActivity.class);
                startActivity(i);
            });
        }

        // Highlight Active Requests Tab
        View navIconRequests = findViewById(R.id.nav_icon_requests);
        TextView navLabelRequests = findViewById(R.id.nav_label_requests);
        if (navIconRequests instanceof ImageView) {
            ((ImageView) navIconRequests).setColorFilter(getColor(R.color.brand_primary));
        }
        if (navLabelRequests != null) {
            navLabelRequests.setTextColor(getColor(R.color.brand_primary));
        }
    }

    private void setupAdapter(RecyclerView rv) {
        RequestAdapter adapter = new RequestAdapter(filteredRequests, isReceivedTab, new RequestAdapter.Listener() {
            @Override
            public void onAccept(Request request) {
                updateRequestStatus(request.requestId, "accepted", "Request accepted");
            }

            @Override
            public void onDecline(Request request) {
                updateRequestStatus(request.requestId, "declined", "Request declined");
            }

            @Override
            public void onCancel(Request request) {
                updateRequestStatus(request.requestId, "declined", "Request cancelled");
            }

            @Override
            public void onChat(Request request) {
                Intent i = new Intent(RequestsActivity.this, ChatActivity.class);
                i.putExtra("requestId", request.requestId);
                i.putExtra("peerName", isReceivedTab ? request.studentName : request.tutorName);
                i.putExtra("course", request.course);
                startActivity(i);
            }

            @Override
            public void onComplete(Request request) {
                updateRequestStatus(request.requestId, "completed", "Marked as completed");
            }

            @Override
            public void onRate(Request request) {
                Intent i = new Intent(RequestsActivity.this, RateHelperActivity.class);
                i.putExtra("tutorId", request.tutorId);
                i.putExtra("tutorName", request.tutorName);
                i.putExtra("course", request.course);
                startActivity(i);
            }

            @Override
            public void onItemClick(Request request) {
                if ("accepted".equalsIgnoreCase(request.status)) {
                    onChat(request);
                } else if (!isReceivedTab && "completed".equalsIgnoreCase(request.status)) {
                    onRate(request);
                }
            }
        });
        rv.setAdapter(adapter);
    }

    private void loadAllRequests() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Fetch received requests
        db.collection("requests")
                .whereEqualTo("tutorId", uid)
                .get()
                .addOnSuccessListener(snapReceived -> {
                    allReceivedRequests.clear();
                    for (DocumentSnapshot doc : snapReceived.getDocuments()) {
                        Long time = doc.getLong("createdAt");
                        allReceivedRequests.add(new Request(
                                doc.getId(),
                                doc.getString("studentId"),
                                doc.getString("studentName"),
                                doc.getString("tutorId"),
                                doc.getString("tutorName"),
                                doc.getString("course"),
                                doc.getString("status"),
                                time != null ? time : 0L
                        ));
                    }
                    Collections.sort(allReceivedRequests, (a, b) -> Long.compare(b.createdAt, a.createdAt));

                    // Fetch sent requests
                    db.collection("requests")
                            .whereEqualTo("studentId", uid)
                            .get()
                            .addOnSuccessListener(snapSent -> {
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                                allSentRequests.clear();
                                for (DocumentSnapshot doc : snapSent.getDocuments()) {
                                    Long time = doc.getLong("createdAt");
                                    allSentRequests.add(new Request(
                                            doc.getId(),
                                            doc.getString("studentId"),
                                            doc.getString("studentName"),
                                            doc.getString("tutorId"),
                                            doc.getString("tutorName"),
                                            doc.getString("course"),
                                            doc.getString("status"),
                                            time != null ? time : 0L
                                    ));
                                }
                                Collections.sort(allSentRequests, (a, b) -> Long.compare(b.createdAt, a.createdAt));

                                updateTabBadges();
                                applyFilterAndRender();
                            })
                            .addOnFailureListener(e -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                updateTabBadges();
                                applyFilterAndRender();
                            });
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    updateTabBadges();
                    applyFilterAndRender();
                });
    }

    private void updateTabBadges() {
        if (btnTabReceived != null) {
            btnTabReceived.setText("Received (" + allReceivedRequests.size() + ")");
        }
        if (btnTabSent != null) {
            btnTabSent.setText("Sent (" + allSentRequests.size() + ")");
        }
    }

    private void applyFilterAndRender() {
        List<Request> source = isReceivedTab ? allReceivedRequests : allSentRequests;
        filteredRequests.clear();

        for (Request r : source) {
            String status = r.status != null ? r.status.toLowerCase() : "pending";
            if ("all".equalsIgnoreCase(currentFilter) || status.equalsIgnoreCase(currentFilter)) {
                filteredRequests.add(r);
            }
        }

        // Re-setup adapter with current mode
        RecyclerView rv = findViewById(R.id.rv_requests);
        if (rv != null) {
            setupAdapter(rv);
        }

        boolean isEmpty = filteredRequests.isEmpty();
        if (llEmptyState != null) {
            llEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }

        if (isEmpty) {
            if (isReceivedTab) {
                tvEmpty.setText("No received requests");
                tvEmptySubtitle.setText("When students request your help with a course, they will appear here.");
                btnFindHelper.setVisibility(View.GONE);
            } else {
                tvEmpty.setText("No sent requests");
                tvEmptySubtitle.setText("Looking for academic help? Search for student tutors to send a request.");
                btnFindHelper.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateRequestStatus(String requestId, String status, String confirmationMsg) {
        db.collection("requests").document(requestId)
                .update("status", status)
                .addOnSuccessListener(unused -> {
                    showConfirmation(confirmationMsg);
                    loadAllRequests();
                })
                .addOnFailureListener(e ->
                        showConfirmation("Failed: " + e.getMessage()));
    }

    private void showConfirmation(String msg) {
        if (rootLayout != null) {
            Snackbar.make(rootLayout, msg, Snackbar.LENGTH_SHORT).show();
        }
    }
}