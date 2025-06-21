package vn.ctiep.jobhunter.domain.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class NotificationDTO {
    private String type; // APPROVED, REJECTED, HIRED,
    private String message;
    private String jobName;
    private String companyName;
    private Long resumeId;
    private Long userId;
    private Date timestamp;
}
