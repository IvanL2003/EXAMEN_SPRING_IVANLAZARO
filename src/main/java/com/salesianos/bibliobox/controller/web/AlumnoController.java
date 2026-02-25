package com.salesianos.bibliobox.controller.web;

import com.salesianos.bibliobox.entity.Alumno;
import com.salesianos.bibliobox.service.AlumnoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/alumnos")
public class AlumnoController {

    @Autowired
    private AlumnoService alumnoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("alumnos", alumnoService.findAll());
        return "alumnos/list";
    }

    @GetMapping("/new")
    public String mostrarFormCrear(Model model) {
        model.addAttribute("alumno", new Alumno());
        return "alumnos/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("alumno") Alumno alumno,
                        BindingResult result,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "alumnos/form";
        }
        alumnoService.save(alumno);
        redirectAttributes.addFlashAttribute("successMessage", "Alumno creado correctamente");
        return "redirect:/alumnos";
    }

    // GET /alumnos/{id} → detalle del alumno con sus préstamos
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return alumnoService.findById(id).map(alumno -> {
            model.addAttribute("alumno", alumno);
            return "alumnos/detail";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Alumno no encontrado");
            return "redirect:/alumnos";
        });
    }

    @GetMapping("/edit/{id}")
    public String mostrarFormEditar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return alumnoService.findById(id).map(alumno -> {
            model.addAttribute("alumno", alumno);
            return "alumnos/form";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Alumno no encontrado");
            return "redirect:/alumnos";
        });
    }

    @PostMapping("/edit/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("alumno") Alumno alumno,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "alumnos/form";
        }
        alumno.setId(id);
        alumnoService.save(alumno);
        redirectAttributes.addFlashAttribute("successMessage", "Alumno actualizado correctamente");
        return "redirect:/alumnos";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            alumnoService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Alumno eliminado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se puede eliminar el alumno");
        }
        return "redirect:/alumnos";
    }
}
