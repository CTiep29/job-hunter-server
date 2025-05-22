package vn.ctiep.jobhunter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.turkraft.springfilter.builder.FilterBuilder;
import com.turkraft.springfilter.converter.FilterSpecification;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import com.turkraft.springfilter.parser.node.FilterNode;

import vn.ctiep.jobhunter.domain.Job;
import vn.ctiep.jobhunter.domain.Resume;
import vn.ctiep.jobhunter.domain.User;
import vn.ctiep.jobhunter.domain.response.ResultPaginationDTO;
import vn.ctiep.jobhunter.domain.response.job.ResCreateJobDTO;
import vn.ctiep.jobhunter.domain.response.resume.ResCreateResumeDTO;
import vn.ctiep.jobhunter.domain.response.resume.ResFetchResumeDTO;
import vn.ctiep.jobhunter.domain.response.resume.ResUpdateResumeDTO;
import vn.ctiep.jobhunter.repository.JobRepository;
import vn.ctiep.jobhunter.repository.ResumeRepository;
import vn.ctiep.jobhunter.repository.UserRepository;
import vn.ctiep.jobhunter.util.SecurityUtil;
import vn.ctiep.jobhunter.util.constant.ResumeStateEnum;
import vn.ctiep.jobhunter.util.error.IdInvalidException;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    @Autowired
    FilterBuilder fb;
    @Autowired
    private FilterParser filterParser;
    @Autowired
    private FilterSpecificationConverter filterSpecificationConverter;

    public ResumeService(ResumeRepository resumeRepository, UserRepository userRepository,
            JobRepository jobRepository) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
    }

    public Optional<Resume> fetchById(long id) {
        return this.resumeRepository.findById(id);
    }

    public boolean checkResumeExistByUserAndJob(Resume resume) throws IdInvalidException {
        // check user by id
        if (resume.getUser() == null) {
            return false;
        }
        Optional<User> userOptional = this.userRepository.findById(resume.getUser().getId());
        if (userOptional.isEmpty()) {
            return false;
        }
        // check job by id
        if (resume.getJob() == null) {
            return false;
        }
        Optional<Job> jobOptional = this.jobRepository.findById(resume.getJob().getId());
        if (jobOptional.isEmpty()) {
            return false;
        }
        // Check if the user has already applied for this job
        boolean alreadyApplied = this.resumeRepository.existsByUserIdAndJobId(
                resume.getUser().getId(),
                resume.getJob().getId()
        );
        if (alreadyApplied) {
            // Nếu muốn, có thể throw luôn 1 custom exception
            throw new IdInvalidException("Ứng viên đã ứng tuyển công việc này rồi.");
        }
        return true;
    }

    public ResCreateResumeDTO create(Resume resume) {
        resume = this.resumeRepository.save(resume);

        ResCreateResumeDTO res = new ResCreateResumeDTO();
        res.setId(resume.getId());
        res.setCreatedAt(resume.getCreatedAt());
        res.setCreatedBy(resume.getCreatedBy());

        return res;
    }

    public ResUpdateResumeDTO update(Resume resume) {
        resume = this.resumeRepository.save(resume);

        ResUpdateResumeDTO res = new ResUpdateResumeDTO();
        res.setCreatedAt(resume.getCreatedAt());
        res.setCreatedBy(resume.getCreatedBy());

        return res;
    }

    public void delete(long id) {
        Optional<Resume> resumeOptional = this.resumeRepository.findById(id);
        if (resumeOptional.isPresent()) {
            Resume resume = resumeOptional.get();
            resume.setActive(false);
            this.resumeRepository.save(resume);
        }
    }

    public ResFetchResumeDTO getResume(Resume resume) {
        ResFetchResumeDTO res = new ResFetchResumeDTO();
        res.setId(resume.getId());
        res.setEmail(resume.getEmail());
        res.setUrl(resume.getUrl());
        res.setStatus(resume.getStatus());
        res.setCreatedAt(resume.getCreatedAt());
        res.setCreatedBy(resume.getCreatedBy());
        res.setUpdatedAt(resume.getUpdatedAt());
        res.setUpdatedBy(resume.getUpdatedBy());
        if (resume.getJob() != null) {
            res.setCompanyName(resume.getJob().getCompany().getName());
        }
        res.setUser(new ResFetchResumeDTO.UserResume(resume.getUser().getId(), resume.getUser().getName()));
        res.setJob(new ResFetchResumeDTO.JobResume(resume.getJob().getId(), resume.getJob().getName()));

        return res;
    }

    public ResultPaginationDTO fetchAllResume(Specification<Resume> spec, Pageable pageable) {
        // Thêm điều kiện chỉ lấy các resume active
        Specification<Resume> activeSpec = (root, query, cb) -> cb.equal(root.get("active"), true);
        Specification<Resume> finalSpec = Specification.where(activeSpec).and(spec);
        
        Page<Resume> pageUser = this.resumeRepository.findAll(finalSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());

        rs.setMeta(mt);
        // remove sensitive data
        List<ResFetchResumeDTO> listResume = pageUser.getContent()
                .stream().map(item -> this.getResume(item))
                .collect(Collectors.toList());

        rs.setResult(listResume);

        return rs;
    }

    public ResultPaginationDTO fetchResumeByUser(Pageable pageable) {
        // query builder
        String email = SecurityUtil.getCurrentUserLogin().isPresent() == true ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        FilterNode node = filterParser.parse("email='" + email + "' and active=true");
        FilterSpecification<Resume> spec = filterSpecificationConverter.convert(node);
        Page<Resume> pageResume = this.resumeRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageResume.getTotalPages());
        mt.setTotal(pageResume.getTotalElements());

        rs.setMeta(mt);
        // remove sensitive data
        List<ResFetchResumeDTO> listResume = pageResume.getContent()
                .stream().map(item -> this.getResume(item))
                .collect(Collectors.toList());

        rs.setResult(listResume);

        return rs;
    }

    // Chạy mỗi 30 phút
    @Scheduled(cron = "0 */30 * * * *")
    public void autoDeactivateJobsWhenHiredFull() {
        List<Job> activeJobs = jobRepository.findByActiveTrue();
        for (Job job : activeJobs) {
            long hiredCount = resumeRepository.countByJobIdAndStatus(job.getId(), ResumeStateEnum.HIRED);
            if (hiredCount >= job.getQuantity()) {
                job.setActive(false);
                jobRepository.save(job);
                System.out.println("[Cronjob] Vô hiệu hóa job ID: " + job.getId() + " do đã tuyển đủ: " + hiredCount + "/" + job.getQuantity());
            }
        }
    }

}
