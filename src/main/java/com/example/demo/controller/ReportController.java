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
}
