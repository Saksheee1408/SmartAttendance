package com.example.attendance.ui;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private List<> examList;

    // Constructor
    public MyAdapter(List<ExamItem> examList) {
        this.examList = examList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.exam_card, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ExamItem examItem = examList.get(position);

        holder.examName.setText(examItem.getName());
        holder.examDate.setText(examItem.getDate());
        holder.examMessage.setText(examItem.getMessage());
        holder.examPic.setImageResource(examItem.getImage1());
        holder.examPic2.setImageResource(examItem.getImage2());
    }

    @Override
    public int getItemCount() {
        return examList.size();
    }

    // ViewHolder class
    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView examName, examDate, examMessage;
        ImageView examPic, examPic2;

        public MyViewHolder(@NonNull View itemView) {

            super(itemView);

            examName = itemView.findViewById(R.id.examName);
            examDate = itemView.findViewById(R.id.examDate);
            examMessage = itemView.findViewById(R.id.examMessage);
            examPic = itemView.findViewById(R.id.examPic);
            examPic2 = itemView.findViewById(R.id.examPic2);
        }
    }
}

