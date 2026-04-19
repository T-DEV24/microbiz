package com.microbiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/docs")
public class ApiDocsController {

    @GetMapping
    public String index() {
        return "api-docs";
    }
}
