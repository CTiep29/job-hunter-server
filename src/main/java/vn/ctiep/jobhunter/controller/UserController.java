package vn.ctiep.jobhunter.controller;

import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import vn.ctiep.jobhunter.domain.User;
import vn.ctiep.jobhunter.domain.request.ReqChangePasswordDTO;
import vn.ctiep.jobhunter.domain.response.ResCreateUserDTO;
import vn.ctiep.jobhunter.domain.response.ResUpdateUserDTO;
import vn.ctiep.jobhunter.domain.response.ResUserDTO;
import vn.ctiep.jobhunter.domain.response.ResultPaginationDTO;
import vn.ctiep.jobhunter.service.UserService;
import vn.ctiep.jobhunter.util.annotation.ApiMessage;
import vn.ctiep.jobhunter.util.error.IdInvalidException;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("api/v1")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/users")
    @ApiMessage("Create a new user")
    public ResponseEntity<ResCreateUserDTO> createNewUser(@RequestBody User postManUser) throws IdInvalidException {
        boolean isEmailExist = this.userService.isEmailExist(postManUser.getEmail());
        if (isEmailExist) {
            throw new IdInvalidException(
                    "Email " + postManUser.getEmail() + "đã tồn tại, vui lòng sử dụng email khác.");
        }
        String hashPassword = this.passwordEncoder.encode(postManUser.getPassword());
        postManUser.setPassword(hashPassword);
        User newUser = this.userService.handleCreateUser(postManUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.convertToResCreateUserDTO(newUser));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") long id) throws IdInvalidException {
        User currentUser = this.userService.fetchUserById(id);
        if (currentUser == null) {
            throw new IdInvalidException("User với id = " + id + " không tồn tại");
        }
        this.userService.handleDeleteUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ResUserDTO> getUserById(@PathVariable("id") long id) throws IdInvalidException {
        User fetchUser = this.userService.fetchUserById(id);
        if (fetchUser == null) {
            throw new IdInvalidException("User với id = " + id + " không tồn tại");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.convertToResUserDTO(fetchUser));
    }

    @GetMapping("/users")
    @ApiMessage("fetch all users")
    public ResponseEntity<ResultPaginationDTO> getAllUser(@Filter Specification<User> spec,
            Pageable pageable) {

        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.fetchAllUser(spec, pageable));
    }

    @PutMapping("/users")
    public ResponseEntity<ResUpdateUserDTO> updateUser(@RequestBody User user) throws IdInvalidException {
        User currentUser = this.userService.handleUpdateUser(user);
        if (currentUser == null) {
            throw new IdInvalidException("User với id = " + user.getId() + " không tồn tại");
        }
        return ResponseEntity.ok().body(this.userService.convertToResUpdateUserDTO(currentUser));
    }
    @PostMapping("/users/change-password")
    @ApiMessage("Thay đổi mật khẩu")
    public ResponseEntity<?> changePassword(@RequestBody ReqChangePasswordDTO req) throws IdInvalidException{
        User currentUser = this.userService.fetchUserById(req.getUserId());
        if (currentUser == null) {
            throw new IdInvalidException("User với id = " + req.getUserId() + " không tồn tại");
        }
        boolean matches = passwordEncoder.matches(req.getOldPassword(), currentUser.getPassword());
        if (!matches){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mật khẩu cũ không chính xác");
        }
        currentUser.setPassword(passwordEncoder.encode(req.getNewPassword()));
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.handleChangePassword(currentUser));
    }

    @PutMapping("/users/{id}/restore")
    public ResponseEntity<User> restoreUser(@PathVariable long id) {
        User restoredUser = this.userService.restoreUser(id);
        if (restoredUser != null) {
            return ResponseEntity.ok(restoredUser);
        }
        return ResponseEntity.notFound().build();
    }
}
