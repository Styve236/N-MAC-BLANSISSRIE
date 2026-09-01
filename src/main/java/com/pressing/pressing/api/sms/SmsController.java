package com.pressing.pressing.api.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsNotificationRepository smsNotificationRepository;

    @GetMapping
    public List<SmsNotification> historique() {
        return smsNotificationRepository.findAll(Sort.by(Sort.Direction.DESC, "dateEnvoi"));
    }
}