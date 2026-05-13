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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/patients")
public class PatientController {

    @Autowired
    private PatientRepository patientRepository;

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

    @GetMapping("/{id}")
    public String getPatientDetail(@PathVariable Long id, Model model) {
        Patient patient = patientRepository.findById(id).orElse(null);
        model.addAttribute("patient", patient);
        model.addAttribute("charts", chartRepository.findByPatientId(id));
        return "patientDetail";
    }

    @PostMapping
    public String savePatient(
            @Valid @ModelAttribute("patient") Patient patient,
            BindingResult bindingResult,
            Model model,
            @PageableDefault(size = 5) Pageable pageable,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("patients", patientRepository.findAll(pageable));
            return "patientList";
        }
        boolean isNew = (patient.getId() == null);
        patientRepository.save(patient);
        ra.addFlashAttribute("toast", isNew ? "✔ 환자가 등록되었습니다." : "✔ 환자 정보가 수정되었습니다.");
        ra.addFlashAttribute("toastType", "success");
        return "redirect:/patients";
    }

    @GetMapping("/edit/{id}")
    public String editPatientForm(@PathVariable Long id, @PageableDefault(size = 5) Pageable pageable, Model model) {
        model.addAttribute("patient", patientRepository.findById(id).orElse(null));
        model.addAttribute("patients", patientRepository.findAll(pageable));
        return "patientList";
    }

    @GetMapping("/delete/{id}")
    public String deletePatient(@PathVariable Long id, RedirectAttributes ra) {
        patientRepository.deleteById(id);
        ra.addFlashAttribute("toast", "🗑 환자가 삭제되었습니다.");
        ra.addFlashAttribute("toastType", "danger");
        return "redirect:/patients";
    }

    @PostMapping("/{patientId}/charts")
    public String addChart(@PathVariable Long patientId, @ModelAttribute Chart chart, RedirectAttributes ra) {
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient != null) {
            chart.setPatient(patient);
            chartRepository.save(chart);
        }
        ra.addFlashAttribute("toast", "✔ 진료 기록이 추가되었습니다.");
        ra.addFlashAttribute("toastType", "success");
        return "redirect:/patients/" + patientId;
    }

    @GetMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam PatientStatus status, RedirectAttributes ra) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient != null) {
            patient.setStatus(status);
            patientRepository.save(patient);
        }
        ra.addFlashAttribute("toast", "✔ 상태가 변경되었습니다.");
        ra.addFlashAttribute("toastType", "success");
        return "redirect:/patients";
    }

    @GetMapping("/pay/{id}")
    public String payPatient(@PathVariable Long id, RedirectAttributes ra) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient != null) {
            patient.setPaid(true);
            patient.setStatus(PatientStatus.COMPLETED);
            patientRepository.save(patient);
        }
        ra.addFlashAttribute("toast", "💳 수납이 완료되었습니다.");
        ra.addFlashAttribute("toastType", "success");
        return "redirect:/patients";
    }

    @GetMapping("/hospitalize/{id}")
    public String hospitalizePatient(@PathVariable Long id, RedirectAttributes ra) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient != null) {
            patient.setStatus(PatientStatus.HOSPITALIZED);
            patient.setAdmissionDate(LocalDate.now());
            patientRepository.save(patient);
        }
        ra.addFlashAttribute("toast", "🏨 입원 처리되었습니다.");
        ra.addFlashAttribute("toastType", "info");
        return "redirect:/patients";
    }

    @GetMapping("/discharge/{id}")
    public String dischargePatient(@PathVariable Long id, RedirectAttributes ra) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient != null) {
            patient.setStatus(PatientStatus.DISCHARGED);
            patient.setDischargeDate(LocalDate.now());
            patientRepository.save(patient);
        }
        ra.addFlashAttribute("toast", "🚪 퇴원 처리되었습니다.");
        ra.addFlashAttribute("toastType", "warning");
        return "redirect:/patients";
    }

    @GetMapping("/dashboard")
    public String getDashboard(Model model) {
        model.addAttribute("totalPatients", patientRepository.count());
        model.addAttribute("todayVisitors", chartRepository.countDistinctPatientByChartDate(LocalDate.now()));
        model.addAttribute("hospitalizedCount", patientRepository.countByStatus(PatientStatus.HOSPITALIZED));
        model.addAttribute("dischargedCount", patientRepository.countByStatus(PatientStatus.DISCHARGED));
        model.addAttribute("waitingCount", patientRepository.countByStatus(PatientStatus.WAITING));
        model.addAttribute("inProgressCount", patientRepository.countByStatus(PatientStatus.IN_PROGRESS));
        model.addAttribute("completedCount", patientRepository.countByStatus(PatientStatus.COMPLETED));
        return "dashboard";
    }
}
