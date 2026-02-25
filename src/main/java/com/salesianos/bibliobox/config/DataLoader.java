package com.salesianos.bibliobox.config;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.salesianos.bibliobox.entity.Alumno;
import com.salesianos.bibliobox.entity.Prestamo;
import com.salesianos.bibliobox.repository.AlumnoRepository;
import com.salesianos.bibliobox.repository.PrestamoRepository;

@Component
@Profile("!test")  // No se ejecuta cuando el perfil activo es "test"
public class DataLoader implements CommandLineRunner {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Override
    public void run(String... args) throws Exception {
        // Evitar insertar datos si ya existen (por ejemplo si import.sql ya los creó)
        if (alumnoRepository.count() > 0 || prestamoRepository.count() > 0) {
            return;
        }

        // ── Alumnos de prueba ─────────────────────────────────────────────
        Alumno ana    = alumnoRepository.save(new Alumno("Ana García",      "1ºESO-A", "ana.garcia@colegio.es"));
        Alumno carlos = alumnoRepository.save(new Alumno("Carlos López",    "1ºESO-B", "carlos.lopez@colegio.es"));
        Alumno maria  = alumnoRepository.save(new Alumno("María Martínez",  "2ºESO-A", "maria.martinez@colegio.es"));
        Alumno pedro  = alumnoRepository.save(new Alumno("Pedro Sánchez",   "2ºESO-B", "pedro.sanchez@colegio.es"));
        Alumno lucia  = alumnoRepository.save(new Alumno("Lucía Fernández", "3ºESO-A", "lucia.fernandez@colegio.es"));

        // ── Préstamos de prueba distribuidos en diferentes meses ──────────

        // Ana: 2 devueltos, 1 pendiente
        prestamoRepository.save(new Prestamo("El Quijote",           LocalDate.of(2025, 9,  10), true,  LocalDate.of(2025, 9,  20), ana));
        prestamoRepository.save(new Prestamo("Cien años de soledad", LocalDate.of(2025, 10, 5),  true,  LocalDate.of(2025, 10, 15), ana));
        prestamoRepository.save(new Prestamo("El principito",        LocalDate.of(2026, 1,  8),  false, null,                       ana));

        // Carlos: 1 devuelto, 1 pendiente
        prestamoRepository.save(new Prestamo("Harry Potter",         LocalDate.of(2025, 10, 12), true,  LocalDate.of(2025, 10, 25), carlos));
        prestamoRepository.save(new Prestamo("La Odisea",            LocalDate.of(2025, 11, 3),  false, null,                       carlos));

        // María: 3 devueltos, 1 pendiente (la más activa)
        prestamoRepository.save(new Prestamo("Romeo y Julieta",      LocalDate.of(2025, 9,  15), true,  LocalDate.of(2025, 9,  28), maria));
        prestamoRepository.save(new Prestamo("1984",                 LocalDate.of(2025, 10, 20), true,  LocalDate.of(2025, 11, 2),  maria));
        prestamoRepository.save(new Prestamo("Matar a un ruiseñor",  LocalDate.of(2025, 11, 10), true,  LocalDate.of(2025, 11, 22), maria));
        prestamoRepository.save(new Prestamo("El gran Gatsby",       LocalDate.of(2026, 1,  15), false, null,                       maria));

        // Pedro: 1 devuelto
        prestamoRepository.save(new Prestamo("Crimen y castigo",     LocalDate.of(2025, 12, 5),  true,  LocalDate.of(2025, 12, 18), pedro));

        // Lucía: 1 devuelto, 1 pendiente
        prestamoRepository.save(new Prestamo("Orgullo y prejuicio",  LocalDate.of(2025, 11, 20), true,  LocalDate.of(2025, 12, 3),  lucia));
        prestamoRepository.save(new Prestamo("La metamorfosis",      LocalDate.of(2026, 2,  1),  false, null,                       lucia));
    }
}
