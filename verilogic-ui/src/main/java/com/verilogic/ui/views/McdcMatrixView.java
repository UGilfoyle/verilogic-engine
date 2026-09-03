package com.verilogic.ui.views;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.verilogic.application.port.in.InspectMcdcMatrixUseCase;
import com.verilogic.domain.rule.McdcTruthTable;
import com.verilogic.ui.layout.MainLayout;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Credit Policies &amp; Statutory Rules View.
 * Displays the lending policies and regulatory rules evaluated for each loan application.
 */
@Component
@Scope("prototype")
@Route(value = "mcdc", layout = MainLayout.class)
@PageTitle("Credit Policies | VeriLogic")
public class McdcMatrixView extends VerticalLayout {

    private final InspectMcdcMatrixUseCase mcdcUseCase;

    @Autowired
    public McdcMatrixView(InspectMcdcMatrixUseCase mcdcUseCase) {
        this.mcdcUseCase = mcdcUseCase;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        add(buildHeader());
        renderRuleMatrices();
    }

    private VerticalLayout buildHeader() {
        H3 title = new H3("Credit Policies &amp; Statutory Rules");
        title.getStyle().set("margin", "0");
        title.getStyle().set("letter-spacing", "-0.02em");

        Paragraph description = new Paragraph(
                "Standard banking regulations and lending policies evaluated deterministically for every loan application."
        );
        description.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        VerticalLayout layout = new VerticalLayout(title, description);
        layout.setPadding(false);
        layout.setSpacing(false);
        return layout;
    }

    private void renderRuleMatrices() {
        List<McdcTruthTable> tables = mcdcUseCase.getActiveMcdcMatrices();

        for (McdcTruthTable table : tables) {
            VerticalLayout card = new VerticalLayout();
            card.setWidthFull();
            card.addClassName("vl-card");
            card.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

            H4 ruleTitle = new H4(table.ruleId());
            ruleTitle.getStyle().set("margin", "0 0 4px 0");

            Span formulaBadge = new Span("Rule Definition: " + table.booleanFormula());
            formulaBadge.getStyle().set("font-family", "var(--lumo-font-family-monospace)");
            formulaBadge.getStyle().set("font-size", "0.82rem");
            formulaBadge.addClassNames(LumoUtility.TextColor.PRIMARY);

            // Vector Grid
            Grid<McdcTruthTable.McdcVector> vectorGrid = new Grid<>();
            vectorGrid.setAllRowsVisible(true);
            vectorGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);

            vectorGrid.addColumn(McdcTruthTable.McdcVector::testCaseId)
                    .setHeader("Scenario ID")
                    .setWidth("120px")
                    .setFlexGrow(0);

            vectorGrid.addColumn(v -> v.conditionValues().toString())
                    .setHeader("Conditions Evaluated")
                    .setWidth("240px")
                    .setFlexGrow(0);

            vectorGrid.addColumn(new ComponentRenderer<>(v -> {
                Span badge = new Span(v.expectedOutcome() ? "PASSED" : "FAILED");
                badge.addClassName("vl-status-pill");
                badge.addClassName(v.expectedOutcome() ? "certified" : "rejected");
                return badge;
            })).setHeader("Result").setWidth("110px").setFlexGrow(0);

            vectorGrid.addColumn(McdcTruthTable.McdcVector::targetConditionTested)
                    .setHeader("Condition Tested")
                    .setAutoWidth(true);

            vectorGrid.setItems(table.vectors());

            card.add(ruleTitle, formulaBadge, vectorGrid);
            add(card);
        }
    }
}
