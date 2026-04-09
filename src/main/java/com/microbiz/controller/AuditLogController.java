package com.microbiz.controller;

import com.microbiz.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        Model model) {
        int sizeSafe = Math.min(Math.max(5, size), 100);
        var logsPage = auditLogService.findAll(
                PageRequest.of(Math.max(0, page), sizeSafe, Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("logsPage", logsPage);
        model.addAttribute("logs", logsPage.getContent());
        model.addAttribute("size", sizeSafe);
        return "audit-logs";
    }
}
