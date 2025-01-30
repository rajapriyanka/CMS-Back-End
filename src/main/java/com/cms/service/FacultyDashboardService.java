package com.cms.service;

import com.cms.entities.Course;
import com.cms.entities.Faculty;
import com.cms.entities.LeaveRequest;
import com.cms.repository.CourseRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FacultyDashboardService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private JavaMailSender emailSender;

    @Transactional
    public LeaveRequest submitLeaveRequest(Long facultyId, LeaveRequest leaveRequest) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        leaveRequest.setFaculty(faculty);
        leaveRequest.setStatus("Pending");
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        sendLeaveRequestEmail(savedRequest);
        return savedRequest;
    }

    private void sendLeaveRequestEmail(LeaveRequest leaveRequest) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("admin@example.com"); // Replace with actual admin email
        message.setSubject("New Leave Request");
        message.setText(String.format(
                "Faculty: %s\nDepartment: %s\nStart Date: %s\nEnd Date: %s\nReason: %s",
                leaveRequest.getFaculty().getName(),
                leaveRequest.getFaculty().getDepartment(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getReason()
        ));
        emailSender.send(message);
    }

    public List<Course> getFacultyCourses(Long facultyId) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        return courseRepository.findByDepartment(faculty.getDepartment());
    }

    public List<LeaveRequest> getFacultyLeaveRequests(Long facultyId) {
        return leaveRequestRepository.findByFacultyId(facultyId);
    }
}

