package vn.ctiep.jobhunter.domain.request;

import lombok.Data;

@Data
public class GoogleOAuth2DTO {
    private String email;
    private String name;
    private String picture;
}

