package com.example.attendance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Student_list extends AppCompatActivity {

    RecyclerView recyclerView;
    List<Student> studentList;
    StudentAdapter adapter;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        recyclerView = findViewById(R.id.studentRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentList = new ArrayList<>();
        adapter = new StudentAdapter(studentList);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        // Fetch all students
        firestore.collection("Users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Student student = document.toObject(Student.class);
                            studentList.add(student);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(Student_list.this, "Failed to fetch students", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
