package com.hospital.queue.repository;

import com.hospital.queue.domain.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientNumber(String patientNumber);

    boolean existsByPhone(String phone);

    @Query("""
           SELECT p FROM Patient p
           WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(p.patientNumber) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(p.phone) LIKE LOWER(CONCAT('%', :q, '%'))
           """)
    Page<Patient> search(@Param("q") String query, Pageable pageable);
}
