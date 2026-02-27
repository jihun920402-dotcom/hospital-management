package com.example.hospital.repository;

import com.example.hospital.entity.Chart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChartRepository extends JpaRepository<Chart, Long> {
    // 특정 환자의 진료 기록만 조회
    List<Chart> findByPatientId(Long patientId);
}