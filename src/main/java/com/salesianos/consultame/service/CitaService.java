package com.salesianos.consultame.service;

import com.salesianos.consultame.entity.Cita;
import com.salesianos.consultame.repository.CitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CitaService {

    private final CitaRepository citaRepository;

    public CitaService(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    @Transactional(readOnly = true)
    public List<Cita> findAll() {
        return citaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Cita> findById(Long id) {
        return citaRepository.findById(id);
    }

    @Transactional
    public Cita save(Cita cita) {
        return citaRepository.save(cita);
    }

    @Transactional
    public void deleteById(Long id) {
        citaRepository.deleteById(id);
    }
}
