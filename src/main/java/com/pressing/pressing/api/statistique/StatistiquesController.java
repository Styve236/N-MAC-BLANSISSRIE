package com.pressing.pressing.api.statistique;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistiques")
@RequiredArgsConstructor
public class StatistiquesController {

    private final StatistiquesService statistiquesService;

    @GetMapping
    public StatistiquesDTO statistiques() {
        return statistiquesService.getStatistiques();
    }
}