package com.example.demo.service;

import com.example.demo.entity.Department;
import com.example.demo.entity.Member;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final DepartmentRepository departmentRepository;
    private final MemberRepository memberRepository;
    private final JavaMailSender mailSender;
    private final PptMergeService pptMergeService;

    @Value("${file.upload.path}")
    private String fileUploadPath;

    @Value("${report.temp.path")
    private String reportTempPath;

    /**
     * 모든 부서의 주간보고를 처리
     */
    @Transactional
    public void processWeeklyReports() {
        // 임시 디렉토리 생성
        createTempDirectory();

        // 모든 부서 가져오기
        List<Department> departments = departmentRepository.findAll();

        for (Department department : departments) {
            processDepartmentReport(department);
        }
    }

    /**
     * 부서별 주간보고 처리
     */
    private void processDepartmentReport(Department department) {
        // 이미 제출한 부서는 건너뜀
        if (Boolean.TRUE.equals(department.getSubmission())) {
            return;
        }

        // 부서 내 모든 멤버 조회
        List<Member> members = memberRepository.findByDepartment(department);

        // 제출 대상 멤버만 필터링
        List<Member> submissionMembers = members.stream()
                .filter(Member::getIsSubmissionRequired)
                .collect(Collectors.toList());

        // 제출 대상자가 없으면 건너뜀
        if (submissionMembers.isEmpty()) {
            return;
        }

        // 미제출 멤버 확인
        List<Member> nonSubmitters = submissionMembers.stream()
                .filter(member -> member.getIsSubmissionRequired() && member.getPptFilePath() == null || member.getPptFilePath().trim().isEmpty() )
                .collect(Collectors.toList());

        // 미제출 멤버에게 알림 메일 발송
        nonSubmitters.forEach(this::sendReminderEmail);

        // 모두 제출했으면 PPT 병합 후 관리자에게 메일 발송
        if (nonSubmitters.isEmpty()) {
            try {
                // Python 스크립트를 사용하여 PPT 병합
                String mergedPptPath = pptMergeService.mergePptFiles(department, submissionMembers);
                List<String> allFilePaths = copyAdditionalFiles(department, mergedPptPath);

                // 관리자에게 메일 발송
                sendAdminEmail(department, allFilePaths);

                // 제출 상태 업데이트
                department.setSubmission(true);
                departmentRepository.save(department);
            } catch (Exception e) {
                // 로그 기록
                log.error("Error processing department {}: {}", department.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 미제출 멤버에게 알림 메일 발송
     */
    private void sendReminderEmail(Member member) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(member.getEmail());
        message.setSubject("주간보고 미작성 안내");
        message.setText("주간보고가 제출되지 않았습니다. 주간보고 제출 바랍니다.");

        mailSender.send(message);
    }

    /**
     * PPT 파일 병합
     */
    private String mergePptFiles(Department department, List<Member> members) throws IOException {
        // 정렬 순서에 따라 멤버 정렬
        members.sort(Comparator.comparing(Member::getOrders));

        // 병합된 PPT를 저장할 경로 생성
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String mergedFileName = department.getName() + "_주간보고_" + timestamp + ".pptx";
        String mergedFilePath = Paths.get(reportTempPath, mergedFileName).toString();

        // 첫 번째 유효한 PPT 파일을 찾아서 기본 파일로 사용
        XMLSlideShow firstPpt = null;
        List<Member> remainingMembers = new ArrayList<>();

        for (Member member : members) {
            String pptFilePath = member.getPptFilePath();
            if (pptFilePath == null || pptFilePath.trim().isEmpty()) continue;

            File pptFile = new File(pptFilePath);
            if (!pptFile.exists()) continue;

            try (FileInputStream fis = new FileInputStream(pptFile)) {
                firstPpt = new XMLSlideShow(fis);
                break; // 첫 번째 유효한 파일을 찾으면 루프 종료
            } catch (Exception e) {
                // 로그 기록하고 다음 파일 시도
                System.err.println("Error reading first PPT file: " + e.getMessage());
            }
        }

        // 첫 번째 유효한 PPT가 없으면 새 빈 PPT 생성
        if (firstPpt == null) {
            firstPpt = new XMLSlideShow();
        }

        // 나머지 파일 처리
        for (int i = 0; i < members.size(); i++) {
            Member member = members.get(i);

            // 첫 번째 유효한 PPT를 이미 처리했으면 건너뜀
            if (firstPpt != null && i == 0) continue;

            String pptFilePath = member.getPptFilePath();
            if (pptFilePath == null || pptFilePath.trim().isEmpty()) continue;

            File pptFile = new File(pptFilePath);
            if (!pptFile.exists()) continue;

            try (FileInputStream fis = new FileInputStream(pptFile);
                 XMLSlideShow srcPpt = new XMLSlideShow(fis)) {

                // 슬라이드 마스터 정보 복사
                for (XSLFSlideMaster master : srcPpt.getSlideMasters()) {
                    // 마스터 슬라이드 정보를 직접 복사하는 로직은 복잡하므로
                    // 여기서는 슬라이드 자체만 복사
                }

                for (XSLFSlide srcSlide : srcPpt.getSlides()) {
                    // 슬라이드 복사 시 레이아웃도 함께 유지
                    XSLFSlide newSlide = firstPpt.createSlide();
                    newSlide.importContent(srcSlide);
                }
            } catch (Exception e) {
                System.err.println("Error merging PPT file: " + e.getMessage());
            }
        }

        // 최종 파일 저장
        try (FileOutputStream out = new FileOutputStream(mergedFilePath)) {
            firstPpt.write(out);
        } finally {
            if (firstPpt != null) {
                firstPpt.close();
            }
        }

        return mergedFilePath;
    }

    /**
     * 추가 파일 복사
     */
    private List<String> copyAdditionalFiles(Department department, String mergedPptPath) throws IOException {
        List<String> allFilePaths = new ArrayList<>();
        allFilePaths.add(mergedPptPath);

        // 부서의 추가 파일 복사
        for (String filePath : department.getAdditionalFilePaths()) {
            File srcFile = new File(filePath);
            if (srcFile.exists()) {
                String destFileName = srcFile.getName();
                Path destPath = Paths.get(reportTempPath, destFileName);
                Files.copy(srcFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
                allFilePaths.add(destPath.toString());
            }
        }

        return allFilePaths;
    }

    /**
     * 관리자에게 메일 발송
     */
    private void sendAdminEmail(Department department, List<String> filePaths) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo("sskim@secuwow.com");
        helper.setSubject(LocalDateTime.now().getMonthValue()+"월 " +getCurrentWeekNumber()+"주차 "+department.getName()+"주간보고 송부 件");
        helper.setText("안녕하세요\n" +
                "\n" +
                        "시큐와우 주간보고 관리 봇입니다.\n" +
                        "\n" +
                        "\n" +
                        "금주 작성한 "+department.getName()+" 주간보고 전달드립니다.\n" +
                        "\n\n" +
                        "감사합니다.");

        // 파일 첨부
        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (file.exists()) {
                FileSystemResource resource = new FileSystemResource(file);
                helper.addAttachment(file.getName(), resource);
            }
        }

        mailSender.send(message);
    }

    /**
     * 임시 디렉토리 생성
     */
    private void createTempDirectory() {
        File tempDir = new File(reportTempPath);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
    }

    public int getCurrentWeekNumber() {
        LocalDate today = LocalDate.now();

        // 한국은 기본 Locale이 ISO (월요일 시작)
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        return today.get(weekFields.weekOfMonth());
    }

    @Transactional
    public void clearReports() {
        // 1. 모든 Member 가져오기
        List<Member> members = memberRepository.findAll();
        for (Member member : members) {
            member.setPptFilePath(null);   // pptFilePath 초기화
            member.setPptFileTitle(null);  // pptFileTitle 초기화
        }
        memberRepository.saveAll(members); // 일괄 저장

        // 2. 모든 Department 가져오기
        List<Department> departments = departmentRepository.findAll();
        for (Department department : departments) {
            department.setSubmission(false);  // submission을 false로
            department.getAdditionalFilePaths().clear();   // 추가 파일 리스트 비우기
        }
        departmentRepository.saveAll(departments); // 일괄 저장

        log.info("All members' PPT info reset and all departments' submission set to false.");
    }
}