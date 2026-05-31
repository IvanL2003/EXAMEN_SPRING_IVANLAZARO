package com.salesianos.consultame.controller.web;

import com.salesianos.consultame.entity.Cita;
import com.salesianos.consultame.service.CitaService;
import com.salesianos.consultame.service.DoctorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/citas")
public class CitaController {

    private final CitaService citaService;
    private final DoctorService doctorService;

    public CitaController(CitaService citaService, DoctorService doctorService) {
        this.citaService = citaService;
        this.doctorService = doctorService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("citas", citaService.findAll());
        return "citas/list";
    }

    @GetMapping("/new")
    public String formulario(Model model) {
        model.addAttribute("cita", new Cita());
        model.addAttribute("doctores", doctorService.findAll());
        return "citas/form";
    }

    @PostMapping
    public String guardar(@ModelAttribute Cita cita,
                          @RequestParam(value = "doctorId", required = false) Long doctorId,
                          RedirectAttributes redirectAttributes) {
        if (doctorId != null) {
            doctorService.findById(doctorId).ifPresent(cita::setDoctor);
        }
        citaService.save(cita);
        redirectAttributes.addFlashAttribute("success", "Cita guardada correctamente");
        return "redirect:/citas";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        citaService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Cita eliminada correctamente");
        return "redirect:/citas";
    }
}
