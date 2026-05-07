package com.hospital.queue.repository;

import com.hospital.queue.domain.entity.QueueEntry;
import com.hospital.queue.domain.enums.QueueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByTicketNumber(String ticketNumber);

    List<QueueEntry> findByDepartmentIdAndStatusInOrderByPriorityAscRegisteredAtAsc(
            Long departmentId, List<QueueStatus> statuses);

    List<QueueEntry> findByDoctorIdAndStatusIn(Long doctorId, List<QueueStatus> statuses);

    long countByDepartmentIdAndStatus(Long departmentId, QueueStatus status);

    long countByStatus(QueueStatus status);

    @Query("SELECT COUNT(q) FROM QueueEntry q WHERE q.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<QueueStatus> statuses);

    @Query("""
           SELECT q FROM QueueEntry q
           JOIN FETCH q.patient
           JOIN FETCH q.department
           LEFT JOIN FETCH q.doctor d
           LEFT JOIN FETCH d.user
           WHERE q.department.id = :deptId
             AND q.status IN :statuses
           ORDER BY q.priority ASC, q.registeredAt ASC
           """)
    List<QueueEntry> findActiveByDepartment(@Param("deptId") Long deptId,
                                            @Param("statuses") List<QueueStatus> statuses);

    @Query("""
           SELECT q FROM QueueEntry q
           JOIN FETCH q.patient
           JOIN FETCH q.department
           LEFT JOIN FETCH q.doctor d
           LEFT JOIN FETCH d.user
           ORDER BY q.registeredAt DESC
           """)
    Page<QueueEntry> findAllWithDetails(Pageable pageable);

    @Query("SELECT COUNT(q) FROM QueueEntry q WHERE q.registeredAt BETWEEN :start AND :end")
    long countByRegisteredAtBetween(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Query("""
           SELECT q FROM QueueEntry q
           JOIN FETCH q.patient
           JOIN FETCH q.department
           WHERE q.registeredAt BETWEEN :start AND :end
           ORDER BY q.registeredAt DESC
           """)
    List<QueueEntry> findByDateRange(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (completed_at - registered_at)) / 60) FROM queue_entries WHERE status = 'COMPLETED' AND completed_at IS NOT NULL", nativeQuery = true)
    Double findAverageWaitTimeMinutes();
}
