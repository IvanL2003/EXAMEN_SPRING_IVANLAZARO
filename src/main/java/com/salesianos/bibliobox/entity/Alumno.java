package com.salesianos.bibliobox.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entidad JPA que representa un alumno de la biblioteca.
 * Mapeada a la tabla "alumnos". Un alumno puede tener varios prestamos (OneToMany).
 */
@Entity
@Table(name = "alumnos")
public class Alumno {

    /** Clave primaria auto-generada por la BD. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre completo del alumno. Obligatorio, minimo 2 caracteres. */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, message = "El nombre debe tener al menos 2 caracteres")
    private String nombre;

    /** Curso al que pertenece el alumno (ej: "1ESO-A"). Obligatorio. */
    @NotBlank(message = "El curso es obligatorio")
    private String curso;

    /** Email del alumno. Puede ser null, pero si se rellena debe tener formato valido. */
    @Email(message = "El email debe ser válido")
    private String email;

    /**
     * Lista de prestamos del alumno.
     * cascade=ALL: las operaciones se propagan a los prestamos.
     * orphanRemoval=true: si se quita un prestamo de la lista, se borra de la BD.
     * @JsonManagedReference: evita bucle infinito al serializar a JSON.
     */
    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Prestamo> prestamos = new ArrayList<>();

    // ── Constructores ─────────────────────────────────────────────────

    /** Constructor vacio requerido por JPA. */
    public Alumno() {}

    /**
     * Constructor de conveniencia para crear un alumno con sus datos basicos.
     * @param nombre nombre del alumno
     * @param curso  curso al que pertenece
     * @param email  email (puede ser null)
     */
    public Alumno(String nombre, String curso, String email) {
        this.nombre = nombre;
        this.curso = curso;
        this.email = email;
    }

    // ── Getters y Setters ─────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<Prestamo> getPrestamos() { return prestamos; }
    public void setPrestamos(List<Prestamo> prestamos) { this.prestamos = prestamos; }

    // ── Helpers para mantener la coherencia de la relacion bidireccional ──────

    /**
     * Asocia un prestamo a este alumno y apunta prestamo.alumno a this.
     * Usar siempre este metodo en lugar de manipular la lista directamente.
     */
    public void addPrestamo(Prestamo prestamo) {
        prestamos.add(prestamo);
        prestamo.setAlumno(this);
    }

    /**
     * Desasocia un prestamo de este alumno. Con orphanRemoval=true
     * el prestamo se eliminara automaticamente de la BD.
     */
    public void removePrestamo(Prestamo prestamo) {
        prestamos.remove(prestamo);
        prestamo.setAlumno(null);
    }
}
