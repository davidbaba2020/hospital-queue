package com.hospital.queue.repository;

import com.hospital.queue.domain.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByActiveTrue();
    Optional<Department> findByCode(String code);
    boolean existsByCode(String code);
}
