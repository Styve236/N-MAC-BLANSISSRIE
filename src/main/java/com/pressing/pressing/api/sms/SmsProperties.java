package com.pressing.pressing.api.sms;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sms")
@Getter
@Setter
public class SmsProperties {

    private boolean enabled = true;

    private String mode = "console";

    private String apiKey = "";

    private String endpoint = "";

    private String sender = "";

    private String nomPressing = "Mon Pressing";

    private String recuBaseUrl = "http://localhost:8080";
}