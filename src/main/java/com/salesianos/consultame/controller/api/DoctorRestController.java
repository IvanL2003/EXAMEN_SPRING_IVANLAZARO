package com.salesianos.consultame.controller.api;

import com.salesianos.consultame.entity.Doctor;
import com.salesianos.consultame.service.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctores")
public class DoctorRestController {

    private final DoctorService doctorService;

    public DoctorRestController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public List<Doctor> listar() {
        return doctorService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Doctor> obtener(@PathVariable Long id) {
        return doctorService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Doctor> crear(@RequestBody Doctor doctor) {
        Doctor guardado = doctorService.save(doctor);
        return ResponseEntity.ok(guardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Doctor> modificar(@PathVariable Long id, @RequestBody Doctor doctor) {
        return doctorService.findById(id)
                .map(existente -> {
                    existente.setNombre(doctor.getNombre());
                    existente.setEspecialidad(doctor.getEspecialidad());
                    existente.setTelefono(doctor.getTelefono());
                    existente.setEmail(doctor.getEmail());
                    existente.setConsultorio(doctor.getConsultorio());
                    Doctor actualizado = doctorService.save(existente);
                    return ResponseEntity.ok(actualizado);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
