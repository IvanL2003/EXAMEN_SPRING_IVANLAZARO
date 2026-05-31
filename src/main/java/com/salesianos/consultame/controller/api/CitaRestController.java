package com.salesianos.consultame.controller.api;

import com.salesianos.consultame.entity.Cita;
import com.salesianos.consultame.service.CitaService;
import com.salesianos.consultame.service.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaRestController {

    private final CitaService citaService;
    private final DoctorService doctorService;

    public CitaRestController(CitaService citaService, DoctorService doctorService) {
        this.citaService = citaService;
        this.doctorService = doctorService;
    }

    @GetMapping
    public List<Cita> listar() {
        return citaService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cita> obtener(@PathVariable Long id) {
        return citaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Cita> crear(@RequestBody Cita cita) {
        if (cita.getDoctor() != null && cita.getDoctor().getId() != null) {
            doctorService.findById(cita.getDoctor().getId()).ifPresent(cita::setDoctor);
        }
        Cita guardada = citaService.save(cita);
        return ResponseEntity.ok(guardada);
    }
}
