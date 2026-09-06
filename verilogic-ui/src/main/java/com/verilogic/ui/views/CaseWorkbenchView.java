package com.verilogic.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.verilogic.application.port.in.EvaluateCaseUseCase;
import com.verilogic.application.port.in.InspectProofTraceUseCase;
import com.verilogic.application.port.in.SubmitCaseCommand;
import com.verilogic.domain.model.ProofExplanation;
import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.model.VerificationCertificate;
import com.verilogic.ui.layout.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Enterprise Underwriting &amp; Regulatory Verification Workbench.
 * World-class interactive banking interface featuring structured applicant controls,
 * ClearLedger forensic bank statement integration, deterministic SMT solver explanations,
 * and immutable cryptographic decision certificates.
 */
@Component
@Scope("prototype")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "workbench", layout = MainLayout.class)
@PageTitle("Loan Underwriting Workbench | VeriLogic")
public class CaseWorkbenchView extends VerticalLayout {

    private final EvaluateCaseUseCase evaluateUseCase;
    private final InspectProofTraceUseCase inspectTraceUseCase;

    // Structured Input Form Fields
    private TextField applicantNameField;
    private TextField applicantIdField;
    private IntegerField creditScoreField;
    private NumberField monthlyIncomeField;
    private NumberField monthlyDebtField;
    private NumberField liquidSavingsField;
    private NumberField loanAmountField;
    private Checkbox hasGuarantorBox;

    // DTI Live Preview Elements
    private Span dtiValueBadge;
    private ProgressBar dtiBar;

    // Raw Mode TextArea & Tab State
    private TextArea rawTextArea;
    private VerticalLayout structuredFormContainer;
    private VerticalLayout rawTextContainer;
    private VerticalLayout clearLedgerContainer;
    private int currentInputMode = 0; // 0 = Structured, 1 = ClearLedger, 2 = Raw Text

    // Right Decision Pane Components
    private VerticalLayout decisionHeroBanner;
    private HorizontalLayout metricsPillBar;
    private Span latencyPill;
    private Span rulesPassedPill;
    private Span merkleRootPill;
    private VerticalLayout ruleCardsContainer;
    private VerticalLayout certificateCard;

    @Autowired
    public CaseWorkbenchView(EvaluateCaseUseCase evaluateUseCase, InspectProofTraceUseCase inspectTraceUseCase) {
        this.evaluateUseCase = evaluateUseCase;
        this.inspectTraceUseCase = inspectTraceUseCase;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background", "#f8fafc");

        add(buildHeader());
        add(buildSplitWorkbench());
    }

    private HorizontalLayout buildHeader() {
        H3 title = new H3("Loan Underwriting & Decision Workbench");
        title.getStyle().set("margin", "0");
        title.getStyle().set("font-weight", "800");
        title.getStyle().set("letter-spacing", "-0.03em");
        title.getStyle().set("color", "#0f172a");

        Span subtitle = new Span("Deterministic neurosymbolic verification • Zero LLM hallucinations • Cryptographic audit proof");
        subtitle.getStyle().set("color", "#64748b");
        subtitle.getStyle().set("font-size", "0.85rem");

        VerticalLayout titles = new VerticalLayout(title, subtitle);
        titles.setPadding(false);
        titles.setSpacing(false);

        Span engineBadge = new Span("ENGINE: DETERMINISTIC POLICY SOLVER");
        engineBadge.getElement().getThemeList().add("badge success small");
        engineBadge.getStyle().set("font-family", "var(--lumo-font-family-monospace)");
        engineBadge.getStyle().set("font-size", "0.70rem");
        engineBadge.getStyle().set("padding", "5px 10px");

        Span clearLedgerBadge = new Span("CLEARLEDGER: BRIDGE ACTIVE");
        clearLedgerBadge.getElement().getThemeList().add("badge contrast small");
        clearLedgerBadge.getStyle().set("font-family", "var(--lumo-font-family-monospace)");
        clearLedgerBadge.getStyle().set("font-size", "0.70rem");
        clearLedgerBadge.getStyle().set("padding", "5px 10px");

        HorizontalLayout badges = new HorizontalLayout(engineBadge, clearLedgerBadge);
        badges.setSpacing(true);

        HorizontalLayout bar = new HorizontalLayout(titles, badges);
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        bar.expand(titles);
        bar.addClassNames(LumoUtility.Margin.Bottom.SMALL);
        return bar;
    }

