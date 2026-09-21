package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.entite.SmsNotification;
import com.pressing.pressing.api.repository.SmsNotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;




@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "SMS", description = "Envoi et historique des SMS")
public class SmsController {

    private final SmsNotificationRepository smsNotificationRepository;

    @GetMapping
    public List<SmsNotification> historique() {
        return smsNotificationRepository.findAll(Sort.by(Sort.Direction.DESC, "dateEnvoi"));
    }
}