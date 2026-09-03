package com.verilogic.ui.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.verilogic.ui.views.AuditLedgerView;
import com.verilogic.ui.views.CaseWorkbenchView;
import com.verilogic.ui.views.McdcMatrixView;
import com.verilogic.ui.views.SimulationBenchmarkView;

/**
 * Enterprise Application Shell Layout.
 * Clean, user-friendly labels designed for lending officers and compliance teams.
 * Fully responsive across all viewport sizes without collision or squashing.
 */
@StyleSheet("/styles.css")
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
    }

    private void createHeader() {
        // 1. Left Branding
        H1 logo = new H1("VERILOGIC");
        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.TextColor.HEADER
        );
        logo.getStyle().set("margin", "0");
        logo.getStyle().set("letter-spacing", "1.5px");
        logo.getStyle().set("flex-shrink", "0");

        Span versionTag = new Span("LENDING DECISION ENGINE");
        versionTag.addClassName("vl-brand-tag");
        versionTag.getElement().getThemeList().add("badge contrast small");
        versionTag.getStyle().set("font-family", "var(--lumo-font-family-monospace)");
        versionTag.getStyle().set("font-size", "0.68rem");
        versionTag.getStyle().set("letter-spacing", "0.05em");
        versionTag.getStyle().set("flex-shrink", "0");

        HorizontalLayout brand = new HorizontalLayout(logo, versionTag);
        brand.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        brand.setSpacing(true);
        brand.getStyle().set("flex-shrink", "0");

        // 2. Center Navigation Tabs with plain, clear words
        Tabs navTabs = createNavTabs();
        navTabs.getStyle().set("min-width", "0");
        navTabs.getStyle().set("flex-shrink", "1");

        // 3. Right Status Indicator
        Div liveDot = new Div();
        liveDot.addClassName("vl-live-dot");

        Span statusText = new Span("AUDIT TRAIL ACTIVE // SECURE");
        statusText.addClassName("vl-status-verbose");
        statusText.getStyle().set("font-family", "var(--lumo-font-family-monospace)");
        statusText.getStyle().set("font-size", "0.72rem");
        statusText.getStyle().set("font-weight", "500");
        statusText.addClassNames(LumoUtility.TextColor.SECONDARY);

        Span asilBadge = new Span("COMPLIANCE VERIFIED");
        asilBadge.getElement().getThemeList().add("badge success small");
        asilBadge.getStyle().set("font-family", "var(--lumo-font-family-monospace)");
        asilBadge.getStyle().set("font-size", "0.68rem");
        asilBadge.getStyle().set("flex-shrink", "0");

        HorizontalLayout systemStatus = new HorizontalLayout(liveDot, statusText, asilBadge);
        systemStatus.addClassName("vl-system-status");
        systemStatus.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        systemStatus.setSpacing(true);
        systemStatus.getStyle().set("flex-shrink", "0");

        // Header Shell
        HorizontalLayout header = new HorizontalLayout(brand, navTabs, systemStatus);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.expand(navTabs);
        header.addClassNames(
                LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Border.BOTTOM,
                LumoUtility.BorderColor.CONTRAST_10
        );
        header.getStyle().set("background", "var(--lumo-base-color)");
        header.getStyle().set("min-height", "58px");
        header.getStyle().set("overflow", "hidden");

        addToNavbar(header);
    }

    private Tabs createNavTabs() {
        Tabs tabs = new Tabs();
        tabs.getStyle().set("margin-left", "16px");
        tabs.getStyle().set("margin-right", "16px");

        RouterLink workbenchLink = new RouterLink(CaseWorkbenchView.class);
        workbenchLink.add(VaadinIcon.CLIPBOARD_CHECK.create(), new Span(" Loan Workbench"));
        Tab workbenchTab = new Tab(workbenchLink);

        RouterLink mcdcLink = new RouterLink(McdcMatrixView.class);
        mcdcLink.add(VaadinIcon.BOOK.create(), new Span(" Credit Policies"));
        Tab mcdcTab = new Tab(mcdcLink);

        RouterLink simulationLink = new RouterLink(SimulationBenchmarkView.class);
        simulationLink.add(VaadinIcon.DASHBOARD.create(), new Span(" Speed Benchmark"));
        Tab simulationTab = new Tab(simulationLink);

        RouterLink auditLink = new RouterLink(AuditLedgerView.class);
        auditLink.add(VaadinIcon.SHIELD.create(), new Span(" Audit Trail"));
        Tab auditTab = new Tab(auditLink);

        tabs.add(workbenchTab, mcdcTab, simulationTab, auditTab);
        return tabs;
    }
}
