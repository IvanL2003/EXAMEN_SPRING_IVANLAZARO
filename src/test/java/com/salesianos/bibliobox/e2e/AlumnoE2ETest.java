package com.salesianos.bibliobox.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.salesianos.bibliobox.repository.AlumnoRepository;

/**
 * ============================================================
 * TEST E2E (End-to-End) — AlumnoController
 * ============================================================
 *
 * Tipo: Test E2E con @SpringBootTest + MockMvc.
 * Objetivo: verificar el flujo completo de creación de un alumno
 *           desde la petición HTTP hasta la persistencia en BD.
 *
 * ¿Qué hace @SpringBootTest?
 *   - Levanta el contexto Spring COMPLETO (controladores, servicios,
 *     repositorios, configuración, etc.), igual que en producción.
 *   - Es el test más costoso pero el más realista.
 *
 * ¿Qué hace @AutoConfigureMockMvc?
 *   - Configura MockMvc automáticamente para simular peticiones HTTP
 *     sin necesidad de un servidor real.
 *   - Permite hacer GET/POST y verificar status, view, model, etc.
 *
 * IMPORTANTE — Import correcto en Spring Boot 4:
 *   org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
 *   (En SB3 era: org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc)
 *
 * ¿Qué hace @ActiveProfiles("test")?
 *   - Activa el perfil "test", que usa application-test.properties
 *     (BD H2 en memoria en lugar de la BD de desarrollo/producción).
 *
 * ¿Por qué @BeforeEach deleteAll()?
 *   - Con @SpringBootTest la BD NO hace rollback automático entre tests.
 *   - deleteAll() garantiza un estado limpio antes de cada test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlumnoE2ETest {

    // MockMvc: simula peticiones HTTP al servidor sin levantarlo realmente
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlumnoRepository alumnoRepository;

    @BeforeEach
    void setUp() {
        // Limpiar la BD antes de cada test para evitar datos residuales
        alumnoRepository.deleteAll();
    }

    // ────────────────────────────────────────────────────────────────
    // E2E1: Flujo completo — formulario → POST → redirección → BD
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("E2E1 - Flujo completo de creación de alumno")
    void E2E1_flujoCreacionAlumnoCompleto() throws Exception {

        // PASO 1: GET /alumnos/new → debe devolver 200 con el formulario vacío
        // - status().isOk()               → HTTP 200
        // - view().name("alumnos/form")   → Thymeleaf renderiza esa vista
        // - model().attributeExists(...)  → el modelo tiene el atributo "alumno"
        mockMvc.perform(get("/alumnos/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("alumnos/form"))
                .andExpect(model().attributeExists("alumno"));

        // PASO 2: POST /alumnos con datos válidos → debe redirigir a /alumnos
        // - .param() simula campos de formulario HTML (como <input name="nombre">)
        // - status().is3xxRedirection()     → HTTP 302 (redirección tras éxito)
        // - redirectedUrl("/alumnos")       → la URL de destino es /alumnos
        mockMvc.perform(post("/alumnos")
                        .param("nombre", "Juan Pérez")
                        .param("curso",  "3ºESO-A")
                        .param("email",  "juan.perez@colegio.es"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/alumnos"));

        // PASO 3: verificar directamente en BD que el alumno fue persistido
        boolean exists = alumnoRepository.findAll().stream()
                .anyMatch(a -> "Juan Pérez".equals(a.getNombre()) && "3ºESO-A".equals(a.getCurso()));

        assertTrue(exists, "El alumno debe existir en la base de datos tras la creación");
    }
}
