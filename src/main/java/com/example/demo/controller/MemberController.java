package com.example.demo.controller;


import com.example.demo.entity.Department;
import com.example.demo.entity.Member;
import com.example.demo.service.DepartmentService;
import com.example.demo.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final DepartmentService departmentService;

    // 멤버 목록 페이지 표시
    @GetMapping("/members")
    public String showMemberList(Model model) {
        List<Member> members = memberService.getAllMembers();
        List<Department> departments = departmentService.getAllDepartments();
        model.addAttribute("members", members);
        model.addAttribute("departments", departments);
        return "member-list";
    }

    // 멤버 등록 페이지 표시
    @GetMapping("/members/new")
    public String showMemberForm(Model model) {
        List<Member> members  = memberService.getAllMembers();
        List<Department> departments = departmentService.getAllDepartments();
        model.addAttribute("members", members);
        model.addAttribute("departments", departments);
        return "member-form";
    }

    // 멤버 등록 처리
    @PostMapping("/members")
    public String createMember(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("departmentName") String departmentName,
            @RequestParam("position") String position,
            @RequestParam(value = "orders", required = false) Integer orders,
            @RequestParam(value = "isSubmissionRequired", required = false, defaultValue = "false") Boolean isSubmissionRequired,
            @RequestParam(value = "pptFile", required = false) MultipartFile pptFile,
            RedirectAttributes redirectAttributes) {

        try {
            Member member = memberService.createMember(name, email, departmentName, position, orders, isSubmissionRequired);

            if (pptFile != null && !pptFile.isEmpty()) {
                memberService.submitPptFile(email, pptFile);
            }

            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("message", "멤버가 성공적으로 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("success", false);
            redirectAttributes.addFlashAttribute("message", "멤버 등록 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/members";
    }

    // PPT 제출 페이지 표시
    @GetMapping("/submit-ppt")
    public String showSubmitPage() {
        return "ppt-submit";
    }

    // 제출 대상 상태 변경 페이지 표시
    @GetMapping("/submission-status")
    public String showSubmissionStatusPage() {
        return "submission-status";
    }

    // PPT 파일 제출 처리
    @PostMapping("/submit-ppt")
    public String handlePptSubmission(
            @RequestParam("email") String email,
            @RequestParam("pptFile") MultipartFile pptFile,
            RedirectAttributes redirectAttributes) {

        if (pptFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("success", false);
            redirectAttributes.addFlashAttribute("message", "제출할 PPT 파일을 선택해주세요.");
            return "redirect:/submit-ppt";
        }

        try {
            boolean result = memberService.submitPptFile(email, pptFile);

            if (result) {
                redirectAttributes.addFlashAttribute("success", true);
                redirectAttributes.addFlashAttribute("message", "PPT 파일이 성공적으로 제출되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("success", false);
                redirectAttributes.addFlashAttribute("message", "등록되지 않은 이메일입니다.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("success", false);
            redirectAttributes.addFlashAttribute("message", "파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/submit-ppt";
    }

    // 제출 대상 상태 변경 처리
    @PostMapping("/toggle-submission-status")
    public String toggleSubmissionStatus(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        try {
            boolean result = memberService.toggleSubmissionRequired(email);

            if (result) {
                redirectAttributes.addFlashAttribute("success", true);
                redirectAttributes.addFlashAttribute("message", "제출 대상 상태가 성공적으로 변경되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("success", false);
                redirectAttributes.addFlashAttribute("message", "등록되지 않은 이메일입니다.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("success", false);
            redirectAttributes.addFlashAttribute("message", "상태 변경 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/submission-status";
    }

    // 멤버 삭제 폼 페이지 표시
    @GetMapping("/members/delete")
    public String showDeleteMemberForm(Model model) {
        List<Member> members = memberService.getAllMembers();
        model.addAttribute("members", members);
        return "deleteMember";
    }

    // 멤버 삭제 처리
    @PostMapping("/members/delete")
    public String deleteMember(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            memberService.deleteMember(id);
            redirectAttributes.addFlashAttribute("successMessage", "멤버가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "멤버 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/members";
    }
}
