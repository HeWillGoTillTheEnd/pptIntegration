package com.example.demo.service;

import com.example.demo.entity.Department;
import com.example.demo.entity.Member;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${report.temp.path:./temp}")
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

        // 5. 슬라이드쇼 생성 및 병합
        try (XMLSlideShow mergedPpt = new XMLSlideShow()) {
            // 각 PPT 파일 처리
            for (String pptFilePath : validPptFiles) {
                log.info("Processing PPT file: {}", pptFilePath);
                File pptFile = new File(pptFilePath);

                try (FileInputStream fis = new FileInputStream(pptFile);
                     XMLSlideShow srcPpt = new XMLSlideShow(fis)) {

                    // 마스터 슬라이드 및 테마 정보 복사 시도
                    // 참고: 완벽한 복사는 이 방법으로도 불가능할 수 있음

                    // 모든 슬라이드 복사
                    for (XSLFSlide srcSlide : srcPpt.getSlides()) {
                        XSLFSlide newSlide = mergedPpt.createSlide();
                        // 슬라이드 내용 가져오기 - 기본 레이아웃도 최대한 유지 시도
                        newSlide.importContent(srcSlide);
                    }
                } catch (Exception e) {
                    // 개별 파일 처리 오류 기록하고 계속 진행
                    log.error("Error processing file {}: {}", pptFilePath, e.getMessage());
                }
            }

            // 6. 저장
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