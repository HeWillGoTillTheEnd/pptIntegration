package com.example.demo.service;

import com.example.demo.entity.Department;
import com.example.demo.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    @Autowired
    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Optional<Department> getDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }

    public Optional<Department> getDepartmentByName(String name) {
        return departmentRepository.findByName(name);
    }

    @Transactional
    public Department createDepartment(String name) {
        Department department = new Department(name);
        return departmentRepository.save(department);
    }

    @Transactional
    public Department getOrCreateDepartment(String name) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> createDepartment(name));
    }

    @Transactional
    public void toggleSubmission(Long departmentId) {
        departmentRepository.findById(departmentId).ifPresent(department -> {
            department.setSubmission(!department.getSubmission());
            departmentRepository.save(department);
        });
    }

    @Transactional
    public void addFilePath(Long departmentId, String filePath) {
        departmentRepository.findById(departmentId).ifPresent(department -> {
            department.getAdditionalFilePaths().add(filePath);
            departmentRepository.save(department);
        });
    }
}
