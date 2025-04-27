package com.example.attendance.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.attendance.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth auth;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Set up UI elements
        final TextView textName = binding.textStudentName;
        final TextView textEmail = binding.textStudentEmail;
        final TextView textId = binding.textStudentId;
        final TextView textDepartment = binding.textStudentDepartment;
        final TextView textStatus = binding.textStatus;

        // Get current user
        FirebaseUser currentUser = auth.getCurrentUser();

        // Check if user is logged in
        if (currentUser != null) {
            // Display basic user information
            String email = currentUser.getEmail();
            String displayName = currentUser.getDisplayName();
            String uid = currentUser.getUid();

            textEmail.setText(email != null ? email : "No email available");
            textName.setText(displayName != null ? displayName : "Student");
            textId.setText("UID: " + uid);
            textDepartment.setText("Department information not available");
            textStatus.setText("Logged in");

            // You can also get phone number and photo URL if available
            if (currentUser.getPhoneNumber() != null) {
                // If you add a field for phone number
                // textPhone.setText(currentUser.getPhoneNumber());
            }
        } else {
            // User not logged in
            textName.setText("Not logged in");
            textEmail.setText("Please log in to view details");
            textId.setText("");
            textDepartment.setText("");
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