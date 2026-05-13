package com.example.hospital.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.Period;
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

    @NotNull(message = "생년월일은 필수입니다.")
    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private PatientStatus status = PatientStatus.WAITING;

    private boolean isPaid = false;

    private LocalDate admissionDate;
    private LocalDate dischargeDate;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    private List<Chart> charts = new ArrayList<>();

    public int getAge() {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}