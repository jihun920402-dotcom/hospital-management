package com.example.hospital.entity;

public enum PatientStatus {
    WAITING("대기 중"),
    IN_PROGRESS("진료 중"),
    COMPLETED("진료 완료"),
    HOSPITALIZED("입원 중"), // ⭐ 추가
    DISCHARGED("퇴원");      // ⭐ 추가

    private final String displayName;

    PatientStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}