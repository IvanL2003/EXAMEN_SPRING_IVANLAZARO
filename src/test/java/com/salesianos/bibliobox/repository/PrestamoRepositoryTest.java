package com.salesianos.bibliobox.repository;

import com.salesianos.bibliobox.entity.Alumno;
import com.salesianos.bibliobox.entity.Prestamo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================
 * TEST DE INTEGRACIÓN (IT) — PrestamoRepository
 * ============================================================
 *
 * Tipo: Test de INTEGRACIÓN con base de datos real en memoria.
 * Objetivo: verificar que la @Query de conteo por estado
 *           (getPrestamosPorEstado) devuelve los totales correctos.
 *
 * ¿Qué consulta se está probando?
 *   SELECT
 *     SUM(CASE WHEN p.devuelto = true THEN 1 ELSE 0 END) AS devueltos,
 *     SUM(CASE WHEN p.devuelto = false THEN 1 ELSE 0 END) AS pendientes
 *   FROM prestamos p
 *
 *   Devuelve List<Map<String,Object>> con un único elemento:
 *   [{devueltos: N, pendientes: M}]
 *
 * IMPORTANTE — Import correcto en Spring Boot 4:
 *   org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
 *
 * Nota: @DataJpaTest inyecta TODOS los repositorios JPA declarados,
 * por eso podemos usar tanto AlumnoRepository como PrestamoRepository.
 */
@DataJpaTest
@ActiveProfiles("test")
class PrestamoRepositoryTest {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    // ────────────────────────────────────────────────────────────────
    // IT2: La @Query de estado cuenta correctamente devueltos y pendientes
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("IT2 - Conteo préstamos devueltos y pendientes")
    void IT2_conteoDevueltoNoDevuelto() {

        // ARRANGE: crear un alumno (requerido por la FK de Prestamo)
        // Se usan constructores manuales (sin Lombok)
        Alumno alumno = alumnoRepository.save(new Alumno("Alumno IT2", "2ºESO-A", null));

        // Insertar 2 préstamos DEVUELTOS (devuelto=true, con fechaDevolucion)
        prestamoRepository.save(new Prestamo("Libro 1", LocalDate.of(2025, 1, 1),  true, LocalDate.of(2025, 1, 15), alumno));
        prestamoRepository.save(new Prestamo("Libro 2", LocalDate.of(2025, 2, 1),  true, LocalDate.of(2025, 2, 14), alumno));

        // Insertar 3 préstamos PENDIENTES (devuelto=false, sin fechaDevolucion)
        prestamoRepository.save(new Prestamo("Libro 3", LocalDate.of(2025, 3, 1), false, null, alumno));
        prestamoRepository.save(new Prestamo("Libro 4", LocalDate.of(2025, 4, 1), false, null, alumno));
        prestamoRepository.save(new Prestamo("Libro 5", LocalDate.of(2025, 5, 1), false, null, alumno));

        // ACT: ejecutar la @Query de conteo por estado
        List<Map<String, Object>> result = prestamoRepository.getPrestamosPorEstado();

        // ASSERT
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // La query devuelve siempre UN SOLO elemento (es un agregado global)
        Map<String, Object> stats = result.get(0);

        // Extraer los valores — pueden ser Integer, Long, BigInteger según la BD,
        // por eso se usa ((Number) ...).intValue() para compatibilidad universal
        int devueltos  = ((Number) stats.get("devueltos")).intValue();
        int pendientes = ((Number) stats.get("pendientes")).intValue();

        assertEquals(2, devueltos,  "Deben haber 2 préstamos devueltos");
        assertEquals(3, pendientes, "Deben haber 3 préstamos pendientes");
    }
}
