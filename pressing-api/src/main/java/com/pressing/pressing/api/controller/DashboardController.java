package com.pressing.pressing.api.controller;

import com.pressing.pressing.api.dto.response.DashboardResumeDTO;
import com.pressing.pressing.api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tableau de bord", description = "Resume d'activite")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resume")
    public DashboardResumeDTO resume() {
        return dashboardService.getResume();
    }
}