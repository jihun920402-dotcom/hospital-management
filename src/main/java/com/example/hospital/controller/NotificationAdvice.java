package com.example.hospital.controller;

import com.example.hospital.entity.AppointmentStatus;
import com.example.hospital.entity.PatientStatus;
import com.example.hospital.repository.AppointmentRepository;
import com.example.hospital.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;

@ControllerAdvice
public class NotificationAdvice {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @ModelAttribute
    public void addNotifications(Model model) {
        long unpaid     = patientRepository.countByIsPaidFalseAndStatus(PatientStatus.COMPLETED);
        long todayAppts = appointmentRepository.countByAppointmentDateAndStatus(LocalDate.now(), AppointmentStatus.SCHEDULED);

        model.addAttribute("notifCount",      unpaid + todayAppts);
        model.addAttribute("notifUnpaid",     unpaid);
        model.addAttribute("notifTodayAppts", todayAppts);
    }
}
