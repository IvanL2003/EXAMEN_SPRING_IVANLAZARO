package com.salesianos.consultame.controller.web;

import com.salesianos.consultame.entity.Doctor;
import com.salesianos.consultame.service.DoctorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/doctores")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("doctores", doctorService.findAll());
        return "doctores/list";
    }

    @GetMapping("/new")
    public String formulario(Model model) {
        model.addAttribute("doctor", new Doctor());
        return "doctores/form";
    }

    @PostMapping
    public String guardar(@ModelAttribute Doctor doctor, RedirectAttributes redirectAttributes) {
        doctorService.save(doctor);
        redirectAttributes.addFlashAttribute("success", "Doctor guardado correctamente");
        return "redirect:/doctores";
    }
}
