package com.pressing.pressing.api.sms;
import com.pressing.pressing.api.entite.TypeNotificationSms;


public interface SmsService {

    void envoyer(TypeNotificationSms type, String telephone, String message);
}