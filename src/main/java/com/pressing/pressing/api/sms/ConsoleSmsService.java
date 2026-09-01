package com.pressing.pressing.api.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsoleSmsService implements SmsService {

    private final SmsProperties smsProperties;
    private final SmsNotificationRepository smsNotificationRepository;

    @Override
    public void envoyer(TypeNotificationSms type, String telephone, String message) {
        if (!smsProperties.isEnabled()) {
            return;
        }

        String numero = TelephoneUtils.normaliser(telephone);
        if (numero == null) {
            log.warn("[SMS {}] ECHEC : numéro de téléphone invalide ({})", type, telephone);
            enregistrer(type, telephone, message, StatutEnvoiSms.ECHEC);
            return;
        }

        log.info("[SMS {}] vers {} : {}", type, numero, message);
        enregistrer(type, numero, message, StatutEnvoiSms.ENVOYE);
    }

    private void enregistrer(TypeNotificationSms type, String telephone, String message, StatutEnvoiSms statut) {
        smsNotificationRepository.save(SmsNotification.builder()
                .type(type)
                .telephone(telephone)
                .message(message)
                .statut(statut)
                .build());
    }
}