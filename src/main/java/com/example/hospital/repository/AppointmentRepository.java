package com.example.hospital.repository;

import com.example.hospital.entity.Appointment;
import com.example.hospital.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Page<Appointment> findByAppointmentDate(LocalDate date, Pageable pageable);
    long countByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status);
}
