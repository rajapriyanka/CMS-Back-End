package com.cms.service;

import com.cms.dto.FacultyRegistrationRequest;
import com.cms.dto.FacultyUpdateRequest;
import com.cms.entities.Faculty;
import com.cms.entities.User;
import com.cms.enums.UserRole;
import com.cms.repository.FacultyRepository;
import com.cms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FacultyService {

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    public List<Faculty> searchFacultyByName(String name) {
        return facultyRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public Faculty registerFaculty(FacultyRegistrationRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getMobileNo()));
        user.setUserRole(UserRole.FACULTY);

        Faculty faculty = new Faculty();
        faculty.setName(request.getName());
        faculty.setDepartment(request.getDepartment());
        faculty.setDesignation(request.getDesignation());
        faculty.setMobileNo(request.getMobileNo());
        faculty.setUser(user);

        user.setFaculty(faculty);

        userRepository.save(user);
        return facultyRepository.save(faculty);
    }

    @Transactional
    public Faculty updateFaculty(Long id, FacultyUpdateRequest request) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        faculty.setName(request.getName());
        faculty.setDepartment(request.getDepartment());
        faculty.setDesignation(request.getDesignation());
        faculty.setMobileNo(request.getMobileNo());

        User user = faculty.getUser();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getMobileNo()));

        userRepository.save(user);
        return facultyRepository.save(faculty);
    }

    @Transactional
    public void deleteFaculty(Long id) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        User user = faculty.getUser();
        facultyRepository.delete(faculty);
        userRepository.delete(user);
    }
}
