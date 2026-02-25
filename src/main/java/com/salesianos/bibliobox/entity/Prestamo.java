package com.salesianos.bibliobox.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDate;

@Entity
@Table(name = "prestamos")
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título del libro es obligatorio")
    private String tituloLibro;

    @NotNull(message = "La fecha de préstamo es obligatoria")
    private LocalDate fechaPrestamo;

    private boolean devuelto = false;

    private LocalDate fechaDevolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    @NotNull(message = "El alumno es obligatorio")
    @JsonBackReference
    private Alumno alumno;

    // ── Constructores ────────────────────────────────────────────────
    public Prestamo() {}

    public Prestamo(String tituloLibro, LocalDate fechaPrestamo, boolean devuelto,
                    LocalDate fechaDevolucion, Alumno alumno) {
        this.tituloLibro = tituloLibro;
        this.fechaPrestamo = fechaPrestamo;
        this.devuelto = devuelto;
        this.fechaDevolucion = fechaDevolucion;
        this.alumno = alumno;
    }

    // ── Getters y Setters ────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTituloLibro() { return tituloLibro; }
    public void setTituloLibro(String tituloLibro) { this.tituloLibro = tituloLibro; }

    public LocalDate getFechaPrestamo() { return fechaPrestamo; }
    public void setFechaPrestamo(LocalDate fechaPrestamo) { this.fechaPrestamo = fechaPrestamo; }

    public boolean isDevuelto() { return devuelto; }
    public void setDevuelto(boolean devuelto) { this.devuelto = devuelto; }

    public LocalDate getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(LocalDate fechaDevolucion) { this.fechaDevolucion = fechaDevolucion; }

    public Alumno getAlumno() { return alumno; }
    public void setAlumno(Alumno alumno) { this.alumno = alumno; }

    // ── Validaciones personalizadas (@AssertTrue) ────────────────────
    @AssertTrue(message = "Si el préstamo está devuelto, la fecha de devolución es obligatoria")
    public boolean isDevueltoTieneFechaDevolucion() {
        if (devuelto) {
            return fechaDevolucion != null;
        }
        return true;
    }

    @AssertTrue(message = "La fecha de devolución debe ser igual o posterior a la fecha de préstamo")
    public boolean isFechaDevolucionValida() {
        if (fechaDevolucion != null && fechaPrestamo != null) {
            return !fechaDevolucion.isBefore(fechaPrestamo);
        }
        return true;
    }
}
