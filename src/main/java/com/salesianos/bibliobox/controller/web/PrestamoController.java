package com.salesianos.bibliobox.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.salesianos.bibliobox.entity.Prestamo;
import com.salesianos.bibliobox.service.AlumnoService;
import com.salesianos.bibliobox.service.PrestamoService;

@Controller
@RequestMapping("/prestamos")
public class PrestamoController {

    @Autowired
    private PrestamoService prestamoService;

    @Autowired
    private AlumnoService alumnoService;

    // GET /prestamos → listado
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("prestamos", prestamoService.findAll());
        return "prestamos/list";
    }

    // GET /prestamos/{id} → detalle de un préstamo
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return prestamoService.findById(id).map(prestamo -> {
            model.addAttribute("prestamo", prestamo);
            return "prestamos/detail";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Préstamo no encontrado");
            return "redirect:/prestamos";
        });
    }

    // GET /prestamos/new → formulario nuevo
    @GetMapping("/new")
    public String mostrarFormCrear(Model model) {
        model.addAttribute("prestamo", new Prestamo());
        model.addAttribute("alumnos", alumnoService.findAll());
        return "prestamos/form";
    }

    // POST /prestamos → guardar nuevo
    // CLAVE: se asigna el alumno al objeto ANTES de llamar a @Valid manualmente
    // mediante BindingResult, para que @NotNull(alumno) no falle incorrectamente.
    @PostMapping
    public String crear(@ModelAttribute("prestamo") Prestamo prestamo,
                        BindingResult result,
                        @RequestParam(value = "alumnoId", required = false) Long alumnoId,
                        Model model,
                        RedirectAttributes redirectAttributes) {

        // 1. Asignar el alumno ANTES de validar, para que @NotNull no falle
        if (alumnoId != null) {
            alumnoService.findById(alumnoId).ifPresent(prestamo::setAlumno);
        }

        // 2. Validar manualmente tras asignar el alumno
        jakarta.validation.Validator validator = jakarta.validation.Validation
                .buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(prestamo);
        if (!violations.isEmpty()) {
            // Registrar cada violación en el BindingResult para mostrarlas en el form
            for (var v : violations) {
                String field = v.getPropertyPath().toString();
                result.rejectValue(field, "error." + field, v.getMessage());
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("alumnos", alumnoService.findAll());
            return "prestamos/form";
        }

        prestamoService.save(prestamo);
        redirectAttributes.addFlashAttribute("successMessage", "Préstamo creado correctamente");
        return "redirect:/prestamos";
    }

    // GET /prestamos/edit/{id} → formulario editar
    @GetMapping("/edit/{id}")
    public String mostrarFormEditar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return prestamoService.findById(id).map(prestamo -> {
            model.addAttribute("prestamo", prestamo);
            model.addAttribute("alumnos", alumnoService.findAll());
            return "prestamos/form";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Préstamo no encontrado");
            return "redirect:/prestamos";
        });
    }

    // POST /prestamos/edit/{id} → actualizar
    @PostMapping("/edit/{id}")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("prestamo") Prestamo prestamo,
                             BindingResult result,
                             @RequestParam(value = "alumnoId", required = false) Long alumnoId,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        // 1. Asignar el alumno ANTES de validar
        if (alumnoId != null) {
            alumnoService.findById(alumnoId).ifPresent(prestamo::setAlumno);
        }

        // 2. Validar manualmente
        jakarta.validation.Validator validator = jakarta.validation.Validation
                .buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(prestamo);
        if (!violations.isEmpty()) {
            for (var v : violations) {
                String field = v.getPropertyPath().toString();
                result.rejectValue(field, "error." + field, v.getMessage());
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("alumnos", alumnoService.findAll());
            return "prestamos/form";
        }

        prestamo.setId(id);
        prestamoService.save(prestamo);
        redirectAttributes.addFlashAttribute("successMessage", "Préstamo actualizado correctamente");
        return "redirect:/prestamos";
    }

    // GET /prestamos/delete/{id} → eliminar
    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            prestamoService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Préstamo eliminado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se puede eliminar el préstamo");
        }
        return "redirect:/prestamos";
    }
}
