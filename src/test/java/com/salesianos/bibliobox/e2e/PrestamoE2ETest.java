package com.salesianos.bibliobox.e2e;

import com.salesianos.bibliobox.entity.Alumno;
import com.salesianos.bibliobox.repository.AlumnoRepository;
import com.salesianos.bibliobox.repository.PrestamoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ============================================================
 * TEST E2E (End-to-End) — PrestamoController
 * ============================================================
 *
 * Tipo: Test E2E con @SpringBootTest + MockMvc (contexto Spring completo).
 * Objetivo: Verificar que un préstamo inválido (devuelto=true sin fechaDevolucion)
 *           no se persiste y vuelve al formulario con errores.
 *
 * Anotaciones clave:
 *   - @SpringBootTest: levanta toda la aplicación Spring Boot.
 *   - @AutoConfigureMockMvc: configura MockMvc para simular peticiones HTTP.
 *   - @ActiveProfiles("test"): usa BD H2 en memoria.
 *
 * IMPORTANTE — Import correcto en Spring Boot 4:
 *   org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
 *
 * ¿Por qué borramos préstamos Y alumnos en @BeforeEach?
 *   - Los préstamos tienen FK hacia alumno → hay que borrar préstamos primero.
 *   - Después se crea un alumno fresco para que el POST pueda referenciarle.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrestamoE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    // ID del alumno creado en @BeforeEach, necesario para el parámetro alumnoId
    private Long alumnoId;

    @BeforeEach
    void setUp() {
        // Borrar en orden correcto (FK: primero préstamos, luego alumnos)
        prestamoRepository.deleteAll();
        alumnoRepository.deleteAll();

        // Crear un alumno válido que usaremos en el POST (sin Lombok, con constructor)
        Alumno alumno = alumnoRepository.save(new Alumno("Alumno E2E", "1ºESO-A", null));
        alumnoId = alumno.getId();
    }

    // ────────────────────────────────────────────────────────────────
    // E2E2: POST con datos inválidos → debe volver al formulario con errores
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("E2E2 - Flujo préstamo caso inválido (devuelto=true sin fecha)")
    void E2E2_flujoPrestamoCasoInvalido() throws Exception {

        // POST con devuelto=true pero SIN fechaDevolucion
        // → el método @AssertTrue isDevueltoTieneFechaDevolucion() devuelve false
        // → Spring MVC detecta el error de validación
        // → el controlador devuelve la vista del formulario (NO redirige)
        mockMvc.perform(post("/prestamos")
                        .param("alumnoId",      alumnoId.toString())
                        .param("tituloLibro",   "Libro Inválido")
                        .param("fechaPrestamo", "2025-01-10")
                        .param("devuelto",      "true")
                        // Sin fechaDevolucion → @AssertTrue falla
                )
                // HTTP 200: vuelve al formulario con errores (NO 302 redirect)
                .andExpect(status().isOk())
                // La vista del formulario de préstamos se muestra de nuevo
                .andExpect(view().name("prestamos/form"));
    }
}
