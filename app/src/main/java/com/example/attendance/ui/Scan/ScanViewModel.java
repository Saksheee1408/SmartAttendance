package com.example.attendance.ui.Scan;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ScanViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> scanStatus;

    public ScanViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Attendance & Laptop Borrowing");

        scanStatus = new MutableLiveData<>();
        scanStatus.setValue("Tap 'Scan QR Code' to begin");
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<String> getScanStatus() {
        return scanStatus;
    }

    public void setScanStatus(String status) {
        scanStatus.setValue(status);
    }
}