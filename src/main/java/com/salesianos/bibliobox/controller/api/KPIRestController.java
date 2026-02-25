package com.salesianos.bibliobox.controller.api;

import com.salesianos.bibliobox.service.AlumnoService;
import com.salesianos.bibliobox.service.PrestamoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kpi")
public class KPIRestController {

    @Autowired
    private AlumnoService alumnoService;

    @Autowired
    private PrestamoService prestamoService;

    @GetMapping("/alumnos-por-curso")
    public List<Map<String, Object>> alumnosPorCurso() {
        return alumnoService.getAlumnosPorCurso();
    }

    @GetMapping("/prestamos-por-estado")
    public List<Map<String, Object>> prestamosPorEstado() {
        return prestamoService.getPrestamosPorEstado();
    }

    @GetMapping("/prestamos-por-mes")
    public List<Map<String, Object>> prestamosPorMes() {
        return prestamoService.getPrestamosPorMes();
    }

    @GetMapping("/top-alumnos")
    public List<Map<String, Object>> topAlumnos() {
        return alumnoService.getTop5AlumnosConMasPrestamos();
    }
}
