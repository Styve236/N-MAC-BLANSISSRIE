package com.pressing.pressing.api.sms;

public interface SmsService {

    void envoyer(TypeNotificationSms type, String telephone, String message);
}