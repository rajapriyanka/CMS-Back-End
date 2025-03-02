package com.cms.service;

import com.cms.dto.FacultyCourseDTO;
import com.cms.entities.*;
import com.cms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FacultyCourseService {

    @Autowired
    private FacultyCourseRepository facultyCourseRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private BatchRepository batchRepository;

    /**
     * Fetches all available courses.
     */
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * Fetches all available batches.
     */
    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }

    @Transactional
    public void addCourseToBatch(Long facultyId, Long courseId, Long batchId) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        // Ensure faculty is assigned
        FacultyCourse facultyCourse = new FacultyCourse();
        facultyCourse.setFaculty(faculty);
        facultyCourse.setCourse(course);
        facultyCourse.setBatch(batch);

        facultyCourseRepository.save(facultyCourse);
    }

    @Transactional
    public FacultyCourse addCourse(Long facultyId, FacultyCourseDTO dto) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        // Ensure course exists before adding
        Course course = courseRepository.findByCode(dto.getCode())
                .orElseThrow(() -> new RuntimeException("Course not found. Please select an existing course."));

        // Ensure batch exists before adding
        Batch batch = batchRepository.findByBatchNameAndDepartmentAndSection(
                dto.getBatchName(), dto.getDepartment(), dto.getSection())
                .orElseThrow(() -> new RuntimeException("Batch not found. Please select an existing batch."));

        // Check if faculty is already assigned to the same course and batch
        if (facultyCourseRepository.findByFacultyIdAndCourseIdAndBatchId(facultyId, course.getId(), batch.getId()).isPresent()) {
            throw new RuntimeException("This course is already assigned to the faculty for the selected batch.");
        }

        // Assign faculty to the course and batch
        FacultyCourse facultyCourse = new FacultyCourse(faculty, course, batch);
        return facultyCourseRepository.save(facultyCourse);
    }

    @Transactional
    public void removeCourse(Long facultyId, Long courseId, Long batchId) {
        FacultyCourse facultyCourse = facultyCourseRepository.findByFacultyIdAndCourseIdAndBatchId(facultyId, courseId, batchId)
                .orElseThrow(() -> new RuntimeException("Faculty course not found"));
        facultyCourseRepository.delete(facultyCourse);
    }
}
