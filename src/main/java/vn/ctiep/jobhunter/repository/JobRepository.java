package vn.ctiep.jobhunter.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.ctiep.jobhunter.domain.Job;
import vn.ctiep.jobhunter.domain.Skill;
import vn.ctiep.jobhunter.domain.Company;

import java.time.LocalDate;
import java.util.Map;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>,
                JpaSpecificationExecutor<Job> {

        List<Job> findBySkillsInAndActiveTrue(List<Skill> skills);
        List<Job> findByActiveTrue();
        List<Job> findByEndDateBeforeAndActiveTrue(Instant now);
        List<Job> findByCompanyAndActiveTrue(Company company);

        @Query("SELECT COUNT(j) FROM Job j WHERE j.company.id = :companyId")
        long countByCompanyId(@Param("companyId") Long companyId);

        @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count " +
               "FROM jobs " +
               "WHERE (:startDate IS NULL OR created_at >= :startDate) " +
               "AND (:endDate IS NULL OR created_at <= :endDate) " +
               "GROUP BY DATE_FORMAT(created_at, '%Y-%m') " +
               "ORDER BY month", nativeQuery = true)
        List<Map<String, Object>> countNewJobsByMonth(
                @Param("startDate") LocalDate startDate,
                @Param("endDate") LocalDate endDate);

        @Query(value = "SELECT c.name as companyName, COUNT(j.id) as activeJobs " +
                "FROM jobs j " +
                "JOIN companies c ON j.company_id = c.id " +
                "WHERE j.active = true " +
                "GROUP BY c.id, c.name " +
                "ORDER BY activeJobs DESC", nativeQuery = true)
        List<Map<String, Object>> countActiveJobsByCompany();
}
