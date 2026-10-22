package com.example.studentconnect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StudyGroupAdapter extends RecyclerView.Adapter<StudyGroupAdapter.VH> implements Filterable {

    public interface Listener {
        void onGroupClick(StudyGroup group);
    }

    private List<StudyGroup> groupsFull;
    private List<StudyGroup> groupsCurrent;
    private final String currentUserId;
    private final Listener listener;

    public StudyGroupAdapter(List<StudyGroup> groups, String currentUserId, Listener listener) {
        this.groupsFull = new ArrayList<>(groups);
        this.groupsCurrent = new ArrayList<>(groups);
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateData(List<StudyGroup> newGroups) {
        this.groupsFull = new ArrayList<>(newGroups);
        this.groupsCurrent = new ArrayList<>(newGroups);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_group, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        StudyGroup g = groupsCurrent.get(position);
        Context context = holder.itemView.getContext();

        holder.tvName.setText(g.getName() != null ? g.getName() : "Study Group");
        holder.tvCourseCode.setText(g.getCourseCode() != null ? g.getCourseCode() : "");
        holder.tvDescription.setText(g.getDescription() != null ? g.getDescription() : "");

        String memberText = g.getMemberCount() + "/" + g.getMaxMembers() + " members";
        holder.tvMembersCount.setText(memberText);
        holder.tvMode.setText(g.getMode() != null ? g.getMode() : "Online");
        holder.tvSchedule.setText(g.getSchedule() != null ? g.getSchedule() : "TBD");

        // Status badge setup
        if (g.isMember(currentUserId)) {
            holder.tvStatus.setText("Joined");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_accepted);
            holder.tvStatus.setVisibility(View.VISIBLE);
        } else if (g.isFull()) {
            holder.tvStatus.setText("Full");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_declined);
            holder.tvStatus.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onGroupClick(g));
    }

    @Override
    public int getItemCount() {
        return groupsCurrent.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<StudyGroup> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(groupsFull);
                } else {
                    String query = constraint.toString().toLowerCase().trim();
                    for (StudyGroup g : groupsFull) {
                        boolean matchName = g.getName() != null && g.getName().toLowerCase().contains(query);
                        boolean matchCourse = g.getCourseCode() != null && g.getCourseCode().toLowerCase().contains(query);
                        boolean matchDesc = g.getDescription() != null && g.getDescription().toLowerCase().contains(query);

                        if (matchName || matchCourse || matchDesc) {
                            filtered.add(g);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                groupsCurrent.clear();
                if (results.values != null) {
                    groupsCurrent.addAll((List<StudyGroup>) results.values);
                }
                notifyDataSetChanged();
            }
        };
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvCourseCode, tvStatus, tvDescription, tvMembersCount, tvMode, tvSchedule;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_group_name);
            tvCourseCode = itemView.findViewById(R.id.tv_course_code);
            tvStatus = itemView.findViewById(R.id.tv_group_status);
            tvDescription = itemView.findViewById(R.id.tv_group_description);
            tvMembersCount = itemView.findViewById(R.id.tv_members_count);
            tvMode = itemView.findViewById(R.id.tv_mode);
            tvSchedule = itemView.findViewById(R.id.tv_schedule);
        }
    }
}