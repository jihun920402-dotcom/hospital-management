package com.example.hospital.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Data
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "병명은 필수입니다.")
    private String disease;

    @NotNull(message = "나이는 필수입니다.")
    @Min(value = 0, message = "나이는 0세 이상이어야 합니다.")
    private Integer age;

    @Enumerated(EnumType.STRING)
    private PatientStatus status = PatientStatus.WAITING; // 기본값은 대기 중

    // ⭐ 결제 여부 필드 추가 (기본값 false)
    private boolean isPaid = false;

    // ⭐ 입원/퇴원일 필드 추가
    private LocalDate admissionDate;
    private LocalDate dischargeDate;

    // ⭐ 환자 삭제 시 진료 기록도 함께 삭제 (Cascade)
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    private List<Chart> charts = new ArrayList<>();
}