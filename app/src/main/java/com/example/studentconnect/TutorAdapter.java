package com.example.studentconnect;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TutorAdapter extends RecyclerView.Adapter<TutorAdapter.VH> implements Filterable {

    public interface Listener {
        void onTutorClick(Tutor tutor);
    }

    private final List<Tutor> tutorListFull;
    private final List<Tutor> tutorListCurrent;
    private final Listener listener;

    public TutorAdapter(List<Tutor> tutors, Listener listener) {
        this.tutorListFull = new ArrayList<>(tutors);
        this.tutorListCurrent = new ArrayList<>(tutors);
        this.listener = listener;
    }

    public void updateTutors(List<Tutor> newTutors) {
        this.tutorListFull.clear();
        this.tutorListFull.addAll(newTutors);
        this.tutorListCurrent.clear();
        this.tutorListCurrent.addAll(newTutors);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tutor, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Tutor t = tutorListCurrent.get(position);

        String initial = t.name.isEmpty() ? "?" : t.name.substring(0, 1).toUpperCase();
        holder.tvAvatar.setText(initial);
        holder.tvName.setText(t.name);
        holder.tvCourses.setText(TextUtils.join("  ·  ", t.matchingCourses));
        holder.tvHelped.setText(t.studentsHelped == 1
                ? "1 person helped" : t.studentsHelped + " people helped");

        if (t.ratingCount > 0) {
            holder.tvRating.setText(String.format(Locale.US, "%.1f ★", t.avgRating));
        } else {
            holder.tvRating.setText("New");
        }

        holder.itemView.setOnClickListener(v -> listener.onTutorClick(t));
    }

    @Override
    public int getItemCount() {
        return tutorListCurrent.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Tutor> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(tutorListFull);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (Tutor item : tutorListFull) {
                        boolean matchName = item.name != null && item.name.toLowerCase().contains(filterPattern);
                        boolean matchBio = item.bio != null && item.bio.toLowerCase().contains(filterPattern);
                        boolean matchCourse = false;
                        if (item.matchingCourses != null) {
                            for (String c : item.matchingCourses) {
                                if (c.toLowerCase().contains(filterPattern)) {
                                    matchCourse = true;
                                    break;
                                }
                            }
                        }
                        if (matchName || matchBio || matchCourse) {
                            filteredList.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                tutorListCurrent.clear();
                if (results.values != null) {
                    tutorListCurrent.addAll((List<Tutor>) results.values);
                }
                notifyDataSetChanged();
            }
        };
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvCourses, tvHelped, tvRating;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvName = itemView.findViewById(R.id.tv_tutor_name);
            tvCourses = itemView.findViewById(R.id.tv_tutor_courses);
            tvHelped = itemView.findViewById(R.id.tv_tutor_helped);
            tvRating = itemView.findViewById(R.id.tv_tutor_rating);
        }
    }
}