package com.salesianos.bibliobox.repository;

import com.salesianos.bibliobox.entity.Alumno;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================
 * TEST DE INTEGRACIÓN (IT) — AlumnoRepository
 * ============================================================
 *
 * Tipo: Test de INTEGRACIÓN con base de datos real en memoria.
 * Objetivo: verificar que la @Query de agrupación por curso
 *           (getAlumnosPorCurso) devuelve los datos correctamente.
 *
 * ¿Qué hace @DataJpaTest?
 *   - Levanta SOLO la capa JPA (entidades, repositorios, H2).
 *   - NO carga controladores, servicios ni la aplicación completa.
 *   - Usa H2 en memoria automáticamente (aunque tengas MySQL en prod).
 *   - Cada test se ejecuta en una transacción que hace ROLLBACK al final,
 *     así los datos de un test no afectan a los demás.
 *
 * IMPORTANTE — Import correcto en Spring Boot 4:
 *   org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
 *   (En SB3 era: org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest)
 *
 * @Autowired → Spring inyecta automáticamente el repositorio real.
 */
@DataJpaTest
@ActiveProfiles("test")
class AlumnoRepositoryTest {

    @Autowired
    private AlumnoRepository alumnoRepository;

    // ────────────────────────────────────────────────────────────────
    // IT1: La @Query GROUP BY curso agrupa correctamente los alumnos
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("IT1 - Repositorio Alumnos y agrupación por curso")
    void IT1_repositorioAlumnosYAgrupacionPorCurso() {

        // ARRANGE: insertar 3 alumnos — 2 en "1ºESO-A" y 1 en "2ºESO-B"
        // Se usan constructores manuales de la entidad (sin Lombok)
        alumnoRepository.save(new Alumno("Alumno 1", "1ºESO-A", null));
        alumnoRepository.save(new Alumno("Alumno 2", "1ºESO-A", null));
        alumnoRepository.save(new Alumno("Alumno 3", "2ºESO-B", null));

        // ACT: ejecutar la @Query de agrupación
        // Devuelve List<Map<String,Object>> con {curso: "...", total: N}
        List<Map<String, Object>> result = alumnoRepository.getAlumnosPorCurso();

        // ASSERT: verificar resultado
        assertNotNull(result);
        // Debe haber exactamente 2 grupos (2 cursos distintos)
        assertEquals(2, result.size(), "Deben existir 2 cursos distintos");

        // Verificar que cada curso tiene el conteo correcto
        // Usamos stream().anyMatch() para buscar en la lista sin depender del orden
        boolean found1ESO = result.stream()
                .anyMatch(m -> "1ºESO-A".equals(m.get("curso")) && ((Number) m.get("total")).intValue() == 2);
        boolean found2ESO = result.stream()
                .anyMatch(m -> "2ºESO-B".equals(m.get("curso")) && ((Number) m.get("total")).intValue() == 1);

        assertTrue(found1ESO, "Debe haber 2 alumnos en 1ºESO-A");
        assertTrue(found2ESO, "Debe haber 1 alumno en 2ºESO-B");
    }
}
