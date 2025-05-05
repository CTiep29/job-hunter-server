package vn.ctiep.jobhunter.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import vn.ctiep.jobhunter.domain.Job;
import vn.ctiep.jobhunter.domain.Skill;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>,
                JpaSpecificationExecutor<Job> {

        List<Job> findBySkillsIn(List<Skill> skills);
        List<Job> findByActiveTrue();
        List<Job> findByEndDateBeforeAndActiveTrue(Instant now);

}
