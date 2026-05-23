package com.wikiforge.orchestration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.wikiforge")
public class WikiForgeOrchestrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(WikiForgeOrchestrationApplication.class, args);
    }
}
