package com.example.studentconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HomeStudyGroupAdapter extends RecyclerView.Adapter<HomeStudyGroupAdapter.VH> {

    public interface Listener {
        void onGroupClick(StudyGroup group);
    }

    private final List<StudyGroup> groups;
    private final Listener listener;

    public HomeStudyGroupAdapter(List<StudyGroup> groups, Listener listener) {
        this.groups = groups;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_study_group, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        StudyGroup g = groups.get(position);

        holder.tvGroupName.setText(g.getName() != null ? g.getName() : "Group");
        holder.tvCourseCode.setText(g.getCourseCode() != null ? g.getCourseCode() : "");

        String memberText = g.getMemberCount() + "/" + g.getMaxMembers() + " members";
        holder.tvMembersCount.setText(memberText);

        holder.itemView.setOnClickListener(v -> listener.onGroupClick(g));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvGroupName, tvCourseCode, tvMembersCount;

        VH(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tv_group_name);
            tvCourseCode = itemView.findViewById(R.id.tv_course_code);
            tvMembersCount = itemView.findViewById(R.id.tv_members_count);
        }
    }
}