package com.salesianos.bibliobox.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================
 * TESTS UNITARIOS (UT) — entidad Prestamo
 * ============================================================
 *
 * Tipo: Test UNITARIO puro (sin Spring, sin base de datos).
 * Objetivo: verificar que las reglas de validación de la entidad
 *           Prestamo funcionan correctamente de forma aislada.
 *
 * ¿Cómo funciona?
 *   - Se crea un Validator de Jakarta (el mismo motor que usa Spring
 *     internamente cuando valida formularios o peticiones).
 *   - Se construye un objeto Prestamo con el constructor y setters manuales
 *     con datos intencionalmente inválidos.
 *   - Se llama a validator.validate(prestamo) que devuelve un Set
 *     de ConstraintViolation: una por cada regla violada.
 *   - Se comprueba que existe la violación esperada.
 *
 * ¿Por qué @BeforeAll y no @BeforeEach?
 *   - El Validator es caro de crear (reflexión interna). Con @BeforeAll
 *     se crea UNA SOLA VEZ para todos los tests de la clase.
 *   - @BeforeAll debe ser static porque se ejecuta antes de
 *     que exista ninguna instancia de la clase.
 *
 * Anotaciones clave:
 *   @Test           → marca el método como caso de prueba
 *   @DisplayName    → nombre legible que aparece en el informe
 *   assertTrue      → falla el test si la condición es false
 */
class PrestamoTest {

    // Validator se inicializa una vez y se reutiliza en todos los tests
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        // ValidatorFactory es la fábrica que construye el Validator
        // siguiendo las anotaciones Jakarta (@NotBlank, @AssertTrue, etc.)
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ────────────────────────────────────────────────────────────────
    // UT1: Regla de negocio → si devuelto=true, fechaDevolucion debe existir
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT1 - devuelto=true exige fechaDevolucion")
    void UT1_devueltoTrueExigeFechaDevolucion() {

        // Alumno auxiliar (no se persiste, solo sirve para montar el Prestamo)
        Alumno alumno = new Alumno("Test Alumno", "1ºESO-A", null);

        // Prestamo INVÁLIDO: devuelto=true pero fechaDevolucion=null
        // Esto viola el método isDevueltoTieneFechaDevolucion() de la entidad,
        // que está anotado con @AssertTrue → genera una ConstraintViolation
        // cuya propertyPath es "devueltoTieneFechaDevolucion"
        Prestamo prestamo = new Prestamo(
                "Libro de prueba",
                LocalDate.of(2025, 1, 10),
                true,
                null,   // <-- fechaDevolucion null: obligatoria cuando devuelto=true
                alumno
        );

        // Ejecutar la validación → devuelve todas las violaciones encontradas
        Set<ConstraintViolation<Prestamo>> violations = validator.validate(prestamo);

        // Buscar si existe una violación con el nombre de la propiedad "devueltoTieneFechaDevolucion"
        // (el nombre del método @AssertTrue en la entidad, sin el prefijo "is")
        boolean hasDevueltoViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("devueltoTieneFechaDevolucion"));

        assertTrue(hasDevueltoViolation,
                "Debe existir una violación de validación cuando devuelto=true y fechaDevolucion es null");
    }

    // ────────────────────────────────────────────────────────────────
    // UT2: Regla de negocio → fechaDevolucion debe ser >= fechaPrestamo
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT2 - fechaDevolucion debe ser >= fechaPrestamo")
    void UT2_fechaDevolucionDebeSerMayorOIgualFechaPrestamo() {

        Alumno alumno = new Alumno("Test Alumno", "1ºESO-A", null);

        // Prestamo INVÁLIDO: fechaDevolucion (10 junio) es ANTERIOR a fechaPrestamo (15 junio)
        // Esto viola el método isFechaDevolucionValida() de la entidad,
        // que está anotado con @AssertTrue
        Prestamo prestamo = new Prestamo(
                "Libro de prueba",
                LocalDate.of(2025, 6, 15),
                true,
                LocalDate.of(2025, 6, 10),  // <-- Fecha ANTERIOR al préstamo
                alumno
        );

        Set<ConstraintViolation<Prestamo>> violations = validator.validate(prestamo);

        boolean hasFechaViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("fechaDevolucionValida"));

        assertTrue(hasFechaViolation,
                "Debe existir una violación cuando fechaDevolucion es anterior a fechaPrestamo");
    }
}
