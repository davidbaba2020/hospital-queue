package com.hospital.queue.repository;

import com.hospital.queue.domain.entity.Doctor;
import com.hospital.queue.domain.enums.DoctorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(Long userId);

    List<Doctor> findByDepartmentId(Long departmentId);

    List<Doctor> findByStatus(DoctorStatus status);

    List<Doctor> findByDepartmentIdAndStatus(Long departmentId, DoctorStatus status);

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("SELECT d FROM Doctor d JOIN FETCH d.user JOIN FETCH d.department")
    List<Doctor> findAllWithDetails();
}
