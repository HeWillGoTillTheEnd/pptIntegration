package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class FileCleanupService {

    public void cleanDirectory(String dirPath) {
        File directory = new File(dirPath);

        if (!directory.exists() || !directory.isDirectory()) {
            log.warn("존재하지 않거나 디렉토리가 아닙니다: {}", dirPath);
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            log.warn("디렉토리 목록을 불러올 수 없습니다: {}", dirPath);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (File file : files) {
            if (file.isFile()) {
                Instant fileInstant = Instant.ofEpochMilli(file.lastModified());
                LocalDateTime fileDate = LocalDateTime.ofInstant(fileInstant, ZoneId.systemDefault());

                long daysBetween = ChronoUnit.DAYS.between(fileDate, now);
                if (daysBetween > 90) { // 90일 넘은 파일 삭제
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.info("삭제된 파일: {}", file.getAbsolutePath());
                    } else {
                        log.error("파일 삭제 실패: {}", file.getAbsolutePath());
                    }
                }
            }
        }
    }
}
