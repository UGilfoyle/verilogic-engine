package com.verilogic.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
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
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.verilogic.application.port.in.RunSimulationUseCase;
import com.verilogic.application.port.in.SimulationReport;
import com.verilogic.ui.layout.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;

/**
 * High-Volume Performance Benchmark Console.
 * Fully responsive, non-colliding layout with dynamic grid auto-fitting,
 * wrapping button controls, and structured metric tiles.
 */
@org.springframework.stereotype.Component
@Scope("prototype")
@Route(value = "simulation", layout = MainLayout.class)
@PageTitle("Speed Benchmark | VeriLogic")
public class SimulationBenchmarkView extends VerticalLayout {

    private final RunSimulationUseCase simulationUseCase;
    private final List<SimulationReport> history = new ArrayList<>();
    private final Grid<SimulationReport> benchmarkGrid = new Grid<>();

    // KPI Metric Elements
    private final Span throughputVal = new Span("&mdash;");
    private final Span p50Val = new Span("&mdash;");
    private final Span p99Val = new Span("&mdash;");
    private final Span durationVal = new Span("&mdash;");
    private final Span integrityVal = new Span("&mdash;");

    @Autowired
    public SimulationBenchmarkView(RunSimulationUseCase simulationUseCase) {
        this.simulationUseCase = simulationUseCase;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background", "#f8fafc");

        add(buildHeader());
        add(buildControlBar());
        add(buildKpiTiles());
        add(buildScorecardGrid());
    }

    private HorizontalLayout buildHeader() {
        H3 title = new H3("High-Volume Performance Benchmark");
        title.getStyle().set("margin", "0");
        title.getStyle().set("font-weight", "800");
        title.getStyle().set("letter-spacing", "-0.02em");
        title.getStyle().set("color", "#0f172a");

        Paragraph subtitle = new Paragraph(
                "Benchmark automated loan decision speed, latency percentiles, and audit proof verification across high-volume application batches."
        );
        subtitle.getStyle().set("margin", "4px 0 0 0");
        subtitle.getStyle().set("color", "#64748b");
        subtitle.getStyle().set("font-size", "0.85rem");

        VerticalLayout titles = new VerticalLayout(title, subtitle);
        titles.setPadding(false);
        titles.setSpacing(false);

        HorizontalLayout bar = new HorizontalLayout(titles);
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        return bar;
    }

    private HorizontalLayout buildControlBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.addClassName("vl-card");
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        bar.getStyle().set("flex-wrap", "wrap");
        bar.getStyle().set("gap", "10px");
        bar.getStyle().set("padding", "12px 18px");

        Span label = new Span("Select Batch Size:");
        label.getStyle().set("font-size", "0.82rem");
        label.getStyle().set("font-weight", "700");
        label.getStyle().set("color", "#475569");
        label.getStyle().set("flex-shrink", "0");
        label.getStyle().set("margin-right", "6px");

