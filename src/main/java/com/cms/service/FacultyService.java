package com.cms.service;

import com.cms.dto.FacultyRegistrationRequest;
import com.cms.dto.FacultyUpdateRequest;
import com.cms.entities.Faculty;
import com.cms.entities.User;
import com.cms.enums.UserRole;
import com.cms.repository.FacultyRepository;
import com.cms.repository.FacultyCourseRepository;
import com.cms.repository.TimetableEntryRepository;
import com.cms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FacultyService {

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TimetableEntryRepository timetableEntryRepository;
    
    @Autowired
    private FacultyCourseRepository facultyCourseRepository;

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
    public Faculty updateFacultyByUserId(Long userId, FacultyUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Faculty faculty = facultyRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Faculty not found for the given User ID"));

        faculty.setName(request.getName());
        faculty.setDepartment(request.getDepartment());
        faculty.setDesignation(request.getDesignation());
        faculty.setMobileNo(request.getMobileNo());

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getMobileNo()));

        userRepository.save(user);
        return facultyRepository.save(faculty);
    }


    @Transactional
    public void deleteFacultyByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Faculty faculty = facultyRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found for the given User ID"));

        // Delete related timetable entries first
        timetableEntryRepository.deleteByFaculty(faculty);

        // Delete faculty record
        facultyRepository.delete(faculty);

        // Delete associated user record
        userRepository.delete(user);
    }


}