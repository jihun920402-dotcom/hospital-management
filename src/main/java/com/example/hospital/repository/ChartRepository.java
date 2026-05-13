package com.example.hospital.repository;

import com.example.hospital.entity.Chart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ChartRepository extends JpaRepository<Chart, Long> {
    // 특정 환자의 진료 기록만 조회
    List<Chart> findByPatientId(Long patientId);

    // 오늘 방문자 수 (chartDate 기준 중복 환자 제거)
    @Query("SELECT COUNT(DISTINCT c.patient.id) FROM Chart c WHERE c.chartDate = :today")
    long countDistinctPatientByChartDate(@Param("today") LocalDate today);
}