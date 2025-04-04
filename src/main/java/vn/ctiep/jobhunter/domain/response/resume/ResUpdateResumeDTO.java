package vn.ctiep.jobhunter.domain.response.resume;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class ResUpdateResumeDTO {
    private Instant createdAt;
    private String createdBy;
}
