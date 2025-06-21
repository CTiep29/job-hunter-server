package vn.ctiep.jobhunter.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.turkraft.springfilter.boot.Filter;
import com.turkraft.springfilter.builder.FilterBuilder;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import jakarta.validation.Valid;
import vn.ctiep.jobhunter.domain.Company;
import vn.ctiep.jobhunter.domain.Job;
import vn.ctiep.jobhunter.domain.Resume;
import vn.ctiep.jobhunter.domain.User;
import vn.ctiep.jobhunter.domain.response.NotificationDTO;
import vn.ctiep.jobhunter.domain.response.ResultPaginationDTO;
import vn.ctiep.jobhunter.domain.response.resume.ResCreateResumeDTO;
import vn.ctiep.jobhunter.domain.response.resume.ResFetchResumeDTO;
import vn.ctiep.jobhunter.domain.response.resume.ResUpdateResumeDTO;
import vn.ctiep.jobhunter.repository.ResumeRepository;
import vn.ctiep.jobhunter.service.EmailService;
import vn.ctiep.jobhunter.service.ResumeService;
import vn.ctiep.jobhunter.service.UserService;
import vn.ctiep.jobhunter.service.WebSocketService;
import vn.ctiep.jobhunter.util.SecurityUtil;
import vn.ctiep.jobhunter.util.annotation.ApiMessage;
import vn.ctiep.jobhunter.util.constant.ResumeStateEnum;
import vn.ctiep.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("api/v1")
public class ResumeController {
    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);
    private final ResumeService resumeService;
    private final UserService userService;
    private final EmailService emailService;
    private final ResumeRepository resumeRepository;
    private final FilterBuilder filterBuilder;
    private final FilterSpecificationConverter filterSpecificationConverter;
    private final WebSocketService webSocketService;

    public ResumeController(
            ResumeService resumeService,
            UserService userService,
            EmailService emailService,
            ResumeRepository resumeRepository,
            FilterBuilder filterBuilder,
            FilterSpecificationConverter filterSpecificationConverter,
            WebSocketService webSocketService) {
        this.resumeService = resumeService;
        this.userService = userService;
        this.emailService = emailService;
        this.resumeRepository = resumeRepository;
        this.filterBuilder = filterBuilder;
        this.filterSpecificationConverter = filterSpecificationConverter;
        this.webSocketService = webSocketService;
    }

    @PostMapping("/resumes")
    @ApiMessage("Create a resume")
    public ResponseEntity<ResCreateResumeDTO> create(@Valid @RequestBody Resume resume) throws IdInvalidException {
        // check id exists
        boolean isIdExist = this.resumeService.checkResumeExistByUserAndJob(resume);
        if (!isIdExist) {
            throw new IdInvalidException("User id/Job id không tồn tại");
        }
        // create new resume
        return ResponseEntity.status(HttpStatus.CREATED).body(this.resumeService.create(resume));
    }

    @PutMapping("/resumes")
    @ApiMessage("Update a resume")
    public ResponseEntity<ResUpdateResumeDTO> update(@RequestBody Resume resume) throws IdInvalidException {
        // kiem tra resume co ton tai khong
        Optional<Resume> reqResumeOptional = this.resumeService.fetchById(resume.getId());
        if (reqResumeOptional.isEmpty()) {
            throw new IdInvalidException("Resume với id =" + resume.getId() + " không tồn tại");
        }

        // Lấy resume từ database để đảm bảo có đầy đủ thông tin
        Resume reqResume = reqResumeOptional.get();
        
        // Kiểm tra user có tồn tại không
        if (reqResume.getUser() == null) {
            throw new IdInvalidException("Không tìm thấy thông tin người dùng cho hồ sơ này");
        }

        //Kiem tra neu trang thai la HIRED
        if (resume.getStatus() == ResumeStateEnum.HIRED) {
            if (resume.getJob() == null) {
                resume.setJob(reqResume.getJob());
            }
            //Kiem tra so luong
            long hiredCount = resumeRepository.countByJobIdAndStatus(resume.getJob().getId(), ResumeStateEnum.HIRED);
            int jobQuantity = resume.getJob().getQuantity();
            if (hiredCount >= jobQuantity) {
                throw new IdInvalidException("Số lượng ứng viên đã đạt giới hạn cho công việc này.");
            }
        }

        // Cập nhật trạng thái
        reqResume.setStatus(resume.getStatus());

        // Tạo notification
        NotificationDTO notification = new NotificationDTO();
        notification.setType(resume.getStatus().toString());
        notification.setResumeId(reqResume.getId());
        notification.setUserId(reqResume.getUser().getId()); // Sử dụng user từ reqResume
        notification.setTimestamp(new Date());

        switch (resume.getStatus()) {
            case APPROVED:
                String confirmationUrl = "http://localhost:5173/" + reqResume.getId(); // frontend xử lý confirm
                this.emailService.sendInterviewInvitationEmail(
                        reqResume.getEmail(),
                        reqResume.getUser().getName(),
                        reqResume.getJob().getName(),
                        reqResume.getJob().getCompany().getName(),
                        confirmationUrl
                );
                notification.setMessage("Bạn đã được mời phỏng vấn cho vị trí " + reqResume.getJob().getName());
                break;
            case REJECTED:
                notification.setMessage("Đơn ứng tuyển của bạn đã bị từ chối");
                break;
            case PASSED:
                String confirmationUrl2 = "http://localhost:5173/" + reqResume.getId(); // frontend xử lý confirm
                this.emailService.sendInterviewPassedEmail(
                        reqResume.getEmail(),
                        reqResume.getUser().getName(),
                        reqResume.getJob().getName(),
                        reqResume.getJob().getCompany().getName(),
                        confirmationUrl2
                );
                notification.setMessage("Chức mừng! Bạn đã vượt qua vòng phỏng vấn cho vị trí " + reqResume.getJob().getName());
                break;
            case FAILED:
                this.emailService.sendInterviewFailedEmail(
                        reqResume.getEmail(),
                        reqResume.getUser().getName(),
                        reqResume.getJob().getName(),
                        reqResume.getJob().getCompany().getName()
                );
                notification.setMessage("Bạn đã không vượt qua vòng phỏng vấn cho vị trí " + reqResume.getJob().getName());
                break;
            case HIRED:
                this.emailService.sendHiredEmail(
                        reqResume.getEmail(),
                        reqResume.getUser().getName(),
                        reqResume.getJob().getName(),
                        reqResume.getJob().getCompany().getName()
                );
                notification.setMessage("Chúc mừng! Bạn đã được nhận vào vị trí " + reqResume.getJob().getName());
                break;
        }
        //log.info("Sending notification to user {}: {}", reqResume.getUser().getId(), notification);
        webSocketService.sendNotificationToUser(reqResume.getUser().getId(), notification);

        // update a resume
        ResUpdateResumeDTO updated = this.resumeService.update(reqResume);

        //Kiem tra xem có phải là người cuối tuyển không
        boolean isLastHire = false;
        if (resume.getStatus() == ResumeStateEnum.HIRED) {
            long hiredCountAfter = resumeRepository.countByJobIdAndStatus(resume.getJob().getId(), ResumeStateEnum.HIRED);
            int jobQuantity = resume.getJob().getQuantity();
            if (hiredCountAfter == jobQuantity) {
                isLastHire = true;
            }
        }
        
        ResUpdateResumeDTO res = new ResUpdateResumeDTO();
        res.setCreatedAt(updated.getCreatedAt());
        res.setCreatedBy(updated.getCreatedBy());
        res.setMessage(isLastHire ? "Đã tuyển đủ số lượng ứng viên cho công việc này." : null);

        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/resumes/{id}")
    @ApiMessage("Delete a resume")
    public ResponseEntity<Void> deleteResume(@PathVariable long id) {
        this.resumeService.handleDeleteResume(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/resumes/{id}/restore")
    @ApiMessage("Restore a soft-deleted resume")
    public ResponseEntity<Resume> restoreResume(@PathVariable long id) {
        Resume restoredResume = this.resumeService.restoreResume(id);
        if (restoredResume != null) {
            return ResponseEntity.ok(restoredResume);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/resumes/{id}")
    @ApiMessage("Fetch a resume by id")
    public ResponseEntity<ResFetchResumeDTO> fetchById(@PathVariable("id") long id) throws IdInvalidException {
        Optional<Resume> reqResumeOptional = this.resumeService.fetchById(id);
        if (reqResumeOptional.isEmpty()) {
            throw new IdInvalidException("Resume với id =" + id + " không tồn tại");
        }
        return ResponseEntity.ok().body(this.resumeService.getResume(reqResumeOptional.get()));
    }

    @GetMapping("/resumes")
    @ApiMessage("Fetch all resume with paginate")
    public ResponseEntity<ResultPaginationDTO> fetchAll(
            @Filter Specification<Resume> spec,
            Pageable pageable) {

        List<Long> arrJobIds = null;
        List<Long> arrUserIds = null;
        String email = SecurityUtil.getCurrentUserLogin().orElse("");

        User currentUser = this.userService.handleGetUserByUsername(email);

        if (currentUser != null) {
            Company userCompany = currentUser.getCompany();
            if (userCompany == null) {
            } else {

                List<Job> companyJobs = userCompany.getJobs();
                arrJobIds = (companyJobs != null) ? companyJobs.stream().map(Job::getId).collect(Collectors.toList()) : null;

                List<User> companyUsers = userCompany.getUsers();
                arrUserIds = (companyUsers != null) ? companyUsers.stream().map(User::getId).collect(Collectors.toList()) : null;
            }
        }

        Specification<Resume> jobInSpec = null;
        Specification<Resume> userInSpec = null;

        List<Long> finalArrJobIds = arrJobIds;
        List<Long> finalArrUserIds = arrUserIds;
        //Tao specification cho job
        if (finalArrJobIds != null && !finalArrJobIds.isEmpty()) {
            jobInSpec = (root, query, criteriaBuilder) -> root.get("job").get("id").in(finalArrJobIds);
        }
        //Tao specification cho user
        if (finalArrUserIds != null && !finalArrUserIds.isEmpty()) {
            userInSpec = (root, query, criteriaBuilder) -> root.get("user").get("id").in(finalArrUserIds);
        }


        Specification<Resume> companySpec = null;
        //Ket hop cac specification
        if (jobInSpec != null && userInSpec != null) {
            companySpec = jobInSpec.or(userInSpec);
        } else if (jobInSpec != null) {
            companySpec = jobInSpec;
        } else if (userInSpec != null) {
            companySpec = userInSpec;

        }
        //Ket hop với specification tu client
        Specification<Resume> finalSpec = Specification.where(companySpec).and(spec);

        return ResponseEntity.ok().body(this.resumeService.fetchAllResume(finalSpec, pageable));
    }

    @PostMapping("/resumes/by-user")
    @ApiMessage("Get list resumes by user")
    public ResponseEntity<ResultPaginationDTO> fetchResumeByUser(Pageable pageable) {

        return ResponseEntity.ok().body(this.resumeService.fetchResumeByUser(pageable));
    }


    @PutMapping("/resumes/{id}/confirm")
    @ApiMessage("Xác nhận tham gia phỏng vấn")
    public ResponseEntity<ResUpdateResumeDTO> confirmInterview(@PathVariable("id") long id) throws IdInvalidException {
        Optional<Resume> reqResumeOptional = this.resumeService.fetchById(id);
        if (reqResumeOptional.isEmpty()) {
            throw new IdInvalidException("Resume với id =" + id + " không tồn tại");
        }

        Resume reqResume = reqResumeOptional.get();

        // Chỉ cho phép xác nhận nếu trạng thái hiện tại là APPROVED
        if (reqResume.getStatus() != ResumeStateEnum.APPROVED) {
            throw new IdInvalidException("Chỉ có thể xác nhận phỏng vấn khi đơn đang ở trạng thái APPROVED");
        }

        // Cập nhật trạng thái
        reqResume.setStatus(ResumeStateEnum.INTERVIEW_CONFIRMED);

        // Cập nhật trong DB
        return ResponseEntity.ok().body(this.resumeService.update(reqResume));
    }

}
