package com.salesianos.bibliobox.controller.api;

import com.salesianos.bibliobox.entity.Alumno;
import com.salesianos.bibliobox.service.AlumnoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alumnos")
public class AlumnoRestController {

    @Autowired
    private AlumnoService alumnoService;

    @GetMapping
    public List<Alumno> listar() {
        return alumnoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alumno> obtenerPorId(@PathVariable Long id) {
        return alumnoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Alumno> crear(@Valid @RequestBody Alumno alumno) {
        Alumno saved = alumnoService.save(alumno);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Alumno> actualizar(@PathVariable Long id,
                                              @Valid @RequestBody Alumno alumno) {
        return alumnoService.findById(id).map(existing -> {
            alumno.setId(id);
            return ResponseEntity.ok(alumnoService.save(alumno));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return alumnoService.findById(id).map(a -> {
            alumnoService.deleteById(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
