package com.salesianos.bibliobox.service;

import com.salesianos.bibliobox.entity.Alumno;
import com.salesianos.bibliobox.repository.AlumnoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AlumnoService {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Transactional(readOnly = true)
    public List<Alumno> findAll() {
        return alumnoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Alumno> findById(Long id) {
        return alumnoRepository.findById(id);
    }

    public Alumno save(Alumno alumno) {
        return alumnoRepository.save(alumno);
    }

    public void deleteById(Long id) {
        alumnoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAlumnosPorCurso() {
        return alumnoRepository.getAlumnosPorCurso();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTop5AlumnosConMasPrestamos() {
        return alumnoRepository.getTop5AlumnosConMasPrestamos();
    }
}
