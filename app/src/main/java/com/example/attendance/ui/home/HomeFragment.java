package com.example.attendance.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.attendance.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Set up UI elements
        final TextView textName = binding.textStudentName;
        final TextView textEmail = binding.textStudentEmail;
        final TextView textId = binding.textStudentId;
        final TextView textDepartment = binding.textStudentDepartment;
        final TextView textCollege = binding.textStudentCollege; // Assuming you have this in your layout
        final TextView textPhone = binding.textStudentPhone;     // Assuming you have this in your layout
        final TextView textStatus = binding.textStatus;

        // Get current user
        FirebaseUser currentUser = auth.getCurrentUser();

        // Check if user is logged in
        if (currentUser != null) {
            String uid = currentUser.getUid();
            textId.setText("UID: " + uid);
            textStatus.setText("Loading student information...");

            // Fetch additional student data from Firestore
            firestore.collection("Users")
                    .document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // Extract student details from Firestore document
                                String username = document.getString("Username");
                                String email = document.getString("Email");
                                String department = document.getString("Department");
                                String college = document.getString("College");
                                String phone = document.getString("Phone");

                                // Update UI with the fetched data
                                textName.setText(username != null ? username : "Student");
                                textEmail.setText(email != null ? email : "No email available");
                                textDepartment.setText(department != null ? department : "Department not available");

                                // Update college and phone if the TextViews exist
                                if (textCollege != null) {
                                    textCollege.setText(college != null ? college : "College not available");
                                }

                                if (textPhone != null) {
                                    textPhone.setText(phone != null ? phone : "Phone not available");
                                }

                                textStatus.setText("Logged in");
                            } else {
                                // Document doesn't exist for this user
                                textStatus.setText("Student details not found");
                                textName.setText("Unknown Student");
                                textEmail.setText(currentUser.getEmail());
                                textDepartment.setText("Department not available");

                                if (textCollege != null) {
                                    textCollege.setText("College not available");
                                }

                                if (textPhone != null) {
                                    textPhone.setText("Phone not available");
                                }

                                Toast.makeText(getContext(), "Student details not found in database", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Error getting document
                            textStatus.setText("Error loading student information");
                            Toast.makeText(getContext(), "Error loading student details", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // User not logged in
            textName.setText("Not logged in");
            textEmail.setText("Please log in to view details");
            textId.setText("");
            textDepartment.setText("");

            if (textCollege != null) {
                textCollege.setText("");
            }

            if (textPhone != null) {
                textPhone.setText("");
            }

            textStatus.setText("Not authenticated");
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}