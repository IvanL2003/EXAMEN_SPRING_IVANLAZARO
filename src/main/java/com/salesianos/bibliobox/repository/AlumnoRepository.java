package com.salesianos.bibliobox.repository;

import com.salesianos.bibliobox.entity.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AlumnoRepository extends JpaRepository<Alumno, Long> {

    List<Alumno> findByCurso(String curso);

    @Query("SELECT a.curso AS curso, COUNT(a) AS total FROM Alumno a GROUP BY a.curso")
    List<Map<String, Object>> getAlumnosPorCurso();

    @Query("SELECT a.nombre AS nombre, COUNT(p) AS totalPrestamos " +
           "FROM Alumno a LEFT JOIN a.prestamos p " +
           "GROUP BY a.id, a.nombre " +
           "ORDER BY totalPrestamos DESC " +
           "LIMIT 5")
    List<Map<String, Object>> getTop5AlumnosConMasPrestamos();
}
