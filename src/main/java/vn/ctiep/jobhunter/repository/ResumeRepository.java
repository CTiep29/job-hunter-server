package vn.ctiep.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.ctiep.jobhunter.domain.Resume;
import vn.ctiep.jobhunter.util.constant.ResumeStateEnum;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long>, JpaSpecificationExecutor<Resume> {

    long countByJobIdAndStatus(Long jobId, ResumeStateEnum status);
    boolean existsByUserIdAndJobId(Long userId, Long jobId);

}
