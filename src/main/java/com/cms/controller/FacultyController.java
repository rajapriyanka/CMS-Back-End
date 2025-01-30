package com.cms.controller;

import com.cms.dto.FacultyDTO;
import com.cms.dto.FacultyRegistrationRequest;
import com.cms.dto.FacultyUpdateRequest;
import com.cms.entities.Faculty;
import com.cms.service.ExcelService;
import com.cms.service.FacultyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    @Autowired
    private FacultyService facultyService;


    @Autowired
    private ExcelService excelService;
    
   

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFaculty(@RequestParam("file") MultipartFile file) {
        try {
            List<Faculty> faculties = excelService.extractFacultyFromExcel(file);
            for (Faculty faculty : faculties) {
                facultyService.registerFaculty(new FacultyRegistrationRequest(
                    faculty.getName(),
                    faculty.getUser().getEmail(),
                    faculty.getUser().getPassword(),
                    faculty.getDepartment(),
                    faculty.getDesignation(),
                    faculty.getMobileNo()
                ));
            }
            return ResponseEntity.ok("Faculty members uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error uploading faculty members: " + e.getMessage());
        }
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<FacultyDTO>> getAllFaculty() {
        List<Faculty> faculties = facultyService.getAllFaculty();
        List<FacultyDTO> facultyDTOs = faculties.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(facultyDTOs);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<FacultyDTO>> searchFacultyByName(@RequestParam String name) {
        List<Faculty> faculties = facultyService.searchFacultyByName(name);
        List<FacultyDTO> facultyDTOs = faculties.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(facultyDTOs);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FacultyDTO> registerFaculty(@RequestBody FacultyRegistrationRequest request) {
        Faculty faculty = facultyService.registerFaculty(request);
        return ResponseEntity.ok(convertToDTO(faculty));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<FacultyDTO> updateFaculty(@PathVariable Long id, @RequestBody FacultyUpdateRequest request) {
        Faculty faculty = facultyService.updateFaculty(id, request);
        return ResponseEntity.ok(convertToDTO(faculty));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFaculty(@PathVariable Long id) {
        facultyService.deleteFaculty(id);
        return ResponseEntity.ok().build();
    }

    private FacultyDTO convertToDTO(Faculty faculty) {
        return new FacultyDTO(
            faculty.getId(),
            faculty.getName(),
            faculty.getUser().getEmail(),
            faculty.getDepartment(),
            faculty.getDesignation(),
            faculty.getMobileNo()
        );
    }
}

