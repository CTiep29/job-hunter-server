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
import vn.ctiep.jobhunter.util.constant.JobStatusEnum;

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
        // trang thai ban dau
        j.setActive(false);
        j.setStatus(JobStatusEnum.PENDING);

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
            // Lay danh sach skill id
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
        dto.setStatus(currentJob.getStatus());
        dto.setCreatedAt(currentJob.getCreatedAt());
        dto.setCreatedBy(currentJob.getCreatedBy());

        if (currentJob.getSkills() != null) {
            // Lay danh sach skill name
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
    @Transactional
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

    @Transactional
    public Job restoreJob(long id) {
        Optional<Job> jobOptional = this.jobRepository.findByIdAndActiveFalse(id);
        if (jobOptional.isPresent()) {
            Job job = jobOptional.get();
            
            // 1. Khôi phục Job
            job.setActive(true);
            job = this.jobRepository.save(job);
            
            // 2. Khôi phục tất cả Resume liên quan
            List<Resume> relatedResumes = resumeRepository.findByJobId(job.getId());
            for (Resume resume : relatedResumes) {
                resume.setActive(true);
                resumeRepository.save(resume);
            }
            
            return job;
        }
        return null;
    }

    public ResultPaginationDTO fetchAll(Specification<Job> spec, Pageable pageable) {

        Page<Job> pageJob = this.jobRepository.findAll(spec, pageable);

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
            // 1. Cập nhật trạng thái job
            job.setActive(false);
            jobRepository.save(job);

            // 2. Cập nhật các resume liên quan
            List<Resume> relatedResumes = resumeRepository.findByJobIdAndActiveTrue(job.getId());
            for (Resume resume : relatedResumes) {
                // Chỉ cập nhật các resume đang ở trạng thái chờ xử lý
                if (resume.getStatus() == ResumeStateEnum.PENDING) {
                    resume.setActive(false);
                    resumeRepository.save(resume);
                }
            }
        }

        System.out.println("✅ Cronjob đã cập nhật " + expiredJobs.size() + " job hết hạn.");
    }

    @Transactional
    public Job approveJob(long id) {
        Optional<Job> jobOptional = this.jobRepository.findById(id);
        if (jobOptional.isPresent()) {
            Job job = jobOptional.get();
            
            // Chỉ duyệt những job đang ở trạng thái PENDING
            if (job.getStatus() == JobStatusEnum.PENDING) {
                job.setStatus(JobStatusEnum.APPROVED);
                job.setActive(true);
                return this.jobRepository.save(job);
            }
        }
        return null;
    }

    @Transactional
    public Job rejectJob(long id) {
        Optional<Job> jobOptional = this.jobRepository.findById(id);
        if (jobOptional.isPresent()) {
            Job job = jobOptional.get();
            
            // Chỉ từ chối những job đang ở trạng thái PENDING
            if (job.getStatus() == JobStatusEnum.PENDING) {
                job.setStatus(JobStatusEnum.REJECTED);
                job.setActive(false);
                return this.jobRepository.save(job);
            }
        }
        return null;
    }
    public long countByStatus(JobStatusEnum status) {
        return jobRepository.countByStatus(status);
    }
}
