package com.example.attendance.ui.Logout;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.widget.Toast;

import com.example.attendance.Login;
import com.example.attendance.R;
import com.example.attendance.User;
import com.example.attendance.databinding.FragmentLogoutBinding;

public class LogoutFragment extends Fragment {

    private FragmentLogoutBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LogoutViewModel logoutViewModel =
                new ViewModelProvider(this).get(LogoutViewModel.class);

        binding = FragmentLogoutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textSlideshow;
        logoutViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // Add a logout button
        Button logoutButton = binding.logoutButton; // You'll need to add this to your layout
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        return root;
    }

    private void performLogout() {
        // Clear any user session data
        // This depends on how you're storing user session
        // For example, if using SharedPreferences:
        if (getContext() != null) {
            getContext().getSharedPreferences("user_prefs", 0).edit().clear().apply();

            // Show a message
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Navigate to login screen
            Intent loginIntent = new Intent(getActivity(), User.class);
            // Clear the back stack so user can't go back after logout
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}