package vn.ctiep.jobhunter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ctiep.jobhunter.domain.Company;
import vn.ctiep.jobhunter.domain.Role;
import vn.ctiep.jobhunter.domain.User;
import vn.ctiep.jobhunter.domain.request.GoogleOAuth2DTO;
import vn.ctiep.jobhunter.domain.request.ReqLoginDTO;
import vn.ctiep.jobhunter.domain.request.ReqRegisterRecruiterDTO;
import vn.ctiep.jobhunter.domain.response.ResCreateUserDTO;
import vn.ctiep.jobhunter.domain.response.ResLoginDTO;
import vn.ctiep.jobhunter.service.CompanyService;
import vn.ctiep.jobhunter.service.RoleService;
import vn.ctiep.jobhunter.service.UserService;
import vn.ctiep.jobhunter.util.SecurityUtil;
import vn.ctiep.jobhunter.util.annotation.ApiMessage;
import vn.ctiep.jobhunter.util.error.IdInvalidException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("api/v1")
public class AuthController {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    @Value("${ctiep.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;
    private final CompanyService companyService;
    private final RoleService roleService;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder, SecurityUtil securityUtil,
            UserService userService, PasswordEncoder passwordEncoder, CompanyService companyService,RoleService roleService) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.companyService = companyService;
        this.roleService = roleService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO logindDto) {
        // Nạp input gồm username/password vào Security
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                logindDto.getUsername(), logindDto.getPassword());
        // xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // set thông tin người dùng đăng nhập vào context (có thể sử dụng sau này)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResLoginDTO res = new ResLoginDTO();
        User currentUserDB = this.userService.handleGetUserByUsername(logindDto.getUsername());
        if (currentUserDB != null) {
            Long companyId = currentUserDB.getCompany() != null ? currentUserDB.getCompany().getId() : null;
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    currentUserDB.getId(),
                    currentUserDB.getEmail(),
                    currentUserDB.getName(),
                    currentUserDB.getRole(),
                    companyId,
                    currentUserDB.getAvatar(),
                    currentUserDB.getCv(),
                    currentUserDB.getGender(),
                    currentUserDB.getAddress(),
                    currentUserDB.getAge()
            );
            res.setUser(userLogin);
        }
        // create a token
        String access_token = this.securityUtil.createAccessToken(authentication.getName(), res);
        res.setAccessToken(access_token);

        // create refreshToken
        String refresh_token = this.securityUtil.createRefreshToken(logindDto.getUsername(), res);

        // update user
        this.userService.updateUserToken(refresh_token, logindDto.getUsername());

        // set cookies
        ResponseCookie resCookies = ResponseCookie.from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @GetMapping("/auth/account")
    @ApiMessage("fetch account")
    public ResponseEntity<ResLoginDTO.UserGetAccount> getAccount() {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User currentUserDB = this.userService.handleGetUserByUsername(email);
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin();
        ResLoginDTO.UserGetAccount userGetAccount = new ResLoginDTO.UserGetAccount();
        if (currentUserDB != null) {
            userLogin.setId(currentUserDB.getId());
            userLogin.setEmail(currentUserDB.getEmail());
            userLogin.setName(currentUserDB.getName());
            userLogin.setRole(currentUserDB.getRole());
            if (currentUserDB.getCompany() != null) {
                userLogin.setCompany_id(currentUserDB.getCompany().getId()); // Lấy companyId
            }
            // Gán thêm các thông tin mới:
            userLogin.setAvatar(currentUserDB.getAvatar());
            userLogin.setCv(currentUserDB.getCv());
            userLogin.setGender(currentUserDB.getGender());
            userLogin.setAge(currentUserDB.getAge());
            userLogin.setAddress(currentUserDB.getAddress());
            userGetAccount.setUser(userLogin);
        }
        return ResponseEntity.ok().body(userGetAccount);
    }


    @GetMapping("/auth/refresh")
    @ApiMessage("Get User by refresh token")
    public ResponseEntity<ResLoginDTO> getRefreshToken(
            @CookieValue(name = "refresh_token", defaultValue = "abc") String refresh_token)
            throws IdInvalidException {
        if (refresh_token.equals("abc")) {
            throw new IdInvalidException("Bạn không có refresh token ở cookie");
        }
        // Check valid
        Jwt decodedToken = this.securityUtil.checkValidRefreshToken(refresh_token);
        String email = decodedToken.getSubject();
        // check user by token + email
        User currentUser = this.userService.getUserByRefreshTokenAndEmail(refresh_token, email);
        if (currentUser == null) {
            throw new IdInvalidException("Refresh Token không hợp lệ");
        }
        // issue new token/set refresh token as cookies
        ResLoginDTO res = new ResLoginDTO();
        User currentUserDB = this.userService.handleGetUserByUsername(email);
        if (currentUserDB != null) {
            Long companyId = currentUserDB.getCompany() != null ? currentUserDB.getCompany().getId() : null;
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    currentUserDB.getId(),
                    currentUserDB.getEmail(),
                    currentUserDB.getName(),
                    currentUserDB.getRole(),
                    companyId,
                    currentUserDB.getAvatar(),
                    currentUserDB.getCv(),
                    currentUserDB.getGender(),
                    currentUserDB.getAddress(),
                    currentUserDB.getAge()
            );
            res.setUser(userLogin);
        }
        // create a token
        String access_token = this.securityUtil.createAccessToken(email, res);
        res.setAccessToken(access_token);

        // create refreshToken
        String new_refresh_token = this.securityUtil.createRefreshToken(email, res);

        // update user
        this.userService.updateUserToken(new_refresh_token, email);

        // set cookies
        ResponseCookie resCookies = ResponseCookie.from("refresh_token", new_refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @PostMapping("auth/logout")
    @ApiMessage("Logout User")
    public ResponseEntity<Void> logout() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";

        if (email.equals("")) {
            throw new IdInvalidException("Access Token không hợp lệ");
        }
        // update refresh token = null
        this.userService.updateUserToken(null, email);
        // remove refresh token cookie
        ResponseCookie deleteSpringCookie = ResponseCookie
                .from("refresh_token", null)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString())
                .body(null);
    }

    @PostMapping("/auth/register")
    @ApiMessage("Register a new user")
    public ResponseEntity<ResCreateUserDTO> register(@Valid @RequestBody User postManUser) throws IdInvalidException {
        boolean isEmailExist = this.userService.isEmailExist(postManUser.getEmail());
        if (isEmailExist) {
            throw new IdInvalidException(
                    "Email " + postManUser.getEmail() + "đã tồn tại, vui lòng sử dụng email khác.");
        }

        String hashPassword = this.passwordEncoder.encode(postManUser.getPassword());
        postManUser.setPassword(hashPassword);
        User ericUser = this.userService.handleCreateUser(postManUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.convertToResCreateUserDTO(ericUser));
    }
    @PostMapping("/auth/register-recruiter")
    @ApiMessage("Register recruiter with company info")
    public ResponseEntity<ResCreateUserDTO> registerRecruiter(
            @Valid @RequestBody ReqRegisterRecruiterDTO dto) throws IdInvalidException {

        if (userService.isEmailExist(dto.getEmail())) {
            throw new IdInvalidException("Email " + dto.getEmail() + " đã tồn tại.");
        }

        // Tạo mới company
        Company company = new Company();
        company.setName(dto.getCompanyName());
        company.setAddress(dto.getCompanyAddress());
        company.setCreatedBy(dto.getEmail());

        company = companyService.handleCreateCompany(company);

        // Tạo user
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setCompany(company);

        // Gán role HR (id = 2)
        Role hrRole = roleService.fetchById(2L);
        if (hrRole == null) {
            throw new IdInvalidException("Role nhà tuyển dụng không tồn tại.");
        }
        user.setRole(hrRole);
        User savedUser = userService.handleCreateUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.convertToResCreateUserDTO(savedUser));
    }
    @PostMapping("/auth/oauth2-login")
    @ApiMessage("Login with Google OAuth2")
    public ResponseEntity<ResLoginDTO> loginWithGoogle(@RequestBody Map<String, String> body)
            throws GeneralSecurityException, IOException, IdInvalidException {

        String idTokenString = body.get("credential");
        if (idTokenString == null || idTokenString.isEmpty()) {
            throw new IdInvalidException("ID Token không được để trống");
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList("323789704789-fvlghj1e9ajvrt8n1gpv5gpmf77k36a4.apps.googleusercontent.com"))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new IdInvalidException("ID Token không hợp lệ");
        }

        Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        if (!emailVerified) {
            throw new IdInvalidException("Email chưa được xác minh bởi Google");
        }

        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setAvatar(picture);
            user.setPassword(passwordEncoder.encode("123456"));
            user = userService.handleCreateUser(user);
        }

        Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                companyId,
                user.getAvatar(),
                user.getCv(),
                user.getGender(),
                user.getAddress(),
                user.getAge()
        );

        ResLoginDTO res = new ResLoginDTO();
        res.setUser(userLogin);

        String accessToken = securityUtil.createAccessToken(user.getEmail(), res);
        String refreshToken = securityUtil.createRefreshToken(user.getEmail(), res);
        userService.updateUserToken(refreshToken, user.getEmail());

        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        res.setAccessToken(accessToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(res);
    }

}
