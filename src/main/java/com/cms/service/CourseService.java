package com.cms.service;

import com.cms.entities.Course;
import com.cms.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    public Course registerCourse(Course course) {
        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, Course updatedCourse) {
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (updatedCourse.getTitle() != null && !updatedCourse.getTitle().trim().isEmpty()) {
            existingCourse.setTitle(updatedCourse.getTitle().trim());
        }
        if (updatedCourse.getCode() != null && !updatedCourse.getCode().trim().isEmpty()) {
            existingCourse.setCode(updatedCourse.getCode().trim());
        }
        if (updatedCourse.getContactPeriods() != null && updatedCourse.getContactPeriods() > 0) {
            existingCourse.setContactPeriods(updatedCourse.getContactPeriods());
        }
        if (updatedCourse.getSemesterNo() != null && updatedCourse.getSemesterNo() > 0) {
            existingCourse.setSemesterNo(updatedCourse.getSemesterNo());
            
        }
        if (updatedCourse.getDepartment() != null && !updatedCourse.getDepartment().trim().isEmpty()) {
            existingCourse.setDepartment(updatedCourse.getDepartment().trim());
        }
        if (updatedCourse.getType() != null) {
            existingCourse.setType(updatedCourse.getType());
        }

        return courseRepository.save(existingCourse);
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public List<Course> searchCourses(String query) {
        return courseRepository.findByTitleContainingOrCodeContaining(query, query);
    }
}

