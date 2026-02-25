package com.salesianos.bibliobox.controller.api;

import com.salesianos.bibliobox.entity.Prestamo;
import com.salesianos.bibliobox.service.PrestamoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoRestController {

    @Autowired
    private PrestamoService prestamoService;

    @GetMapping
    public List<Prestamo> listar() {
        return prestamoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prestamo> obtenerPorId(@PathVariable Long id) {
        return prestamoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Prestamo> crear(@Valid @RequestBody Prestamo prestamo) {
        Prestamo saved = prestamoService.save(prestamo);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Prestamo> actualizar(@PathVariable Long id,
                                                @Valid @RequestBody Prestamo prestamo) {
        return prestamoService.findById(id).map(existing -> {
            prestamo.setId(id);
            return ResponseEntity.ok(prestamoService.save(prestamo));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return prestamoService.findById(id).map(p -> {
            prestamoService.deleteById(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
