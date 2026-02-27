package com.example.hospital.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Chart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symptom; // 증상
    private String diagnosis; // 진단명
    private String prescription; // 처방
    private LocalDate chartDate; // 진료일

    private String doctorName;

    private Integer fee = 0;

    // ⭐ 환자 한 명은 여러 진료 기록을 가질 수 있음 (1:N)
    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;
}