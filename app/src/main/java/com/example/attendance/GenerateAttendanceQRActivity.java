package com.example.attendance;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class GenerateAttendanceQRActivity extends AppCompatActivity {

    private ImageView qrCodeImageView;
    private Button generateQRButton;
    private TextView infoTextView;

    // This is the fixed QR code content that all students will scan
    private static final String ATTENDANCE_QR_CODE = "ATTENDANCE_VERIFICATION_CODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_attendance_qr);

        // Initialize UI elements
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        generateQRButton = findViewById(R.id.generateQRButton);
        infoTextView = findViewById(R.id.infoTextView);

        // Set up button click listener
        generateQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateQRCode();
            }
        });
    }

    private void generateQRCode() {
        try {
            // Generate QR code with the fixed content
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(ATTENDANCE_QR_CODE, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

            // Display the QR code
            qrCodeImageView.setImageBitmap(bitmap);
            qrCodeImageView.setVisibility(View.VISIBLE);

            // Update info text
            infoTextView.setText("QR Code generated. Have students scan this code to mark attendance.\n" +
                    "Note: This is the official attendance QR code. Display it in class for students to scan.");

            // Disable button after generation
            generateQRButton.setText("QR Code Generated");
            generateQRButton.setEnabled(false);

            Toast.makeText(this, "QR Code generated successfully!", Toast.LENGTH_SHORT).show();

        } catch (WriterException e) {
            Toast.makeText(this, "Error generating QR code: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}