        Button b500 = new Button("500 Applications", VaadinIcon.TASKS.create(), e -> runBenchmark(500));
        b500.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);

        Button b1k = new Button("1,000 Applications", VaadinIcon.TASKS.create(), e -> runBenchmark(1000));
        b1k.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        Button b5k = new Button("5,000 Applications", VaadinIcon.DASHBOARD.create(), e -> runBenchmark(5000));
        b5k.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);

        Button b10k = new Button("10,000 Applications", VaadinIcon.ROCKET.create(), e -> runBenchmark(10000));
        b10k.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);

        bar.add(label, b500, b1k, b5k, b10k);
        return bar;
    }

    private Component buildKpiTiles() {
        Div kpiGrid = new Div();
        kpiGrid.setWidthFull();
        kpiGrid.getStyle().set("display", "grid");
        kpiGrid.getStyle().set("grid-template-columns", "repeat(auto-fit, minmax(180px, 1fr))");
        kpiGrid.getStyle().set("gap", "14px");

        kpiGrid.add(createTile("DECISIONS / SEC", throughputVal, "accent", "Processing Throughput"));
        kpiGrid.add(createTile("MEDIAN TIME (P50)", p50Val, "primary", "Typical response time"));
        kpiGrid.add(createTile("99% SLA TIME (P99)", p99Val, "warning", "Worst-case latency"));
        kpiGrid.add(createTile("TOTAL RUN TIME", durationVal, "contrast", "Time to process batch"));
        kpiGrid.add(createTile("RECORD INTEGRITY", integrityVal, "success", "100% Audit Verified"));

        return kpiGrid;
    }

    private Div createTile(String labelText, Span valueSpan, String variant, String subtext) {
        Div card = new Div();
        card.addClassName("vl-metric-card");
        card.addClassName(variant);

        Span label = new Span(labelText);
        label.addClassName("vl-metric-label");

        valueSpan.addClassName("vl-metric-value");

        Span sub = new Span(subtext);
        sub.addClassName("vl-metric-subtext");

        card.add(label, valueSpan, sub);
        return card;
    }

    private VerticalLayout buildScorecardGrid() {
        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.addClassName("vl-card");
        container.setPadding(true);
        container.setSpacing(true);

        Span tableTitle = new Span("BENCHMARK EXECUTION RESULTS");
        tableTitle.getStyle().set("font-weight", "700");
        tableTitle.getStyle().set("font-size", "0.85rem");
        tableTitle.getStyle().set("color", "#0f172a");
        tableTitle.getStyle().set("letter-spacing", "0.02em");

        benchmarkGrid.setSizeFull();
        benchmarkGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);

        benchmarkGrid.addColumn(SimulationReport::totalCases)
                .setHeader("Applications")
                .setWidth("120px")
                .setFlexGrow(0);

        benchmarkGrid.addColumn(r -> String.format("%,d decisions/sec", (long) r.throughputPerSecond()))
                .setHeader("Throughput")
                .setWidth("170px")
                .setFlexGrow(0);

        benchmarkGrid.addColumn(r -> String.format("%.2f ms", r.p50LatencyMs()))
                .setHeader("Median Time")
                .setWidth("120px")
                .setFlexGrow(0);

        benchmarkGrid.addColumn(r -> String.format("%.2f ms", r.p99LatencyMs()))
                .setHeader("99th % Time")
                .setWidth("120px")
                .setFlexGrow(0);

        benchmarkGrid.addColumn(r -> String.format("%d ms", r.totalDurationMs()))
                .setHeader("Total Time")
                .setWidth("110px")
                .setFlexGrow(0);

        benchmarkGrid.addColumn(r -> String.format("Approved: %d | Conditional: %d | Rejected: %d", r.certifiedCount(), r.reconciledCount(), r.rejectedCount()))
                .setHeader("Decisions Summary")
                .setAutoWidth(true);

        benchmarkGrid.addColumn(new ComponentRenderer<>(r -> {
            Span pill = new Span(r.allMerkleRootsValid() ? "VERIFIED" : "FAILED");
            pill.addClassName("vl-status-pill");
            pill.addClassName(r.allMerkleRootsValid() ? "certified" : "rejected");
            return pill;
        })).setHeader("Audit Status").setWidth("130px").setFlexGrow(0);

        container.add(tableTitle, benchmarkGrid);
        return container;
    }

    private void runBenchmark(int batchSize) {
        Notification.show(String.format("Processing batch of %,d loan applications...", batchSize), 2000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_PRIMARY);

        SimulationReport report = simulationUseCase.runSimulation(batchSize, true);
        history.add(0, report);
        benchmarkGrid.setItems(history);

        // Update KPI tiles
        throughputVal.setText(String.format("%,d", (long) report.throughputPerSecond()));
        p50Val.setText(String.format("%.2f ms", report.p50LatencyMs()));
        p99Val.setText(String.format("%.2f ms", report.p99LatencyMs()));
        durationVal.setText(String.format("%d ms", report.totalDurationMs()));
        integrityVal.setText(report.allMerkleRootsValid() ? "100% OK" : "FAILED");

        Notification.show(String.format("Benchmark Complete! Throughput: %,d decisions/sec", (long) report.throughputPerSecond()), 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
