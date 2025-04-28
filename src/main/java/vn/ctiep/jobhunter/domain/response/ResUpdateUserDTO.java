package vn.ctiep.jobhunter.domain.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.ctiep.jobhunter.util.constant.GenderEnum;

@Getter
@Setter
public class ResUpdateUserDTO {
    private long id;
    private String name;
    private GenderEnum gender;
    private String address;
    private int age;
    private Instant updatedAt;
    private CompanyUser company;
    private String avatar;
    private String cv;
    @Getter
    @Setter
    public static class CompanyUser {
        private long id;
        private String name;

    }
}
