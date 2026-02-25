package com.salesianos.bibliobox.service;

import com.salesianos.bibliobox.entity.Prestamo;
import com.salesianos.bibliobox.repository.PrestamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PrestamoService {

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Transactional(readOnly = true)
    public List<Prestamo> findAll() {
        return prestamoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Prestamo> findById(Long id) {
        return prestamoRepository.findById(id);
    }

    public Prestamo save(Prestamo prestamo) {
        return prestamoRepository.save(prestamo);
    }

    public void deleteById(Long id) {
        prestamoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPrestamosPorEstado() {
        return prestamoRepository.getPrestamosPorEstado();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPrestamosPorMes() {
        return prestamoRepository.getPrestamosPorMes();
    }
}