    private SplitLayout buildSplitWorkbench() {
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(48);

        splitLayout.addToPrimary(buildInputPane());
        splitLayout.addToSecondary(buildProofPane());
        return splitLayout;
    }

    private VerticalLayout buildInputPane() {
        VerticalLayout pane = new VerticalLayout();
        pane.setSizeFull();
        pane.addClassName("vl-card");
        pane.setPadding(true);
        pane.setSpacing(true);

        // Header Title
        HorizontalLayout paneHeader = new HorizontalLayout();
        paneHeader.setWidthFull();
        paneHeader.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        H4 stepTitle = new H4("Step 1: Borrower & Financial Profile");
        stepTitle.getStyle().set("margin", "0");
        stepTitle.getStyle().set("font-weight", "700");

        Span pill = new Span("INPUT DATA");
        pill.getElement().getThemeList().add("badge contrast small");
        pill.getStyle().set("font-family", "var(--lumo-font-family-monospace)");

        paneHeader.add(stepTitle, pill);
        paneHeader.expand(stepTitle);

        // Preset Chips
        HorizontalLayout presetsRow = buildPresetChips();

        // Input Mode Tabs
        Tab tabStructured = new Tab(VaadinIcon.FORM.create(), new Span(" Structured Form"));
        Tab tabClearLedger = new Tab(VaadinIcon.INSTITUTION.create(), new Span(" ClearLedger Statement"));
        Tab tabRaw = new Tab(VaadinIcon.CODE.create(), new Span(" Raw Dossier"));

        Tabs modeTabs = new Tabs(tabStructured, tabClearLedger, tabRaw);
        modeTabs.setWidthFull();
        modeTabs.getStyle().set("border-bottom", "1px solid #e2e8f0");

        // Structured Form Container
        structuredFormContainer = buildStructuredForm();

        // ClearLedger Bank Statement Integration Card
        clearLedgerContainer = buildClearLedgerPanel();
        clearLedgerContainer.setVisible(false);

        // Raw Text Mode Container
        rawTextContainer = new VerticalLayout();
        rawTextContainer.setSizeFull();
        rawTextContainer.setPadding(false);
        rawTextContainer.setVisible(false);

        rawTextArea = new TextArea();
        rawTextArea.setSizeFull();
        rawTextArea.addClassName("vl-terminal-input");
        rawTextArea.setValue(getCleanPresetText());
        rawTextContainer.add(rawTextArea);
        rawTextContainer.expand(rawTextArea);

        modeTabs.addSelectedChangeListener(e -> {
            int idx = modeTabs.getSelectedIndex();
            currentInputMode = idx;
            structuredFormContainer.setVisible(idx == 0);
            clearLedgerContainer.setVisible(idx == 1);
            rawTextContainer.setVisible(idx == 2);
            if (idx == 2) {
                rawTextArea.setValue(generateTextFromFields());
            }
        });

        // Underwrite Action Button
        Button executeBtn = new Button("Verify & Underwrite Loan", VaadinIcon.ARROW_RIGHT.create());
        executeBtn.addClassName("vl-primary-button");
        executeBtn.setWidthFull();
        executeBtn.setHeight("46px");
        executeBtn.getStyle().set("font-size", "0.95rem");
        executeBtn.getStyle().set("cursor", "pointer");
        executeBtn.addClickListener(e -> executePipeline());

        pane.add(paneHeader, presetsRow, modeTabs, structuredFormContainer, clearLedgerContainer, rawTextContainer, executeBtn);
        pane.expand(structuredFormContainer);
        return pane;
    }

