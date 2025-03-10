package com.cms.service;

import com.cms.dto.StudentProfileUpdateRequest;
import com.cms.entities.Student;
import com.cms.entities.User;
import com.cms.enums.UserRole;
import com.cms.repository.StudentRepository;
import com.cms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ExcelService excelService;
    
    /**
     * Get student by email
     */
    public Student getStudentByEmail(String email) {
        if (Objects.isNull(email) || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must not be null or empty");
        }

        return studentRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found with email: " + email));
    }
    
    /**
     * Update student profile
     */
    @Transactional
    public Student updateStudentProfile(String email, StudentProfileUpdateRequest request) {
        Student student = getStudentByEmail(email);
        
        if (request.getName() != null && !request.getName().isEmpty()) {
            student.setName(request.getName());
        }
        
        if (request.getMobileNumber() != null && !request.getMobileNumber().isEmpty()) {
            student.setMobileNumber(request.getMobileNumber());
        }
        
        return studentRepository.save(student);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAllWithUsers();
    }

    public List<Student> searchStudentsByName(String name) {
        return studentRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public Student registerStudent(Student student) {
        if (student.getUser() == null || student.getUser().getEmail() == null || student.getUser().getPassword() == null) {
            throw new IllegalArgumentException("User, email, and password are required for student registration");
        }
        User user = new User();
        user.setName(student.getName());
        user.setEmail(student.getUser().getEmail());
        user.setPassword(passwordEncoder.encode(student.getUser().getPassword()));
        user.setUserRole(UserRole.STUDENT);

        user = userRepository.save(user);

        student.setUser(user);
        student.setMobileNumber(student.getMobileNumber());  // Ensure mobile number is set
        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudent(Long id, Student updatedStudent) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        student.setName(updatedStudent.getName());
        student.setDno(updatedStudent.getDno());
        student.setDepartment(updatedStudent.getDepartment());
        student.setBatchName(updatedStudent.getBatchName());
        student.setMobileNumber(updatedStudent.getMobileNumber());  // Ensure mobile number is updated

        User user = student.getUser();
        user.setName(updatedStudent.getName());
        user.setEmail(updatedStudent.getUser().getEmail());
        user.setPassword(passwordEncoder.encode(updatedStudent.getUser().getPassword()));

        userRepository.save(user);
        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        User user = student.getUser();
        studentRepository.delete(student);
        userRepository.delete(user);
    }

    @Transactional
    public List<Student> registerStudentsFromExcel(MultipartFile file) throws IOException {
        // Extract student data from the Excel file
        List<Student> students = excelService.extractStudentsFromExcel(file);
        List<Student> registeredStudents = new ArrayList<>();

        for (Student student : students) {
            // Create and set user details (email, password, role)
            User user = new User();
            user.setEmail(student.getUser().getEmail());
            user.setPassword(passwordEncoder.encode(student.getUser().getPassword()));
            user.setUserRole(UserRole.STUDENT);

            // Save the user (this will save the user to the database)
            user = userRepository.save(user);

            // Now, set the user and mobileNumber in the student object
            student.setUser(user);

            // Ensure the mobile number is being set and saved with the student
            if (student.getMobileNumber() != null && !student.getMobileNumber().isEmpty()) {
                student.setMobileNumber(student.getMobileNumber()); // This step ensures the mobile number is saved
            }

            // Save the student with the associated user (including mobile number) to the database
            registeredStudents.add(studentRepository.save(student));
        }

        return registeredStudents;
    }
    public List<Student> filterStudents(String department, String batchName) {
        return studentRepository.findAllWithUsers().stream()
                .filter(student -> {
                    boolean departmentMatch = (department == null || department.isEmpty()) || student.getDepartment().equalsIgnoreCase(department);
                    boolean batchMatch = (batchName == null || batchName.isEmpty()) || student.getBatchName().equalsIgnoreCase(batchName);
                    return departmentMatch && batchMatch;
                })
                .toList(); // Collect the filtered students into a list
    }
    public List<Student> getFilteredStudents(String department, String batchName) {
        return filterStudents(department, batchName);
    }

}
