package com.gameon.repository;

import com.gameon.model.entity.Report;
import com.gameon.model.enums.ReportStatus;
import com.gameon.model.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatus(ReportStatus status);

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    List<Report> findByReferenceIdAndReportType(Long referenceId, ReportType reportType);

    long countByStatus(ReportStatus status);

    List<Report> findByReporterUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT r FROM Report r JOIN FETCH r.reporter WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<Report> findPendingWithReporter(@Param("status") ReportStatus status);
}
