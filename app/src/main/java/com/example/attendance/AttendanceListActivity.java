package com.example.attendance;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<AttendanceRecord> attendanceList;
    private AttendanceAdapter adapter;
    private FirebaseFirestore firestore;

    private Button btnStartDate, btnEndDate, btnApplyFilter, btnClearFilter;
    private EditText etNameFilter;

    private Date startDate = null;
    private Date endDate = null;
    private String nameFilter = "";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_list);

        initializeViews();
        setupRecyclerView();
        setupFirebase();
        setupListeners();

        loadAttendanceRecords();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerAttendance);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnClearFilter = findViewById(R.id.btnClearFilter);
        etNameFilter = findViewById(R.id.etNameFilter); // <-- Add EditText in your layout
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
        btnStartDate.setOnClickListener(v -> showDatePickerDialog(true));
        btnEndDate.setOnClickListener(v -> showDatePickerDialog(false));

        btnApplyFilter.setOnClickListener(v -> loadAttendanceRecords());

        btnClearFilter.setOnClickListener(v -> {
            startDate = null;
            endDate = null;
            nameFilter = "";
            etNameFilter.setText("");
            btnStartDate.setText("Select Start Date");
            btnEndDate.setText("Select End Date");
            loadAttendanceRecords();
        });

        etNameFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                nameFilter = s.toString().trim().toLowerCase();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showDatePickerDialog(final boolean isStartDate) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    if (isStartDate) {
                        startDate = calendar.getTime();
                        btnStartDate.setText(dateFormat.format(startDate));
                    } else {
                        endDate = calendar.getTime();
                        btnEndDate.setText(dateFormat.format(endDate));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadAttendanceRecords() {
        Query query = firestore.collection("attendance").orderBy("timestamp", Query.Direction.DESCENDING);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                attendanceList.clear();
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
                            attendanceList.add(record);
                        }
                    }
                }

                if (attendanceList.isEmpty()) {
                    Toast.makeText(this, "No attendance records match the filter", Toast.LENGTH_SHORT).show();
                }

                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed to fetch attendance", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean shouldIncludeRecord(Date recordDate, String username) {
        boolean matchesDate = true;
        boolean matchesName = true;

        if (startDate != null && recordDate.before(startDate)) matchesDate = false;
        if (endDate != null && recordDate.after(endDate)) matchesDate = false;
        if (nameFilter != null && !nameFilter.isEmpty() && username != null) {
            matchesName = username.toLowerCase().contains(nameFilter);
        }

        return matchesDate && matchesName;
    }
}
