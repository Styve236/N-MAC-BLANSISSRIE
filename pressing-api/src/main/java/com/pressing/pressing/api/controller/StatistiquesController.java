package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.dto.response.StatistiquesDTO;
import com.pressing.pressing.api.service.StatistiquesService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/statistiques")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
public class StatistiquesController {

    private final StatistiquesService statistiquesService;

    @GetMapping
    public StatistiquesDTO statistiques() {
        return statistiquesService.getStatistiques();
    }
}