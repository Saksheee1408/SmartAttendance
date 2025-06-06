package com.example.attendance;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private List<AttendanceRecord> attendanceRecords;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());

    public AttendanceAdapter(List<AttendanceRecord> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRecord record = attendanceRecords.get(position);

        holder.tvUserName.setText(record.getUserName());
        holder.tvDate.setText(dateFormat.format(record.getTimestamp()));
        holder.tvCourse.setText(record.getCourseName());

        // Highlight absent students
        if ("ABSENT".equals(record.getCourseName())) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFCCCC"));  // Light red for absent
            holder.tvUserName.setTextColor(Color.parseColor("#CC0000"));      // Darker red for text
            holder.tvCourse.setTextColor(Color.parseColor("#CC0000"));
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));  // White for present
            holder.tvUserName.setTextColor(Color.parseColor("#000000"));      // Black text
            holder.tvCourse.setTextColor(Color.parseColor("#000000"));
        }
    }

    @Override
    public int getItemCount() {
        return attendanceRecords.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvDate, tvCourse;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCourse = itemView.findViewById(R.id.tvCourse);
        }
    }
}