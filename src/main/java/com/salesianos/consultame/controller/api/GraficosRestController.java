package com.salesianos.consultame.controller.api;

import com.salesianos.consultame.service.DoctorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/graficos")
public class GraficosRestController {

    private final DoctorService doctorService;

    public GraficosRestController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/doctores-por-consultorio")
    public List<Map<String, Object>> doctoresPorConsultorio() {
        return convertir(doctorService.countByConsultorio(), "consultorio", "total");
    }

    @GetMapping("/doctores-por-especialidad")
    public List<Map<String, Object>> doctoresPorEspecialidad() {
        return convertir(doctorService.countByEspecialidad(), "especialidad", "total");
    }

    private List<Map<String, Object>> convertir(List<Object[]> datos, String claveLabel, String claveValor) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object[] row : datos) {
            Map<String, Object> map = new HashMap<>();
            map.put(claveLabel, row[0]);
            map.put(claveValor, row[1]);
            resultado.add(map);
        }
        return resultado;
    }
}
