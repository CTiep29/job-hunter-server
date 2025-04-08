package vn.ctiep.jobhunter.domain.request;

import lombok.Data;

@Data
public class ReqChangePasswordDTO {
    private Long userId;
    private String oldPassword;
    private String newPassword;
}
