package com.projects.adnan.EmailProject.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.yml:
 * app:
 *   mail:
 *     smtp: { host: smtp.example.com, port: 587, starttls: true }
 *     pop3: { host: pop.example.com, port: 995, ssl: true }
 */
@Component
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class MailProperties {

    private Smtp smtp = new Smtp();
    private Pop3 pop3 = new Pop3();

    @Getter
    @Setter
    public static class Smtp {
        private String host = "localhost";
        private int port = 587;
        private boolean starttls = true;
    }

    @Getter
    @Setter
    public static class Pop3 {
        private String host = "localhost";
        private int port = 995;
        private boolean ssl = true;
    }
}
