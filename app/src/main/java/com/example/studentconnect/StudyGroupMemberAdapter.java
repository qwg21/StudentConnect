package com.example.studentconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class StudyGroupMemberAdapter extends RecyclerView.Adapter<StudyGroupMemberAdapter.VH> {

    private final List<Map<String, Object>> members;
    private final String creatorId;

    public StudyGroupMemberAdapter(List<Map<String, Object>> members, String creatorId) {
        this.members = members;
        this.creatorId = creatorId;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_group_member, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map<String, Object> member = members.get(position);

        String uid = (String) member.get("uid");
        String name = (String) member.get("name");
        String email = (String) member.get("email");

        if (name == null || name.isEmpty()) {
            name = "Student Member";
        }

        String initial = name.substring(0, 1).toUpperCase();
        holder.tvAvatar.setText(initial);
        holder.tvName.setText(name);
        holder.tvEmail.setText(email != null ? email : "");

        if (uid != null && uid.equals(creatorId)) {
            holder.tvOrganizerBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvOrganizerBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvEmail, tvOrganizerBadge;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_member_avatar);
            tvName = itemView.findViewById(R.id.tv_member_name);
            tvEmail = itemView.findViewById(R.id.tv_member_email);
            tvOrganizerBadge = itemView.findViewById(R.id.tv_organizer_badge);
        }
    }
}