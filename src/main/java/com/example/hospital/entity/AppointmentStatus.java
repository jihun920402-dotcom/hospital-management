package com.example.hospital.entity;

public enum AppointmentStatus {
    SCHEDULED("예약됨"),
    COMPLETED("완료"),
    CANCELLED("취소됨");

    private final String displayName;

    AppointmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
