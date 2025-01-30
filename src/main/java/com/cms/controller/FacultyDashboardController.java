package com.cms.controller;

import com.cms.entities.Course;
import com.cms.entities.LeaveRequest;
import com.cms.service.FacultyDashboardService;
import com.cms.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculty")
public class FacultyDashboardController {

    @Autowired
    private FacultyDashboardService facultyDashboardService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/leave-request")
    public ResponseEntity<?> submitLeaveRequest(Authentication authentication, @RequestBody LeaveRequest leaveRequest) {
        Long facultyId = getFacultyIdFromAuthentication(authentication);
        LeaveRequest submittedRequest = facultyDashboardService.submitLeaveRequest(facultyId, leaveRequest);
        return ResponseEntity.ok(submittedRequest);
    }

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getFacultyCourses(Authentication authentication) {
        Long facultyId = getFacultyIdFromAuthentication(authentication);
        List<Course> courses = facultyDashboardService.getFacultyCourses(facultyId);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/leave-requests")
    public ResponseEntity<List<LeaveRequest>> getFacultyLeaveRequests(Authentication authentication) {
        Long facultyId = getFacultyIdFromAuthentication(authentication);
        List<LeaveRequest> leaveRequests = facultyDashboardService.getFacultyLeaveRequests(facultyId);
        return ResponseEntity.ok(leaveRequests);
    }

    private Long getFacultyIdFromAuthentication(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        return jwtUtil.extractFacultyId(token);
    }
}

