package com.example.hospital.controller;

import com.example.hospital.entity.Chart;
import com.example.hospital.entity.Patient;
import com.example.hospital.entity.PatientStatus;
import com.example.hospital.repository.ChartRepository;
import com.example.hospital.repository.PatientRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/patients")
public class PatientController {

    @Autowired
    private PatientRepository patientRepository;

    // ⭐ ChartRepository 추가
    @Autowired
    private ChartRepository chartRepository;

    @GetMapping
    public String getAllPatients(
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 5) Pageable pageable,
            Model model) {

        Page<Patient> patients = (keyword != null && !keyword.isEmpty()) ?
                patientRepository.findByNameContaining(keyword, pageable) :
                patientRepository.findAll(pageable);

        model.addAttribute("patients", patients);
        model.addAttribute("keyword", keyword);
        model.addAttribute("patient", new Patient());
        return "patientList";
    }

    // ⭐ 환자 상세 조회 및 진료 기록 조회 추가
    @GetMapping("/{id}")
    public String getPatientDetail(@PathVariable Long id, Model model) {
        Patient patient = patientRepository.findById(id).orElse(null);
        model.addAttribute("patient", patient);
        model.addAttribute("charts", chartRepository.findByPatientId(id));
        return "patientDetail"; // 새로운 뷰
    }

    @PostMapping
    public String savePatient(
            @Valid @ModelAttribute("patient") Patient patient,
            BindingResult bindingResult,
            Model model,
            @PageableDefault(size = 5) Pageable pageable) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("patients", patientRepository.findAll(pageable));
            return "patientList";
        }
        patientRepository.save(patient);
        return "redirect:/patients";
    }

    @GetMapping("/edit/{id}")
    public String editPatientForm(@PathVariable Long id, @PageableDefault(size = 5) Pageable pageable, Model model) {
        model.addAttribute("patient", patientRepository.findById(id).orElse(null));
        model.addAttribute("patients", patientRepository.findAll(pageable));
        return "patientList";
    }

    @GetMapping("/delete/{id}")
    public String deletePatient(@PathVariable Long id) {
        patientRepository.deleteById(id);
        return "redirect:/patients";
    }

    // ⭐ 진료 기록 저장 메서드 추가
    @PostMapping("/{patientId}/charts")
    public String addChart(@PathVariable Long patientId, @ModelAttribute Chart chart) {
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient != null) {
            chart.setPatient(patient); // 진료 기록에 환자 연결
            chartRepository.save(chart);
        }
        return "redirect:/patients/" + patientId; // 상세 페이지로 리다이렉트
    }

    // ⭐ 환자 상태 변경 메서드 추가
    @GetMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam PatientStatus status) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient != null) {
            patient.setStatus(status);
            patientRepository.save(patient);
        }
        return "redirect:/patients"; // 목록 화면으로 이동
    }

    // ⭐ 수납 완료 처리 메서드 추가
    @GetMapping("/pay/{id}")
    public String payPatient(@PathVariable Long id) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient != null) {
            patient.setPaid(true); // 결제 완료로 변경
            patient.setStatus(PatientStatus.COMPLETED); // 상태도 완료로 변경
            patientRepository.save(patient);
        }
        return "redirect:/patients";
    }

    // ⭐ 입원 처리 메서드
    @GetMapping("/hospitalize/{id}")
    public String hospitalizePatient(@PathVariable Long id) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient != null) {
            patient.setStatus(PatientStatus.HOSPITALIZED);
            patient.setAdmissionDate(LocalDate.now()); // 오늘 날짜로 입원일 설정
            patientRepository.save(patient);
        }
        return "redirect:/patients";
    }

    // ⭐ 퇴원 처리 메서드
    @GetMapping("/discharge/{id}")
    public String dischargePatient(@PathVariable Long id) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient != null) {
            patient.setStatus(PatientStatus.DISCHARGED);
            patient.setDischargeDate(LocalDate.now()); // 오늘 날짜로 퇴원일 설정
            patientRepository.save(patient);
        }
        return "redirect:/patients";
    }

    @GetMapping("/dashboard")
    public String getDashboard(Model model) {
        model.addAttribute("totalPatients", patientRepository.count());
        model.addAttribute("hospitalizedCount", patientRepository.countByStatus(PatientStatus.HOSPITALIZED));
        model.addAttribute("unpaidCount", patientRepository.countByIsPaidFalse());
        return "dashboard"; // templates/dashboard.html
    }
}