package com.example.studentconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.VH> {

    public interface Listener {
        void onRemove(String course);
    }

    private final List<String> courses;
    private final Listener listener;

    public CourseAdapter(List<String> courses, Listener listener) {
        this.courses = courses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String course = courses.get(position);
        holder.tvCourse.setText(course);
        holder.tvRemove.setOnClickListener(v -> listener.onRemove(course));
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvCourse, tvRemove;

        VH(@NonNull View itemView) {
            super(itemView);
            tvCourse = itemView.findViewById(R.id.tv_course);
            tvRemove = itemView.findViewById(R.id.tv_remove);
        }
    }
}