package vn.ctiep.jobhunter.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String message;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "resume_id")
    private Long resumeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Date timestamp;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}
