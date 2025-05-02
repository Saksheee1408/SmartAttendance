package com.example.attendance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Add_Student extends AppCompatActivity {

    EditText username, college, department, email, phone;
    Button registerButton;
    FirebaseAuth auth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student); // your XML

        // Initializing Views
        username = findViewById(R.id.name);
        college = findViewById(R.id.college);
        department = findViewById(R.id.department);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.contact);
        registerButton = findViewById(R.id.submit);

        // Initializing Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Button Click Listener
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String userNameStr = username.getText().toString().trim();
                String collegeStr = college.getText().toString().trim();
                String departmentStr = department.getText().toString().trim();
                String emailStr = email.getText().toString().trim();
                String phoneStr = phone.getText().toString().trim();

                if (userNameStr.isEmpty() || collegeStr.isEmpty() || departmentStr.isEmpty() || emailStr.isEmpty() || phoneStr.isEmpty()) {
                    Toast.makeText(Add_Student.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                } else {
                    // 🔥 Generate random password
                    String generatedPassword = generateRandomPassword(8); // password length 8

                    // Register user with email and generated password
                    auth.createUserWithEmailAndPassword(emailStr, generatedPassword)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // Save user data to Firestore
                                        Map<String, Object> user = new HashMap<>();
                                        user.put("Username", userNameStr);
                                        user.put("College", collegeStr);
                                        user.put("Department", departmentStr);
                                        user.put("Email", emailStr);
                                        user.put("Phone", phoneStr);
                                        user.put("Password", generatedPassword); // Save password too

                                        firestore.collection("Users")
                                                .document(auth.getCurrentUser().getUid())
                                                .set(user)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> firestoreTask) {
                                                        if (firestoreTask.isSuccessful()) {
                                                            Toast.makeText(Add_Student.this,
                                                                    "Registration Successful!\n" +
                                                                            "Username (Email): " + emailStr + "\n" +
                                                                            "Password: " + generatedPassword,
                                                                    Toast.LENGTH_LONG).show();

                                                            finish(); // close the activity
                                                        } else {
                                                            Toast.makeText(Add_Student.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });

                                    } else {
                                        Toast.makeText(Add_Student.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });
    }

    // Method to generate random password
    private String generateRandomPassword(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$";
        StringBuilder password = new StringBuilder();
        Random rnd = new Random();
        while (password.length() < length) {
            int index = (int) (rnd.nextFloat() * characters.length());
            password.append(characters.charAt(index));
        }
        return password.toString();
    }
}
