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

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<ChatMessage> messages;
    private final String currentUserId;

    public ChatAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
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
            holder.llBubble.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.brand_primary));
            holder.tvText.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.tvTime.setTextColor(ContextCompat.getColor(context, R.color.primary_tint));
        } else {
            holder.llContainer.setGravity(Gravity.START);
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
        TextView tvText, tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            llContainer = itemView.findViewById(R.id.ll_message_container);
            llBubble = itemView.findViewById(R.id.ll_bubble);
            tvText = itemView.findViewById(R.id.tv_message_text);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }
    }
}