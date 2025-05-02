package com.example.attendance.ui.Scan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.attendance.databinding.FragmentScanBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ScanFragment extends Fragment {

    private FragmentScanBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ScanViewModel scanViewModel;

    // The fixed QR code value for attendance - match this with the GenerateAttendanceQRActivity
    private static final String ATTENDANCE_QR_CODE = "ATTENDANCE_VERIFICATION_CODE";

    // Permission request launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startQRScanner();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                    scanViewModel.setScanStatus("Camera permission denied. Cannot scan QR codes.");
                }
            });

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        scanViewModel = new ViewModelProvider(this).get(ScanViewModel.class);

        binding = FragmentScanBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Setup scan button
        binding.btnScan.setOnClickListener(v -> {
            checkCameraPermissionAndScan();
        });

        // Update UI based on scan status
        scanViewModel.getScanStatus().observe(getViewLifecycleOwner(), status -> {
            binding.scanStatusText.setText(status);
        });

        scanViewModel.getText().observe(getViewLifecycleOwner(), text -> {
            binding.textGallery.setText(text);
        });

        return root;
    }

    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            startQRScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startQRScanner() {
        scanViewModel.setScanStatus("Scanner started... Please scan a QR code");

        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan QR Code");
        integrator.setCameraId(0);  // Use default camera
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                scanViewModel.setScanStatus("Scan cancelled");
            } else {
                processQrCode(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processQrCode(String qrContent) {
        try {
            scanViewModel.setScanStatus("Processing scan: " + qrContent);

            // Check if this is the attendance QR code
            if (qrContent.equals(ATTENDANCE_QR_CODE)) {
                handleAttendance();
            } else {
                // If not the attendance code, process it as a laptop QR code
                // Assuming format: laptopId
                handleLaptopBorrowing(qrContent);
            }
        } catch (Exception e) {
            scanViewModel.setScanStatus("Error: " + e.getMessage());
        }
    }

    private void handleAttendance() {
        // Get the current user
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Debug information
            scanViewModel.setScanStatus("Fetching user details for ID: " + userId);

            // Get user details from Firestore and record attendance
            db.collection("Users").document(userId).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            // Extract user details
                            String username = document.getString("Username");
                            String department = document.getString("Department");
                            String college = document.getString("College");
                            String email = document.getString("Email");

                            scanViewModel.setScanStatus("User found: " + username + ". Processing attendance...");

                            // Check if any field is null
                            if (username == null) {
                                scanViewModel.setScanStatus("Warning: Username is missing from user profile");
                            }

                            // Record attendance with these details
                            recordAttendance(userId, username != null ? username : "Unknown User",
                                    department != null ? department : "",
                                    college != null ? college : "",
                                    email != null ? email : "");
                        } else {
                            scanViewModel.setScanStatus("Error: User details not found in database for ID: " + userId);
                            Toast.makeText(getContext(), "User details not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        scanViewModel.setScanStatus("Error fetching user details: " + e.getMessage());
                        Toast.makeText(getContext(), "Failed to fetch user details", Toast.LENGTH_SHORT).show();
                    });
        } else {
            scanViewModel.setScanStatus("Please log in to mark attendance");
            Toast.makeText(getContext(), "Please log in to mark attendance", Toast.LENGTH_SHORT).show();
        }
    }

    private void recordAttendance(String userId, String username, String department, String college, String email) {
        // Debug logging
        scanViewModel.setScanStatus("Processing attendance for: " + username);

        // Create attendance record
        Map<String, Object> attendanceRecord = new HashMap<>();
        attendanceRecord.put("userId", userId);
        attendanceRecord.put("username", username);
        attendanceRecord.put("department", department);
        attendanceRecord.put("college", college);
        attendanceRecord.put("email", email);
        attendanceRecord.put("timestamp", new Timestamp(new Date()));
        attendanceRecord.put("date", new Date()); // For easier date-based queries
        attendanceRecord.put("type", "attendance"); // Add type for distinguishing from laptop records

        // SOLUTION 1: Use a simpler query that doesn't require a composite index
        db.collection("attendance")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean alreadyMarkedToday = false;

                    // Filter results for today's attendance in code
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        if (timestamp != null) {
                            Date recordDate = timestamp.toDate();
                            // Check if this record is from today and has type "attendance"
                            if (isSameDay(recordDate, new Date()) &&
                                    "attendance".equals(doc.getString("type"))) {
                                alreadyMarkedToday = true;
                                break;
                            }
                        }
                    }

                    if (alreadyMarkedToday) {
                        scanViewModel.setScanStatus("Attendance already marked for today!");
                        Toast.makeText(getContext(), "You have already marked attendance for today", Toast.LENGTH_SHORT).show();
                    } else {
                        // No attendance record found for today, so add one
                        scanViewModel.setScanStatus("No existing record found. Adding new attendance record...");

                        db.collection("attendance")
                                .add(attendanceRecord)
                                .addOnSuccessListener(documentReference -> {
                                    scanViewModel.setScanStatus("Attendance marked successfully for: " + username);
                                    Toast.makeText(getContext(), "Attendance marked successfully!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    scanViewModel.setScanStatus("Failed to mark attendance: " + e.getMessage());
                                    Toast.makeText(getContext(), "Failed to mark attendance", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    scanViewModel.setScanStatus("Error checking attendance record: " + e.getMessage());
                    Toast.makeText(getContext(), "Error checking attendance record", Toast.LENGTH_SHORT).show();
                });
    }

    // Helper method to check if two dates are on the same day
    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }


    private void handleLaptopBorrowing(String laptopId) {
        // Get the current user
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // First check if this laptop is already borrowed
            db.collection("laptopBorrowings")
                    .whereEqualTo("laptopId", laptopId)
                    .whereEqualTo("status", "borrowed")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // If laptop is already borrowed
                            if (!task.getResult().isEmpty()) {
                                // Check if it's borrowed by the current user (for returning)
                                boolean borrowedByCurrentUser = false;
                                String documentId = "";

                                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                    if (doc.getString("studentId").equals(userId)) {
                                        borrowedByCurrentUser = true;
                                        documentId = doc.getId();
                                        break;
                                    }
                                }

                                if (borrowedByCurrentUser) {
                                    // This is a return operation
                                    returnLaptop(documentId, laptopId);
                                } else {
                                    // Laptop is borrowed by someone else
                                    scanViewModel.setScanStatus("This laptop (ID: " + laptopId + ") is currently borrowed by another student");
                                    Toast.makeText(getContext(), "Laptop already borrowed by another student", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // Laptop is available, borrow it
                                borrowLaptop(userId, laptopId);
                            }
                        } else {
                            // Fixed error handling
                            String errorMessage = "Error checking laptop status";
                            if (task.getException() != null) {
                                errorMessage += ": " + task.getException().getMessage();
                            }
                            scanViewModel.setScanStatus(errorMessage);
                        }
                    });
        } else {
            scanViewModel.setScanStatus("Please log in to borrow laptops");
            Toast.makeText(getContext(), "Please log in to borrow laptops", Toast.LENGTH_SHORT).show();
        }
    }

    private void borrowLaptop(String userId, String laptopId) {
        // Get user details first
        db.collection("Users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot userDoc = task.getResult();
                        String username = userDoc.getString("Username");

                        // Create a record in the attendance collection first
                        Map<String, Object> attendanceRecord = new HashMap<>();
                        attendanceRecord.put("studentId", userId);
                        attendanceRecord.put("studentName", username);
                        attendanceRecord.put("laptopId", laptopId);
                        attendanceRecord.put("timestamp", new Timestamp(new Date()));
                        attendanceRecord.put("type", "laptop_borrowing");

                        db.collection("attendance")
                                .add(attendanceRecord)
                                .addOnSuccessListener(attendanceRef -> {
                                    // Create the borrowing record
                                    Map<String, Object> laptopRecord = new HashMap<>();
                                    laptopRecord.put("studentId", userId);
                                    laptopRecord.put("studentName", username);
                                    laptopRecord.put("laptopId", laptopId);
                                    laptopRecord.put("borrowedAt", new Timestamp(new Date()));
                                    laptopRecord.put("attendanceId", attendanceRef.getId());
                                    laptopRecord.put("status", "borrowed");

                                    db.collection("laptopBorrowings")
                                            .add(laptopRecord)
                                            .addOnSuccessListener(documentReference -> {
                                                scanViewModel.setScanStatus("Successfully borrowed Laptop " + laptopId);
                                                Toast.makeText(getContext(), "Laptop borrowed successfully", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                scanViewModel.setScanStatus("Failed to borrow laptop: " + e.getMessage());
                                                Toast.makeText(getContext(), "Failed to record laptop borrowing", Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    scanViewModel.setScanStatus("Failed to record laptop borrowing: " + e.getMessage());
                                    Toast.makeText(getContext(), "Failed to record laptop borrowing", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        scanViewModel.setScanStatus("Error: Could not find user details");
                        Toast.makeText(getContext(), "User details not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void returnLaptop(String borrowingDocId, String laptopId) {
        // Update the borrowing record to mark laptop as returned
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("returnedAt", new Timestamp(new Date()));
        updateData.put("status", "returned");

        db.collection("laptopBorrowings").document(borrowingDocId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    scanViewModel.setScanStatus("Successfully returned Laptop " + laptopId);
                    Toast.makeText(getContext(), "Laptop returned successfully", Toast.LENGTH_SHORT).show();

                    // Create a return record in attendance
                    FirebaseUser currentUser = auth.getCurrentUser();
                    if (currentUser != null) {
                        db.collection("Users").document(currentUser.getUid()).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    String username = documentSnapshot.getString("Username");

                                    Map<String, Object> returnRecord = new HashMap<>();
                                    returnRecord.put("studentId", currentUser.getUid());
                                    returnRecord.put("studentName", username);
                                    returnRecord.put("laptopId", laptopId);
                                    returnRecord.put("timestamp", new Timestamp(new Date()));
                                    returnRecord.put("type", "laptop_return");

                                    db.collection("attendance").add(returnRecord);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    scanViewModel.setScanStatus("Failed to return laptop: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to record laptop return", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}