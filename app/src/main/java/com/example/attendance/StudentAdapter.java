package com.example.attendance;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    List<Student> studentList;

    public StudentAdapter(List<Student> studentList) {
        this.studentList = studentList;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        return new StudentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.name.setText(student.getUsername());
        holder.college.setText(student.getCollege());
        holder.department.setText(student.getDepartment());
        holder.email.setText(student.getEmail());
        holder.phone.setText(student.getPhone());
        holder.password.setText(student.getPassword());
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView name, college, department, email, phone, password;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            college = itemView.findViewById(R.id.college);
            department = itemView.findViewById(R.id.department);
            email = itemView.findViewById(R.id.email);
            phone = itemView.findViewById(R.id.phone);
            password = itemView.findViewById(R.id.password);
        }
    }
}
