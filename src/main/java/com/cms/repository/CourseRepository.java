package com.cms.repository;

import com.cms.entities.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTitleContainingOrCodeContaining(String title, String code);
    List<Course> findByDepartment(String department);
}

