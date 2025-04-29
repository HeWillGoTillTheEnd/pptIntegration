package com.example.demo.schedule;

import com.example.demo.service.FileCleanupService;
import com.example.demo.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportScheduler {

    private final ReportService reportService;
    private final FileCleanupService fileCleanupService;

    @Value("${report.temp.path}")
    private String tempReportPath;

    @Value("${file.upload.path}")
    private String uploadPath;

    // 매주 금요일 오후 5시에 실행
    @Scheduled(cron = "0 0 17 * * FRI")
    public void processWeeklyReports() {
        reportService.processWeeklyReports();
    }

    // 수동 실행용 메소드
    public void manualProcessReports() {
        reportService.processWeeklyReports();
    }

    // 매주 월요일 오전 8시에 실행
    @Scheduled(cron = "0 0 8 * * MON")
    public void clearReports() {
        reportService.clearReports();
    }

    // 수동 실행용 메소드
    public void clearProcessReports() {
        reportService.clearReports();
    }

    /**
     * 3달 이상된 파일들을 삭제하는 스케줄러
     * (90일 = 3개월 기준)
     */
    @Scheduled(cron = "0 0 2 1 */3 *") // 3개월마다 1일 새벽 2시에 실행
    public void cleanOldFiles() {
        log.info("파일 정리 작업 시작");

        fileCleanupService.cleanDirectory(tempReportPath);
        fileCleanupService.cleanDirectory(uploadPath);

        log.info("파일 정리 작업 완료");
    }

    // 수동 실행용 메소드
    public void cleanFiles() {
        fileCleanupService.cleanDirectory(tempReportPath);
        fileCleanupService.cleanDirectory(uploadPath);
    }
}
