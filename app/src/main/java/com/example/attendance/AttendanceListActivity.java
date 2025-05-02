package com.example.attendance;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceListActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceListActivity";
    private RecyclerView recyclerView;
    private List<AttendanceRecord> attendanceList;
    private AttendanceAdapter adapter;
    private FirebaseFirestore firestore;

    private Button btnDateFilter, btnClearFilter, btnExportCsv;
    private EditText etNameFilter;

    private Date selectedDate = null;
    private String nameFilter = "";
    private List<Student> registeredStudents = new ArrayList<>();
    private Map<String, AttendanceRecord> attendanceMap = new HashMap<>();
    private Map<String, Boolean> presentStudents = new HashMap<>();

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_list);

        initializeViews();
        setupRecyclerView();
        setupFirebase();
        setupListeners();

        // First load registered students
        loadRegisteredStudents();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerAttendance);
        btnDateFilter = findViewById(R.id.btnDateFilter);
        btnClearFilter = findViewById(R.id.btnClearFilter);
        btnExportCsv = findViewById(R.id.btnExportCsv);
        etNameFilter = findViewById(R.id.etNameFilter);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        attendanceList = new ArrayList<>();
        adapter = new AttendanceAdapter(attendanceList);
        recyclerView.setAdapter(adapter);
    }

    private void setupFirebase() {
        firestore = FirebaseFirestore.getInstance();
    }

    private void setupListeners() {
        btnDateFilter.setOnClickListener(v -> showDatePickerDialog());

        btnClearFilter.setOnClickListener(v -> {
            selectedDate = null;
            nameFilter = "";
            etNameFilter.setText("");
            btnDateFilter.setText("Select Date");
            loadAttendanceRecords();
        });

        btnExportCsv.setOnClickListener(v -> exportToCsv());

        etNameFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                nameFilter = s.toString().trim().toLowerCase();
                loadAttendanceRecords();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    selectedDate = calendar.getTime();
                    btnDateFilter.setText(dateFormat.format(selectedDate));
                    loadAttendanceRecords();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadRegisteredStudents() {
        firestore.collection("Users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        registeredStudents.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Student student = document.toObject(Student.class);
                            // Store the document ID for reference
                            student.setUsername(student.getUsername() != null ?
                                    student.getUsername() : document.getId());
                            registeredStudents.add(student);
                        }
                        Log.d(TAG, "Loaded " + registeredStudents.size() + " registered students");
                        // Now load attendance records
                        loadAttendanceRecords();
                    } else {
                        Log.e(TAG, "Error getting students", task.getException());
                        Toast.makeText(this, "Failed to load student list", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAttendanceRecords() {
        Query query = firestore.collection("attendance").orderBy("timestamp", Query.Direction.DESCENDING);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                attendanceList.clear();
                attendanceMap.clear();
                presentStudents.clear();

                // First, collect all attendance records that match the filter
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    if (doc.contains("timestamp") && doc.getTimestamp("timestamp") != null) {
                        Date recordDate = doc.getTimestamp("timestamp").toDate();
                        String username = doc.getString("username");

                        if (shouldIncludeRecord(recordDate, username)) {
                            String courseName = doc.getString("courseName");

                            AttendanceRecord record = new AttendanceRecord(
                                    doc.getId(),
                                    username != null ? username : "Unknown",
                                    recordDate,
                                    courseName != null ? courseName : "General"
                            );

                            // Use username as the key
                            if (username != null) {
                                attendanceMap.put(username, record);
                                presentStudents.put(username, true);
                            }

                            attendanceList.add(record);
                        }
                    }
                }

                // Now add absent students (those who are registered but not in attendance)
                // Always add absent students, even if no date is selected
                addAbsentStudents();

                if (attendanceList.isEmpty()) {
                    Toast.makeText(this, "No attendance records match the filter", Toast.LENGTH_SHORT).show();
                }

                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed to fetch attendance", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addAbsentStudents() {
        for (Student student : registeredStudents) {
            String username = student.getUsername();

            // Skip if we're filtering by name and this student doesn't match
            if (!nameFilter.isEmpty() &&
                    (username == null || !username.toLowerCase().contains(nameFilter))) {
                continue;
            }

            // If student is not in our presentStudents map, they're absent
            if (username != null && !presentStudents.containsKey(username)) {
                // Use current date if no date is selected
                Date recordDate = selectedDate != null ? selectedDate : new Date();

                AttendanceRecord absentRecord = new AttendanceRecord(
                        "absent_" + username,
                        username,
                        recordDate,
                        "ABSENT"
                );
                attendanceList.add(absentRecord);
            }
        }
    }

    private boolean shouldIncludeRecord(Date recordDate, String username) {
        boolean matchesDate = true;
        boolean matchesName = true;

        if (selectedDate != null) {
            // Check if the record's date falls on the same day as selectedDate
            Calendar recordCal = Calendar.getInstance();
            recordCal.setTime(recordDate);

            Calendar selectedCal = Calendar.getInstance();
            selectedCal.setTime(selectedDate);

            matchesDate = recordCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                    recordCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR);
        }

        if (!nameFilter.isEmpty() && username != null) {
            matchesName = username.toLowerCase().contains(nameFilter);
        }

        return matchesDate && matchesName;
    }

    private void exportToCsv() {
        if (attendanceList.isEmpty()) {
            Toast.makeText(this, "No records to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create a file in the external files directory
            File exportDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AttendanceExports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            String fileName = "attendance_" + fileNameDateFormat.format(new Date()) + ".csv";
            File file = new File(exportDir, fileName);

            FileWriter fw = new FileWriter(file);

            // Write header
            fw.append("Name,Date,Course\n");

            // Write data
            SimpleDateFormat csvDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            for (AttendanceRecord record : attendanceList) {
                fw.append(record.getUserName())
                        .append(",")
                        .append(csvDateFormat.format(record.getTimestamp()))
                        .append(",")
                        .append(record.getCourseName())
                        .append("\n");
            }

            fw.flush();
            fw.close();

            // Share the file
            shareFile(file);

            Toast.makeText(this, "Exported to " + fileName, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e(TAG, "Error exporting to CSV", e);
            Toast.makeText(this, "Error exporting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".provider",
                file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Data");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share Attendance Data"));
    }
}