package com.cms.repository;

import com.cms.entities.FacultyCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyCourseRepository extends JpaRepository<FacultyCourse, Long> {

	 Optional<FacultyCourse> findByFacultyIdAndCourseIdAndBatchId(Long facultyId, Long courseId, Long batchId);

	List<FacultyCourse> findByFacultyId(Long facultyId);
}

