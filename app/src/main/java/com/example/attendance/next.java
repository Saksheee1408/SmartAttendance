package com.example.attendance;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class next extends AppCompatActivity {
   Button add,details,laptop,check;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        add=findViewById(R.id.add);
        details=findViewById(R.id.details);
        laptop=findViewById(R.id.laptop);
        check=findViewById(R.id.check);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(next.this,Add_Student.class));
            }
        });
        details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(next.this,Student_list.class));
            }
        });
    }
}