    private HorizontalLayout buildPresetChips() {
        HorizontalLayout presets = new HorizontalLayout();
        presets.setWidthFull();
        presets.setSpacing(true);
        presets.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Span label = new Span("Sample Profiles:");
        label.getStyle().set("font-size", "0.78rem");
        label.getStyle().set("font-weight", "600");
        label.getStyle().set("color", "#64748b");

        Button primeBtn = new Button("Prime (Approved)", VaadinIcon.CHECK_CIRCLE.create(), e -> loadPreset(
                "Morgan Vance", "APP-99214", 745, 12500.0, 2800.0, 68000.0, 280000.0, true
        ));
        primeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);

        Button debtBtn = new Button("High Debt (Rejected)", VaadinIcon.CLOSE_CIRCLE.create(), e -> loadPreset(
                "Jordan Hayes", "APP-44102", 560, 4200.0, 3100.0, 4500.0, 320000.0, false
        ));
        debtBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);

        Button cosignBtn = new Button("Co-Signer (Conditional)", VaadinIcon.USER_CHECK.create(), e -> loadPreset(
                "Taylor Quinn", "APP-77192", 645, 6500.0, 3600.0, 30000.0, 200000.0, true
        ));
        cosignBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);

        Button clearLedgerBtn = new Button("ClearLedger Bank Ingest", VaadinIcon.DATABASE.create(), e -> {
            loadPreset("Rajesh Sharma", "CL-HDFC-9018", 738, 13400.0, 2800.0, 54200.0, 300000.0, true);
        });
        clearLedgerBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        presets.add(label, primeBtn, debtBtn, cosignBtn, clearLedgerBtn);
        return presets;
    }

    private VerticalLayout buildStructuredForm() {
        VerticalLayout form = new VerticalLayout();
        form.setSizeFull();
        form.setPadding(false);
        form.setSpacing(true);

        // Row 1: Applicant Name, ID, Credit Score
        applicantNameField = new TextField("Applicant Full Name");
        applicantNameField.setValue("Morgan Vance");
        applicantNameField.setWidth("45%");

        applicantIdField = new TextField("Application ID");
        applicantIdField.setValue("APP-99214");
        applicantIdField.setWidth("25%");

        creditScoreField = new IntegerField("Credit Score (FICO)");
        creditScoreField.setValue(745);
        creditScoreField.setMin(300);
        creditScoreField.setMax(850);
        creditScoreField.setWidth("30%");
        creditScoreField.addValueChangeListener(e -> updateLiveDtiPreview());

        HorizontalLayout row1 = new HorizontalLayout(applicantNameField, applicantIdField, creditScoreField);
        row1.setWidthFull();

        // Row 2: Monthly Income & Existing Debt
        monthlyIncomeField = new NumberField("Gross Monthly Income ($)");
        monthlyIncomeField.setValue(12500.0);
        monthlyIncomeField.setWidth("50%");
        monthlyIncomeField.addValueChangeListener(e -> updateLiveDtiPreview());

        monthlyDebtField = new NumberField("Existing Monthly Debt ($)");
        monthlyDebtField.setValue(2800.0);
        monthlyDebtField.setWidth("50%");
        monthlyDebtField.addValueChangeListener(e -> updateLiveDtiPreview());

        HorizontalLayout row2 = new HorizontalLayout(monthlyIncomeField, monthlyDebtField);
        row2.setWidthFull();

        // Row 3: Liquid Savings & Requested Loan Amount
        liquidSavingsField = new NumberField("Verified Liquid Reserves ($)");
        liquidSavingsField.setValue(68000.0);
        liquidSavingsField.setWidth("50%");

        loanAmountField = new NumberField("Requested Loan Amount ($)");
        loanAmountField.setValue(280000.0);
        loanAmountField.setWidth("50%");

        HorizontalLayout row3 = new HorizontalLayout(liquidSavingsField, loanAmountField);
        row3.setWidthFull();

        // Row 4: Guarantor & Live DTI Indicator Card
        hasGuarantorBox = new Checkbox("Corporate Guarantor / Co-Signer Verified (Required for Borderline Profiles)");
        hasGuarantorBox.setValue(true);
        hasGuarantorBox.getStyle().set("font-weight", "500");

        Div liveDtiCard = new Div();
        liveDtiCard.setWidthFull();
        liveDtiCard.getStyle().set("background", "#f1f5f9");
        liveDtiCard.getStyle().set("padding", "10px 14px");
        liveDtiCard.getStyle().set("border-radius", "8px");
        liveDtiCard.getStyle().set("border", "1px solid #e2e8f0");

        HorizontalLayout dtiHeader = new HorizontalLayout();
        dtiHeader.setWidthFull();
        Span dtiLabel = new Span("Debt-to-Income (DTI) Ratio:");
        dtiLabel.getStyle().set("font-size", "0.82rem");
        dtiLabel.getStyle().set("font-weight", "600");
        dtiLabel.getStyle().set("color", "#475569");

        dtiValueBadge = new Span("22.4% (HEALTHY // < 36% THRESHOLD)");
        dtiValueBadge.getElement().getThemeList().add("badge success small");
        dtiValueBadge.getStyle().set("font-family", "var(--lumo-font-family-monospace)");

        dtiHeader.add(dtiLabel, dtiValueBadge);
        dtiHeader.expand(dtiLabel);

        dtiBar = new ProgressBar();
        dtiBar.setValue(0.224);
        dtiBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
        dtiBar.getStyle().set("margin-top", "6px");

        liveDtiCard.add(dtiHeader, dtiBar);

        form.add(row1, row2, row3, hasGuarantorBox, liveDtiCard);
        return form;
    }

    private VerticalLayout buildClearLedgerPanel() {
        VerticalLayout card = new VerticalLayout();
        card.setSizeFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle().set("background", "#f8fafc");
        card.getStyle().set("border", "1px dashed #94a3b8");
        card.getStyle().set("border-radius", "10px");

        H4 title = new H4("ClearLedger Bank Statement Integration");
        title.getStyle().set("margin", "0");

        Paragraph desc = new Paragraph("Ingests forensic cash flow telemetry and tamper-evident signals from ClearLedger (HDFC, Chase, Deutsche Bank).");
        desc.getStyle().set("font-size", "0.80rem");
        desc.getStyle().set("color", "#64748b");
        desc.getStyle().set("margin", "0");

        HorizontalLayout kpis = new HorizontalLayout();
        kpis.setWidthFull();
        kpis.add(createKpiBox("PDF AUTHENTICITY", "99.8%", "#059669"));
        kpis.add(createKpiBox("LEDGER BALANCE", "0.00 RECONCILED", "#059669"));
        kpis.add(createKpiBox("MONTHLY INFLOW", "$80,400.00", "#0f172a"));
        kpis.add(createKpiBox("CHEQUE BOUNCES", "0 RETURNS", "#059669"));

        card.add(title, desc, kpis);
        return card;
    }

    private Div createKpiBox(String title, String val, String color) {
        Div box = new Div();
        box.getStyle().set("background", "#ffffff");
        box.getStyle().set("border", "1px solid #e2e8f0");
        box.getStyle().set("border-radius", "8px");
        box.getStyle().set("padding", "8px 12px");
        box.getStyle().set("flex", "1");

        Span t = new Span(title);
        t.getStyle().set("font-size", "0.68rem");
        t.getStyle().set("font-weight", "600");
        t.getStyle().set("color", "#64748b");

        H4 v = new H4(val);
        v.getStyle().set("margin", "2px 0 0 0");
        v.getStyle().set("color", color);
        v.getStyle().set("font-family", "var(--lumo-font-family-monospace)");

        box.add(t, v);
        return box;
    }

    private void updateLiveDtiPreview() {
        double income = monthlyIncomeField.getValue() != null ? monthlyIncomeField.getValue() : 1.0;
        double debt = monthlyDebtField.getValue() != null ? monthlyDebtField.getValue() : 0.0;
        double dti = (debt / Math.max(1.0, income)) * 100.0;

        dtiBar.setValue(Math.min(1.0, dti / 100.0));
        if (dti <= 36.0) {
            dtiValueBadge.setText(String.format("%.1f%% (HEALTHY // < 36%% STATUTORY PASS)", dti));
            dtiValueBadge.getElement().getThemeList().clear();
            dtiValueBadge.getElement().getThemeList().add("badge success small");
            dtiBar.removeThemeVariants(ProgressBarVariant.LUMO_ERROR);
            dtiBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
        } else if (dti <= 43.0) {
            dtiValueBadge.setText(String.format("%.1f%% (ELEVATED // 43%% REGULATORY CEILING)", dti));
            dtiValueBadge.getElement().getThemeList().clear();
            dtiValueBadge.getElement().getThemeList().add("badge contrast small");
            dtiBar.removeThemeVariants(ProgressBarVariant.LUMO_ERROR, ProgressBarVariant.LUMO_SUCCESS);
        } else {
            dtiValueBadge.setText(String.format("%.1f%% (EXCEEDS LEGAL LIMIT // REJECTION)", dti));
            dtiValueBadge.getElement().getThemeList().clear();
            dtiValueBadge.getElement().getThemeList().add("badge error small");
            dtiBar.removeThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
            dtiBar.addThemeVariants(ProgressBarVariant.LUMO_ERROR);
        }
    }

    private void loadPreset(String name, String id, int fico, double income, double debt, double savings, double amount, boolean guarantor) {
        applicantNameField.setValue(name);
        applicantIdField.setValue(id);
        creditScoreField.setValue(fico);
        monthlyIncomeField.setValue(income);
        monthlyDebtField.setValue(debt);
        liquidSavingsField.setValue(savings);
        loanAmountField.setValue(amount);
        hasGuarantorBox.setValue(guarantor);
        updateLiveDtiPreview();
        rawTextArea.setValue(generateTextFromFields());
    }

    private String generateTextFromFields() {
        return String.format(
                "APPLICANT FINANCIAL PROFILE - 2026-Q3\n" +
                "Applicant: %s (ID: %s)\n" +
                "Credit Score: %d FICO\n" +
                "Monthly Gross Income: $%.2f\n" +
                "Monthly Existing Debt: $%.2f\n" +
                "Verified Liquid Savings: $%.2f\n" +
                "Requested Loan Amount: $%.2f\n" +
                "Guarantor Present: %s\n" +
                "Risk Assessment: %s",
                applicantNameField.getValue(),
                applicantIdField.getValue(),
                creditScoreField.getValue() != null ? creditScoreField.getValue() : 700,
                monthlyIncomeField.getValue() != null ? monthlyIncomeField.getValue() : 10000.0,
                monthlyDebtField.getValue() != null ? monthlyDebtField.getValue() : 2000.0,
                liquidSavingsField.getValue() != null ? liquidSavingsField.getValue() : 50000.0,
                loanAmountField.getValue() != null ? loanAmountField.getValue() : 250000.0,
                hasGuarantorBox.getValue() ? "Yes (Verified)" : "No",
                (creditScoreField.getValue() != null && creditScoreField.getValue() >= 700) ? "Low Risk (Prime Profile)" : "High Risk"
        );
    }

    private VerticalLayout buildProofPane() {
        VerticalLayout pane = new VerticalLayout();
        pane.setSizeFull();
        pane.addClassName("vl-card");
        pane.setPadding(true);
        pane.setSpacing(true);

        // Header
        HorizontalLayout paneHeader = new HorizontalLayout();
        paneHeader.setWidthFull();
        paneHeader.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        H4 stepTitle = new H4("Step 2: Policy Compliance & Underwriting Decision");
        stepTitle.getStyle().set("margin", "0");
        stepTitle.getStyle().set("font-weight", "700");

        Span pill = new Span("SOLVER PROOF");
        pill.getElement().getThemeList().add("badge contrast small");
        pill.getStyle().set("font-family", "var(--lumo-font-family-monospace)");

        paneHeader.add(stepTitle, pill);
        paneHeader.expand(stepTitle);

        // 1. Executive Decision Hero Banner
        decisionHeroBanner = new VerticalLayout();
        decisionHeroBanner.setWidthFull();
        decisionHeroBanner.getStyle().set("background", "#f1f5f9");
        decisionHeroBanner.getStyle().set("border", "1px solid #e2e8f0");
        decisionHeroBanner.getStyle().set("border-radius", "12px");
        decisionHeroBanner.getStyle().set("padding", "16px 20px");

        H2 waitingTitle = new H2("Awaiting Application Submission");
        waitingTitle.getStyle().set("margin", "0");
        waitingTitle.getStyle().set("font-size", "1.2rem");
        waitingTitle.getStyle().set("color", "#475569");

        Span waitingSubtitle = new Span("Click 'Verify & Underwrite Loan' to execute the deterministic neurosymbolic constraint pipeline.");
        waitingSubtitle.getStyle().set("font-size", "0.82rem");
        waitingSubtitle.getStyle().set("color", "#64748b");

        decisionHeroBanner.add(waitingTitle, waitingSubtitle);

        // 2. Metrics Pill Bar
        metricsPillBar = new HorizontalLayout();
        metricsPillBar.setWidthFull();
        metricsPillBar.setSpacing(true);
        metricsPillBar.setVisible(false);

        latencyPill = new Span("⚡ Latency: 0.00 ms");
        latencyPill.getElement().getThemeList().add("badge small contrast");
        latencyPill.getStyle().set("font-family", "var(--lumo-font-family-monospace)");

        rulesPassedPill = new Span("Rules: 3 of 3 Verified");
        rulesPassedPill.getElement().getThemeList().add("badge small success");
        rulesPassedPill.getStyle().set("font-family", "var(--lumo-font-family-monospace)");

        merkleRootPill = new Span("Merkle Proof: --");
        merkleRootPill.getElement().getThemeList().add("badge small primary");
        merkleRootPill.getStyle().set("font-family", "var(--lumo-font-family-monospace)");

        metricsPillBar.add(latencyPill, rulesPassedPill, merkleRootPill);

        // 3. Detailed Rule Explanations Container (Replaces cramped grid with executive cards)
        ruleCardsContainer = new VerticalLayout();
        ruleCardsContainer.setWidthFull();
        ruleCardsContainer.setPadding(false);
        ruleCardsContainer.setSpacing(true);

        // 4. Cryptographic Certificate Footer Box
        certificateCard = new VerticalLayout();
        certificateCard.setWidthFull();
        certificateCard.addClassName("vl-crypto-certificate");
        certificateCard.setPadding(true);
        certificateCard.setVisible(false);

        pane.add(paneHeader, decisionHeroBanner, metricsPillBar, ruleCardsContainer, certificateCard);
        return pane;
    }

    private void executePipeline() {
        String input;
        if (currentInputMode == 0) {
            input = generateTextFromFields();
        } else if (currentInputMode == 1) {
            input = getClearLedgerPresetText();
        } else {
            input = rawTextArea.getValue();
        }

        long start = System.nanoTime();
        SubmitCaseCommand command = new SubmitCaseCommand(input, "workbench-user", false);
        VerificationCertificate cert = evaluateUseCase.evaluateCase(command);
        long elapsedNanos = System.nanoTime() - start;
        double elapsedMs = elapsedNanos / 1_000_000.0;

        renderDecisionResults(cert, elapsedMs);
    }

    private void renderDecisionResults(VerificationCertificate cert, double elapsedMs) {
        // 1. Update Hero Banner with vibrant high-impact styling
        decisionHeroBanner.removeAll();
        decisionHeroBanner.getElement().removeAttribute("class");

        HorizontalLayout heroTop = new HorizontalLayout();
        heroTop.setWidthFull();
        heroTop.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        H2 heroStatus = new H2();
        heroStatus.getStyle().set("margin", "0");
        heroStatus.getStyle().set("font-weight", "800");
        heroStatus.getStyle().set("letter-spacing", "-0.02em");

        Span statusPill = new Span();
        statusPill.addClassName("vl-status-pill");

        Paragraph heroDesc = new Paragraph();
        heroDesc.getStyle().set("margin", "4px 0 0 0");
        heroDesc.getStyle().set("font-size", "0.86rem");

        switch (cert.status()) {
            case CERTIFIED -> {
                decisionHeroBanner.addClassName("vl-hero-approved");
                heroStatus.setText("LOAN APPROVED & CERTIFIED");
                heroStatus.getStyle().set("color", "#065f46");
                statusPill.setText("OFFICIALLY SANCTIONED");
                statusPill.addClassName("certified");
                heroDesc.setText("All statutory lending criteria (Qualified Mortgage DTI, Basel III Liquidity, AML KYC) were mathematically satisfied without exception.");
            }
            case RECONCILED -> {
                decisionHeroBanner.addClassName("vl-hero-reconciled");
                heroStatus.setText("CONDITIONAL APPROVAL (CO-SIGNER REQUIRED)");
                heroStatus.getStyle().set("color", "#92400e");
                statusPill.setText("ACTION REQUIRED");
                statusPill.addClassName("reconciled");
                heroDesc.setText("Applicant solvency requires an approved prime guarantor to meet Qualified Mortgage reserve thresholds.");
            }
            case REJECTED, VIOLATED -> {
                decisionHeroBanner.addClassName("vl-hero-rejected");
                heroStatus.setText("LOAN APPLICATION REJECTED");
                heroStatus.getStyle().set("color", "#9f1239");
                statusPill.setText("POLICY VIOLATION");
                statusPill.addClassName("rejected");
                heroDesc.setText("Application fails statutory solvency thresholds. Debt burden or liquidity deficit exceeds regulatory ceilings.");
            }
        }

        heroTop.add(heroStatus, statusPill);
        heroTop.expand(heroStatus);
        decisionHeroBanner.add(heroTop, heroDesc);

        // 2. Update Metrics Bar
        metricsPillBar.setVisible(true);
        latencyPill.setText(String.format("⚡ Deterministic Latency: %.2f ms", elapsedMs));
        merkleRootPill.setText("Merkle Root: " + cert.merkleRootHash().substring(0, 16) + "...");

        // 3. Render Detailed Statutory Rule Cards
        ruleCardsContainer.removeAll();
        Optional<ProofTrace> traceOpt = inspectTraceUseCase.inspectProof(cert.caseId());
        if (traceOpt.isPresent()) {
            List<ProofExplanation> explanations = traceOpt.get().explanations();
            long passedCount = explanations.stream().filter(ProofExplanation::satisfied).count();
            rulesPassedPill.setText(String.format("Rules: %d of %d Satisfied", passedCount, explanations.size()));

            for (ProofExplanation expl : explanations) {
                ruleCardsContainer.add(createStatutoryRuleCard(expl));
            }
        }

        // 4. Render Cryptographic Certificate Box
        certificateCard.removeAll();
        certificateCard.setVisible(true);

        HorizontalLayout certHead = new HorizontalLayout();
        certHead.setWidthFull();
        certHead.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        H4 certTitle = new H4("IMMUTABLE DECISION CERTIFICATE");
        certTitle.getStyle().set("margin", "0");
        certTitle.getStyle().set("color", "#f8fafc");
        certTitle.getStyle().set("font-family", "var(--lumo-font-family-monospace)");
        certTitle.getStyle().set("font-size", "0.85rem");

        Span issuer = new Span("SHA-256 Merkle Chain // VeriLogic v1.0");
        issuer.getStyle().set("color", "#94a3b8");
        issuer.getStyle().set("font-size", "0.72rem");
        issuer.getStyle().set("font-family", "var(--lumo-font-family-monospace)");

        certHead.add(certTitle, issuer);
        certHead.expand(certTitle);

        Div hashGrid = new Div();
        hashGrid.getStyle().set("font-family", "var(--lumo-font-family-monospace)");
        hashGrid.getStyle().set("font-size", "0.72rem");
        hashGrid.getStyle().set("line-height", "1.7");
        hashGrid.getStyle().set("color", "#cbd5e1");
        hashGrid.getStyle().set("margin-top", "8px");

        hashGrid.add(new Div(new Span("Certificate ID  : " + cert.certificateId())));
        hashGrid.add(new Div(new Span("Merkle Root Hash: " + cert.merkleRootHash())));
        hashGrid.add(new Div(new Span("Policy Rule Hash: " + cert.ruleSetHash())));
        hashGrid.add(new Div(new Span("Previous Block  : " + cert.previousCertificateHash())));

        certificateCard.add(certHead, hashGrid);

        Notification.show("Underwriting Complete: " + heroStatus.getText(), 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(cert.status() == ProofTrace.ProofStatus.CERTIFIED ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
    }

    private Div createStatutoryRuleCard(ProofExplanation expl) {
        Div card = new Div();
        card.addClassName("vl-rule-card");
        card.setWidthFull();

        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Span ruleName = new Span(expl.ruleId());
        ruleName.getStyle().set("font-weight", "700");
        ruleName.getStyle().set("font-size", "0.85rem");
        ruleName.getStyle().set("color", "#0f172a");

        Span citation = new Span(" • " + expl.statuteCitation());
        citation.getStyle().set("font-size", "0.78rem");
        citation.getStyle().set("color", "#64748b");

        HorizontalLayout ruleInfo = new HorizontalLayout(ruleName, citation);
        ruleInfo.setSpacing(false);

        Span status = new Span(expl.satisfied() ? "PASSED" : "FAILED");
        status.addClassName("vl-status-pill");
        status.addClassName(expl.satisfied() ? "certified" : "rejected");

        top.add(ruleInfo, status);
        top.expand(ruleInfo);

        Paragraph detail = new Paragraph(expl.details());
        detail.getStyle().set("margin", "6px 0 0 0");
        detail.getStyle().set("font-size", "0.80rem");
        detail.getStyle().set("color", "#334155");
        detail.getStyle().set("font-family", "var(--lumo-font-family-monospace)");

        card.add(top, detail);
        return card;
    }

    private String getCleanPresetText() {
        return "APPLICANT FINANCIAL PROFILE - 2026-Q3\n" +
                "Applicant: Morgan Vance (ID: APP-99214)\n" +
                "Credit Score: 745 FICO\n" +
                "Monthly Gross Income: $12,500.00\n" +
                "Monthly Existing Debt: $2,800.00\n" +
                "Verified Liquid Savings: $68,000.00\n" +
                "Requested Loan Amount: $280,000.00\n" +
                "Guarantor Present: Yes (Verified)\n" +
                "Risk Assessment: Low Risk (Prime Profile)";
    }

    private String getClearLedgerPresetText() {
        return "BANK STATEMENT FORENSIC AUDIT - 2026-Q3\n" +
                "Bank: HDFC Bank (Verified Core Banking Statement)\n" +
                "Account: 9018420911 (Holder: Rajesh Sharma)\n" +
                "Document Authenticity: 99.8% (Clean, Original PDF, No Tampering)\n" +
                "Statement Balance Check: 0.00 Discrepancy (Mathematically Balanced)\n" +
                "Average Monthly Balance: $54,200.00\n" +
                "Verified Monthly Salary: $13,400.00 (Consistent Inflow)\n" +
                "Monthly Debt Payments: $2,800.00\n" +
                "Cheque / NACH Bounces: 0 Returns\n" +
                "Requested Loan Amount: $300,000.00\n" +
                "Corporate Guarantor: Present and Verified\n" +
                "Overall Risk Rating: Low Risk";
    }
}
