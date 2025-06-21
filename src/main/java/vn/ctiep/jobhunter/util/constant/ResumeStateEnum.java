package vn.ctiep.jobhunter.util.constant;

public enum ResumeStateEnum {
    PENDING,            //Đang chờ duyệt
    REVIEWING,          //Đang xem xét
    APPROVED,           //Mời phỏng vấn
    REJECTED,           //Từ chối
    INTERVIEW_CONFIRMED,//Xác nhận tham gia phỏng vấn
    INTERVIEW_REJECTED, // Ứng viên từ chối tham gia phỏng vấn
    FAILED,             // Ứng viên không đạt yêu cầu sau phỏng vấn
    PASSED,             //Đạt yêu cầu sau phỏng vấn
    JOB_ACCEPTED,       //Xác nhận nhận việc
    JOB_REJECTED,       //Từ chối nhận việc
    HIRED               //Đã Tuyển
}
