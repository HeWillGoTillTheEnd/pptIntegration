package com.example.demo.controller;

import com.example.demo.schedule.ReportScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportScheduler reportScheduler;

    @PostMapping("/admin/process-reports")
    public ResponseEntity<String> processReports() {
        reportScheduler.manualProcessReports();
        return ResponseEntity.ok("주간보고 처리가 시작되었습니다.");
    }

    @PostMapping("/admin/clear-reports")
    public ResponseEntity<String> clearProcessReports() {
        reportScheduler.clearProcessReports();
        return ResponseEntity.ok("주간 보고 삭제로직이 시작되었습니다.");
    }

    @PostMapping("/admin/clear-files")
    public ResponseEntity<String> clearProcessFiles() {
        reportScheduler.cleanFiles();
        return ResponseEntity.ok("내부 파일 삭제 로직이 시작되었습니다.");
    }
}
