package com.example.studentconnect;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;

public class StudyGroupChatAdapter extends RecyclerView.Adapter<StudyGroupChatAdapter.VH> {

    private final List<ChatMessage> messages;
    private final String currentUserId;

    public StudyGroupChatAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_group_chat_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChatMessage msg = messages.get(position);
        Context context = holder.itemView.getContext();

        boolean isMe = msg.senderId != null && msg.senderId.equals(currentUserId);

        holder.tvText.setText(msg.text);

        CharSequence timeStr = DateFormat.format("hh:mm a", new Date(msg.timestamp));
        holder.tvTime.setText(timeStr);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.llBubble.getLayoutParams();
        if (isMe) {
            holder.llContainer.setGravity(Gravity.END);
            holder.tvSenderName.setVisibility(View.GONE);
            holder.llBubble.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.brand_primary));
            holder.tvText.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.tvTime.setTextColor(ContextCompat.getColor(context, R.color.primary_tint));
        } else {
            holder.llContainer.setGravity(Gravity.START);
            if (msg.senderName != null && !msg.senderName.isEmpty()) {
                holder.tvSenderName.setText(msg.senderName);
                holder.tvSenderName.setVisibility(View.VISIBLE);
            } else {
                holder.tvSenderName.setVisibility(View.GONE);
            }
            holder.llBubble.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.white));
            holder.tvText.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            holder.tvTime.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }
        holder.llBubble.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout llContainer, llBubble;
        TextView tvSenderName, tvText, tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            llContainer = itemView.findViewById(R.id.ll_message_container);
            llBubble = itemView.findViewById(R.id.ll_bubble);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            tvText = itemView.findViewById(R.id.tv_message_text);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }
    }
}