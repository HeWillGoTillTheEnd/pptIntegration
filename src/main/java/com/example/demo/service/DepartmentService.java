package com.example.demo.service;

import com.example.demo.entity.Department;
import com.example.demo.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    @Value("${file.upload.path}")
    private String fileUploadPath;
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
    @Transactional
    public boolean uploadAdditionalFileV2(Long departmentId, MultipartFile file) throws IOException {
        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);

        if (departmentOptional.isEmpty()) {
            return false;
        }

        Department department = departmentOptional.get();

        // 저장 디렉토리 확인 및 생성
        File directory = new File(fileUploadPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new IOException("디렉토리 생성 실패: " + fileUploadPath);
            }
        }

// 파일명 구성
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IOException("파일 이름이 유효하지 않습니다.");
        }

// 파일명과 확장자 분리
        int dotIndex = originalFilename.lastIndexOf(".");
        String namePart = (dotIndex != -1) ? originalFilename.substring(0, dotIndex) : originalFilename;
        String extension = (dotIndex != -1) ? originalFilename.substring(dotIndex) : "";

// 타임스탬프 붙이기
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String newFilename = namePart + "_" + timestamp + extension;

        Path filePath = Paths.get(fileUploadPath, newFilename);

        // 파일 복사
        try (InputStream inputStream = file.getInputStream();
             OutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        // 파일 경로 저장
        department.getAdditionalFilePaths().add(filePath.toString().replace("\\", "/"));
        departmentRepository.save(department);

        return true;
    }

}
