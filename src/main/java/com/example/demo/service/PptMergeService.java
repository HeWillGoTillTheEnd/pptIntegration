package com.example.demo.service;

import com.example.demo.entity.Department;
import com.example.demo.entity.Member;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j

public class PptMergeService {

    @Value("${report.temp.path}")
    private String reportTempPath;

    /**
     * 향상된 Apache POI 방식으로 PPT 파일들을 병합합니다.
     */
    public String mergePptFiles(Department department, List<Member> members) throws IOException {
        // 1. 정렬 순서에 따라 멤버 정렬
        members.sort(Comparator.comparing(Member::getOrders));

        // 2. 유효한 PPT 파일 경로 추출
        List<String> validPptFiles = members.stream()
                .filter(member -> member.getPptFilePath() != null && !member.getPptFilePath().trim().isEmpty())
                .map(Member::getPptFilePath)
                .filter(path -> new File(path).exists())
                .collect(Collectors.toList());

        if (validPptFiles.isEmpty()) {
            throw new IOException("No valid PPT files found for department: " + department.getName());
        }

        // 3. 출력 파일 경로 설정
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String mergedFileName = department.getName() + "_주간보고_" + timestamp + ".pptx";
        Path mergedFilePath = Paths.get(reportTempPath, mergedFileName);

        // 4. 임시 디렉토리 생성
        File tempDir = new File(reportTempPath);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        log.info("Merging {} PPT files for department {}", validPptFiles.size(), department.getName());

// basePpt.pptx 열기
        ClassPathResource basePptResource = new ClassPathResource("pptfile/basePpt.pptx");

        try (InputStream basePptInputStream = basePptResource.getInputStream();
             XMLSlideShow mergedPpt = new XMLSlideShow(basePptInputStream)) {

            List<XSLFSlide> baseSlides = mergedPpt.getSlides();
            if (baseSlides.isEmpty()) {
                throw new IOException("basePpt.pptx must have at least one slide!");
            }

            // basePpt의 첫 번째 슬라이드를 저장
            XSLFSlide baseSlideTemplate = baseSlides.get(0);

            for (String pptFilePath : validPptFiles) {
                log.info("Processing PPT file: {}", pptFilePath);
                File pptFile = new File(pptFilePath);

                try (FileInputStream fis = new FileInputStream(pptFile);
                     XMLSlideShow srcPpt = new XMLSlideShow(fis)) {

                    for (XSLFSlide srcSlide : srcPpt.getSlides()) {
                        // baseSlideTemplate을 복제한 새 슬라이드 생성
                        XSLFSlide newSlide = mergedPpt.createSlide(baseSlideTemplate.getSlideLayout());

                        // base 템플릿 내용 복사
                        newSlide.importContent(baseSlideTemplate);

                        // srcSlide의 내용으로 덮어쓰기 (이 부분은 선택적)
                        newSlide.importContent(srcSlide);
                    }
                } catch (Exception e) {
                    log.error("Error processing file {}: {}", pptFilePath, e.getMessage());
                }
            }

            // 🔥 병합 완료 후 첫 번째 템플릿 슬라이드 삭제
            mergedPpt.removeSlide(0);
            // 저장
            try (FileOutputStream out = new FileOutputStream(mergedFilePath.toFile())) {
                mergedPpt.write(out);
            }

            log.info("Successfully merged PPT files to: {}", mergedFilePath);
        } catch (Exception e) {
            log.error("Error during PPT merge: {}", e.getMessage(), e);
            throw new IOException("Failed to merge PPT files: " + e.getMessage(), e);
        }

        return mergedFilePath.toString();
    }
}