package com.salesianos.consultame.repository;

import com.salesianos.consultame.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("SELECT d.consultorio, COUNT(d) FROM Doctor d GROUP BY d.consultorio")
    List<Object[]> countByConsultorio();

    @Query("SELECT d.especialidad, COUNT(d) FROM Doctor d GROUP BY d.especialidad")
    List<Object[]> countByEspecialidad();
}
