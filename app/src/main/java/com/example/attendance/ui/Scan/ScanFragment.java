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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ScanFragment extends Fragment {

    private FragmentScanBinding binding;
    private FirebaseFirestore db;
    private ScanViewModel scanViewModel;

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
        scanViewModel.setScanStatus("Scanner started...");

        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan QR Code for Attendance");
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

            // Parse QR content (assuming format: studentId|laptopId)
            String[] parts = qrContent.split("\\|");

            if (parts.length >= 1) {
                String studentId = parts[0];
                String laptopId = parts.length > 1 ? parts[1] : "Not specified";

                // Record attendance with timestamp
                recordAttendance(studentId, laptopId);
            } else {
                scanViewModel.setScanStatus("Invalid QR format");
            }
        } catch (Exception e) {
            scanViewModel.setScanStatus("Error: " + e.getMessage());
        }
    }

    private void recordAttendance(String studentId, String laptopId) {
        // Create attendance record
        Map<String, Object> attendanceRecord = new HashMap<>();
        attendanceRecord.put("studentId", studentId);
        attendanceRecord.put("laptopId", laptopId);
        attendanceRecord.put("timestamp", new Timestamp(new Date()));
        attendanceRecord.put("type", "attendance");

        // Add data to Firestore
        db.collection("attendance")
                .add(attendanceRecord)
                .addOnSuccessListener(documentReference -> {
                    scanViewModel.setScanStatus("Attendance marked successfully for Student ID: " + studentId);

                    // If a laptop was borrowed, record that too
                    if (!laptopId.equals("Not specified")) {
                        recordLaptopBorrowing(studentId, laptopId, documentReference.getId());
                    }
                })
                .addOnFailureListener(e -> {
                    scanViewModel.setScanStatus("Failed to mark attendance: " + e.getMessage());
                });
    }

    private void recordLaptopBorrowing(String studentId, String laptopId, String attendanceId) {
        Map<String, Object> laptopRecord = new HashMap<>();
        laptopRecord.put("studentId", studentId);
        laptopRecord.put("laptopId", laptopId);
        laptopRecord.put("borrowedAt", new Timestamp(new Date()));
        laptopRecord.put("attendanceId", attendanceId);
        laptopRecord.put("status", "borrowed");

        db.collection("laptopBorrowings")
                .add(laptopRecord)
                .addOnSuccessListener(documentReference -> {
                    scanViewModel.setScanStatus("Attendance marked and Laptop " + laptopId + " recorded as borrowed by Student ID: " + studentId);
                })
                .addOnFailureListener(e -> {
                    scanViewModel.setScanStatus("Attendance marked but failed to record laptop: " + e.getMessage());
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}