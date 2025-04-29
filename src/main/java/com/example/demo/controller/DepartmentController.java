package com.example.demo.controller;


import com.example.demo.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    // 파일 업로드 폼 화면
    @GetMapping("/departments/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "uploadForm";
    }

    @PostMapping("/departments/upload")
    public String uploadAdditionalFile(@RequestParam("departmentId") Long id,
                                       @RequestParam("file") MultipartFile file) {
        try {
            departmentService.uploadAdditionalFileV2(id, file);
            return "redirect:/departments/upload?success";
        } catch (Exception e) {
            return "redirect:/departments/upload?error";
        }
    }
}
