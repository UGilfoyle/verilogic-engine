package com.verilogic.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.verilogic.application.port.out.AuditStoragePort;
import com.verilogic.domain.model.VerificationCertificate;
import com.verilogic.ui.layout.MainLayout;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Decision Audit Trail View.
 * Displays an immutable, tamper-proof log of every loan underwriting decision.
 */
@Component
@Scope("prototype")
@Route(value = "audit", layout = MainLayout.class)
@RouteAlias(value = "ledger", layout = MainLayout.class)
@PageTitle("Audit Trail | VeriLogic")
public class AuditLedgerView extends VerticalLayout {

    private final AuditStoragePort auditStoragePort;
    private final Grid<VerificationCertificate> certificateGrid;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Autowired
    public AuditLedgerView(AuditStoragePort auditStoragePort) {
        this.auditStoragePort = auditStoragePort;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        add(buildHeader());

        VerticalLayout card = new VerticalLayout();
        card.setSizeFull();
        card.addClassName("vl-card");
        card.setPadding(true);

        certificateGrid = new Grid<>();
        certificateGrid.setSizeFull();
        certificateGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);

        configureGrid();
        card.add(certificateGrid);

        add(card);
        refreshData();
    }

    private HorizontalLayout buildHeader() {
        H3 title = new H3("Decision Audit Trail");
        title.getStyle().set("margin", "0");
        title.getStyle().set("letter-spacing", "-0.02em");

        Paragraph subtitle = new Paragraph(
                "Permanent, tamper-evident record of all automated credit decisions and regulatory compliance proofs."
        );
        subtitle.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        VerticalLayout titles = new VerticalLayout(title, subtitle);
        titles.setPadding(false);
        titles.setSpacing(false);

        Button verifyChainBtn = new Button("Verify Audit Trail Integrity", VaadinIcon.SHIELD.create(), e -> verifyLedgerChain());
        verifyChainBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button refreshBtn = new Button("Refresh", VaadinIcon.REFRESH.create(), e -> refreshData());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(verifyChainBtn, refreshBtn);
        actions.setSpacing(true);
        actions.getStyle().set("flex-shrink", "0");

        HorizontalLayout bar = new HorizontalLayout(titles, actions);
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        bar.getStyle().set("flex-wrap", "wrap");
        bar.getStyle().set("gap", "12px");
        bar.expand(titles);
        return bar;
    }

    private void configureGrid() {
        certificateGrid.addColumn(cert -> FORMATTER.format(cert.issuedAt()))
                .setHeader("Date & Time")
                .setWidth("160px")
                .setFlexGrow(0);

        certificateGrid.addColumn(VerificationCertificate::caseId)
                .setHeader("Application ID")
                .setWidth("150px")
                .setFlexGrow(0);

        certificateGrid.addColumn(new ComponentRenderer<>(cert -> {
            String label = switch (cert.status()) {
                case CERTIFIED -> "APPROVED";
                case RECONCILED -> "CONDITIONAL";
                case REJECTED, VIOLATED -> "REJECTED";
            };
            Span badge = new Span(label);
            badge.addClassName("vl-status-pill");
            switch (cert.status()) {
                case CERTIFIED -> badge.addClassName("certified");
                case RECONCILED -> badge.addClassName("reconciled");
                case REJECTED, VIOLATED -> badge.addClassName("rejected");
            }
            return badge;
        })).setHeader("Decision").setWidth("140px").setFlexGrow(0);

        certificateGrid.addColumn(new ComponentRenderer<>(cert -> {
            Span code = new Span(cert.merkleRootHash());
            code.getStyle().set("font-family", "var(--lumo-font-family-monospace)");
            code.getStyle().set("font-size", "0.78rem");
            return code;
        })).setHeader("Proof Fingerprint (SHA-256)").setAutoWidth(true);

        certificateGrid.addColumn(new ComponentRenderer<>(cert -> {
            String shortPrev = cert.previousCertificateHash().substring(0, Math.min(16, cert.previousCertificateHash().length())) + "...";
            Span prev = new Span(shortPrev);
            prev.getStyle().set("font-family", "var(--lumo-font-family-monospace)");
            prev.getStyle().set("font-size", "0.78rem");
            prev.addClassNames(LumoUtility.TextColor.SECONDARY);
            return prev;
        })).setHeader("Previous Record").setWidth("180px").setFlexGrow(0);

        certificateGrid.addColumn(new ComponentRenderer<>(cert -> {
            Button inspectBtn = new Button("Inspect", VaadinIcon.SEARCH.create(), e -> inspectCertificate(cert));
            inspectBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return inspectBtn;
        })).setHeader("Actions").setWidth("120px").setFlexGrow(0);
    }

    private void refreshData() {
        List<VerificationCertificate> certificates = auditStoragePort.findRecentCertificates(200);
        certificateGrid.setItems(certificates);
    }

    private void verifyLedgerChain() {
        boolean valid = auditStoragePort.verifyFullLedgerChain();
        if (valid) {
            Notification.show("Audit Trail Verified: All records are unbroken and 100% authentic.", 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            Notification.show("AUDIT WARNING: Discontinuity detected in records!", 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void inspectCertificate(VerificationCertificate cert) {
        Notification.show(
                String.format("Record [%s]: Proof Hash = %s", cert.caseId(), cert.merkleRootHash()),
                3500,
                Notification.Position.BOTTOM_END
        ).addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }
}
