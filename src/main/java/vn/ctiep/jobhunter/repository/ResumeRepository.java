package vn.ctiep.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.ctiep.jobhunter.domain.Resume;
import vn.ctiep.jobhunter.util.constant.ResumeStateEnum;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long>, JpaSpecificationExecutor<Resume> {

    long countByJobIdAndStatus(Long jobId, ResumeStateEnum status);
    boolean existsByUserIdAndJobId(Long userId, Long jobId);
    List<Resume> findByJobIdAndActiveTrue(Long jobId);
    
    @Query("SELECT r.status as status, COUNT(r) as count " +
        "FROM Resume r " +
        "WHERE r.job.company.id = :companyId " +
        "GROUP BY r.status")
    List<Map<String, Object>> countResumesByStatus(@Param("companyId") Long companyId);

    @Query("SELECT r.job.name as jobName, COUNT(r) as count " +
        "FROM Resume r " +
        "WHERE r.job.company.id = :companyId " +
        "GROUP BY r.job.name " +
        "ORDER BY count DESC")
    List<Map<String, Object>> countResumesByJob(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(r) FROM Resume r WHERE r.job.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);
    }
