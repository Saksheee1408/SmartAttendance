package com.example.attendance;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import androidx.annotation.NonNull;
import android.util.Log;

public class Admin extends AppCompatActivity {

    FirebaseAuth auth;
    EditText usernameEditText, passwordEditText;
    Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user); // connecting to your XML layout

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Link Java variables with XML elements
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.pass);
        loginButton = findViewById(R.id.button);

        // Set click listener on Login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(Admin.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                } else {
                    loginUser(email, password);
                }
            }
        });

        // Handle email link login if any (Optional Part)
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            String emailLink = intent.getData().toString();
            if (auth.isSignInWithEmailLink(emailLink)) {
                String email = "someemail@domain.com"; // Should be retrieved from local storage ideally
                auth.signInWithEmailLink(email, emailLink)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(Admin.this, "Logged in via email link!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(Admin.this, "Failed to login with email link", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        }
    }

    private void loginUser(String email, String password) {
        // Define your static credentials
        String staticEmail = "admin@example.com";
        String staticPassword = "admin123";


        if (email.equals(staticEmail) && password.equals(staticPassword)) {
            Toast.makeText(Admin.this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
            // Redirect to another activity after successful login
            startActivity(new Intent(Admin.this, next.class)); // Example Dashboard
            finish();
        } else {
            Toast.makeText(Admin.this, "Invalid Admin Credentials", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.usermenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.Admin) {
            startActivity(new Intent(Admin.this, Admin.class));
            return true;
        } else if (id == R.id.User) {
            startActivity(new Intent(Admin.this, User.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
