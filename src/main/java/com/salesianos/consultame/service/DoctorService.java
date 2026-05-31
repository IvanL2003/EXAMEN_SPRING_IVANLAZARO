package com.salesianos.consultame.service;

import com.salesianos.consultame.entity.Doctor;
import com.salesianos.consultame.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Transactional(readOnly = true)
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Doctor> findById(Long id) {
        return doctorRepository.findById(id);
    }

    @Transactional
    public Doctor save(Doctor doctor) {
        return doctorRepository.save(doctor);
    }

    @Transactional
    public void deleteById(Long id) {
        doctorRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Object[]> countByConsultorio() {
        return doctorRepository.countByConsultorio();
    }

    @Transactional(readOnly = true)
    public List<Object[]> countByEspecialidad() {
        return doctorRepository.countByEspecialidad();
    }
}
