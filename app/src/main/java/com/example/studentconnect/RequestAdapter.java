package com.example.studentconnect;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onAccept(Request request);
        void onDecline(Request request);
        void onCancel(Request request);
        void onChat(Request request);
        void onComplete(Request request);
        void onRate(Request request);
        void onItemClick(Request request);
    }

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CARD = 1;

    // Display list containing headers and requests
    private final List<Object> displayItems = new ArrayList<>();
    private final boolean isReceived;
    private final Listener listener;

    public RequestAdapter(List<Request> rawRequests, boolean isReceived, Listener listener) {
        this.isReceived = isReceived;
        this.listener = listener;
        buildDisplayList(rawRequests);
    }

    public void updateData(List<Request> rawRequests) {
        buildDisplayList(rawRequests);
        notifyDataSetChanged();
    }

    private void buildDisplayList(List<Request> rawRequests) {
        displayItems.clear();
        if (rawRequests == null || rawRequests.isEmpty()) {
            return;
        }

        if (isReceived) {
            List<Request> pendingList = new ArrayList<>();
            List<Request> earlierList = new ArrayList<>();

            for (Request r : rawRequests) {
                if ("pending".equalsIgnoreCase(r.status)) {
                    pendingList.add(r);
                } else {
                    earlierList.add(r);
                }
            }

            if (!pendingList.isEmpty()) {
                displayItems.add("Needs your response");
                displayItems.addAll(pendingList);
            }

            if (!earlierList.isEmpty()) {
                displayItems.add("Earlier");
                displayItems.addAll(earlierList);
            }
        } else {
            displayItems.addAll(rawRequests);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (displayItems.get(position) instanceof String) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_CARD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_request_section_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_request, parent, false);
            return new CardVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_HEADER) {
            HeaderVH headerVH = (HeaderVH) holder;
            headerVH.tvSectionTitle.setText((String) displayItems.get(position));
        } else {
            CardVH cardVH = (CardVH) holder;
            Request r = (Request) displayItems.get(position);
            bindCard(cardVH, r);
        }
    }

    private void bindCard(CardVH holder, Request r) {
        Context context = holder.itemView.getContext();

        String displayName = isReceived ? r.studentName : r.tutorName;
        if (displayName == null || displayName.isEmpty()) {
            displayName = "Student";
        }

        String initial = displayName.substring(0, 1).toUpperCase();
        holder.tvAvatar.setText(initial);
        holder.tvTitle.setText(displayName);
        holder.tvCourse.setText(r.course);

        // Formatted timestamp
        if (r.createdAt > 0) {
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    r.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.tvTime.setText(relativeTime);
            holder.tvTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvTime.setVisibility(View.GONE);
        }

        String status = r.status != null ? r.status : "pending";
        holder.tvStatus.setText(capitalize(status));

        // Reset status style
        if ("accepted".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_accepted);
        } else if ("declined".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_declined);
        } else if ("completed".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_primary));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
        } else { // pending
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warning));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }

        // Reset all action buttons visibility
        holder.btnDecline.setVisibility(View.GONE);
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);
        holder.btnChat.setVisibility(View.GONE);
        holder.btnComplete.setVisibility(View.GONE);
        holder.btnRate.setVisibility(View.GONE);
        holder.llActions.setVisibility(View.GONE);

        // Action button configurations strictly by tab and status
        if (isReceived) {
            if ("pending".equalsIgnoreCase(status)) {
                holder.llActions.setVisibility(View.VISIBLE);
                holder.btnDecline.setVisibility(View.VISIBLE);
                holder.btnAccept.setVisibility(View.VISIBLE);
            } else if ("accepted".equalsIgnoreCase(status)) {
                holder.llActions.setVisibility(View.VISIBLE);
                holder.btnChat.setVisibility(View.VISIBLE);
                holder.btnComplete.setVisibility(View.VISIBLE);
            }
        } else { // Sent tab
            if ("pending".equalsIgnoreCase(status)) {
                holder.llActions.setVisibility(View.VISIBLE);
                holder.btnCancel.setVisibility(View.VISIBLE);
            } else if ("accepted".equalsIgnoreCase(status)) {
                holder.llActions.setVisibility(View.VISIBLE);
                holder.btnChat.setVisibility(View.VISIBLE);
            } else if ("completed".equalsIgnoreCase(status)) {
                holder.llActions.setVisibility(View.VISIBLE);
                holder.btnRate.setVisibility(View.VISIBLE);
            }
        }

        // Click listeners
        holder.btnAccept.setOnClickListener(v -> listener.onAccept(r));
        holder.btnDecline.setOnClickListener(v -> listener.onDecline(r));
        holder.btnCancel.setOnClickListener(v -> listener.onCancel(r));
        holder.btnChat.setOnClickListener(v -> listener.onChat(r));
        holder.btnComplete.setOnClickListener(v -> listener.onComplete(r));
        holder.btnRate.setOnClickListener(v -> listener.onRate(r));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(r));
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvSectionTitle;

        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvSectionTitle = itemView.findViewById(R.id.tv_section_title);
        }
    }

    static class CardVH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvTitle, tvCourse, tvTime, tvStatus, tvMessagePreview;
        View llActions;
        Button btnDecline, btnAccept, btnCancel, btnChat, btnComplete, btnRate;

        CardVH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvCourse = itemView.findViewById(R.id.tv_course);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvMessagePreview = itemView.findViewById(R.id.tv_message_preview);
            llActions = itemView.findViewById(R.id.ll_actions);
            btnDecline = itemView.findViewById(R.id.btn_decline);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            btnChat = itemView.findViewById(R.id.btn_chat);
            btnComplete = itemView.findViewById(R.id.btn_complete);
            btnRate = itemView.findViewById(R.id.btn_rate);
        }
    }
}