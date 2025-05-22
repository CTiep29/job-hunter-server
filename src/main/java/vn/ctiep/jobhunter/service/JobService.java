package vn.ctiep.jobhunter.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import vn.ctiep.jobhunter.domain.Company;
import vn.ctiep.jobhunter.domain.Job;
import vn.ctiep.jobhunter.domain.Skill;
import vn.ctiep.jobhunter.domain.User;
import vn.ctiep.jobhunter.domain.response.ResLoginDTO;
import vn.ctiep.jobhunter.domain.response.ResultPaginationDTO;
import vn.ctiep.jobhunter.domain.response.job.ResCreateJobDTO;
import vn.ctiep.jobhunter.domain.response.job.ResUpdateJobDTO;
import vn.ctiep.jobhunter.repository.CompanyRepository;
import vn.ctiep.jobhunter.repository.JobRepository;
import vn.ctiep.jobhunter.repository.SkillRepository;
import vn.ctiep.jobhunter.repository.UserRepository;
import vn.ctiep.jobhunter.util.SecurityUtil;
import vn.ctiep.jobhunter.domain.Resume;
import vn.ctiep.jobhunter.repository.ResumeRepository;
import vn.ctiep.jobhunter.util.constant.ResumeStateEnum;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;

    public JobService(JobRepository jobRepository,
            SkillRepository skillRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            ResumeRepository resumeRepository) {
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
    }

    public Optional<Job> fetchJobById(long id) {
        return this.jobRepository.findById(id);
    }

    public ResCreateJobDTO create(Job j) {
        // gan cong ty
        Optional<ResLoginDTO.UserInsideToken> optionalUser = SecurityUtil.getCurrentUserInsideToken();
        if (optionalUser.isPresent()){
            Long userId = optionalUser.get().getId();
            Optional<User> userOptional = userRepository.findById(userId);
            if(userOptional.isPresent()){
                User currentUser = userOptional.get();
                Company company = currentUser.getCompany();
                if (company != null){
                    j.setCompany(company);
                }
            }
        }
        // check skills
        if (j.getSkills() != null) {
            List<Long> reqSkills = j.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            j.setSkills(dbSkills);
        }
        // create job
        Job currentJob = this.jobRepository.save(j);

        // convert response
        ResCreateJobDTO dto = new ResCreateJobDTO();
        dto.setId(currentJob.getId());
        dto.setName(currentJob.getName());
        dto.setSalary(currentJob.getSalary());
        dto.setQuantity(currentJob.getQuantity());
        dto.setLocation(currentJob.getLocation());
        dto.setLevel(currentJob.getLevel());
        dto.setStartDate(currentJob.getStartDate());
        dto.setEndDate(currentJob.getEndDate());
        dto.setActive(currentJob.isActive());
        dto.setCreatedAt(currentJob.getCreatedAt());
        dto.setCreatedBy(currentJob.getCreatedBy());

        if (currentJob.getSkills() != null) {
            List<String> skills = currentJob.getSkills()
                    .stream().map(item -> item.getName())
                    .collect(Collectors.toList());
            dto.setSkills(skills);
        }

        return dto;
    }

    public ResUpdateJobDTO update(Job j, Job jobInDB) {
        // check skills
        if (j.getSkills() != null) {
            List<Long> reqSkills = j.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            jobInDB.setSkills(dbSkills);
        }
        // gan cong ty
        Optional<ResLoginDTO.UserInsideToken> optionalUser = SecurityUtil.getCurrentUserInsideToken();
        if (optionalUser.isPresent()){
            Long userId = optionalUser.get().getId();
            Optional<User> userOptional = userRepository.findById(userId);
            if(userOptional.isPresent()){
                User currentUser = userOptional.get();
                Company company = currentUser.getCompany();
                if (company != null){
                    j.setCompany(company);
                }
            }
        }
        // update correct info
        jobInDB.setName(j.getName());
        jobInDB.setSalary(j.getSalary());
        jobInDB.setQuantity(j.getQuantity());
        jobInDB.setLocation(j.getLocation());
        jobInDB.setLevel(j.getLevel());
        jobInDB.setStartDate(j.getStartDate());
        jobInDB.setEndDate(j.getEndDate());
        jobInDB.setActive(j.isActive());
        // update job
        Job currentJob = this.jobRepository.save(jobInDB);

        // convert response
        ResUpdateJobDTO dto = new ResUpdateJobDTO();
        dto.setId(currentJob.getId());
        dto.setName(currentJob.getName());
        dto.setSalary(currentJob.getSalary());
        dto.setQuantity(currentJob.getQuantity());
        dto.setLocation(currentJob.getLocation());
        dto.setLevel(currentJob.getLevel());
        dto.setStartDate(currentJob.getStartDate());
        dto.setEndDate(currentJob.getEndDate());
        dto.setActive(currentJob.isActive());
        dto.setUpdatedAt(currentJob.getUpdatedAt());
        dto.setUpdatedBy(currentJob.getUpdatedBy());

        if (currentJob.getSkills() != null) {
            List<String> skills = currentJob.getSkills()
                    .stream().map(item -> item.getName())
                    .collect(Collectors.toList());
            dto.setSkills(skills);
        }

        return dto;
    }

    public void handleDeleteJob(long id) {
        Optional<Job> jobOptional = this.jobRepository.findById(id);
        if (jobOptional.isPresent()) {
            Job job = jobOptional.get();
            
            // 1. Xóa mềm Job
            job.setActive(false);
            this.jobRepository.save(job);
            
            // 2. Xóa mềm tất cả Resume liên quan
            List<Resume> relatedResumes = resumeRepository.findByJobIdAndActiveTrue(job.getId());
            for (Resume resume : relatedResumes) {
                resume.setActive(false);
                resumeRepository.save(resume);
            }
        }
    }

    public ResultPaginationDTO fetchAll(Specification<Job> spec, Pageable pageable) {

        Page<Job> pageUser = this.jobRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageUser.getContent());

        return rs;
    }

    public ResultPaginationDTO fetchByCompanyId(long companyId, Specification<Job> spec, Pageable pageable) {
        Specification<Job> companySpec = (root, query, cb) -> cb.equal(root.get("company").get("id"), companyId);

        Specification<Job> finalSpec = companySpec;
        if (spec != null) {
            finalSpec = finalSpec.and(spec);
        }

        Page<Job> pageJob = this.jobRepository.findAll(finalSpec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageJob.getTotalPages());
        mt.setTotal(pageJob.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageJob.getContent());
        return rs;
    }
    //Chay moi ngay luc 00:00
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deactivateExpiredJobs() {
        Instant now = Instant.now();
        List<Job> expiredJobs = jobRepository.findByEndDateBeforeAndActiveTrue(now);

        for (Job job : expiredJobs) {
            job.setActive(false);
        }

        jobRepository.saveAll(expiredJobs);
        System.out.println("✅ Cronjob đã cập nhật " + expiredJobs.size() + " job hết hạn.");
    }

}
