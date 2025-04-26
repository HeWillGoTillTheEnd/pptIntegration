package com.example.demo.service;

import com.example.demo.entity.Department;
import com.example.demo.entity.Member;
import com.example.demo.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final DepartmentService departmentService;

    private String fileUploadPath = "C:\\Users\\Legion\\Desktop\\testt\\demo\\src\\main\\resources\\files";

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Transactional
    public Member createMember(String name, String email, String departmentName, String position, Integer order, Boolean isSubmissionRequired) {
        // 부서 찾거나 생성
        Department department = departmentService.getOrCreateDepartment(departmentName);

        Member member = new Member(name, email, department, position);
        if (order != null) {
            member.setOrders(order);
        }

        if (isSubmissionRequired != null) {
            member.setIsSubmissionRequired(isSubmissionRequired);
        }

        return memberRepository.save(member);
    }

    @Transactional
    public boolean submitPptFile(String email, MultipartFile pptFile) throws IOException {
        Optional<Member> memberOptional = memberRepository.findByEmail(email);

        if (memberOptional.isEmpty()) {
            return false;
        }

        Member member = memberOptional.get();

        // 파일 저장 디렉토리 확인 및 생성
        File directory = new File(fileUploadPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 파일명 생성
        String originalFilename = pptFile.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String newFilename = member.getName() + "_" + timestamp + fileExtension;

        Path filePath = Paths.get(fileUploadPath, newFilename);

        // 파일 복사 - 스트림을 명시적으로 닫도록 수정
        try (InputStream inputStream = pptFile.getInputStream();
             OutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        // 파일 경로와 파일 제목을 각각 저장
        member.setPptFilePath(filePath.toString().replace("\\", "/"));
        member.setPptFileTitle(originalFilename);

        memberRepository.save(member);

        return true;
    }

    @Transactional
    public boolean toggleSubmissionRequired(String email) {
        Optional<Member> memberOptional = memberRepository.findByEmail(email);

        if (memberOptional.isEmpty()) {
            return false;
        }

        Member member = memberOptional.get();
        // 현재 상태의 반대로 설정
        member.setIsSubmissionRequired(!member.getIsSubmissionRequired());
        memberRepository.save(member);

        return true;
    }

    /**
     * ID로 멤버 삭제
     */
    @Transactional
    public void deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다: " + id));

        // 파일이 있다면 파일도 삭제
        if (member.getPptFilePath() != null && !member.getPptFilePath().isEmpty()) {
            File pptFile = new File(member.getPptFilePath());
            if (pptFile.exists()) {
                pptFile.delete();
            }
        }

        memberRepository.delete(member);
    }

}
