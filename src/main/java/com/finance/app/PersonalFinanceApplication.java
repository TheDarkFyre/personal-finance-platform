package com.finance.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
@Slf4j
public class PersonalFinanceApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(PersonalFinanceApplication.class)
                .headless(false)
                .run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowserOnStartup() {
        String url = "http://localhost:8080";
        log.info("🌟 Personal Finance Platform is live at: {}", url);

        // Skip auto-launch in CI pipelines or test environments
        if (System.getenv("CI") != null || Boolean.getBoolean("test.mode")) {
            return;
        }

        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            log.debug("Auto-browser launch skipped or unavailable: {}", e.getMessage());
        }
    }
}
