package com.cms.service;

import com.cms.entities.Course;
import com.cms.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private ExcelService excelService;

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
    
    public List<Course> uploadCoursesFromExcel(MultipartFile file) throws IOException {
        List<Course> courses = excelService.extractCoursesFromExcel(file);
        List<Course> savedCourses = new ArrayList<>();
        
        for (Course course : courses) {
            // Skip courses with missing required fields
            if (course.getTitle() == null || course.getTitle().isEmpty() ||
                course.getCode() == null || course.getCode().isEmpty() ||
                course.getContactPeriods() == null || course.getSemesterNo() == null ||
                course.getDepartment() == null || course.getDepartment().isEmpty() ||
                course.getType() == null) {
                continue;
            }
            
            try {
                savedCourses.add(courseRepository.save(course));
            } catch (Exception e) {
                // Log the error and continue with the next course
                System.err.println("Error saving course: " + course.getCode() + " - " + e.getMessage());
            }
        }
        
        return savedCourses;
    }
}
