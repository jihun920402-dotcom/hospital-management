package com.example.hospital.repository;

import com.example.hospital.entity.Patient;
import com.example.hospital.entity.PatientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    // 이름으로 검색 기능 추가
    Page<Patient> findByNameContaining(String name, Pageable pageable);
    // 상태별 환자 수 카운트
    long countByStatus(PatientStatus status);
    // 수납 미완료 환자 수
    long countByIsPaidFalse();
}
