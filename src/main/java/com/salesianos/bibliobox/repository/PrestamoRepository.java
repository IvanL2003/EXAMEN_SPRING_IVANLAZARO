package com.salesianos.bibliobox.repository;

import com.salesianos.bibliobox.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findByAlumnoId(Long alumnoId);

    List<Prestamo> findByDevueltoFalse();

    @Query("SELECT " +
           "SUM(CASE WHEN p.devuelto = true THEN 1 ELSE 0 END) AS devueltos, " +
           "SUM(CASE WHEN p.devuelto = false THEN 1 ELSE 0 END) AS pendientes " +
           "FROM Prestamo p")
    List<Map<String, Object>> getPrestamosPorEstado();

    @Query("SELECT MONTH(p.fechaPrestamo) AS mes, COUNT(p) AS total " +
           "FROM Prestamo p " +
           "GROUP BY MONTH(p.fechaPrestamo) " +
           "ORDER BY mes")
    List<Map<String, Object>> getPrestamosPorMes();
}
