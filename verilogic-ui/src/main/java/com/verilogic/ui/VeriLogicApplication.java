package com.verilogic.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application entrypoint for VeriLogic.
 * Pure Java enterprise stack: Spring Boot 3.3 + Vaadin 24 Lumo Design System.
 */
@SpringBootApplication(scanBasePackages = "com.verilogic")
@Theme(themeClass = Lumo.class)
@Push
public class VeriLogicApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(VeriLogicApplication.class, args);
    }
}
