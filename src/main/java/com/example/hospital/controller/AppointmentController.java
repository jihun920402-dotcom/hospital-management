package com.example.hospital.controller;

import com.example.hospital.entity.Appointment;
import com.example.hospital.entity.AppointmentStatus;
import com.example.hospital.repository.AppointmentRepository;
import com.example.hospital.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @GetMapping
    public String getAppointments(
            @RequestParam(required = false) String date,
            @PageableDefault(size = 10, sort = {"appointmentDate", "appointmentTime"}) Pageable pageable,
            Model model) {

        Page<Appointment> appointments;
        if (date != null && !date.isEmpty()) {
            appointments = appointmentRepository.findByAppointmentDate(LocalDate.parse(date), pageable);
        } else {
            appointments = appointmentRepository.findAll(pageable);
        }

        model.addAttribute("appointments", appointments);
        model.addAttribute("patients", patientRepository.findAll());
        model.addAttribute("filterDate", date != null ? date : "");
        model.addAttribute("today", LocalDate.now());
        return "appointments";
    }

    @PostMapping
    public String saveAppointment(
            @RequestParam Long patientId,
            @ModelAttribute Appointment appointment,
            RedirectAttributes ra) {
        patientRepository.findById(patientId).ifPresent(patient -> {
            appointment.setPatient(patient);
            appointmentRepository.save(appointment);
        });
        ra.addFlashAttribute("toast", "✔ 예약이 등록되었습니다.");
        ra.addFlashAttribute("toastType", "success");
        return "redirect:/appointments";
    }

    @GetMapping("/complete/{id}")
    public String completeAppointment(@PathVariable Long id, RedirectAttributes ra) {
        appointmentRepository.findById(id).ifPresent(a -> {
            a.setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(a);
        });
        ra.addFlashAttribute("toast", "✔ 예약이 완료 처리되었습니다.");
        ra.addFlashAttribute("toastType", "success");
        return "redirect:/appointments";
    }

    @GetMapping("/cancel/{id}")
    public String cancelAppointment(@PathVariable Long id, RedirectAttributes ra) {
        appointmentRepository.findById(id).ifPresent(a -> {
            a.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(a);
        });
        ra.addFlashAttribute("toast", "예약이 취소되었습니다.");
        ra.addFlashAttribute("toastType", "warning");
        return "redirect:/appointments";
    }

    @GetMapping("/delete/{id}")
    public String deleteAppointment(@PathVariable Long id, RedirectAttributes ra) {
        appointmentRepository.deleteById(id);
        ra.addFlashAttribute("toast", "🗑 예약이 삭제되었습니다.");
        ra.addFlashAttribute("toastType", "danger");
        return "redirect:/appointments";
    }
}
