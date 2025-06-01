package vn.ctiep.jobhunter.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.ctiep.jobhunter.domain.Company;
import vn.ctiep.jobhunter.domain.Resume;
import vn.ctiep.jobhunter.domain.Role;
import vn.ctiep.jobhunter.domain.User;
import vn.ctiep.jobhunter.domain.response.ResCreateUserDTO;
import vn.ctiep.jobhunter.domain.response.ResUpdateUserDTO;
import vn.ctiep.jobhunter.domain.response.ResUserDTO;
import vn.ctiep.jobhunter.domain.response.ResultPaginationDTO;
import vn.ctiep.jobhunter.repository.CompanyRepository;
import vn.ctiep.jobhunter.repository.ResumeRepository;
import vn.ctiep.jobhunter.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final RoleService roleService;
    private final ResumeRepository resumeRepository;
    private final CompanyRepository companyRepository;

    public UserService(UserRepository userRepository, CompanyService companyService, RoleService roleService, ResumeRepository resumeRepository, CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.companyService = companyService;
        this.roleService = roleService;
        this.resumeRepository = resumeRepository;
        this.companyRepository = companyRepository;
    }

    public User handleCreateUser(User user) {
        // check company
        if (user.getCompany() != null) {
            Optional<Company> companyOptional = this.companyService.findById(user.getCompany().getId());
            user.setCompany(companyOptional.isPresent() ? companyOptional.get() : null);
        }
        // check role
        if (user.getRole() != null) {
            Role r = this.roleService.fetchById(user.getRole().getId());
            user.setRole(r != null ? r : null);
        }
        return this.userRepository.save(user);
    }

    @Transactional
    public void handleDeleteUser(long id) {
        User user = this.fetchUserById(id);
        if (user != null) {
            // 1. Xóa mềm User
            user.setActive(false);
            
            // 2. Xóa mềm tất cả Resume của user
            if (user.getResumes() != null) {
                for (Resume resume : user.getResumes()) {
                    resume.setActive(false);
                    resumeRepository.save(resume);
                }
            }
            
            // 3. Nếu user là HR của company (role_id = 2), cần xử lý company
            if (user.getCompany() != null && user.getRole() != null && user.getRole().getId() == 2) {
                Company company = user.getCompany();
                // Kiểm tra xem còn HR nào khác trong company không
                List<User> otherHRs = userRepository.findByCompanyAndActiveTrue(company);
                otherHRs.remove(user); // Loại bỏ user hiện tại khỏi danh sách
                
                // Nếu không còn HR nào khác, xóa mềm company
                if (otherHRs.isEmpty()) {
                    company.setActive(false);
                    companyRepository.save(company);
                }
            }
            
            this.userRepository.save(user);
        }
    }

    public User fetchUserById(long id) {
        Optional<User> userOptional = this.userRepository.findById(id);
        if (userOptional.isPresent()) {
            return userOptional.get();
        }
        return null;
    }
    public User handleChangePassword(User user){
        return this.userRepository.save(user);
    }

    public ResultPaginationDTO fetchAllUser(Specification<User> spec, Pageable pageable) {
        Page<User> pageUser = this.userRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageUser.getContent());

        // remove sensitive data
        List<ResUserDTO> listUser = pageUser.getContent()
                .stream().map(item -> this.convertToResUserDTO(item))
                .collect(Collectors.toList());

        rs.setResult(listUser);

        return rs;
    }

    public User handleUpdateUser(User user) {
        User currentUser = this.fetchUserById(user.getId());
        if (currentUser != null) {
            currentUser.setAddress(user.getAddress());
            if (user.getGender() != null) {
                currentUser.setGender(user.getGender());
            }
            currentUser.setAge(user.getAge());
            currentUser.setName(user.getName());
            currentUser.setAvatar(user.getAvatar());
            currentUser.setCv(user.getCv());
            // check company
            if (user.getCompany() != null) {
                Optional<Company> companyOptional = this.companyService.findById(user.getCompany().getId());
                currentUser.setCompany(companyOptional.isPresent() ? companyOptional.get() : null);
            }
            // check role
            if (user.getRole() != null) {
                Role r = this.roleService.fetchById(user.getRole().getId());
                currentUser.setRole(r != null ? r : null);
            }
            // update
            currentUser = this.userRepository.save(currentUser);
        }
        return currentUser;
    }

    public User handleGetUserByUsername(String username) {
        return this.userRepository.findByEmail(username);
    }

    public boolean isEmailExist(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public ResCreateUserDTO convertToResCreateUserDTO(User user) {
        ResCreateUserDTO res = new ResCreateUserDTO();
        ResCreateUserDTO.CompanyUser com = new ResCreateUserDTO.CompanyUser();
        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setCreatedAt(user.getCreatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());

        if (user.getCompany() != null) {
            com.setId(user.getCompany().getId());
            com.setName(user.getCompany().getName());
            res.setCompany(com);
        }
        return res;
    }

    public ResUpdateUserDTO convertToResUpdateUserDTO(User user) {
        ResUpdateUserDTO res = new ResUpdateUserDTO();
        ResUpdateUserDTO.CompanyUser com = new ResUpdateUserDTO.CompanyUser();
        if (user.getCompany() != null) {
            com.setId(user.getCompany().getId());
            com.setName(user.getCompany().getName());
            res.setCompany(com);
        }
        res.setId(user.getId());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setUpdatedAt(user.getUpdatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());
        res.setAvatar(user.getAvatar());
        res.setCv(user.getCv());

        return res;
    }

    public ResUserDTO convertToResUserDTO(User user) {
        ResUserDTO res = new ResUserDTO();
        ResUserDTO.CompanyUser com = new ResUserDTO.CompanyUser();
        ResUserDTO.RoleUser roleUser = new ResUserDTO.RoleUser();
        if (user.getCompany() != null) {
            com.setId(user.getCompany().getId());
            com.setName(user.getCompany().getName());
            res.setCompany(com);
        }
        if (user.getRole() != null) {
            roleUser.setId(user.getRole().getId());
            roleUser.setName(user.getRole().getName());
            res.setRole(roleUser);
        }
        res.setActive(user.isActive());
        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setUpdatedAt(user.getUpdatedAt());
        res.setCreatedAt(user.getCreatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());
        return res;
    }

    public void updateUserToken(String token, String email) {
        User currentUser = this.handleGetUserByUsername(email);
        if (currentUser != null) {
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }

    public User getUserByRefreshTokenAndEmail(String token, String email) {
        return this.userRepository.findByRefreshTokenAndEmail(token, email);
    }
    @Transactional
    public User restoreUser(long id) {
        Optional<User> userOptional = this.userRepository.findByIdAndActiveFalse(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // 1. Khôi phục tất cả Resume của user
            if (user.getResumes() != null) {
                for (Resume resume : user.getResumes()) {
                    resume.setActive(true);
                    resumeRepository.save(resume);
                }
            }
            
            // 2. Nếu user là HR của company (role_id = 2), cần xử lý company
            if (user.getCompany() != null && user.getRole() != null && user.getRole().getId() == 2) {
                Company company = user.getCompany();
                // Kiểm tra xem còn HR nào khác trong company không
                List<User> otherHRs = userRepository.findByCompanyAndActiveTrue(company);
                
                // Nếu không còn HR nào khác và company đang bị xóa mềm, khôi phục company
                if (otherHRs.isEmpty() && !company.isActive()) {
                    company.setActive(true);
                    companyRepository.save(company);
                }
            }
            
            // 3. Khôi phục user
            user.setActive(true);
            return this.userRepository.save(user);
        }
        return null;
    }
}
