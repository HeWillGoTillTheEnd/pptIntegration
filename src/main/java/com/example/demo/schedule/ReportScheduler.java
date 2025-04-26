package com.example.demo.schedule;

import com.example.demo.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportScheduler {

    private final ReportService reportService;

    // 매주 금요일 오후 5시에 실행
    @Scheduled(cron = "0 0 17 * * FRI")
    public void processWeeklyReports() {
        reportService.processWeeklyReports();
    }

    // 수동 실행용 메소드
    public void manualProcessReports() {
        reportService.processWeeklyReports();
    }
}
